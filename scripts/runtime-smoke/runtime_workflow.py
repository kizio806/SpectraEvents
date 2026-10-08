#!/usr/bin/env python3
"""Real-server fresh-install, bundled-template, lifecycle, and recovery gate."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import pathlib
import queue
import re
import shutil
import subprocess
import sys
import threading
import time
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
JAVA = pathlib.Path("/usr/lib/jvm/java-25-openjdk/bin/java")
JAVA_BIN = str(JAVA if JAVA.is_file() else pathlib.Path("java"))
ANSI = re.compile(r"\x1b\[[0-9;]*m")
UUID = r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
MAX_SERVER_BYTES = 512 * 1024 * 1024
ASSET_SMOKE_TEXTURE = (
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg=="
)


def upstream_version(declared_version: str) -> str:
    # The public compatibility band calls this line 26.1, while all current server projects publish
    # its stable runtime as 26.1.2. Keep the release matrix label stable and make the tested patch
    # explicit in the result instead of requesting a non-existent Paper/Purpur/Folia build.
    return "26.1.2" if declared_version == "26.1" else declared_version


def download(url: str, target: pathlib.Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "SpectraEvents-Runtime-Gate/1.0"})
    temporary = target.with_suffix(target.suffix + ".part")
    total = 0
    with urllib.request.urlopen(request, timeout=30) as response, temporary.open("wb") as output:
        while chunk := response.read(1024 * 1024):
            total += len(chunk)
            if total > MAX_SERVER_BYTES:
                raise RuntimeError(f"Server download exceeds {MAX_SERVER_BYTES} bytes")
            output.write(chunk)
    temporary.replace(target)


def fill_download(project: str, version: str) -> str:
    url = f"https://fill.papermc.io/v3/projects/{project}/versions/{version}/builds"
    request = urllib.request.Request(url, headers={"User-Agent": "SpectraEvents-Runtime-Gate/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = json.load(response)
    builds = payload if isinstance(payload, list) else payload.get("builds", [])
    if not builds:
        raise RuntimeError(f"No {project} build exists for {version}")
    downloads = builds[-1].get("downloads", {})
    for candidate in downloads.values():
        if isinstance(candidate, dict) and candidate.get("url"):
            return candidate["url"]
    raise RuntimeError(f"No downloadable {project} server JAR exists for {version}")


def buildtools_server(server: str, version: str, cache: pathlib.Path) -> pathlib.Path:
    build_dir = cache / f"buildtools-{version}"
    expected = build_dir / f"{server}-{version}.jar"
    if expected.is_file() and expected.stat().st_size > 0:
        return expected
    build_dir.mkdir(parents=True, exist_ok=True)
    tool = build_dir / "BuildTools.jar"
    if not tool.is_file():
        download(
            "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/"
            "artifact/target/BuildTools.jar",
            tool,
        )
    build_target = "CRAFTBUKKIT" if server == "craftbukkit" else "SPIGOT"
    subprocess.run(
        [
            JAVA_BIN,
            "-jar",
            str(tool),
            "--rev",
            version,
            "--compile",
            build_target,
        ],
        cwd=build_dir,
        check=True,
        timeout=30 * 60,
    )
    candidates = sorted(build_dir.glob(f"{server}-{version}*.jar"))
    if not candidates:
        raise RuntimeError(f"BuildTools did not produce {server} {version}")
    return candidates[-1]


def acquire_server(server: str, version: str, cache: pathlib.Path) -> pathlib.Path:
    cache.mkdir(parents=True, exist_ok=True)
    target = cache / f"{server}-{version}.jar"
    if target.is_file() and target.stat().st_size > 0:
        return target
    if server in {"paper", "folia"}:
        download(fill_download(server, version), target)
    elif server == "purpur":
        download(f"https://api.purpurmc.org/v2/purpur/{version}/latest/download", target)
    elif server in {"spigot", "craftbukkit"}:
        return buildtools_server(server, version, cache)
    else:
        raise ValueError(f"Unsupported server: {server}")
    return target


def validate_artifact(artifact: pathlib.Path, family: str) -> None:
    expected_main = {
        "paper": "io.github.kizio806.spectraevents.platform.paper.SpectraEventsPlugin",
        "spigot": "io.github.kizio806.spectraevents.platform.spigot.SpectraEventsSpigotPlugin",
    }[family]
    forbidden = "/platform/spigot/" if family == "paper" else "/platform/paper/"
    with zipfile.ZipFile(artifact) as archive:
        names = archive.namelist()
        descriptor = archive.read("plugin.yml").decode("utf-8")
    if f"main: {expected_main}" not in descriptor or "api-version: '26.1'" not in descriptor:
        raise RuntimeError(f"Invalid plugin.yml in {artifact.name}")
    if family == "spigot" and ("commands:" not in descriptor or "spectraevents:" not in descriptor):
        raise RuntimeError(f"The /spectraevents command is missing from {artifact.name}")
    if family == "paper" and not any(name.endswith("/command/SpectraMainCommand.class") for name in names):
        raise RuntimeError(f"The Paper command implementation is missing from {artifact.name}")
    if any(forbidden in f"/{name}" for name in names):
        raise RuntimeError(f"{artifact.name} contains classes from the other platform family")
    if "org/sqlite/JDBC.class" not in names:
        raise RuntimeError(f"{artifact.name} does not contain the SQLite driver")


class ServerSession:
    def __init__(self, directory: pathlib.Path, server_jar: pathlib.Path):
        self.directory = directory
        self.process = subprocess.Popen(
            [JAVA_BIN, "-jar", str(server_jar), "--nogui", "--nojline"],
            cwd=directory,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        self.lines: list[str] = []
        self.cursor = 0
        self.updates: queue.Queue[str] = queue.Queue()
        self.reader = threading.Thread(target=self._read, daemon=True)
        self.reader.start()

    def _read(self) -> None:
        assert self.process.stdout is not None
        for raw in self.process.stdout:
            line = ANSI.sub("", raw.rstrip())
            self.lines.append(line)
            self.updates.put(line)
            print(line, flush=True)

    def wait_for(self, pattern: str, timeout: float = 60) -> re.Match[str]:
        regex = re.compile(pattern, re.IGNORECASE)
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            while self.cursor < len(self.lines):
                match = regex.search(self.lines[self.cursor])
                self.cursor += 1
                if match:
                    return match
            if self.process.poll() is not None:
                raise RuntimeError(
                    f"Server exited with {self.process.returncode} before matching {pattern!r}"
                )
            try:
                self.updates.get(timeout=min(0.25, deadline - time.monotonic()))
            except queue.Empty:
                pass
        raise TimeoutError(f"Timed out waiting for {pattern!r}")

    def command(self, command: str) -> None:
        if self.process.poll() is not None or self.process.stdin is None:
            raise RuntimeError(f"Cannot send command to stopped server: {command}")
        print(f">>> {command}", flush=True)
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()

    def stop(self) -> None:
        if self.process.poll() is None:
            self.command("stop")
        deadline = time.monotonic() + 45
        nudge_at = time.monotonic() + 5
        nudged = False
        while self.process.poll() is None and time.monotonic() < deadline:
            # Some CraftBukkit builds can begin one more blocking console read before the main
            # thread applies the stop command. Give that reader one nudge only if it has not exited
            # after the normal shutdown grace period; continuously writing stdin can keep Paper's
            # shutdown path alive indefinitely.
            if not nudged and time.monotonic() >= nudge_at:
                try:
                    assert self.process.stdin is not None
                    self.process.stdin.write("\n")
                    self.process.stdin.flush()
                except (BrokenPipeError, OSError):
                    break
                nudged = True
            time.sleep(0.05)
        if self.process.poll() is None:
            self.process.kill()
            raise RuntimeError("Server did not stop cleanly")
        self.process.wait()
        self.reader.join(timeout=2)
        if self.process.returncode != 0:
            raise RuntimeError(f"Server exited with {self.process.returncode}")

    def assert_no_plugin_errors(self) -> None:
        critical = (
            "NoClassDefFoundError",
            "NoSuchMethodError",
            "InvalidPluginException",
            "Could not load 'plugins/SpectraEvents",
            "Error occurred while enabling SpectraEvents",
            "Error occurred while disabling SpectraEvents",
            "Thread context violation",
        )
        failures = [
            line
            for line in self.lines
            if (
                not self._is_benign_console_shutdown_error(line)
                and (
                any(token in line for token in critical)
                    or "/ERROR]:" in line
                    or "/SEVERE]:" in line
                    or "Asynchronous platform action failed" in line
                    or (
                        line.lstrip().startswith("at ")
                        and (
                            "SpectraEvents-" in line
                            or "io.github.kizio806.spectraevents" in line
                        )
                    )
                )
            )
        ]
        if failures:
            raise RuntimeError("Critical plugin/server errors:\n" + "\n".join(failures))

    def _is_benign_console_shutdown_error(self, line: str) -> bool:
        # Some Spigot bundled-server builds close JLine after processing `stop`, then log this
        # console-reader exception despite a clean zero exit. It is not an application failure.
        return (
            self.process.returncode == 0
            and "[Server console handler/ERROR]: Exception handling console input" in line
        )


def wait_ready(session: ServerSession) -> None:
    session.wait_for(r"(?:\[SpectraEvents\] READY platform=|SpectraEvents enabled successfully\.)", timeout=90)
    session.wait_for(r"Done \(.*\)! For help", timeout=90)


def start_event(session: ServerSession, definition_id: str = "airdrop") -> str:
    session.command(f"spectraevents event start {definition_id}")
    return session.wait_for(rf"(?:Started event instance |Instance: )(?P<instance>{UUID})", timeout=20).group(
        "instance"
    )


def inspect(session: ServerSession, instance_id: str) -> tuple[str, bool, int, int]:
    session.command(f"spectraevents event inspect {instance_id}")
    match = session.wait_for(
        rf"Event {instance_id} state=(\w+) runtimeState=(true|false) tasks=(-?\d+) resources=(-?\d+)",
        timeout=20,
    )
    return match.group(1), match.group(2).lower() == "true", int(match.group(3)), int(match.group(4))


def assert_running(snapshot: tuple[str, bool, int, int], context: str) -> None:
    state, runtime, tasks, resources = snapshot
    if state != "RUNNING" or not runtime or tasks < 1 or resources < 1:
        raise RuntimeError(f"{context}: expected RUNNING/state/task/resources, got {snapshot}")


def assert_running_with_model(snapshot: tuple[str, bool, int, int], context: str) -> None:
    state, runtime, _tasks, resources = snapshot
    if state != "RUNNING" or not runtime or resources < 1:
        raise RuntimeError(f"{context}: expected RUNNING/state/model resources, got {snapshot}")


def assert_cancelled(snapshot: tuple[str, bool, int, int], context: str) -> None:
    state, runtime, tasks, resources = snapshot
    if state != "CANCELLED" or runtime or tasks != 0 or resources != 0:
        raise RuntimeError(
            f"{context}: expected CANCELLED/false/0/0, got {(state, runtime, tasks, resources)}"
        )


def start_and_cancel_reference_event(session: ServerSession, definition_id: str) -> None:
    instance_id = start_event(session, definition_id)
    time.sleep(1)
    assert_running_with_model(inspect(session, instance_id), f"{definition_id} reference start")
    session.command(f"spectraevents event cancel {instance_id}")
    session.wait_for(rf"(?:Stopped|Cancelled) event instance {instance_id}", timeout=20)
    assert_cancelled(inspect(session, instance_id), f"{definition_id} reference cleanup")


def write_asset_smoke_fixture(plugin_directory: pathlib.Path) -> None:
    """Creates the smallest signed bundle and event that exercise the server asset workflow."""
    texture = base64.b64decode(ASSET_SMOKE_TEXTURE)
    texture_path = "textures/texture_0.png"
    model = {
        "meta": {"format_version": "5.0", "model_format": "free"},
        "textures": [{"id": "texture", "name": "texture.png", "source": texture_path}],
        "elements": [
            {
                "uuid": "cube",
                "from": [0.0, 0.0, 0.0],
                "to": [16.0, 16.0, 16.0],
                "origin": [8.0, 8.0, 8.0],
                "rotation": [0.0, 0.0, 0.0],
                "faces": {"north": {"uv": [0.0, 0.0, 16.0, 16.0], "texture": "#texture"}},
            }
        ],
        "outliner": [
            {
                "uuid": "root",
                "name": "root",
                "origin": [0.0, 0.0, 0.0],
                "rotation": [0.0, 0.0, 0.0],
                "children": ["cube"],
            }
        ],
        "animations": [
            {
                "name": "pulse",
                "length": 1.0,
                "loop": "once",
                "animators": {
                    "root": {
                        "keyframes": [
                            {
                                "time": 0.0,
                                "channel": "position",
                                "interpolation": "linear",
                                "data_points": [{"x": 0.0, "y": 0.0, "z": 0.0}],
                            },
                            {
                                "time": 1.0,
                                "channel": "position",
                                "interpolation": "linear",
                                "data_points": [{"x": 0.0, "y": 2.0, "z": 0.0}],
                            },
                        ]
                    }
                },
            }
        ],
    }
    model_bytes = json.dumps(model, sort_keys=True, separators=(",", ":")).encode("utf-8")
    checksums = {
        "model.bbmodel": hashlib.sha256(model_bytes).hexdigest(),
        texture_path: hashlib.sha256(texture).hexdigest(),
    }
    manifest = {
        "schemaVersion": 1,
        "modelId": "asset_smoke",
        "model": "model.bbmodel",
        "textures": [texture_path],
        "sha256": checksums,
    }
    source_directory = plugin_directory / "assets" / "source"
    source_directory.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(source_directory / "asset_smoke.spectra.zip", "w") as bundle:
        bundle.writestr("manifest.json", json.dumps(manifest, sort_keys=True))
        bundle.writestr("model.bbmodel", model_bytes)
        bundle.writestr(texture_path, texture)

    event_directory = plugin_directory / "events"
    event_directory.mkdir(parents=True, exist_ok=True)
    (event_directory / "asset_smoke.yml").write_text(
        """id: asset_smoke
schema-version: "1"
initial-phase: active

phases:
  active:
    on-enter:
      - type: spawn_model
        model: asset_smoke
      - type: play_animation
        model: asset_smoke
        animation: pulse
        loop: ONCE
""",
        encoding="utf-8",
    )


def resource_pack_profile(version: str) -> str:
    if version.startswith("26.1"):
        return "26_1"
    if version.startswith("26.2"):
        return "26_2"
    if version.startswith("26.3"):
        return "26_3"
    raise RuntimeError(f"Unsupported Minecraft version for resource-pack smoke test: {version}")


def run_workflow(
    server_jar: pathlib.Path, artifact: pathlib.Path, work: pathlib.Path, minecraft_version: str
) -> None:
    if work.exists():
        shutil.rmtree(work)
    (work / "plugins").mkdir(parents=True)
    shutil.copy2(artifact, work / "plugins" / artifact.name)
    (work / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    plugin_directory = work / "plugins" / "SpectraEvents"

    first = ServerSession(work, server_jar)
    try:
        wait_ready(first)
        template_archive = plugin_directory / "templates" / "metin.spectra.zip"
        if not template_archive.is_file() or template_archive.stat().st_size == 0:
            raise RuntimeError("Fresh install did not expose the bundled Metin template")
        first.command("spectraevents template list")
        first.wait_for(r"Bundled templates: metin", timeout=20)
        first.command("spectraevents template install metin")
        first.wait_for(r"Template metin is installed and validated\.", timeout=45)
        if not (plugin_directory / "events" / "metin.yml").is_file():
            raise RuntimeError("Template command did not install events/metin.yml")
        if not (plugin_directory / "assets" / "source" / "metin_stone.bbmodel").is_file():
            raise RuntimeError("Template command did not install Metin's model source")
        first.command("spectraevents validate")
        first.wait_for(
            r"(?:Validation complete: all 1 files are valid\.|Definitions validated=1 failed=0)",
            timeout=30,
        )
        generated_pack = (
            plugin_directory
            / "cache"
            / "resource-pack"
            / f"spectraevents-profile_{resource_pack_profile(minecraft_version)}.zip"
        )
        if not generated_pack.is_file() or generated_pack.stat().st_size == 0:
            raise RuntimeError("Metin installation did not produce the expected resource-pack ZIP")
        recoverable = start_event(first, "metin")
        time.sleep(1)
        assert_running_with_model(inspect(first, recoverable), "Metin model start")
    finally:
        first.stop()
    first.assert_no_plugin_errors()

    second = ServerSession(work, server_jar)
    try:
        wait_ready(second)
        time.sleep(2)
        assert_running_with_model(inspect(second, recoverable), "Metin post-restart recovery")
        second.command(f"spectraevents event cancel {recoverable}")
        second.wait_for(rf"(?:Stopped|Cancelled) event instance {recoverable}", timeout=20)
        assert_cancelled(inspect(second, recoverable), "recovered cleanup")
    finally:
        second.stop()
    second.assert_no_plugin_errors()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--server", choices=["paper", "purpur", "folia", "spigot", "craftbukkit"], required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--artifact", type=pathlib.Path, required=True)
    parser.add_argument("--server-jar", type=pathlib.Path)
    parser.add_argument("--work-dir", type=pathlib.Path)
    args = parser.parse_args()

    artifact = args.artifact.resolve()
    family = "paper" if args.server in {"paper", "purpur", "folia"} else "spigot"
    validate_artifact(artifact, family)
    cache = ROOT / ".gradle" / "runtime-server-cache"
    resolved_version = upstream_version(args.version)
    server_jar = (
        args.server_jar.resolve()
        if args.server_jar
        else acquire_server(args.server, resolved_version, cache)
    )
    work = args.work_dir or ROOT / "build" / "runtime-workflow" / f"{args.server}-{args.version}"
    run_workflow(server_jar, artifact, work, args.version)
    print(
        "RUNTIME WORKFLOW PASS: "
        f"{args.server} {args.version} (upstream {resolved_version}) / {artifact.name}"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"RUNTIME WORKFLOW FAIL: {error}", file=sys.stderr)
        raise
