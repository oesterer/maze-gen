from __future__ import annotations

import random
from typing import List, Tuple

from .base import EMPTY, HALLWAY, ROOM, MazeGenerator, Room
from ..config import MazeConfig


class RoomsAndCorridorsGenerator:
    def __init__(self) -> None:
        self.random = random.Random()

    def generate(self, config: MazeConfig) -> Tuple[List[List[int]], List[Room]]:
        if config.seed is not None:
            self.random.seed(config.seed)

        # Start with an empty grid; 0 = empty, 1 = room, 2 = hallway.
        grid: List[List[int]] = [[EMPTY for _ in range(config.width)] for _ in range(config.height)]
        rooms: List[Room] = []
        filled = 0
        target_filled = int(config.area * config.coverage)

        filled = self._place_rooms(config, grid, rooms, target_filled)
        filled = self._connect_rooms(config, grid, rooms, filled)
        filled = self._prune_dead_ends(grid, rooms)

        return grid, rooms

    def _place_rooms(
        self,
        config: MazeConfig,
        grid: List[List[int]],
        rooms: List[Room],
        target: int,
    ) -> int:
        filled = 0
        attempts = 0
        max_attempts = 10_000

        while filled < target and attempts < max_attempts:
            attempts += 1
            width = self.random.randint(config.min_room_width, min(config.max_room_width, config.width))
            height = self.random.randint(config.min_room_height, min(config.max_room_height, config.height))

            if width <= 0 or height <= 0 or width > config.width or height > config.height:
                continue

            x = self.random.randint(0, config.width - width)
            y = self.random.randint(0, config.height - height)

            if not self._can_place_room(grid, x, y, width, height):
                continue

            filled += self._fill_rect(grid, x, y, width, height, ROOM)
            rooms.append(Room(x=x, y=y, width=width, height=height))

        return filled

    def _connect_rooms(
        self,
        config: MazeConfig,
        grid: List[List[int]],
        rooms: List[Room],
        filled: int,
    ) -> int:
        if len(rooms) < 2:
            return filled

        # Prim's algorithm for a loose spanning tree over room centers.
        # Build a loose spanning tree over room centers (Prim-like greedy) so every room connects.
        connected = {0}
        remaining = set(range(1, len(rooms)))
        edges: List[Tuple[int, int]] = []

        while remaining:
            best = None
            best_dist = None
            for i in connected:
                for j in remaining:
                    dist = self._manhattan(rooms[i].center, rooms[j].center)
                    if best_dist is None or dist < best_dist:
                        best_dist = dist
                        best = (i, j)
            if best is None:
                break
            i, j = best
            connected.add(j)
            remaining.remove(j)
            edges.append((i, j))

        for a, b in edges:
            start = self._connection_point(rooms[a], rooms[b].center, len(grid[0]), len(grid))
            end = self._connection_point(rooms[b], rooms[a].center, len(grid[0]), len(grid))
            filled += self._carve_corridor(
                grid,
                start,
                end,
                config.hallway_width,
            )

        return filled

    def _can_place_room(self, grid: List[List[int]], x: int, y: int, width: int, height: int) -> bool:
        for yy in range(y, y + height):
            row = grid[yy]
            for xx in range(x, x + width):
                if row[xx] != EMPTY:
                    return False
        return True

    def _fill_rect(self, grid: List[List[int]], x: int, y: int, width: int, height: int, value: int) -> int:
        filled = 0
        for yy in range(y, y + height):
            row = grid[yy]
            for xx in range(x, x + width):
                if row[xx] == EMPTY:
                    filled += 1
                row[xx] = value
        return filled

    def _carve_corridor(
        self,
        grid: List[List[int]],
        start: Tuple[int, int],
        end: Tuple[int, int],
        width: int,
    ) -> int:
        sx, sy = start
        ex, ey = end

        # Choose an L-shaped path: horizontal then vertical, or the reverse.
        # Carve an L shape; flip a coin for horizontal-first vs vertical-first.
        carve_h_first = self.random.choice([True, False])
        filled = 0

        def carve_line(x0: int, y0: int, x1: int, y1: int) -> int:
            return self._fill_line(grid, x0, y0, x1, y1, width)

        if carve_h_first:
            filled += carve_line(sx, sy, ex, sy)
            filled += carve_line(ex, sy, ex, ey)
        else:
            filled += carve_line(sx, sy, sx, ey)
            filled += carve_line(sx, ey, ex, ey)

        return filled

    def _fill_line(self, grid: List[List[int]], x0: int, y0: int, x1: int, y1: int, width: int) -> int:
        filled = 0
        if x0 == x1:
            x_start = x0 - width // 2
            x_end = x_start + width
            for yy in self._range_inclusive(y0, y1):
                for xx in range(x_start, x_end):
                    if 0 <= yy < len(grid) and 0 <= xx < len(grid[0]):
                        cell = grid[yy][xx]
                        if cell == ROOM:
                            continue  # do not overwrite rooms
                        if cell == EMPTY:
                            filled += 1
                        grid[yy][xx] = HALLWAY
        elif y0 == y1:
            y_start = y0 - width // 2
            y_end = y_start + width
            for yy in range(y_start, y_end):
                for xx in self._range_inclusive(x0, x1):
                    if 0 <= yy < len(grid) and 0 <= xx < len(grid[0]):
                        cell = grid[yy][xx]
                        if cell == ROOM:
                            continue  # do not overwrite rooms
                        if cell == EMPTY:
                            filled += 1
                        grid[yy][xx] = HALLWAY
        else:
            # Fallback to Manhattan steps if diagonal unexpectedly appears.
            filled += self._fill_line(grid, x0, y0, x1, y0, width)
            filled += self._fill_line(grid, x1, y0, x1, y1, width)
        return filled

    def _connection_point(self, room: Room, target: Tuple[int, int], grid_width: int, grid_height: int) -> Tuple[int, int]:
        tx, ty = target
        x0, y0 = room.x, room.y
        x1, y1 = room.x + room.width - 1, room.y + room.height - 1

        # Nearest point on room perimeter to target.
        cx = min(max(tx, x0), x1)
        cy = min(max(ty, y0), y1)

        # Decide which side to exit based on closest edge.
        distances = {
            "left": abs(tx - x0),
            "right": abs(tx - x1),
            "top": abs(ty - y0),
            "bottom": abs(ty - y1),
        }
        side = min(distances, key=distances.get)

        if side == "left":
            cx = x0 - 1
        elif side == "right":
            cx = x1 + 1
        elif side == "top":
            cy = y0 - 1
        else:  # bottom
            cy = y1 + 1

        # Clamp to grid boundaries.
        cx = max(0, min(grid_width - 1, cx))
        cy = max(0, min(grid_height - 1, cy))
        return (cx, cy)

    def _range_inclusive(self, start: int, end: int):
        step = 1 if end >= start else -1
        return range(start, end + step, step)

    def _manhattan(self, a: Tuple[int, int], b: Tuple[int, int]) -> int:
        ax, ay = a
        bx, by = b
        return abs(ax - bx) + abs(ay - by)

    def _prune_dead_ends(self, grid: List[List[int]], rooms: List[Room]) -> int:
        height = len(grid)
        if height == 0:
            return 0
        width = len(grid[0])

        # Precompute room adjacency for quick lookups.
        room_adjacent = [[False for _ in range(width)] for _ in range(height)]
        for room in rooms:
            for yy in range(room.y, room.y + room.height):
                for xx in range(room.x, room.x + room.width):
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = xx + dx, yy + dy
                        if 0 <= nx < width and 0 <= ny < height:
                            room_adjacent[ny][nx] = True

        def hallway_neighbors(x: int, y: int) -> int:
            count = 0
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height and grid[ny][nx] == HALLWAY:
                    count += 1
            return count

        queue: list[Tuple[int, int]] = []
        for y in range(height):
            for x in range(width):
                if grid[y][x] == HALLWAY and hallway_neighbors(x, y) <= 1 and not room_adjacent[y][x]:
                    queue.append((x, y))

        removed = 0
        while queue:
            x, y = queue.pop()
            if grid[y][x] != HALLWAY:
                continue
            if room_adjacent[y][x]:
                continue
            if hallway_neighbors(x, y) > 1:
                continue
            grid[y][x] = EMPTY
            removed += 1
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < width and 0 <= ny < height:
                    if grid[ny][nx] == HALLWAY and hallway_neighbors(nx, ny) <= 1 and not room_adjacent[ny][nx]:
                        queue.append((nx, ny))

        # Return updated filled count (rooms + hallways after pruning).
        filled = 0
        for row in grid:
            for cell in row:
                if cell != EMPTY:
                    filled += 1
        return filled
