package maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public abstract class AbstractRoomMazeGenerator implements MazeGenerator {
    protected final Random random = new Random();

    @Override
    public MazeResult generate(MazeConfig config) {
        // Orchestrate room placement, connection strategy, and hallway cleanup.
        if (config.seed != null) {
            random.setSeed(config.seed);
        }
        Tile[][] grid = new Tile[config.height][config.width];
        for (int y = 0; y < config.height; y++) {
            for (int x = 0; x < config.width; x++) {
                grid[y][x] = Tile.EMPTY;
            }
        }
        List<Room> rooms = new ArrayList<>();
        int targetFilled = (int) (config.area() * config.coverage);
        int filled = placeRooms(config, grid, rooms, targetFilled);
        filled = connectRooms(config, grid, rooms, filled);
        filled = pruneDeadEnds(grid, rooms);
        return new MazeResult(grid, rooms);
    }

    protected abstract int connectRooms(MazeConfig config, Tile[][] grid, List<Room> rooms, int filled);

    protected int placeRooms(MazeConfig config, Tile[][] grid, List<Room> rooms, int target) {
        // Randomly drop non-overlapping rooms until target coverage or max attempts.
        int filled = 0;
        int attempts = 0;
        int maxAttempts = 10_000;
        while (filled < target && attempts < maxAttempts) {
            attempts++;
            int w = randInRange(config.minRoomWidth, Math.min(config.maxRoomWidth, config.width));
            int h = randInRange(config.minRoomHeight, Math.min(config.maxRoomHeight, config.height));
            if (w <= 0 || h <= 0 || w > config.width || h > config.height) continue;
            int x = random.nextInt(config.width - w + 1);
            int y = random.nextInt(config.height - h + 1);
            if (!canPlace(grid, x, y, w, h)) continue;
            filled += fillRect(grid, x, y, w, h, Tile.ROOM);
            rooms.add(new Room(x, y, w, h));
        }
        return filled;
    }

    protected boolean canPlace(Tile[][] grid, int x, int y, int w, int h) {
        // Ensure target rectangle is empty.
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                if (grid[yy][xx] != Tile.EMPTY) return false;
            }
        }
        return true;
    }

    protected int fillRect(Tile[][] grid, int x, int y, int w, int h, Tile value) {
        // Fill rectangle with tile value, counting newly filled cells.
        int filled = 0;
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                if (grid[yy][xx] == Tile.EMPTY) filled++;
                grid[yy][xx] = value;
            }
        }
        return filled;
    }

    protected int carveCorridor(Tile[][] grid, int[] start, int[] end, int width) {
        // Carve an L-shaped corridor between two points with random orientation.
        boolean horizontalFirst = random.nextBoolean();
        int filled = 0;
        if (horizontalFirst) {
            filled += fillLine(grid, start[0], start[1], end[0], start[1], width);
            filled += fillLine(grid, end[0], start[1], end[0], end[1], width);
        } else {
            filled += fillLine(grid, start[0], start[1], start[0], end[1], width);
            filled += fillLine(grid, start[0], end[1], end[0], end[1], width);
        }
        return filled;
    }

    protected int fillLine(Tile[][] grid, int x0, int y0, int x1, int y1, int width) {
        // Fill straight line segments (horizontal/vertical), respecting room tiles.
        int filled = 0;
        int gw = grid[0].length;
        int gh = grid.length;
        if (x0 == x1) {
            int xs = x0 - width / 2;
            int xe = xs + width;
            for (int y : rangeInclusive(y0, y1)) {
                for (int x = xs; x < xe; x++) {
                    if (x >= 0 && x < gw && y >= 0 && y < gh) {
                        Tile cell = grid[y][x];
                        if (cell == Tile.ROOM) continue;
                        if (cell == Tile.EMPTY) filled++;
                        grid[y][x] = Tile.HALLWAY;
                    }
                }
            }
        } else if (y0 == y1) {
            int ys = y0 - width / 2;
            int ye = ys + width;
            for (int y = ys; y < ye; y++) {
                for (int x : rangeInclusive(x0, x1)) {
                    if (x >= 0 && x < gw && y >= 0 && y < gh) {
                        Tile cell = grid[y][x];
                        if (cell == Tile.ROOM) continue;
                        if (cell == Tile.EMPTY) filled++;
                        grid[y][x] = Tile.HALLWAY;
                    }
                }
            }
        } else {
            filled += fillLine(grid, x0, y0, x1, y0, width);
            filled += fillLine(grid, x1, y0, x1, y1, width);
        }
        return filled;
    }

    protected Iterable<Integer> rangeInclusive(int start, int end) {
        // Inclusive integer range helper.
        List<Integer> values = new ArrayList<>();
        int step = end >= start ? 1 : -1;
        for (int v = start; v != end + step; v += step) {
            values.add(v);
        }
        return values;
    }

    protected int[] connectionPoint(Room room, Room target, int gridW, int gridH) {
        // Pick a point just outside the room closest to the target center.
        int tx = target.centerX();
        int ty = target.centerY();
        int x0 = room.x;
        int y0 = room.y;
        int x1 = room.x + room.width - 1;
        int y1 = room.y + room.height - 1;

        int cx = clamp(tx, x0, x1);
        int cy = clamp(ty, y0, y1);

        int leftDist = Math.abs(tx - x0);
        int rightDist = Math.abs(tx - x1);
        int topDist = Math.abs(ty - y0);
        int bottomDist = Math.abs(ty - y1);

        int min = leftDist;
        String side = "left";
        if (rightDist < min) { min = rightDist; side = "right"; }
        if (topDist < min) { min = topDist; side = "top"; }
        if (bottomDist < min) { side = "bottom"; }

        switch (side) {
            case "left" -> cx = x0 - 1;
            case "right" -> cx = x1 + 1;
            case "top" -> cy = y0 - 1;
            case "bottom" -> cy = y1 + 1;
        }

        cx = clamp(cx, 0, gridW - 1);
        cy = clamp(cy, 0, gridH - 1);
        return new int[]{cx, cy};
    }

    protected int clamp(int v, int min, int max) {
        // Clamp value to provided bounds.
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    protected int[] center(Room room) {
        // Center coordinate of a room.
        return new int[]{room.centerX(), room.centerY()};
    }

    protected int manhattan(int[] a, int[] b) {
        // Manhattan distance helper.
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    protected int pruneDeadEnds(Tile[][] grid, List<Room> rooms) {
        // Remove hallway dead ends not adjacent to rooms and return filled count.
        int h = grid.length;
        if (h == 0) return 0;
        int w = grid[0].length;

        boolean[][] roomAdjacent = new boolean[h][w];
        for (Room room : rooms) {
            for (int y = room.y; y < room.y + room.height; y++) {
                for (int x = room.x; x < room.x + room.width; x++) {
                    for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int nx = x + d[0];
                        int ny = y + d[1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                            roomAdjacent[ny][nx] = true;
                        }
                    }
                }
            }
        }

        Deque<int[]> queue = new ArrayDeque<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (grid[y][x] == Tile.HALLWAY && neighCount(grid, x, y) <= 1 && !roomAdjacent[y][x]) {
                    queue.add(new int[]{x, y});
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] pos = queue.removeLast();
            int x = pos[0], y = pos[1];
            if (grid[y][x] != Tile.HALLWAY) continue;
            if (roomAdjacent[y][x]) continue;
            if (neighCount(grid, x, y) > 1) continue;
            grid[y][x] = Tile.EMPTY;
            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                    if (grid[ny][nx] == Tile.HALLWAY && neighCount(grid, nx, ny) <= 1 && !roomAdjacent[ny][nx]) {
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }

        int filled = 0;
        for (Tile[] row : grid) {
            for (Tile cell : row) {
                if (cell != Tile.EMPTY) filled++;
            }
        }
        return filled;
    }

    protected int neighCount(Tile[][] grid, int x, int y) {
        // Count hallway neighbors for dead-end pruning.
        int count = 0;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (ny >= 0 && ny < grid.length && nx >= 0 && nx < grid[0].length) {
                if (grid[ny][nx] == Tile.HALLWAY) count++;
            }
        }
        return count;
    }

    protected int randInRange(int min, int max) {
        // Inclusive random integer helper.
        if (max < min) return min;
        return min + random.nextInt(max - min + 1);
    }
}
