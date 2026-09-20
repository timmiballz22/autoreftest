#!/usr/bin/env python3
from pathlib import Path
import math
import struct

root = Path("src/main/resources/assets/skibidi")
source = root / "mesh" / "skibidi.skbm"
target = root / "mesh" / "skibidi_v3.skbm"
raw = source.read_bytes()

if raw[:4] != b"SKBM":
    raise SystemExit("Expected original SKBM source mesh")
version, head_count, toilet_count = struct.unpack_from("<iii", raw, 4)
if version != 1:
    raise SystemExit(f"Unexpected source mesh version {version}")
if (head_count, toilet_count) != (3444, 720):
    raise SystemExit(f"Unexpected source counts {(head_count, toilet_count)}")

offset = 16
head_and_interior = []
for _ in range(head_count):
    head_and_interior.append(list(struct.unpack_from("<8f", raw, offset)))
    offset += 32
toilet = []
for _ in range(toilet_count):
    toilet.append(list(struct.unpack_from("<8f", raw, offset)))
    offset += 32
if offset != len(raw):
    raise SystemExit("Trailing bytes in source mesh")

# The original packer concatenated head material 0 then mouth/interior material 1.
# Verify that exact known boundary before splitting so a changed source fails loudly.
head = [row[:] for row in head_and_interior[:-18]]
interior = [row[:] for row in head_and_interior[-18:]]
if len(head) != 3426 or len(interior) != 18:
    raise SystemExit("Unexpected material split")
if not all(abs(row[2] - (-0.309)) < 1.0e-6 for row in interior):
    raise SystemExit("Interior signature mismatch; refusing unsafe mesh split")
if any(abs(row[2] - (-0.309)) < 1.0e-6 for row in head):
    raise SystemExit("Interior signature leaked into head group")

# Undo the old atlas transforms for the two small materials.
# Toilet keeps its proven top-half atlas mapping/texture.
for row in head:
    row[3] = row[3] * 8.0
    row[4] = (row[4] - 0.5) * 8.0
for row in interior:
    row[3] = (row[3] - 0.125) * 32.0
    row[4] = (row[4] - 0.5) * 128.0

for name, rows, expected in (
    ("head", head, 3426),
    ("interior", interior, 18),
    ("toilet", toilet, 720),
):
    if len(rows) != expected or len(rows) % 3:
        raise SystemExit(f"Invalid {name} vertex count: {len(rows)}")
    for row in rows:
        if len(row) != 8 or not all(math.isfinite(v) for v in row):
            raise SystemExit(f"Non-finite/corrupt {name} vertex")

out = bytearray(b"SKB3")
out += struct.pack("<ii", 3, 3)
for gid, material, rows in ((0, 0, head), (1, 1, interior), (2, 2, toilet)):
    out += struct.pack("<iii", gid, material, len(rows))
    for row in rows:
        out += struct.pack("<8f", *row)

target.write_bytes(out)
print(f"Generated {target}: {len(out)} bytes")
print("Groups: head=3426 (1142 tris), interior=18 (6 tris), toilet=720 (240 tris)")
