package graph;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

/*
 * Author: Emily Hazen, Rachel Froling
 * Date: 04/03/16
 * 
 * Wrapper class that handles graphs for algorithms and graphs for display
 */

public class MyGraph extends MultiGraph {
	public MyGraph (String id){
		// generates a graph without strictChecking and with autoCreate
		// allows to add edges without initiated nodes; initiates nodes automatically
		super(id, false, true);     
	}

	/**
	 * @return list of all edges in the graph
	 */

	public List<Edge> getEdgeList() {
		Iterator<Edge> it = this.getEdgeIterator();
		List<Edge> toReturn = new ArrayList<Edge>();

		while(it.hasNext()) {
			toReturn.add(it.next());
		}

		return toReturn;
	}



	/**
	 * Returns a graph loaded from a file
	 * The file is a list of edges, one edge per line
	 * it should be formatted <source> <destination> <weight>
	 * @param filename the name of the file to load the graph from
	 * @param graphId the id to pass into the multigraph constructor
	 * @return a new graph loaded from the file
	 */
	public static MyGraph loadGraphFromFile( String filename, String graphId )
			throws FileNotFoundException {
		Scanner inputScanner = new Scanner( new File( filename ) );

		// construct the new multigraph
		MyGraph graph = new MyGraph( graphId );
		graph.addAttribute("ui.stylesheet", styleSheet); // build graph

		Integer edgeId = 0;

		while ( inputScanner.hasNext() ) {
			// grab the 2 node id's
			String nodeId1 = inputScanner.next();
			String nodeId2 = inputScanner.next();

			Edge newEdge = graph.addEdge( edgeId.toString(), nodeId1, nodeId2 );
			newEdge.setAttribute("weight", inputScanner.nextInt() );
			newEdge.setAttribute("reservations", new ArrayList<Integer>()); // initialize reservations, to be used in the improved case
			edgeId++;

		}
		inputScanner.close();

		// give each node a label for display
		// all nodes are immediately available
		for(Node node : graph) {
			node.addAttribute("ui.label", node.getId());
		}

		return graph;
	}


	public static MyGraph generateGraph(int numNodes, int numConnections, String graphId)throws IOException {
		Random r = new Random(Long.getLong("seed", System.nanoTime()));
		String filename = "src/graphs/RandGraph_" + numNodes + "_" + numConnections + ".txt";
		//a new bufferedwriter to write to file
		BufferedWriter bwtemp = new BufferedWriter(new FileWriter(filename));
		for(int c = 0; c < numConnections; c++) {
			for(int i = 0; i < numNodes; i++) {
				int source = i;
				int destination = r.nextInt(numNodes);
				while(destination == source) {
					destination = r.nextInt(numNodes);
				}
				int weight = r.nextInt(numNodes/2) + 1;
				String line = source + " " + destination + " " + weight;
				bwtemp.write(line);
				bwtemp.newLine();
			}
		}
		bwtemp.close();

		return loadGraphFromFile(filename, graphId);
	}

	// CSS for GraphStream
	protected static String styleSheet =
			"node {" +
					"fill-color: black;" +
					"text-alignment: under;" +
					"}" +
					"edge {" +
					"size: 2px;" +
					"}" +
					"edge.locked {" +
					"fill-color: red;" +
					"}" +
					"sprite {" +
					"fill-color: red;" +
					"text-alignment: above;" + 
					"}" +
					"sprite.delayed {" +
					"fill-color: yellow;" +
					"}";
}
