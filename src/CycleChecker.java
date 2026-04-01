import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CycleChecker {
    private CycleChecker() {
    }

    public static AnalysisResult analyze(Graph originalGraph) {
        Graph workingGraph = originalGraph.deepCopy();
        Deque<Integer> sinkQueue = new ArrayDeque<>(workingGraph.getSinks());
        Set<Integer> queued = new HashSet<>(sinkQueue);

        List<String> trace = new ArrayList<>();
        List<Integer> eliminationOrder = new ArrayList<>();

        trace.add("Starting sink elimination.");
        trace.add("Initial graph: " + originalGraph.vertexCount() + " vertices, "
                + originalGraph.edgeCount() + " edges.");
        trace.add("Initial sinks: " + formatList(new ArrayList<>(sinkQueue)) + ".");

        while (!sinkQueue.isEmpty()) {
            int sink = sinkQueue.removeFirst();
            queued.remove(sink);

            if (!workingGraph.containsVertex(sink)) {
                continue;
            }
            if (workingGraph.getOutDegree(sink) != 0) {
                continue;
            }

            List<Integer> predecessors = new ArrayList<>(workingGraph.getIncomingNeighbours(sink));
            Collections.sort(predecessors);
            trace.add("Eliminating sink " + sink + ". Predecessors: " + formatList(predecessors) + ".");

            workingGraph.removeVertex(sink);
            eliminationOrder.add(sink);

            List<Integer> newSinks = new ArrayList<>();
            for (int predecessor : predecessors) {
                if (workingGraph.containsVertex(predecessor) && workingGraph.getOutDegree(predecessor) == 0) {
                    if (queued.add(predecessor)) {
                        sinkQueue.addLast(predecessor);
                        newSinks.add(predecessor);
                    }
                }
            }

            Collections.sort(newSinks);
            trace.add("New sinks added to queue: " + formatList(newSinks) + ".");
            trace.add("Remaining vertices: " + formatList(new ArrayList<>(workingGraph.getVertices())) + ".");
        }

        if (workingGraph.isEmpty()) {
            trace.add("All vertices were eliminated. The graph is acyclic.");
            return new AnalysisResult(true, eliminationOrder, null, trace,  originalGraph.vertexCount(), 0);
        }

        List<Integer> remainingVertices = new ArrayList<>(workingGraph.getVertices());
        Collections.sort(remainingVertices);
        trace.add("No sink remains, but vertices are still present: " + formatList(remainingVertices) + ".");
        trace.add("Therefore the graph is cyclic.");

        List<Integer> cycle = findCycle(workingGraph);
        if (cycle != null) {
            trace.add("Cycle found: " + formatCycle(cycle) + ".");
        } else {
            trace.add("A cycle should exist, but DFS did not reconstruct one.");
        }

        return new AnalysisResult(false, eliminationOrder, cycle, trace,
                originalGraph.vertexCount(), workingGraph.vertexCount());
    }

    public static List<Integer> findCycle(Graph graph) {
        Map<Integer, Integer> colour = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        for (int vertex : graph.getVertices()) {
            colour.put(vertex, 0);
        }

        CycleState state = new CycleState();
        List<Integer> vertices = new ArrayList<>(graph.getVertices());
        Collections.sort(vertices);

        for (int vertex : vertices) {
            if (colour.get(vertex) == 0 && dfs(vertex, graph, colour, parent, state)) {
                return reconstructCycle(parent, state.start, state.end);
            }
        }
        return null;
    }

    private static boolean dfs(int vertex, Graph graph, Map<Integer, Integer> colour,
                               Map<Integer, Integer> parent, CycleState state) {
        colour.put(vertex, 1);
        List<Integer> neighbours = new ArrayList<>(graph.getOutgoingNeighbours(vertex));
        Collections.sort(neighbours);

        for (int neighbour : neighbours) {
            int neighbourColour = colour.getOrDefault(neighbour, 0);
            if (neighbourColour == 0) {
                parent.put(neighbour, vertex);
                if (dfs(neighbour, graph, colour, parent, state)) {
                    return true;
                }
            } else if (neighbourColour == 1) {
                state.start = neighbour;
                state.end = vertex;
                return true;
            }
        }

        colour.put(vertex, 2);
        return false;
    }

    private static List<Integer> reconstructCycle(Map<Integer, Integer> parent, int start, int end) {
        List<Integer> cycle = new ArrayList<>();
        cycle.add(start);

        int current = end;
        while (current != start) {
            cycle.add(current);
            current = parent.get(current);
        }
        cycle.add(start);
        Collections.reverse(cycle);
        return cycle;
    }

    public static String formatCycle(List<Integer> cycle) {
        if (cycle == null || cycle.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cycle.size(); i++) {
            if (i > 0) {
                builder.append(" -> ");
            }
            builder.append(cycle.get(i));
        }
        return builder.toString();
    }

    private static String formatList(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        Collections.sort(values);
        return values.toString();
    }

    private static final class CycleState {
        private Integer start;
        private Integer end;
    }

    public static final class AnalysisResult {
        private final boolean acyclic;
        private final List<Integer> eliminationOrder;
        private final List<Integer> cycle;
        private final List<String> trace;
        private final int totalVertices;
        private final int remainingVertices;

        public AnalysisResult(boolean acyclic, List<Integer> eliminationOrder, List<Integer> cycle,
                              List<String> trace, int totalVertices, int remainingVertices) {
            this.acyclic = acyclic;
            this.eliminationOrder = new ArrayList<>(eliminationOrder);
            this.cycle = cycle == null ? null : new ArrayList<>(cycle);
            this.trace = new ArrayList<>(trace);
            this.totalVertices = totalVertices;
            this.remainingVertices = remainingVertices;
        }

        public boolean isAcyclic() {
            return acyclic;
        }

        public List<Integer> getEliminationOrder() {
            return new ArrayList<>(eliminationOrder);
        }

        public List<Integer> getCycle() {
            return cycle == null ? null : new ArrayList<>(cycle);
        }

        public List<String> getTrace() {
            return new ArrayList<>(trace);
        }

        public int getTotalVertices() {
            return totalVertices;
        }

        public int getRemainingVertices() {
            return remainingVertices;
        }
    }
}
