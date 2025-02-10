package cz.ctu.fee.dsv.grpc.lomet;

import cz.ctu.fee.dsv.grpc.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Graph {
    final Logger logger = LoggerFactory.getLogger(Graph.class);

    private final Map<GraphNode, List<GraphNode>> adjacencyList;

    // Constructor to initialize the adjacency list
    public Graph() {
        adjacencyList = new HashMap<>();
    }

    public void processPreliminaryRequest(GraphNode preliminaryRequester, GraphNode lastRequester, GraphNode resourceNode){
        logger.info("Preliminary request in graph. preliminaryRequester:{}; lasstRequester:{}; resource:{}\n", preliminaryRequester, lastRequester, resourceNode);
        GraphNode currentNode = lastRequester;
        List<GraphNode> neighboursOfPreviousNode = null;
        while(true){
            logger.info("current node: {}; neighboursOfPreviousNode: {}", currentNode, neighboursOfPreviousNode);
            if (preliminaryRequester.getTimestamp() < currentNode.getTimestamp()){
                List<GraphNode> neighboursWithoutResource = adjacencyList.get(currentNode)
                        .stream().filter(n -> !n.equals(resourceNode)).collect(Collectors.toList());
                if (neighboursWithoutResource.isEmpty()){
                    addEdge(currentNode, preliminaryRequester);
                    break;
                } else {
                    currentNode = neighboursWithoutResource.get(0);
                    neighboursOfPreviousNode = neighboursWithoutResource;
                }
            } else if (preliminaryRequester.getTimestamp() > currentNode.getTimestamp()){
                /* changing dependant nodes of currentNode to become dependant on preliminaryRequester */
                if (neighboursOfPreviousNode != null) {
                    /* putting node after node with earlier timestamp */
                    for (GraphNode neighbourOfPreviousNode : neighboursOfPreviousNode){
                        addEdge(neighbourOfPreviousNode, preliminaryRequester);
                        removeEdge(neighbourOfPreviousNode, currentNode);
                    }
                }
                /* preliminaryRequester->currentNode */
                addEdge(preliminaryRequester, currentNode);
                break;
            } else { /* compare based on node id */
                if (preliminaryRequester.getId().compareTo(currentNode.getId()) < 0) {
                    List<GraphNode> neighboursWithoutResource = adjacencyList.get(currentNode)
                            .stream().filter(n -> !n.equals(resourceNode)).collect(Collectors.toList());
                    if (neighboursWithoutResource.isEmpty()){
                        addEdge(currentNode, preliminaryRequester);
                        logger.info("< New dependency {}->{}", currentNode, preliminaryRequester);
                        break;
                    } else {
                        currentNode = neighboursWithoutResource.get(0);
                        neighboursOfPreviousNode = neighboursWithoutResource;
                    }
                } else {
                    /* changing dependant nodes of currentNode to become dependant on preliminaryRequester */
                    if (neighboursOfPreviousNode != null) {
                        /* putting node after node with earlier timestamp */
                        for (GraphNode neighbourOfPreviousNode : neighboursOfPreviousNode){
                            addEdge(neighbourOfPreviousNode, preliminaryRequester);
                            removeEdge(neighbourOfPreviousNode, currentNode);
                        }
                    }
                    /* preliminaryRequester->currentNode */
                    addEdge(preliminaryRequester, currentNode);
                    logger.info("> New dependency {}->{}", preliminaryRequester, currentNode);
                    break;
                }
            }
        }
    }

    // Method to add a new vertex to the graph
    public void addVertex(GraphNode vertex) {
        adjacencyList.put(vertex, new ArrayList<>());
    }

    // Method to add an edge between two vertices
    public void addEdge(GraphNode source, GraphNode destination) {
        List<GraphNode> neighbors = adjacencyList.get(source);
        if (!neighbors.contains(destination)) { // Prevent duplicate edges
            neighbors.add(destination);
        }
    }

    // Method to remove a vertex from the graph
    public void removeVertex(GraphNode vertex) {
        adjacencyList.remove(vertex);
        // Remove the vertex from the neighbors of other vertices
        for (List<GraphNode> neighbors : adjacencyList.values()) {
            neighbors.remove(vertex);
        }
    }

    // Method to remove an edge between two vertices
    public void removeEdge(GraphNode source, GraphNode destination) {
        adjacencyList.get(source).remove(destination);
    }

    // Method to get the neighbors of a vertex
    public List<GraphNode> getNeighbors(GraphNode vertex) {
        return adjacencyList.get(vertex);
    }

    // Method to print the graph
    public void printGraph() {
        for (Map.Entry<GraphNode, List<GraphNode>> entry : adjacencyList.entrySet()) {
            System.out.print(entry.getKey() + " -> ");
            for (GraphNode neighbor : entry.getValue()) {
                System.out.print(neighbor + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public String getStringGraph() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<GraphNode, List<GraphNode>> entry : adjacencyList.entrySet()) {
            sb.append(entry.getKey()).append(" -> [");
            for (GraphNode neighbor : entry.getValue()) {
                sb.append(neighbor).append(", ");
            }
            sb.append("]\n");
        }
        return sb.toString();
    }

    // Method to detect cycles in the graph
    public boolean hasCycle() {
        Set<GraphNode> visited = new HashSet<>();
        Set<GraphNode> recursionStack = new HashSet<>();

        // Check for cycles starting from each unvisited vertex
        for (GraphNode node : adjacencyList.keySet()) {
            if (hasCycleUtil(node, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleUtil(GraphNode node, Set<GraphNode> visited, Set<GraphNode> recursionStack) {
        // If node is already in recursion stack, we found a cycle
        if (recursionStack.contains(node)) {
            return true;
        }

        // If node is already visited and not in recursion stack, no cycle through this path
        if (visited.contains(node)) {
            return false;
        }

        // Add the node to both visited and recursion stack
        visited.add(node);
        recursionStack.add(node);

        // Visit all neighbors
        for (GraphNode neighbor : adjacencyList.get(node)) {
            if (hasCycleUtil(neighbor, visited, recursionStack)) {
                return true;
            }
        }

        // Remove the node from recursion stack as we're done exploring all paths through it
        recursionStack.remove(node);
        return false;
    }

}