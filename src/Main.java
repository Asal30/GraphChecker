//public class Main {
//    public static void main(String[] args) {
//        try {
//            boolean runBenchmark = false;
//            String filename = "acyclic.txt";
//
//            if (!runBenchmark) {
//                Graph graph = GraphParser.parse(filename);
//                CycleChecker.AnalysisResult result = CycleChecker.analyze(graph);
//
//                System.out.println("================================================");
//                System.out.println("               GRAPH CYCLE CHECKER              ");
//                System.out.println("================================================");
//                System.out.println("  Input file : " + filename);
//                System.out.println("  Vertices   : " + graph.vertexCount());
//                System.out.println("  Edges      : " + graph.edgeCount());
//                System.out.println("================================================");
//
//                System.out.println();
//                System.out.println("  TRACE");
//                System.out.println("  -----");
//                for (String line : result.getTrace()) {
//                    System.out.println("  " + line);
//                }
//
//                System.out.println();
//                System.out.println("================================================");
//                if (result.isAcyclic()) {
//                    System.out.println("  RESULT: YES -- the graph is acyclic.");
//                    System.out.println();
//                    System.out.println("  Elimination order: " + result.getEliminationOrder());
//                } else {
//                    System.out.println("  RESULT: NO -- the graph is cyclic.");
//                    System.out.println();
//                    System.out.println("  Vertices remaining : " + result.getRemainingVertices());
//                    System.out.println("  Cycle found        : " + CycleChecker.formatCycle(result.getCycle()));
//                }
//                System.out.println("================================================");
//            } else {
//                BenchmarkRunner.runDoublingHypothesis();
//            }
//
//        } catch (Exception e) {
//            System.out.println("================================================");
//            System.out.println("  ERROR");
//            System.out.println("  -----");
//            System.out.println("  " + e.getMessage());
//            System.out.println("================================================");
//            System.exit(1);
//        }
//    }
//}

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("================================================");
            System.out.println("               GRAPH CYCLE CHECKER              ");
            System.out.println("================================================");
            System.out.println("Choose an option:");
            System.out.println("1. Run doubling hypothesis benchmark");
            System.out.println("2. Check a graph file");
            System.out.println("================================================");
            System.out.print("Enter your choice (1 or 2): ");

            int mainChoice = scanner.nextInt();
            scanner.nextLine();

            switch (mainChoice) {
                case 1:
                    BenchmarkRunner.runDoublingHypothesis();
                    break;

                case 2:
                    runGraphCheckMenu(scanner);
                    break;

                default:
                    System.out.println("Invalid choice. Please run the program again and enter 1 or 2.");
                    break;
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

    private static void runGraphCheckMenu(Scanner scanner) throws Exception {
        System.out.println();
        System.out.println("Choose the input graph:");
        System.out.println("1. graph1.txt");
        System.out.println("2. cyclic.txt");
        System.out.println("3. acyclic.txt");
        System.out.print("Enter your choice (1, 2, or 3): ");

        int graphChoice = scanner.nextInt();
        scanner.nextLine();

        String filename;

        switch (graphChoice) {
            case 1:
                filename = "graph1.txt";
                break;
            case 2:
                filename = "cyclic.txt";
                break;
            case 3:
                filename = "acyclic.txt";
                break;
            default:
                System.out.println("Invalid graph choice. Please run the program again and enter 1, 2, or 3.");
                return;
        }

        Graph graph = GraphParser.parse(filename);
        CycleChecker.AnalysisResult result = CycleChecker.analyze(graph);

        System.out.println();
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
    }
}