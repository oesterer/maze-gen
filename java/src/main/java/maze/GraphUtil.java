package maze;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GraphUtil {
    private GraphUtil() {}

    public static List<int[]> buildRoomMst(List<Room> rooms) {
        // Greedy MST (Prim-like) over room centers to match corridor connectivity.
        List<int[]> edges = new ArrayList<>();
        if (rooms.size() < 2) return edges;

        Set<Integer> connected = new HashSet<>();
        connected.add(0);
        Set<Integer> remaining = new HashSet<>();
        for (int i = 1; i < rooms.size(); i++) remaining.add(i);

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
        return edges;
    }

    public static void writeGraphviz(List<Room> rooms, List<int[]> edges, String path) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write("graph rooms {\n");
            bw.write("  node [shape=box, style=filled, color=gray90];\n");
            for (int idx = 0; idx < rooms.size(); idx++) {
                Room r = rooms.get(idx);
                String label = String.format("Room %d\\n(%d,%d) %dx%d", idx, r.x, r.y, r.width, r.height);
                bw.write(String.format("  r%d [label=\"%s\"];\n", idx, label));
            }
            for (int[] e : edges) {
                bw.write(String.format("  r%d -- r%d;\n", e[0], e[1]));
            }
            bw.write("}\n");
        }
    }

    private static int[] center(Room room) {
        return new int[]{room.centerX(), room.centerY()};
    }

    private static int manhattan(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }
}
