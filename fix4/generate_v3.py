#!/usr/bin/env python3
from pathlib import Path
import math
import struct

root = Path("src/main/resources/assets/skibidi")
source = root / "mesh" / "skibidi_v2.skbm"
target = root / "mesh" / "skibidi_v3.skbm"
raw = source.read_bytes()

if raw[:4] != b"SKB2":
    raise SystemExit("Expected SKB2 source mesh")
version, group_count = struct.unpack_from("<ii", raw, 4)
if version != 2:
    raise SystemExit(f"Unexpected source mesh version {version}")

offset = 12
groups = {}
for _ in range(group_count):
    gid, material, count = struct.unpack_from("<iii", raw, offset)
    offset += 12
    records = []
    for _ in range(count):
        records.append(list(struct.unpack_from("<8f", raw, offset)))
        offset += 32
    groups[gid] = records

if offset != len(raw):
    raise SystemExit("Trailing bytes in source mesh")
head_and_interior = groups.get(0)
toilet = groups.get(4)
if head_and_interior is None or toilet is None:
    raise SystemExit("Missing expected v2 source groups")
if len(head_and_interior) != 3444 or len(toilet) != 720:
    raise SystemExit(
        f"Unexpected v2 group sizes: head={len(head_and_interior)}, toilet={len(toilet)}"
    )

# The v2 atlas packer emitted the 18 mouth-interior vertices after the 3426
# ordinary head vertices. Verify the geometry signature before splitting.
head = [row[:] for row in head_and_interior[:-18]]
interior = [row[:] for row in head_and_interior[-18:]]
if not all(abs(row[2] - (-0.309)) < 1.0e-6 for row in interior):
    raise SystemExit("Interior signature mismatch; refusing unsafe mesh split")
if any(abs(row[2] - (-0.309)) < 1.0e-6 for row in head):
    raise SystemExit("Interior signature leaked into head group")

# Reverse only the v2 atlas UV transform for the two small head materials.
# The toilet keeps its proven top-half atlas coordinates and texture.
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
