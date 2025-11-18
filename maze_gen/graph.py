from __future__ import annotations

from typing import Iterable, List, Tuple

from .algorithms import Room


def build_room_mst(rooms: List[Room]) -> List[Tuple[int, int]]:
    """Rebuild the same greedy MST used by the generator to connect rooms.

    Returns edges as pairs of room indices.
    """
    if len(rooms) < 2:
        return []
    connected = {0}
    remaining = set(range(1, len(rooms)))
    edges: List[Tuple[int, int]] = []

    while remaining:
        best = None
        best_dist = None
        for i in connected:
            for j in remaining:
                dist = _manhattan(rooms[i].center, rooms[j].center)
                if best_dist is None or dist < best_dist:
                    best_dist = dist
                    best = (i, j)
        if best is None:
            break
        i, j = best
        connected.add(j)
        remaining.remove(j)
        edges.append((i, j))
    return edges


def write_graphviz(rooms: List[Room], edges: Iterable[Tuple[int, int]], path: str) -> None:
    """Write a simple Graphviz DOT file showing rooms and their connections."""
    lines = ["graph rooms {"]
    lines.append("  node [shape=box, style=filled, color=gray90];")
    for idx, room in enumerate(rooms):
        label = f"Room {idx}\\n({room.x},{room.y}) {room.width}x{room.height}"
        lines.append(f"  r{idx} [label=\"{label}\"];\n")
    for a, b in edges:
        lines.append(f"  r{a} -- r{b};")
    lines.append("}")

    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def _manhattan(a: Tuple[int, int], b: Tuple[int, int]) -> int:
    ax, ay = a
    bx, by = b
    return abs(ax - bx) + abs(ay - by)
