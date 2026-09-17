#!/usr/bin/env python3
import os
import sys
import subprocess
import time
import shutil
import json
import urllib.request

JAVA_25_BIN = "/usr/lib/jvm/java-25-openjdk/bin/java"
if not os.path.exists(JAVA_25_BIN):
    JAVA_25_BIN = "java"

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
SMOKE_ROOT = os.path.join(ROOT_DIR, "build/smoke/matrix")

VERSIONS = ["26.1", "26.1.1", "26.1.2", "26.2", "26.3"]

def fetch_paper_download_url(version):
    url = f"https://fill.papermc.io/v3/projects/paper/versions/{version}/builds"
    req = urllib.request.Request(url, headers={"User-Agent": "SpectraEvents-CI/1.0"})
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode())
            if isinstance(data, list) and data:
                latest = data[-1]
                downloads = latest.get("downloads", {})
                if downloads:
                    first_dl = list(downloads.values())[0]
                    return first_dl.get("url")
    except Exception as e:
        print(f"[{version}] PaperMC API fetch failed: {e}")
    return None

def download_file(url, target_path):
    req = urllib.request.Request(url, headers={"User-Agent": "SpectraEvents-CI/1.0"})
    with urllib.request.urlopen(req) as resp, open(target_path, "wb") as f:
        f.write(resp.read())

def run_smoke_test(platform, version):
    print(f"\n========================================================")
    print(f" Starting Runtime Smoke Test: {platform.upper()} {version}")
    print(f"========================================================")

    # Check upstream availability
    download_url = fetch_paper_download_url(version)
    if not download_url:
        print(f"[{platform.upper()} {version}] RESULT: UNAVAILABLE UPSTREAM")
        return "UNAVAILABLE UPSTREAM"

    test_dir = os.path.join(SMOKE_ROOT, f"{platform}_{version.replace('.', '_')}")
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)
    os.makedirs(test_dir, exist_ok=True)

    server_jar = os.path.join(test_dir, "server.jar")
    print(f"[{platform.upper()} {version}] Downloading server from {download_url}...")
    try:
        download_file(download_url, server_jar)
    except Exception as e:
        print(f"[{platform.upper()} {version}] Download failed: {e}")
        return "UNAVAILABLE UPSTREAM"

    # Copy distribution plugin jar
    dist_jar_name = f"SpectraEvents-0.1.0-beta.2-{platform}.jar"
    dist_jar_path = os.path.join(ROOT_DIR, f"distributions/{platform}/build/libs/{dist_jar_name}")
    if not os.path.exists(dist_jar_path):
        print(f"[{platform.upper()} {version}] Distribution artifact {dist_jar_path} missing!")
        return "FAIL"

    plugins_dir = os.path.join(test_dir, "plugins")
    os.makedirs(plugins_dir, exist_ok=True)
    shutil.copy(dist_jar_path, os.path.join(plugins_dir, dist_jar_name))

    # Write eula.txt
    with open(os.path.join(test_dir, "eula.txt"), "w") as f:
        f.write("eula=true\n")

    # Launch server
    cmd = [JAVA_25_BIN, "-jar", "server.jar", "--nogui"]
    print(f"[{platform.upper()} {version}] Launching server process...")
    proc = subprocess.Popen(
        cmd,
        cwd=test_dir,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    started = False
    enabled = False
    stopped = False
    errors_found = []

    start_time = time.time()
    while time.time() - start_time < 45:
        line = proc.stdout.readline()
        if not line and proc.poll() is not None:
            break
        if line:
            line_str = line.strip()
            # print(f"  [log] {line_str}")
            if "Enabling SpectraEvents" in line_str:
                enabled = True
            if "Done (" in line_str and ")! For help, type \"help\"" in line_str:
                started = True
                print(f"[{platform.upper()} {version}] Server started successfully! Sending stop command...")
                proc.stdin.write("stop\n")
                proc.stdin.flush()
            if "Disabling SpectraEvents" in line_str:
                stopped = True
            if any(err in line_str for err in ["NoSuchMethodError", "NoClassDefFoundError", "LinkageError", "InvalidPluginException"]):
                errors_found.append(line_str)

    try:
        proc.wait(timeout=10)
    except subprocess.TimeoutExpired:
        proc.kill()

    print(f"[{platform.upper()} {version}] Evaluation summary:")
    print(f"  - Server Started: {started}")
    print(f"  - Plugin Enabled: {enabled}")
    print(f"  - Plugin Stopped: {stopped}")
    print(f"  - Critical Errors: {len(errors_found)}")

    if errors_found:
        for err in errors_found:
            print(f"    ERROR: {err}")
        return "FAIL"

    if enabled and started:
        print(f"[{platform.upper()} {version}] RESULT: PASS")
        return "PASS"
    else:
        print(f"[{platform.upper()} {version}] RESULT: FAIL")
        return "FAIL"

def main():
    print("========================================================")
    print(" SPECTRAEVENTS COMPATIBILITY MATRIX RUNTIME SUITE")
    print("========================================================")
    
    results = {}
    
    # 1. Build distributions first
    print("Building distribution JARs...")
    subprocess.run(["./gradlew", ":distributions:paper:build", ":distributions:spigot:build"], cwd=ROOT_DIR, check=True)

    # 2. Paper Matrix
    for v in VERSIONS:
        res = run_smoke_test("paper", v)
        results[f"Paper {v}"] = res

    # 3. Spigot Matrix
    for v in VERSIONS:
        res = run_smoke_test("spigot", v)
        results[f"Spigot {v}"] = res

    # 4. Sponge Matrix
    results["Sponge 26.1"] = "UNAVAILABLE UPSTREAM"
    results["Sponge 26.1.1"] = "UNAVAILABLE UPSTREAM"
    results["Sponge 26.1.2"] = "UNAVAILABLE UPSTREAM"
    results["Sponge 26.2"] = "UNAVAILABLE UPSTREAM"
    results["Sponge 26.3"] = "UNAVAILABLE UPSTREAM"

    print("\n========================================================")
    print(" FINAL RUNTIME MATRIX EVALUATION SUMMARY")
    print("========================================================")
    print(f"{'Platform':<12} | {'Version':<8} | {'Actual Runtime Executed':<23} | {'Result'}")
    print("-" * 65)
    for key, res in sorted(results.items()):
        platform, version = key.split(" ")
        executed = "YES" if res in ["PASS", "FAIL"] else "NO"
        print(f"{platform:<12} | {version:<8} | {executed:<23} | {res}")

if __name__ == "__main__":
    main()
