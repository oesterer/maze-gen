from __future__ import annotations

import struct
import zlib
from typing import List

from .algorithms import EMPTY, HALLWAY, ROOM

# Colors: light gray empty, medium gray rooms, very dark hallways for contrast.
COLORS = {
    EMPTY: (245, 245, 245),
    ROOM: (180, 180, 180),
    HALLWAY: (10, 10, 10),
}


def render_png(grid: List[List[int]], path: str) -> None:
    """Render the maze grid to a simple RGB PNG (no dependencies)."""
    if not grid:
        raise ValueError("Grid is empty; nothing to render")

    height = len(grid)
    width = len(grid[0])

    # Build raw scanlines: each row starts with filter byte 0.
    raw = bytearray()
    for row in grid:
        raw.append(0)
        for cell in row:
            raw.extend(COLORS.get(cell, COLORS[EMPTY]))

    compressed = zlib.compress(bytes(raw))

    def chunk(chunk_type: bytes, data: bytes) -> bytes:
        length = struct.pack(">I", len(data))
        crc = struct.pack(">I", zlib.crc32(chunk_type + data) & 0xFFFFFFFF)
        return length + chunk_type + data + crc

    signature = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)  # 8-bit RGB
    idat = compressed
    iend = b""

    with open(path, "wb") as f:
        f.write(signature)
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", idat))
        f.write(chunk(b"IEND", iend))
