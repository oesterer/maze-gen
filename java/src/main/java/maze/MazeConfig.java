package maze;

import java.util.Objects;

public final class MazeConfig {
    public final int width;
    public final int height;
    public final int minRoomWidth;
    public final int minRoomHeight;
    public final int maxRoomWidth;
    public final int maxRoomHeight;
    public final double coverage; // fraction 0-1
    public final int hallwayWidth;
    public final String algorithm;
    public final String outputPath;
    public final String graphOutputPath;
    public final Long seed;

    public MazeConfig(
            int width,
            int height,
            int minRoomWidth,
            int minRoomHeight,
            int maxRoomWidth,
            int maxRoomHeight,
            double coverage,
            int hallwayWidth,
            String algorithm,
            String outputPath,
            String graphOutputPath,
            Long seed) {
        this.width = width;
        this.height = height;
        this.minRoomWidth = minRoomWidth;
        this.minRoomHeight = minRoomHeight;
        this.maxRoomWidth = maxRoomWidth;
        this.maxRoomHeight = maxRoomHeight;
        this.coverage = coverage;
        this.hallwayWidth = hallwayWidth;
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.outputPath = Objects.requireNonNull(outputPath, "outputPath");
        this.graphOutputPath = graphOutputPath;
        this.seed = seed;
    }

    public int area() {
        return width * height;
    }

    public static MazeConfig fromArgs(String[] args) {
        int width = 1000;
        int height = 1000;
        int minRoomW = 2;
        int minRoomH = 2;
        int maxRoomW = 200;
        int maxRoomH = 200;
        double coverage = 0.5;
        int hallwayWidth = 1;
        String algorithm = "rooms_and_corridors";
        String output = "maze.png";
        String graphOutput = null;
        Long seed = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--width" -> width = Integer.parseInt(requireNext(args, ++i, arg));
                case "--height" -> height = Integer.parseInt(requireNext(args, ++i, arg));
                case "--min-room-width" -> minRoomW = Integer.parseInt(requireNext(args, ++i, arg));
                case "--min-room-height" -> minRoomH = Integer.parseInt(requireNext(args, ++i, arg));
                case "--max-room-width" -> maxRoomW = Integer.parseInt(requireNext(args, ++i, arg));
                case "--max-room-height" -> maxRoomH = Integer.parseInt(requireNext(args, ++i, arg));
                case "--coverage" -> coverage = parseCoverage(requireNext(args, ++i, arg));
                case "--hallway-width" -> hallwayWidth = Integer.parseInt(requireNext(args, ++i, arg));
                case "--algorithm" -> algorithm = requireNext(args, ++i, arg);
                case "--output" -> output = requireNext(args, ++i, arg);
                case "--graph-output" -> graphOutput = requireNext(args, ++i, arg);
                case "--seed" -> seed = Long.parseLong(requireNext(args, ++i, arg));
                case "--help", "-h" -> {
                    printHelp();
                    System.exit(0);
                }
                default -> throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        return new MazeConfig(
                width,
                height,
                minRoomW,
                minRoomH,
                maxRoomW,
                maxRoomH,
                coverage,
                hallwayWidth,
                algorithm,
                output,
                graphOutput,
                seed);
    }

    private static String requireNext(String[] args, int idx, String current) {
        if (idx >= args.length) {
            throw new IllegalArgumentException("Missing value after " + current);
        }
        return args[idx];
    }

    private static double parseCoverage(String raw) {
        double v = Double.parseDouble(raw);
        if (v > 1.0) {
            v = v / 100.0;
        }
        if (v < 0.0) v = 0.0;
        if (v > 1.0) v = 1.0;
        return v;
    }

    public static void printHelp() {
        System.out.println("Maze generator (Java)\n" +
                "Options:\n" +
                "  --width <int>            Area width (default 1000)\n" +
                "  --height <int>           Area height (default 1000)\n" +
                "  --min-room-width <int>   Minimum room width (default 2)\n" +
                "  --min-room-height <int>  Minimum room height (default 2)\n" +
                "  --max-room-width <int>   Maximum room width (default 200)\n" +
                "  --max-room-height <int>  Maximum room height (default 200)\n" +
                "  --coverage <float>       Target coverage fraction or percent (default 50)\n" +
                "  --hallway-width <int>    Hallway width in tiles (default 1)\n" +
                "  --algorithm <name>       Algorithm (default rooms_and_corridors)\n" +
                "  --output <path>          Output PNG path (default maze.png)\n" +
                "  --graph-output <path>    Optional Graphviz DOT path for room connections\n" +
                "  --seed <long>            Optional RNG seed\n" +
                "  --help                   Show this help");
    }
}
