import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GraphParser {
    private GraphParser() {
    }

    public static Graph parse(String filename) throws IOException {
        List<String> lines = readMeaningfulLines(filename);
        if (lines.isEmpty()) {
            throw new IOException("Input file is empty.");
        }

        Graph graph = new Graph();
        int startIndex = 0;
        String firstLine = lines.get(0);
        String[] firstParts = firstLine.split("\\s+");

        if (firstParts.length == 1) {
            int vertexCount = parseInteger(firstParts[0], 1, firstLine);
            if (vertexCount < 0) {
                throw new IOException("Invalid vertex count at line 1: must be non-negative.");
            }
            for (int vertex = 0; vertex < vertexCount; vertex++) {
                graph.addVertex(vertex);
            }
            startIndex = 1;
        } else if (firstParts.length != 2) {
            throw new IOException("Invalid format at line 1: expected either 1 integer or 2 integers.");
        }

        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split("\\s+");
            int lineNumber = i + 1;

            if (parts.length != 2) {
                throw new IOException("Invalid format at line " + lineNumber + ": expected 2 integers.");
            }

            int from = parseInteger(parts[0], lineNumber, line);
            int to = parseInteger(parts[1], lineNumber, line);

            if (graph.vertexCount() > 0) {
                validateVertexInRange(from, graph.vertexCount(), lineNumber);
                validateVertexInRange(to, graph.vertexCount(), lineNumber);
            }

            graph.addEdge(from, to);
        }

        return graph;
    }

    private static List<String> readMeaningfulLines(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                    continue;
                }
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private static int parseInteger(String token, int lineNumber, String line) throws IOException {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid integer at line " + lineNumber + ": " + line);
        }
    }

    private static void validateVertexInRange(int vertex, int vertexCount, int lineNumber) throws IOException {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IOException(
                    "Vertex out of range at line " + lineNumber + ": " + vertex +
                            " is not in [0, " + (vertexCount - 1) + "]");
        }
    }
}
