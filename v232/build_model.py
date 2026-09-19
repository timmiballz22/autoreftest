import base64
import json
import shutil
import struct
import sys
from pathlib import Path

model_root = Path(sys.argv[1])
target_root = Path(sys.argv[2])
gltf_path = model_root / "source" / "model.gltf"

g = json.loads(gltf_path.read_text())
uri = g["buffers"][0]["uri"]
if not uri.startswith("data:application/octet-stream;base64,"):
    raise RuntimeError("Expected embedded glTF binary buffer")
buf = base64.b64decode(uri.split(",", 1)[1])

component_format = {5120: "b", 5121: "B", 5122: "h", 5123: "H", 5125: "I", 5126: "f"}
component_count = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}

def accessor(index):
    acc = g["accessors"][index]
    view = g["bufferViews"][acc["bufferView"]]
    code = component_format[acc["componentType"]]
    width = component_count[acc["type"]]
    scalar_size = struct.calcsize("<" + code)
    stride = view.get("byteStride", width * scalar_size)
    offset = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
    result = []
    for i in range(acc["count"]):
        values = struct.unpack_from("<" + code * width, buf, offset + i * stride)
        result.append(values[0] if width == 1 else tuple(float(v) for v in values))
    return result

groups = {
    0: {"material": 0, "vertices": []},
    1: {"material": 0, "vertices": []},
    2: {"material": 0, "vertices": []},
    3: {"material": 1, "vertices": []},
    4: {"material": 2, "vertices": []},
}

def group_for(name):
    if name in {"ojo_D", "ojo_I"}:
        return 1
    if name in {"pupila_D", "pupila_I"}:
        return 2
    if name == "interior":
        return 3
    if name == "Escusado":
        return 4
    return 0

for node in g["nodes"]:
    if "mesh" not in node:
        continue
    gid = group_for(node.get("name", ""))
    for primitive in g["meshes"][node["mesh"]]["primitives"]:
        if primitive.get("mode", 4) != 4:
            raise RuntimeError(f"Non-triangle primitive on {node.get('name')}")
        material = primitive.get("material", 0)
        if material != groups[gid]["material"]:
            raise RuntimeError(f"Unexpected material {material} on {node.get('name')}")
        positions = accessor(primitive["attributes"]["POSITION"])
        normals = accessor(primitive["attributes"]["NORMAL"])
        uvs = accessor(primitive["attributes"]["TEXCOORD_0"])
        indices = accessor(primitive["indices"])
        for raw_index in indices:
            i = int(raw_index)
            x, y, z = positions[i]
            nx, ny, nz = normals[i]
            u, v = uvs[i]
            groups[gid]["vertices"].append((x, y, z, u, v, nx, ny, nz))

counts = [len(groups[i]["vertices"]) for i in range(5)]
expected_counts = [3282, 72, 72, 18, 720]
if counts != expected_counts:
    raise RuntimeError(f"Unexpected group counts {counts}, expected {expected_counts}")

mesh_dir = target_root / "assets" / "skibidi" / "mesh"
texture_dir = target_root / "assets" / "skibidi" / "textures" / "entity"
mesh_dir.mkdir(parents=True, exist_ok=True)
texture_dir.mkdir(parents=True, exist_ok=True)

out = mesh_dir / "skibidi_v2.skbm"
with out.open("wb") as file:
    file.write(b"SKB2")
    file.write(struct.pack("<ii", 2, 5))
    for gid in range(5):
        part = groups[gid]
        file.write(struct.pack("<iii", gid, part["material"], len(part["vertices"])))
        for vertex in part["vertices"]:
            file.write(struct.pack("<8f", *vertex))

shutil.copyfile(model_root / "textures" / "gltf_embedded_0.png", texture_dir / "skibidi_head.png")
shutil.copyfile(model_root / "textures" / "gltf_embedded_1.png", texture_dir / "skibidi_interior.png")
shutil.copyfile(model_root / "textures" / "gltf_embedded_2.png", texture_dir / "skibidi_toilet.png")

print(f"MODEL_BUILD_PASS counts={counts} bytes={out.stat().st_size}")
