public class Main {
    public static void main(String[] args) {
        try {
            boolean runBenchmark = false;
            String filename = "graph1.txt";

            if (!runBenchmark) {
                Graph graph = GraphParser.parse(filename);
                CycleChecker.AnalysisResult result = CycleChecker.analyze(graph);

                System.out.println("================================================");
                System.out.println("               GRAPH CYCLE CHECKER              ");
                System.out.println("================================================");
                System.out.println("  Input file : " + filename);
                System.out.println("  Vertices   : " + graph.vertexCount());
                System.out.println("  Edges      : " + graph.edgeCount());
                System.out.println("================================================");

                System.out.println();
                System.out.println("  TRACE");
                System.out.println("  -----");
                for (String line : result.getTrace()) {
                    System.out.println("  " + line);
                }

                System.out.println();
                System.out.println("================================================");
                if (result.isAcyclic()) {
                    System.out.println("  RESULT: YES -- the graph is acyclic.");
                    System.out.println();
                    System.out.println("  Elimination order: " + result.getEliminationOrder());
                } else {
                    System.out.println("  RESULT: NO -- the graph is cyclic.");
                    System.out.println();
                    System.out.println("  Vertices remaining : " + result.getRemainingVertices());
                    System.out.println("  Cycle found        : " + CycleChecker.formatCycle(result.getCycle()));
                }
                System.out.println("================================================");
            } else {
                BenchmarkRunner.runDoublingHypothesis();
            }

        } catch (Exception e) {
            System.out.println("================================================");
            System.out.println("  ERROR");
            System.out.println("  -----");
            System.out.println("  " + e.getMessage());
            System.out.println("================================================");
            System.exit(1);
        }
    }
}