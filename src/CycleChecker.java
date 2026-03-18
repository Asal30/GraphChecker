import java.util.*;

public class CycleChecker {
    private static final int WHITE=0, GREY=1, BLACK=2;
    private Map<Integer,Integer> colour;
    private Map<Integer,Integer> parent;
    private int cycleStart=-1, cycleEnd=-1;

    private boolean dfs(int v, Graph g) {
        colour.put(v, GREY);
        for (int nb : g.getOutNeighbours(v)) {
            if (colour.getOrDefault(nb,WHITE) == GREY) {
                cycleEnd=v; cycleStart=nb;
                return true;
            }
            if (colour.getOrDefault(nb,WHITE) == WHITE) {
                parent.put(nb, v);
                if (dfs(nb, g)) return true;
            }
        }
        colour.put(v, BLACK);
        return false;
    }

    public List<Integer> findCycle(Graph g) {
        colour=new HashMap<>(); parent=new HashMap<>();
        cycleStart=-1; cycleEnd=-1;

        for (int v : g.getVertices()) colour.put(v, WHITE);

        for (int v : g.getVertices()) {
            if (colour.get(v)==WHITE) {
                if (dfs(v,g)) break;
            }
        }

        if (cycleStart==-1) return null;

        List<Integer> cycle=new ArrayList<>();
        cycle.add(cycleStart);
        int cur=cycleEnd;
        while (cur!=cycleStart) {
            cycle.add(cur);
            cur=parent.get(cur);
        }
        cycle.add(cycleStart);
        Collections.reverse(cycle);
        return cycle;
    }

    public static boolean isAcyclic(Graph graph) {
        System.out.println("=== Starting Sink Elimination ===");

        while (true) {
            // Rule 1: empty graph — no cycle possible
            if (graph.isEmpty()) {
                System.out.println("Graph is empty -> ACYCLIC (YES)");
                return true;
            }

            // Rule 2: look for a sink
            int sink = graph.findSink();

            if (sink == -1) {
                // Rule 3: no sink found — cycle must exist
                System.out.println("No sink found -> CYCLE EXISTS (NO)");
                return false;
            }
            // Rule 4: remove the sink and repeat
            System.out.println("Found sink: " + sink + " -> removing it");
            graph.removeVertex(sink);
        }
    }

}
