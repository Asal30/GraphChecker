import java.io.*;
import java.util.Scanner;

public class GraphParser {
    public static Graph parse(String filename) throws IOException {
        Graph graph = new Graph();
        Scanner scanner = new Scanner(new File(filename));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(" +");
            int from = Integer.parseInt(parts[0]);
            int to   = Integer.parseInt(parts[1]);
            graph.addEdge(from, to);
        }

        scanner.close();
        return graph;
    }
}
