package main;

import java.util.Comparator;

import org.graphstream.graph.Node;

public class CompareNode implements Comparator<Node>{

	@Override
	public int compare(Node node1, Node node2) {
		return Integer.compare((Integer)node1.getAttribute("minDistance"), (Integer)node2.getAttribute("minDistance"));
	}

}
