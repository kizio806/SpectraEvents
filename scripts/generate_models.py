#!/usr/bin/env python3
import json
import base64
import math
import os
import zlib
import struct

def png_pack(png_tag, data):
    chunk_head = png_tag + data
    return (struct.pack("!I", len(data)) +
            chunk_head +
            struct.pack("!I", 0xFFFFFFFF & zlib.crc32(chunk_head)))

def create_png(width, height, pixels):
    raw_data = b""
    for y in range(height):
        raw_data += b"\x00"
        for x in range(width):
            r, g, b, a = pixels[y * width + x]
            raw_data += struct.pack("4B", r, g, b, a)
    
    compressor = zlib.compressobj()
    compressed = compressor.compress(raw_data) + compressor.flush()

    return b"".join([
        b"\x89PNG\r\n\x1a\n",
        png_pack(b"IHDR", struct.pack("!2I5B", width, height, 8, 6, 0, 0, 0)),
        png_pack(b"IDAT", compressed),
        png_pack(b"IEND", b"")
    ])

def generate_boss_portal_texture():
    pixels = []
    width, height = 64, 64
    for y in range(height):
        for x in range(width):
            noise = int(math.sin(x*1.3) * math.cos(y*1.7) * 20 + 40)
            r, g, b, a = noise, noise, noise, 255
            if math.sin(x*0.5 + math.sin(y*0.2)*5) > 0.8 and math.cos(y*0.5) > 0.5:
                r, g, b = 0, 255, 255
            pixels.append((r, g, b, a))
    return create_png(width, height, pixels)

def generate_pinata_texture():
    pixels = []
    width, height = 64, 64
    colors = [(255, 0, 128), (0, 255, 255), (255, 255, 0), (0, 255, 0)]
    for y in range(height):
        for x in range(width):
            c_idx = ((y // 8) + (x // 16)) % len(colors)
            r, g, b = colors[c_idx]
            noise = int(math.sin(x*50) * math.cos(y*50) * 20)
            r = max(0, min(255, r + noise))
            g = max(0, min(255, g + noise))
            b = max(0, min(255, b + noise))
            pixels.append((r, g, b, 255))
    return create_png(width, height, pixels)

def get_base64_texture(path, fallback_generator=None):
    if os.path.exists(path):
        with open(path, "rb") as f:
            return "data:image/png;base64," + base64.b64encode(f.read()).decode("utf-8")
    elif fallback_generator:
        img_bytes = fallback_generator()
        return "data:image/png;base64," + base64.b64encode(img_bytes).decode("utf-8")
    return ""

def create_cuboid(uuid, name, origin, from_pos, to_pos, uv_map, tex_id):
    return {
        "uuid": uuid,
        "name": name,
        "origin": origin,
        "from": from_pos,
        "to": to_pos,
        "faces": {
            dir: {"uv": uv, "texture": f"#{tex_id}"} 
            for dir, uv in uv_map.items()
        }
    }

def generate_meteor(output_path, texture_b64):
    elements = []
    children = []
    radius = 12
    center = [8, 8, 8]
    uv_all = {"north": [0,0,16,16], "east": [0,0,16,16], "south": [0,0,16,16], "west": [0,0,16,16], "up": [0,0,16,16], "down": [0,0,16,16]}
    
    for i in range(5):
        size = radius - (i * 1.5)
        angle = i * 45
        uuid = f"rock_{i}"
        elem = create_cuboid(
            uuid, f"Rock {i}", center,
            [center[0]-size, center[1]-size, center[2]-size],
            [center[0]+size, center[1]+size, center[2]+size],
            uv_all, "ember"
        )
        if i > 0:
            elem["rotation"] = [angle, angle, 0]
        elements.append(elem)
        children.append(uuid)

    model = {
        "meta": {"format_version": "5.0.0", "model_format": "java_block"},
        "display": {"head": {"translation": [0, -15, 0], "scale": [2.0, 2.0, 2.0]}},
        "textures": [{"id": "ember", "name": "ember.png", "source": texture_b64}],
        "elements": elements,
        "outliner": [{
            "uuid": "meteor_energy", "name": "Meteor", "origin": center, "children": children
        }],
        "animations": [
            {
                "name": "fall", "length": 2.0, "loop": "once",
                "animators": {
                    "meteor_energy": {
                        "keyframes": [
                            {"channel": "position", "time": 0, "data_points": [{"x": 0, "y": 40, "z": 0}]},
                            {"channel": "position", "time": 2, "data_points": [{"x": 0, "y": 0, "z": 0}]},
                            {"channel": "rotation", "time": 0, "data_points": [{"x": 0, "y": 0, "z": 0}]},
                            {"channel": "rotation", "time": 2, "data_points": [{"x": 720, "y": 360, "z": 180}]}
                        ]
                    }
                }
            },
            {
                "name": "impact", "length": 0.5, "loop": "once",
                "animators": {
                    "meteor_energy": {
                        "keyframes": [
                            {"channel": "scale", "time": 0, "data_points": [{"x": 1, "y": 1, "z": 1}]},
                            {"channel": "scale", "time": 0.5, "data_points": [{"x": 1.5, "y": 1.5, "z": 1.5}]}
                        ]
                    }
                }
            },
            {
                "name": "collapse", "length": 1.0, "loop": "once",
                "animators": {
                    "meteor_energy": {
                        "keyframes": [
                            {"channel": "scale", "time": 0, "data_points": [{"x": 1, "y": 1, "z": 1}]},
                            {"channel": "scale", "time": 1, "data_points": [{"x": 0, "y": 0, "z": 0}]}
                        ]
                    }
                }
            }
        ]
    }
    with open(output_path, "w") as f:
        json.dump(model, f)

def generate_airdrop(output_path, texture_b64):
    center = [8, 0, 8]
    elements = []
    children = []
    uv_all = {"north": [0,0,16,16], "east": [0,0,16,16], "south": [0,0,16,16], "west": [0,0,16,16], "up": [0,0,16,16], "down": [0,0,16,16]}
    
    elements.append(create_cuboid("crate_base", "Base", center, [-8, 0, -8], [24, 16, 24], uv_all, "metal"))
    children.append("crate_base")
    elements.append(create_cuboid("panel_1", "Panel 1", center, [-9, 2, -9], [25, 14, 25], uv_all, "metal"))
    children.append("panel_1")
    
    model = {
        "meta": {"format_version": "5.0.0", "model_format": "java_block"},
        "display": {"head": {"translation": [0, -15, 0], "scale": [2.0, 2.0, 2.0]}},
        "textures": [{"id": "metal", "name": "metal.png", "source": texture_b64}],
        "elements": elements,
        "outliner": [{"uuid": "airdrop_group", "name": "Airdrop", "origin": center, "children": children}],
        "animations": [
            {
                "name": "fall", "length": 2.0, "loop": "once",
                "animators": {
                    "airdrop_group": {
                        "keyframes": [
                            {"channel": "position", "time": 0, "data_points": [{"x": 0, "y": 50, "z": 0}]},
                            {"channel": "position", "time": 2, "data_points": [{"x": 0, "y": 0, "z": 0}]}
                        ]
                    }
                }
            },
            {
                "name": "open", "length": 1.0, "loop": "once",
                "animators": {
                    "airdrop_group": {
                        "keyframes": [
                            {"channel": "scale", "time": 0, "data_points": [{"x": 1, "y": 1, "z": 1}]},
                            {"channel": "scale", "time": 1, "data_points": [{"x": 1.2, "y": 0.1, "z": 1.2}]}
                        ]
                    }
                }
            }
        ]
    }
    with open(output_path, "w") as f:
        json.dump(model, f)

def generate_metin(output_path, texture_b64):
    center = [8, 0, 8]
    elements = []
    children = []
    uv_all = {"north": [0,0,16,16], "east": [0,0,16,16], "south": [0,0,16,16], "west": [0,0,16,16], "up": [0,0,16,16], "down": [0,0,16,16]}
    
    for i in range(4):
        h = 32 - (i*4)
        w = 8 - (i*1.5)
        angle = i * 30
        uuid = f"crystal_{i}"
        elem = create_cuboid(uuid, f"Crystal {i}", center, [center[0]-w, 0, center[2]-w], [center[0]+w, h, center[2]+w], uv_all, "magic")
        if i > 0:
            elem["rotation"] = [0, angle, 0]
        elements.append(elem)
        children.append(uuid)

    model = {
        "meta": {"format_version": "5.0.0", "model_format": "java_block"},
        "display": {"head": {"translation": [0, -15, 0], "scale": [2.0, 2.0, 2.0]}},
        "textures": [{"id": "magic", "name": "magic.png", "source": texture_b64}],
        "elements": elements,
        "outliner": [{"uuid": "metin_group", "name": "Metin", "origin": center, "children": children}],
        "animations": [
            {
                "name": "idle", "length": 4.0, "loop": "loop",
                "animators": {
                    "metin_group": {
                        "keyframes": [
                            {"channel": "position", "time": 0, "data_points": [{"x": 0, "y": 0, "z": 0}]},
                            {"channel": "position", "time": 2, "data_points": [{"x": 0, "y": 2, "z": 0}]},
                            {"channel": "position", "time": 4, "data_points": [{"x": 0, "y": 0, "z": 0}]}
                        ]
                    }
                }
            },
            {
                "name": "destroy", "length": 1.0, "loop": "once",
                "animators": {
                    "metin_group": {
                        "keyframes": [
                            {"channel": "scale", "time": 0, "data_points": [{"x": 1, "y": 1, "z": 1}]},
                            {"channel": "scale", "time": 1, "data_points": [{"x": 0, "y": 0, "z": 0}]}
                        ]
                    }
                }
            }
        ]
    }
    with open(output_path, "w") as f:
        json.dump(model, f)

def generate_pinata(output_path, texture_b64):
    center = [8, 16, 8]
    elements = []
    children = []
    uv_all = {"north": [0,0,16,16], "east": [0,0,16,16], "south": [0,0,16,16], "west": [0,0,16,16], "up": [0,0,16,16], "down": [0,0,16,16]}
    
    elements.append(create_cuboid("pinata_body", "Body", center, [4, 12, 4], [12, 20, 12], uv_all, "paper"))
    children.append("pinata_body")
    elements.append(create_cuboid("pinata_head", "Head", [8, 20, 4], [6, 20, -2], [10, 26, 6], uv_all, "paper"))
    children.append("pinata_head")
    elements.append(create_cuboid("leg1", "Leg", [6, 12, 6], [4, 4, 4], [8, 12, 8], uv_all, "paper"))
    children.append("leg1")
    elements.append(create_cuboid("leg2", "Leg", [10, 12, 6], [8, 4, 4], [12, 12, 8], uv_all, "paper"))
    children.append("leg2")
    elements.append(create_cuboid("leg3", "Leg", [6, 12, 10], [4, 4, 8], [8, 12, 12], uv_all, "paper"))
    children.append("leg3")
    elements.append(create_cuboid("leg4", "Leg", [10, 12, 10], [8, 4, 8], [12, 12, 12], uv_all, "paper"))
    children.append("leg4")

    model = {
        "meta": {"format_version": "5.0.0", "model_format": "java_block"},
        "display": {"head": {"translation": [0, -15, 0], "scale": [2.0, 2.0, 2.0]}},
        "textures": [{"id": "paper", "name": "paper.png", "source": texture_b64}],
        "elements": elements,
        "outliner": [{"uuid": "pinata_group", "name": "Pinata", "origin": center, "children": children}],
        "animations": [
            {
                "name": "idle", "length": 2.0, "loop": "loop",
                "animators": {
                    "pinata_group": {
                        "keyframes": [
                            {"channel": "rotation", "time": 0, "data_points": [{"x": 0, "y": 0, "z": -5}]},
                            {"channel": "rotation", "time": 1, "data_points": [{"x": 0, "y": 0, "z": 5}]},
                            {"channel": "rotation", "time": 2, "data_points": [{"x": 0, "y": 0, "z": -5}]}
                        ]
                    }
                }
            },
            {
                "name": "hit", "length": 0.3, "loop": "once",
                "animators": {
                    "pinata_group": {
                        "keyframes": [
                            {"channel": "rotation", "time": 0, "data_points": [{"x": 0, "y": 0, "z": 0}]},
                            {"channel": "rotation", "time": 0.15, "data_points": [{"x": 15, "y": 0, "z": 0}]},
                            {"channel": "rotation", "time": 0.3, "data_points": [{"x": 0, "y": 0, "z": 0}]}
                        ]
                    }
                }
            },
            {
                "name": "destroy", "length": 0.5, "loop": "once",
                "animators": {
                    "pinata_group": {
                        "keyframes": [
                            {"channel": "scale", "time": 0, "data_points": [{"x": 1, "y": 1, "z": 1}]},
                            {"channel": "scale", "time": 0.5, "data_points": [{"x": 2, "y": 2, "z": 2}]}
                        ]
                    }
                }
            }
        ]
    }
    with open(output_path, "w") as f:
        json.dump(model, f)

def generate_portal(output_path, texture_b64):
    center = [8, 16, 8]
    elements = []
    children = []
    uv_all = {"north": [0,0,16,16], "east": [0,0,16,16], "south": [0,0,16,16], "west": [0,0,16,16], "up": [0,0,16,16], "down": [0,0,16,16]}
    
    num_blocks = 16
    radius = 12
    for i in range(num_blocks):
        angle = (i / num_blocks) * 360
        rad = math.radians(angle)
        x = center[0] + math.sin(rad) * radius
        y = center[1] + math.cos(rad) * radius
        uuid = f"block_{i}"
        elem = create_cuboid(uuid, f"Block {i}", [x, y, 8], [x-2, y-2, 6], [x+2, y+2, 10], uv_all, "obsidian")
        elem["rotation"] = [0, 0, -angle]
        elements.append(elem)
        children.append(uuid)

    model = {
        "meta": {"format_version": "5.0.0", "model_format": "java_block"},
        "display": {"head": {"translation": [0, -15, 0], "scale": [2.0, 2.0, 2.0]}},
        "textures": [{"id": "obsidian", "name": "obsidian.png", "source": texture_b64}],
        "elements": elements,
        "outliner": [{"uuid": "portal_group", "name": "Portal", "origin": center, "children": children}],
        "animations": [
            {
                "name": "idle", "length": 10.0, "loop": "loop",
                "animators": {
                    "portal_group": {
                        "keyframes": [
                            {"channel": "rotation", "time": 0, "data_points": [{"x": 0, "y": 0, "z": 0}]},
                            {"channel": "rotation", "time": 10, "data_points": [{"x": 0, "y": 0, "z": 360}]}
                        ]
                    }
                }
            },
            {
                "name": "close", "length": 2.0, "loop": "once",
                "animators": {
                    "portal_group": {
                        "keyframes": [
                            {"channel": "scale", "time": 0, "data_points": [{"x": 1, "y": 1, "z": 1}]},
                            {"channel": "scale", "time": 2, "data_points": [{"x": 0, "y": 0, "z": 0}]}
                        ]
                    }
                }
            }
        ]
    }
    with open(output_path, "w") as f:
        json.dump(model, f)

if __name__ == "__main__":
    out_dir = "/home/kizio/Dokumenty/GitHub/SpectraEvents/spectraevents-application/src/main/resources/assets/source"
    os.makedirs(out_dir, exist_ok=True)
    
    brain_dir = "/home/kizio/.gemini/antigravity-ide/brain/56f5d8e2-6c3f-43ba-83c7-980cb0c3bc44"
    
    print("Generating Meteor...")
    meteor_b64 = get_base64_texture(os.path.join(brain_dir, "meteor_texture_1790195841668.png"))
    generate_meteor(os.path.join(out_dir, "meteor_core.bbmodel"), meteor_b64)
    
    print("Generating Airdrop...")
    airdrop_b64 = get_base64_texture(os.path.join(brain_dir, "airdrop_texture_1790195861051.png"))
    generate_airdrop(os.path.join(out_dir, "airdrop_crate.bbmodel"), airdrop_b64)
    
    print("Generating Metin...")
    metin_b64 = get_base64_texture(os.path.join(brain_dir, "metin_texture_1790195878000.png"))
    generate_metin(os.path.join(out_dir, "metin_stone.bbmodel"), metin_b64)
    
    print("Generating Portal...")
    portal_b64 = get_base64_texture("", generate_boss_portal_texture)
    generate_portal(os.path.join(out_dir, "boss_portal.bbmodel"), portal_b64)
    
    print("Generating Pinata...")
    pinata_b64 = get_base64_texture("", generate_pinata_texture)
    generate_pinata(os.path.join(out_dir, "pinata.bbmodel"), pinata_b64)
    
    print("All models generated successfully!")
