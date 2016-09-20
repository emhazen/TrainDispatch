package main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;


/**
 * Dijkstra's algorithm to find shortest path
 * @author Emily Hazen
 * references: http://en.literateprograms.org/index.php?title=Special%3aDownloadCode/Dijkstra%27s_algorithm_%28Java%29&oldid=15444
 *
 */
public class Dijkstra {
    Node source;
 
    public Dijkstra(Node source) {
        this.source = source;
    }

	/**
	 * Compute dijkstra's looking only at unlocked paths
	 */
	public void computeUnlockedPaths()
    {
		// set the minimum distance on all nodes in the graph to be the maximum value
		for(Node node : source.getGraph()) {
        	node.addAttribute("minDistance", Integer.MAX_VALUE);
        	node.addAttribute("ui.label", node.getId());
        	node.removeAttribute("previous");
        }

        source.setAttribute("minDistance", 0);
        PriorityQueue<Node> nodeQueue = new PriorityQueue<Node>(10, new CompareNode());
        nodeQueue.add(source);

        while (!nodeQueue.isEmpty()) {
            Node next = nodeQueue.poll();

            // Visit each edge exiting next
            for (Edge edge : getAdjacencies(next))
            {
            	// if the edge is locked, skip this edge
            	if (edge.getAttribute("ui.class") == "locked") {
            		continue;
            	}

                Node target = edge.getOpposite(next);
                int weight = edge.getAttribute("weight");
                int distanceThrough = (int)next.getAttribute("minDistance") + weight;
                if (distanceThrough < (int)target.getAttribute("minDistance")) {
                    nodeQueue.remove(target);

                    target.setAttribute("minDistance", distanceThrough);
                    target.addAttribute("previous", next);
                    nodeQueue.add(target);
                }
            }
        }
    }
	
	/**
	 * Compute dijkstra's looking at locked and unlocked paths
	 * Add the delay time of all locked edges to the weight of that edge
	 * The total cost of a train depends on the weight of all the edges plus the total delay
	 */
	public void computeAllPaths(int globalTime)
    {
		// set the minimum distance on all nodes in the graph to be the maximum value
		for(Node node : source.getGraph()) {
        	node.addAttribute("minDistance", Integer.MAX_VALUE);
        	node.addAttribute("ui.label", node.getId());
        	node.removeAttribute("previous");
        }

        source.setAttribute("minDistance", 0);
        // train is ready at the start immediately at global time
        source.setAttribute("ready", globalTime);

        PriorityQueue<Node> nodeQueue = new PriorityQueue<Node>(10, new CompareNode());
        nodeQueue.add(source);
        
        while (!nodeQueue.isEmpty()) {
            Node next = nodeQueue.poll();

            // Visit each edge exiting next
            for (Edge edge : getAdjacencies(next))
            {
                Node target = edge.getOpposite(next);
                
                if (!edge.hasAttribute("available")) {
                	edge.setAttribute("available", globalTime); // set availability to default: globalTime
                }

                // check reservation conflicts at the time the train intends to be there
                checkReservations(edge, (int)next.getAttribute("ready"));

                int cost = (int)edge.getAttribute("weight");
                // the cost of an edge should also include the time spent waiting for it
                int nodeReady = next.getAttribute("ready"); // time at which the node is ready
                int edgeAvailable = edge.getAttribute("available"); // time at which the edge is available
                if (edgeAvailable > nodeReady) { cost += edgeAvailable - nodeReady; }

                int distanceThrough = (int)next.getAttribute("minDistance") + cost;
                if (distanceThrough < (int)target.getAttribute("minDistance")) {
                    nodeQueue.remove(target);

                    target.setAttribute("minDistance", distanceThrough);
                    target.addAttribute("previous", next);
 
                    // set the time at which the train is ready for the next path at the cost of traversing that path
                    target.setAttribute("ready", cost);
                    nodeQueue.add(target);
                }
            }
        }
    }
	
	/**
	 * get all edges adjacent to node
	 * @param node
	 * @return list of edges
	 */
	public static List<Edge> getAdjacencies(Node node) {
		Iterator<Edge> it = node.getEdgeIterator();
		ArrayList<Edge> toReturn = new ArrayList<Edge>();
		
		while(it.hasNext()) {
			toReturn.add(it.next());
		}
		
		return toReturn;
	}

	/**
	 * Build the shortest path using the previous attribute on each node in the target
	 * @param target target node
	 * @return path of Node to target
	 */
	public List<Node> getShortestPathTo(Node target)
	{
		List<Node> path = new ArrayList<Node>();
		for (Node node = target; node != null; node = node.getAttribute("previous")) {
			path.add(node);
		}
		Collections.reverse(path);
		return path;
	}

    /**
     * Makes an edge path based on the shortest path
     * Returns null if no path exists
     * @param target - the target node
     * @return a list of edges the train will traverse
     */
    public List<Edge> getEdgePath(Node target) {

        List<Node> path = new ArrayList<Node>();
        path = getShortestPathTo(target);
        if (path == null) {
            return null;
        }

        List<Edge> edgePath = new ArrayList<Edge>();
        
        for (int i = 0; i < path.size() - 1; i++) {
            edgePath.add(path.get(i).getEdgeBetween(path.get(i+1)));
        }

        return edgePath;
    }
    
    /**
	 * If the train is reserved at any time, checks that there are no conflicts 
	 * with the arrival time.
	 *
	 * If arrivalTime <= reservation time <= arrivalTime + weight
	 * OR reservation time <= arrival time <= reservationTime + weight
	 * Then finds the next available time in the reservations that the train could potentially use it.
	 * 
	 * If there is a time slot where a train could traverse the whole path 
	 * without interfering with other reservations, set the delay of that 
	 * edge to the open time slot.
	 * 
	 * @param path - the path we wish to check
	 * @return the path after the check - may be changed to accommodate reservations
	 */
	public static void checkReservations(Edge edge, int arrivalTime) {
		List<Integer> reservations = edge.getAttribute("reservations");
		int weight = edge.getAttribute("weight");

		edge.setAttribute("available", arrivalTime);
		if (!reservations.isEmpty()) {
			for (int i = 0; i < reservations.size(); i++) {
				if (arrivalTime <= reservations.get(i) && reservations.get(i) <= arrivalTime + weight) {
					for (int j = i; j < reservations.size() - 1; j++) {
						if (reservations.get(j+1) - (reservations.get(j) + weight) >= weight) {
							edge.setAttribute("available", reservations.get(j) + weight);
							return;
						}
					}
					// if there are no time periods available within reservations, set the availability to after
					edge.setAttribute("available", reservations.get(reservations.size() - 1) + weight);
					return;
				}
				if (reservations.get(i) <= arrivalTime && reservations.get(i) + weight >= arrivalTime) {
					for (int j = i; j < reservations.size() - 1; j++) {
						if (reservations.get(j) - (reservations.get(j+1) + weight) == weight) {
							edge.setAttribute("available", reservations.get(j) + weight);
							break;
						}
					}
					// if there are no time periods available within reservations, set the availability to after
					edge.setAttribute("available", reservations.get(reservations.size() - 1) + weight);
				}

			}
		}
	}

} 
