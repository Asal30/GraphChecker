import java.util.*;

public class Graph {
    private final Map<Integer, Set<Integer>> outEdges;

    public Graph() {
        outEdges = new HashMap<>();
    }
    public void addVertex(int v) {
        outEdges.putIfAbsent(v, new HashSet<>());
    }

    public void addEdge(int from, int to) {
        addVertex(from);
        addVertex(to);
        outEdges.get(from).add(to);
    }

    public int findSink() {
        for (int vertex : outEdges.keySet()) {
            if (outEdges.get(vertex).isEmpty()) {
                return vertex;
            }
        }
        return -1;
    }

    public void removeVertex(int v) {
        outEdges.remove(v);
        for (Set<Integer> neighbours : outEdges.values()) {
            neighbours.remove(v);
        }
    }
    public boolean isEmpty() { return outEdges.isEmpty(); }

    public Set<Integer> getVertices() {
        return new HashSet<>(outEdges.keySet());
    }

    public Set<Integer> getOutNeighbours(int v) {
        return outEdges.getOrDefault(v, new HashSet<>());
    }
}

