import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {
    private final Map<Integer, Set<Integer>> outgoing;
    private final Map<Integer, Set<Integer>> incoming;
    private final Map<Integer, Integer> outDegree;

    public Graph() {
        this.outgoing = new LinkedHashMap<>();
        this.incoming = new LinkedHashMap<>();
        this.outDegree = new HashMap<>();
    }

    public void addVertex(int vertex) {
        outgoing.putIfAbsent(vertex, new LinkedHashSet<>());
        incoming.putIfAbsent(vertex, new LinkedHashSet<>());
        outDegree.putIfAbsent(vertex, 0);
    }

    public void addVertices(Collection<Integer> vertices) {
        for (int vertex : vertices) {
            addVertex(vertex);
        }
    }

    public void addEdge(int from, int to) {
        addVertex(from);
        addVertex(to);

        if (outgoing.get(from).add(to)) {
            incoming.get(to).add(from);
            outDegree.put(from, outDegree.get(from) + 1);
        }
    }

    public boolean containsVertex(int vertex) {
        return outgoing.containsKey(vertex);
    }

    public int vertexCount() {
        return outgoing.size();
    }

    public int edgeCount() {
        int edges = 0;
        for (int degree : outDegree.values()) {
            edges += degree;
        }
        return edges;
    }

    public boolean isEmpty() {
        return outgoing.isEmpty();
    }

    public Set<Integer> getVertices() {
        return new LinkedHashSet<>(outgoing.keySet());
    }

    public Set<Integer> getOutgoingNeighbours(int vertex) {
        return new LinkedHashSet<>(outgoing.getOrDefault(vertex, Collections.emptySet()));
    }

    public Set<Integer> getIncomingNeighbours(int vertex) {
        return new LinkedHashSet<>(incoming.getOrDefault(vertex, Collections.emptySet()));
    }

    public int getOutDegree(int vertex) {
        return outDegree.getOrDefault(vertex, 0);
    }

    public List<Integer> getSinks() {
        List<Integer> sinks = new ArrayList<>();
        for (int vertex : outgoing.keySet()) {
            if (getOutDegree(vertex) == 0) {
                sinks.add(vertex);
            }
        }
        Collections.sort(sinks);
        return sinks;
    }

    public void removeVertex(int vertex) {
        if (!containsVertex(vertex)) {
            return;
        }

        Set<Integer> predecessors = new LinkedHashSet<>(incoming.get(vertex));
        for (int predecessor : predecessors) {
            if (outgoing.get(predecessor).remove(vertex)) {
                outDegree.put(predecessor, outDegree.get(predecessor) - 1);
            }
        }

        Set<Integer> successors = new LinkedHashSet<>(outgoing.get(vertex));
        for (int successor : successors) {
            incoming.get(successor).remove(vertex);
        }

        outgoing.remove(vertex);
        incoming.remove(vertex);
        outDegree.remove(vertex);
    }

    public Graph deepCopy() {
        Graph copy = new Graph();
        copy.addVertices(getVertices());
        for (int vertex : outgoing.keySet()) {
            for (int neighbour : outgoing.get(vertex)) {
                copy.addEdge(vertex, neighbour);
            }
        }
        return copy;
    }
}
