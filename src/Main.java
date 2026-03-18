import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {
        Graph graph = GraphParser.parse("graph1.txt");
        Graph graph_copy = GraphParser.parse("graph1.txt"); // keep for cycle search

        CycleChecker checker = new CycleChecker();
        boolean acyclic = CycleChecker.isAcyclic(graph);

        if (acyclic) {
            System.out.println("Answer: YES — the graph is acyclic.");
        } else {
            System.out.println("Answer: NO — the graph has a cycle.");
            List<Integer> cycle = checker.findCycle(graph_copy);
            if (cycle != null) {
                System.out.print("Cycle found: ");
                System.out.println(cycle.stream().map(String::valueOf)
                        .collect(Collectors.joining(" ->  ")));
            }
        }
    }
}