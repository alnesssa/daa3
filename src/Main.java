import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import java.io.*;
import java.util.*;

public class Main {

    // Data structures
    static class Edge {
        String from, to;
        int weight;
        Edge(String f, String t, int w) {
            from = f; to = t; weight = w;
        }
    }

    static class Graph {
        int id;
        List<String> nodes;
        List<Edge> edges;
    }

    static class InputData {
        List<Graph> graphs;
    }

    static class Result {
        @SerializedName("graph_id")
        int graphId;
        @SerializedName("input_stats")
        Map<String, Integer> inputStats;
        AlgorithmResult prim;
        AlgorithmResult kruskal;
    }

    static class AlgorithmResult {
        @SerializedName("mst_edges")
        List<Edge> mstEdges;
        @SerializedName("total_cost")
        int totalCost;
        @SerializedName("operations_count")
        int operationsCount;
        @SerializedName("execution_time_ms")
        double executionTimeMs;
    }

    // Main
    public static void main(String[] args) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Read input
        try (FileReader reader = new FileReader("input.json")) {
            InputData input = gson.fromJson(reader, InputData.class);
            List<Result> results = new ArrayList<>();

            // Process each graph
            for (Graph g : input.graphs) {
                Result result = new Result();
                result.graphId = g.id;
                result.inputStats = Map.of(
                        "vertices", g.nodes.size(),
                        "edges", g.edges.size()
                );

                // Prim
                long start = System.nanoTime();
                AlgorithmResult primResult = primMST(g);
                primResult.executionTimeMs = (System.nanoTime() - start) / 1_000_000.0;
                result.prim = primResult;

                // Kruskal
                start = System.nanoTime();
                AlgorithmResult kruskalResult = kruskalMST(g);
                kruskalResult.executionTimeMs = (System.nanoTime() - start) / 1_000_000.0;
                result.kruskal = kruskalResult;

                // Check MST cost consistency
                if (primResult.totalCost != kruskalResult.totalCost) {
                    System.out.println("⚠️ Warning: MST costs differ in Graph " + g.id);
                }

                results.add(result);
            }

            // Write output
            Map<String, Object> output = Map.of("results", results);
            try (FileWriter fw = new FileWriter("output.json")) {
                gson.toJson(output, fw);
            }

            System.out.println("✅ Results written to output.json");
        }
    }

    // Prim's Algorithm
    private static AlgorithmResult primMST(Graph g) {
        AlgorithmResult res = new AlgorithmResult();
        List<Edge> mst = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<Edge> edges = new ArrayList<>(g.edges);
        int operations = 0;
        int totalCost = 0;

        if (g.nodes.isEmpty()) return res;

        String start = g.nodes.get(0);
        visited.add(start);

        while (visited.size() < g.nodes.size()) {
            Edge minEdge = null;
            int minWeight = Integer.MAX_VALUE;
            for (Edge e : edges) {
                operations++;
                boolean fromIn = visited.contains(e.from);
                boolean toIn = visited.contains(e.to);
                if (fromIn ^ toIn) {
                    if (e.weight < minWeight) {
                        minWeight = e.weight;
                        minEdge = e;
                    }
                }
            }
            if (minEdge == null) break;
            mst.add(minEdge);
            totalCost += minEdge.weight;
            visited.add(minEdge.from);
            visited.add(minEdge.to);
        }

        res.mstEdges = mst;
        res.totalCost = totalCost;
        res.operationsCount = operations;
        return res;
    }

    // Kruskal's Algorithm
    private static AlgorithmResult kruskalMST(Graph g) {
        AlgorithmResult res = new AlgorithmResult();
        List<Edge> edges = new ArrayList<>(g.edges);
        edges.sort(Comparator.comparingInt(e -> e.weight));

        Map<String, String> parent = new HashMap<>();
        for (String node : g.nodes) parent.put(node, node);

        int totalCost = 0, operations = 0;
        List<Edge> mst = new ArrayList<>();

        for (Edge e : edges) {
            operations++;
            String root1 = find(parent, e.from);
            String root2 = find(parent, e.to);
            if (!root1.equals(root2)) {
                mst.add(e);
                totalCost += e.weight;
                parent.put(root1, root2);
            }
        }

        res.mstEdges = mst;
        res.totalCost = totalCost;
        res.operationsCount = operations;
        return res;
    }

    private static String find(Map<String, String> parent, String node) {
        if (!parent.get(node).equals(node))
            parent.put(node, find(parent, parent.get(node)));
        return parent.get(node);
    }
}
