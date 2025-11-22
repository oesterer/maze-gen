package maze;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RoomsAndCorridorsGenerator extends AbstractRoomMazeGenerator {
    @Override
    protected int connectRooms(MazeConfig config, Tile[][] grid, List<Room> rooms, int filled) {
        // Build a greedy spanning tree over room centers and carve corridors.
        if (rooms.size() < 2) return filled;

        Set<Integer> connected = new HashSet<>();
        connected.add(0);
        Set<Integer> remaining = new HashSet<>();
        for (int i = 1; i < rooms.size(); i++) remaining.add(i);
        List<int[]> edges = new ArrayList<>();

        while (!remaining.isEmpty()) {
            int bestI = -1, bestJ = -1;
            int bestDist = Integer.MAX_VALUE;
            for (int i : connected) {
                for (int j : remaining) {
                    int dist = manhattan(center(rooms.get(i)), center(rooms.get(j)));
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
            if (bestI == -1 || bestJ == -1) break;
            connected.add(bestJ);
            remaining.remove(bestJ);
            edges.add(new int[]{bestI, bestJ});
        }

        int width = grid[0].length;
        int height = grid.length;
        for (int[] edge : edges) {
            Room a = rooms.get(edge[0]);
            Room b = rooms.get(edge[1]);
            int[] start = connectionPoint(a, b, width, height);
            int[] end = connectionPoint(b, a, width, height);
            filled += carveCorridor(grid, start, end, config.hallwayWidth);
        }
        return filled;
    }
}
