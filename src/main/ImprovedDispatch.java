package main;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.ui.spriteManager.Sprite;

import graph.MyGraph;
import schedule.Schedule;

public class ImprovedDispatch extends TrainDispatch {
	public ImprovedDispatch(MyGraph g) {
		super(g);
	}
	
	/**
     * Improved Run of train dispatch
     * @param filename - name of the schedule file. only used for testing
     * @throws FileNotFoundException
     */
	@Override
	public void dispatch(String filename) throws FileNotFoundException {
	    PriorityQueue<Schedule> scheduleQueue = Schedule.loadTrainsfromFile(graph, filename); // determines in what order trains should be dispatched
	    
	    while(!scheduleQueue.isEmpty()) {
	    	Schedule next = scheduleQueue.peek();
	    	// if the next train to be dispatched is not yet ready, increment time and continue
	    	if (next.getDispatchTime() > globalTime) {
	    		timeTravel();
	    		continue;
	    	}
	    	Dijkstra d = new Dijkstra(next.getSource());
	    	d.computeAllPaths(globalTime);
	    	
		    List<Edge> path = d.getEdgePath(next.getDestination());
		    List<Node> nodePath = d.getShortestPathTo(next.getDestination());

		    if (!path.isEmpty()) {
		    	// Reserve all edges in the path and add these edges to the map
		        // keep track of the schedule's personal reservations for each edge
		    	
		    	List<Integer> myReservations = new ArrayList<Integer>();
		        
		        int reserve;
		        int arrival = globalTime;
		        for(int i = 0; i < path.size(); i++) {
		        	Edge edge = path.get(i);
		        	// reset and check availability of edge
		        	edge.setAttribute("available", arrival);
		        	Dijkstra.checkReservations(edge, arrival);

		        	reserve = edge.getAttribute("available"); // time of reservation
		        	
		        	List<Integer> reservations = edge.getAttribute("reservations");
		        	makeReservation(reservations, reserve);
		        	myReservations.add(reserve);

		        	// train arrives at the end of path at previous arrival + time of delay + weight of edge
		        	arrival += (reserve - arrival) + (int)edge.getAttribute("weight");
		        }
		        

		        Sprite sprite = addSprite(next, path, nodePath);
		        sprite.addAttribute("reservations", myReservations);
		        
		        // if the sprite must wait, mark delayed, otherwise lock its first edge
		        if (myReservations.get(0) != globalTime) { 
		        	sprite.setAttribute("ui.class", "delayed"); 
		        } else {
		        	sprite.setAttribute("ui.class", "moving");
		        	path.get(0).setAttribute("ui.class", "locked");
		        }

		        // this train has been dispatched; remove from queue
		        System.out.println("Dispatching " + scheduleQueue.poll() + " on train " + sprite.getId() + " at " + globalTime);
		    }
		    // only move forward in time if there are no more trains that can be dispatched at the current time
		    if (scheduleQueue.isEmpty() || scheduleQueue.peek().getDispatchTime() > globalTime || path.isEmpty()) {
		    	timeTravel();
		    }
	    }
	    
	    while (hasLocked() || hasDelayed()) {
	    	timeTravel();
	    }
	    
	}

	/**
	 * Insert the reservation in place
	 * @param reservations - list of reservations on that edge
	 * @param reserve - time to reserve at
	 */
	private void makeReservation(List<Integer> reservations, int reserve) {
		for (int i = 0; i < reservations.size(); i++) {
			if(reservations.get(i) > reserve) {
				reservations.add(i, reserve);
				return;
			}
		}
		reservations.add(reserve);
	}

	/**
	 * Checks if there are any delayed trains
	 * @return true if trains are delayed
	 */
	private boolean hasDelayed() {
		for(Sprite sprite : sman) {
			if(sprite.getAttribute("ui.class") == "delayed") { return true; }
		}
		return false;
	}

	/**
	 * Moves sprites forward on their track.
	 */
	@Override
	protected void moveTrains() {
		//move each train 1/10th of the way on the track every 100 ms
		for(int i = 1; i <= 10; i++) {
			for (Sprite sprite : sman) {
				if(sprite.getAttribute("ui.class") != "delayed") {
					// distance to move should be 1 tenth of a length unit per second
					// keeping in mind where the node is currently on the edge and the weight of the edge
					int pathLength = sprite.getAttachment().getAttribute("weight");
					int location = sprite.getAttribute("location");

					// move backwards or forwards depending on direction of movement
					if(sprite.hasAttribute("fromSource")) {
						sprite.setPosition((i / 10.0 / pathLength) + location / (double)pathLength);
					} else {
						sprite.setPosition((location / (double)pathLength) - (i / 10.0 / pathLength));
					}
				}
			}
			sleep(10);
		}
	}

	/**
	 * Sets the position of a sprite to the next edge if it is at the end of its current edge.
	 * Otherwise, increments the location attribute of the sprite.
	 * If there are no more edges, remove the sprite. The train has arrived.
	 * 
	 * Delayed sprites will be checked AFTER moving sprites to prevent delayed sprites from locking
	 * edges before the edge is unlocked by the sprite currently on it.
	 */
	@Override
	public void shift() {
		List<Sprite> toRemove = new ArrayList<Sprite>();
		for(Sprite sprite : sman) {
			if(sprite.getAttribute("ui.class") != "delayed") {
				List<Edge> path = sprite.getAttribute("path");
				Edge lastEdge = (Edge)sprite.getAttachment();

				if(atEndEdge(sprite)) {
					lastEdge.setAttribute("ui.class", "unlocked"); // unlock edge

					// remove the sprite's reservation on that edge
					List<Integer> myReservations = sprite.getAttribute("reservations");
					
					myReservations.remove(0);

					// if the last edge it was on is not the last edge in the path
					if (path.indexOf(lastEdge) < path.size() - 1) {
						Edge next = path.get(path.indexOf(lastEdge) + 1);
						// determine if the next edge is available
						sprite.attachToEdge(next.getId());
						setDirection(sprite, getCurrentNode(sprite, lastEdge), (Edge)sprite.getAttachment());
						if (myReservations.get(0) == globalTime) {
							// attach and lock the next edge
							next.addAttribute("ui.class", "locked");
						} else {
							sprite.setAttribute("ui.class", "delayed");
						}

					} else {
						getTrainCost(sprite);
						toRemove.add(sprite);
					}
				} else {
					incrementLocation(sprite);
				}
			}
		}
		
		for (Sprite sprite : sman) {
			Edge lastEdge = (Edge)sprite.getAttachment();
			// if the sprite is currently delayed, determine if it is ready to go and continue to the next sprite.
			if (sprite.getAttribute("ui.class") == "delayed") {
				List<Integer> reservations = sprite.getAttribute("reservations");
				if (reservations.get(0) == globalTime) { 
					sprite.setAttribute("ui.class", "moving"); 
					// attach and lock the edge
					lastEdge.setAttribute("ui.class", "locked");
				}
			}
		}

		// remove necessary sprites
		for(Sprite sprite : toRemove) {
			sman.removeSprite(sprite.getId());
		}
	}
	
}
