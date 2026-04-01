import java.util.Locale;

public final class BenchmarkRunner {
    private BenchmarkRunner() {
    }

    public static void runDoublingHypothesis() {
        int[] sizes = {1_000, 2_000, 4_000, 8_000, 16_000, 32_000};
        long previousTime = -1L;

        System.out.println("====================================================");
        System.out.println("           DOUBLING HYPOTHESIS BENCHMARK            ");
        System.out.println("====================================================");
        System.out.println();
        System.out.printf("  %-12s %-12s %-12s %-10s%n", "Vertices", "Edges", "Time (ms)", "Ratio");
        System.out.println("----------------------------------------------------");

        Graph warmupGraph = generateAcyclicGraph(500);
        for (int i = 0; i < 10; i++) {
            CycleChecker.analyze(warmupGraph);
        }

        for (int size : sizes) {
            Graph graph = generateAcyclicGraph(size);

            for (int i = 0; i < 3; i++) {
                CycleChecker.analyze(graph);
            }

            int runs = 5;
            long totalNs = 0;
            for (int i = 0; i < runs; i++) {
                long start = System.nanoTime();
                CycleChecker.analyze(graph);
                totalNs += System.nanoTime() - start;
            }

            long elapsedMs = Math.max(1L, (totalNs / runs) / 1_000_000L);

            String ratioText = previousTime < 0
                    ? "N/A"
                    : String.format(Locale.US, "%.2f", (double) elapsedMs / previousTime);

            System.out.printf("  %-12d %-12d %-12d %-10s%n",
                    graph.vertexCount(), graph.edgeCount(), elapsedMs, ratioText);

            previousTime = elapsedMs;
        }

        System.out.println("  ---------------------------------------------------------------");
        System.out.println("  Ratios above 2.0 are likely due to JVM overhead at small sizes.");
        System.out.println("  Theoretically the algorithm is O(V + E) -- linear.");
        System.out.println("=================================================================");
    }

    public static Graph generateAcyclicGraph(int vertexCount) {
        Graph graph = new Graph();
        for (int i = 0; i < vertexCount; i++) {
            graph.addVertex(i);
        }
        for (int i = 0; i < vertexCount; i++) {
            if (i + 1 < vertexCount) graph.addEdge(i, i + 1);
            if (i + 2 < vertexCount) graph.addEdge(i, i + 2);
            if (i + 3 < vertexCount) graph.addEdge(i, i + 3);
        }
        return graph;
    }
}