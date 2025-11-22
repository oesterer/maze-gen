package maze;

import java.util.ArrayList;
import java.util.List;

public final class ClosestWallConnectorGenerator extends AbstractRoomMazeGenerator {
    @Override
    protected int connectRooms(MazeConfig config, Tile[][] grid, List<Room> rooms, int filled) {
        // Connect rooms by iteratively linking the nearest unconnected room to the growing tree.
        int n = rooms.size();
        if (n < 2) return filled;

        int[][] distances = buildDistanceMatrix(rooms);
        List<Integer> unconnected = new ArrayList<>();
        for (int i = 0; i < n; i++) unconnected.add(i);
        List<Integer> connected = new ArrayList<>();
        int startIndex = random.nextInt(unconnected.size());
        connected.add(unconnected.remove(startIndex));

        while (!unconnected.isEmpty()) {
            int bestUn = -1;
            int bestConn = -1;
            int bestDist = Integer.MAX_VALUE;
            for (int uc : unconnected) {
                for (int c : connected) {
                    int dist = distances[uc][c];
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestUn = uc;
                        bestConn = c;
                    }
                }
            }
            if (bestUn == -1 || bestConn == -1) break;
            filled += carveHallwayBetween(grid, rooms.get(bestConn), rooms.get(bestUn), config.hallwayWidth);
            connected.add(bestUn);
            unconnected.remove(Integer.valueOf(bestUn));
        }

        return filled;
    }

    private int[][] buildDistanceMatrix(List<Room> rooms) {
        // Precompute Manhattan distances between room centers.
        int n = rooms.size();
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int d = manhattan(center(rooms.get(i)), center(rooms.get(j)));
                dist[i][j] = d;
                dist[j][i] = d;
            }
        }
        return dist;
    }

    private int carveHallwayBetween(Tile[][] grid, Room a, Room b, int width) {
        // Prefer direct straight connections when wall projections overlap.
        int straight = tryStraightOverlap(grid, a, b, width);
        if (straight >= 0) {
            return straight;
        }

        // Otherwise determine closest pair of wall midpoints and carve a Z-shaped hallway.
        ConnectionPointPair pair = closestWallMidpoints(a, b, grid[0].length, grid.length);
        if (pair == null) {
            return 0;
        }
        ConnectionPoint start = pair.start;
        ConnectionPoint end = pair.end;
        // If aligned horizontally or vertically, carve straight instead of introducing bends.
        if (start.x == end.x || start.y == end.y) {
            return fillLine(grid, start.x, start.y, end.x, end.y, width);
        }
        return carveZPath(grid, start, end, width);
    }

    private int tryStraightOverlap(Tile[][] grid, Room a, Room b, int width) {
        int overlapTop = Math.max(a.y, b.y);
        int overlapBottom = Math.min(a.y + a.height - 1, b.y + b.height - 1);
        if (overlapTop <= overlapBottom) {
            int y = clamp((overlapTop + overlapBottom) / 2, overlapTop, overlapBottom);
            if (a.x + a.width <= b.x) {
                int startX = a.x + a.width;
                int endX = b.x - 1;
                return fillLine(grid, startX, y, endX, y, width);
            } else if (b.x + b.width <= a.x) {
                int startX = a.x - 1;
                int endX = b.x + b.width;
                return fillLine(grid, startX, y, endX, y, width);
            }
        }

        int overlapLeft = Math.max(a.x, b.x);
        int overlapRight = Math.min(a.x + a.width - 1, b.x + b.width - 1);
        if (overlapLeft <= overlapRight) {
            int x = clamp((overlapLeft + overlapRight) / 2, overlapLeft, overlapRight);
            if (a.y + a.height <= b.y) {
                int startY = a.y + a.height;
                int endY = b.y - 1;
                return fillLine(grid, x, startY, x, endY, width);
            } else if (b.y + b.height <= a.y) {
                int startY = a.y - 1;
                int endY = b.y + b.height;
                return fillLine(grid, x, startY, x, endY, width);
            }
        }
        return -1;
    }

    private ConnectionPointPair closestWallMidpoints(Room a, Room b, int gridW, int gridH) {
        // Evaluate all wall midpoints and return the closest pair between two rooms.
        List<ConnectionPoint> pointsA = wallMidpoints(a, gridW, gridH);
        List<ConnectionPoint> pointsB = wallMidpoints(b, gridW, gridH);
        ConnectionPointPair best = null;
        int bestDist = Integer.MAX_VALUE;
        for (ConnectionPoint pa : pointsA) {
            for (ConnectionPoint pb : pointsB) {
                int dist = Math.abs(pa.x - pb.x) + Math.abs(pa.y - pb.y);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new ConnectionPointPair(pa, pb);
                }
            }
        }
        return best;
    }

    private List<ConnectionPoint> wallMidpoints(Room room, int gridW, int gridH) {
        // Compute midpoints for each wall, projected just outside the room.
        List<ConnectionPoint> points = new ArrayList<>(4);
        int midY = room.y + room.height / 2;
        int midX = room.x + room.width / 2;
        points.add(new ConnectionPoint(clamp(room.x - 1, 0, gridW - 1), clamp(midY, 0, gridH - 1), Side.LEFT));
        points.add(new ConnectionPoint(clamp(room.x + room.width, 0, gridW - 1), clamp(midY, 0, gridH - 1), Side.RIGHT));
        points.add(new ConnectionPoint(clamp(midX, 0, gridW - 1), clamp(room.y - 1, 0, gridH - 1), Side.TOP));
        points.add(new ConnectionPoint(clamp(midX, 0, gridW - 1), clamp(room.y + room.height, 0, gridH - 1), Side.BOTTOM));
        return points;
    }

    private int carveZPath(Tile[][] grid, ConnectionPoint start, ConnectionPoint end, int width) {
        // Carve a Z-shaped connection: exit perpendicular, travel diagonally via midpoint, then approach the target.
        int filled = 0;
        if (start.side == Side.LEFT || start.side == Side.RIGHT) {
            int midX = clamp((start.x + end.x) / 2, 0, grid[0].length - 1);
            filled += fillLine(grid, start.x, start.y, midX, start.y, width);
            filled += fillLine(grid, midX, start.y, midX, end.y, width);
            filled += fillLine(grid, midX, end.y, end.x, end.y, width);
        } else {
            int midY = clamp((start.y + end.y) / 2, 0, grid.length - 1);
            filled += fillLine(grid, start.x, start.y, start.x, midY, width);
            filled += fillLine(grid, start.x, midY, end.x, midY, width);
            filled += fillLine(grid, end.x, midY, end.x, end.y, width);
        }
        return filled;
    }

    private static final class ConnectionPoint {
        final int x;
        final int y;
        final Side side;

        ConnectionPoint(int x, int y, Side side) {
            this.x = x;
            this.y = y;
            this.side = side;
        }
    }

    private record ConnectionPointPair(ConnectionPoint start, ConnectionPoint end) {}

    private enum Side {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }
}
