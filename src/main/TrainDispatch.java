package main;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;

import graph.MyGraph;
import schedule.Schedule;

public class TrainDispatch {
    protected static MyGraph graph; // graph used by display
    protected static int globalTime; //global time incremented by sleep()
    protected Viewer view;
    protected SpriteManager sman; // manages sprites on the graph
    private int spriteCount; // counter that iterates every time a sprite is add for naming purposes
    private List<Integer> trainCost;
    
    public TrainDispatch(MyGraph g) {
        graph = g;
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        view = graph.display(); // begin display
        sleep(2000); // allow the graph to finish building

        sman = new SpriteManager(graph);
        spriteCount = 0;
        globalTime = 0;
        trainCost = new ArrayList<Integer>();
    }
    

    /**
     * Base Run of train dispatch
     * @param filename - name of the schedule file. only used for testing
     * @throws FileNotFoundException
     */
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
	    	d.computeUnlockedPaths();
		    List<Edge> path = d.getEdgePath(next.getDestination());
		    List<Node> nodePath = d.getShortestPathTo(next.getDestination());
		    
		    if (!path.isEmpty()) {
		    	// lock all edges in the path
		        for (Edge edge : path) { edge.setAttribute("ui.class", "locked"); }
		        
		        addSprite(next, path, nodePath); // add the sprite for the schedule and its path	        
		        	        
		        System.out.println("Dispatching " + scheduleQueue.poll()); // this train has been dispatched; remove from queue

		    }
		    // only move forward in time if there are no more trains that can be dispatched at the current time
		    if (scheduleQueue.isEmpty() || scheduleQueue.peek().getDispatchTime() > globalTime || path.isEmpty()) {
		    	timeTravel();
		    }
	    }
	    
	    while (hasLocked()) {
	    	timeTravel();
	    }
	    
	}

	/**
	 * Attach a train sprite its starting edge
	 * @param schedule - train to create sprite for
	 * @param path - path the train will take
	 */
	protected Sprite addSprite(Schedule schedule, List<Edge> path, List<Node> nodePath) {
		Edge startEdge = path.get(0);
		Sprite sprite = sman.addSprite("S" + spriteCount++);

		sprite.addAttribute("schedule", schedule);
		sprite.addAttribute("path", path);
		sprite.attachToEdge(startEdge.getId());
		
		sprite.addAttribute("ui.label", sprite.getId());

		// determine the direction the train is going on the path
		setDirection(sprite, schedule.getSource(), startEdge);
		
		return sprite;
	}
	
	/**
	 * @return if the graph has any locked edges left
	 */
	protected boolean hasLocked() {
		Iterator<Edge> it = graph.getEdgeIterator();
		while(it.hasNext()) {
			if(it.next().getAttribute("ui.class") == "locked") { return true; }
		}
		return false;
	}
	
	/**
	 * travels forward in time, synced with display
	 * comment out sleep if you are not testing display
	 */
	protected void timeTravel() {
		globalTime++;
	    moveTrains(); // move trains on their tracks
	    shift(); // move sprites to their next edge as necessary
	}
	
	/**
	 * Determine which edge to start at
     * if from the edge's source give it that attribute 
	 * @param sprite
	 * @param source
	 * @param startEdge
	 * @return "true" indicates source -> destination and "false" indicates destination -> source
	 */
	protected void setDirection(Sprite sprite, Node source, Edge startEdge) {
        if (startEdge.getSourceNode() == source) {
        	sprite.setAttribute("fromSource");
        	sprite.setAttribute("location", 0); // location in arbitrary distance units, starts at source
        	sprite.setPosition(0);
        } else {
        	sprite.removeAttribute("fromSource");
        	sprite.setAttribute("location", (Integer)startEdge.getAttribute("weight")); // location in distance units, starts at destination
        	sprite.setPosition(1); // percentage of the way down the edge is 1 if from the destination
        }
	}
	
	/**
	 * Moves sprites forward on their track.
	 */
	protected void moveTrains() {
		//move each train 1/10th of the way on the track every 100 ms
		for(int i = 1; i <= 10; i++) {
			for (Sprite sprite : sman) {
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
			sleep(10);
		}
	}

	/**
	 * set position of sprite to next edge if it is at the end of its first edge
	 * otherwise change the location attribute of the sprite appropriately
	 * if no more edges, remove sprite. the train has arrived
	 */
	public void shift() {
		List<Sprite> toRemove = new ArrayList<Sprite>();
		for(Sprite sprite : sman) {
		    List<Edge> path = sprite.getAttribute("path");
		    Edge lastEdge = (Edge)sprite.getAttachment();
		    
		    if(atEndEdge(sprite)) {
		     // if the last edge it was on is not the last edge in the path
              if (path.indexOf(lastEdge) < path.size() - 1) {
                  // move the sprite to the next edge at beginning
                  sprite.attachToEdge(path.get(path.indexOf(lastEdge) + 1).getId());
                  setDirection(sprite, getCurrentNode(sprite, lastEdge), (Edge)sprite.getAttachment());
              } else {
                  getTrainCost(sprite);
                  // unlock all paths
                  for(Edge edge : path) { edge.setAttribute("ui.class", "unlocked"); }
                  toRemove.add(sprite);
              }
		    } else {
		        incrementLocation(sprite);
		    }
		}

		// remove necessary sprites
		for(Sprite sprite : toRemove) {
			sman.removeSprite(sprite.getId());
		}
	}

    protected boolean atEndEdge(Sprite sprite) {
	    Edge edge = (Edge)sprite.getAttachment();
        if (sprite.hasAttribute("fromSource")
                &&(int)sprite.getAttribute("location") + 1 == (int)edge.getAttribute("weight")) {
            return true;
        } else if (!sprite.hasAttribute("fromSource") && (int)sprite.getAttribute("location") - 1 == 0){
            return true;
        }
        return false;
    }
    
    protected Node getCurrentNode(Sprite sprite, Edge prevEdge) {
        if(sprite.hasAttribute("fromSource")) {
            return prevEdge.getTargetNode();
        } else {
            return prevEdge.getSourceNode();
        }
    }
    
    protected void incrementLocation(Sprite sprite) {
        if(sprite.hasAttribute("fromSource")) {
            sprite.setAttribute("location", (int)sprite.getAttribute("location") + 1);
        } else {
            sprite.setAttribute("location", (int)sprite.getAttribute("location") - 1);
        }
    }


    /**
	 * if the train has reached the end of the path, the train has arrived
	 * the train cost is the total travel time
	 * @param sprite - the sprite that represents the train that arrived
	 */
	protected void getTrainCost(Sprite sprite) {
		int dispatchTime = ((Schedule) sprite.getAttribute("schedule")).getDispatchTime();
		trainCost.add((globalTime - dispatchTime));
	}
	
	protected double getAverageCost() {
		double averageCost = 0.0;
		for (int i = 0; i < trainCost.size(); i++) {
			averageCost += trainCost.get(i);
		}
		averageCost = averageCost / trainCost.size();
		return averageCost;
	}
	 
	protected static void sleep(int s) {
		try { Thread.sleep(s); } catch (Exception e) {}
	}

}
