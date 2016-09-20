package schedule;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;

import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiNode;

import graph.MyGraph;

public class Schedule implements Comparable<Schedule> {

	private int dispatchTime;   //time the train is ready for dispatch
	private Node source;		//starting position
	private Node destination;	//target position

	public Schedule(Node source, Node destination, int dispatchTime) {
		this.dispatchTime = dispatchTime;
		this.source = source;
		this.destination = destination;
	}

	public int getDispatchTime() {
		return dispatchTime;
	}

	/**
	 * Adds time to the dispatch time making the train wait.
	 * @param waitTime how long the train should wait.
	 */
	public void wait( int waitTime ) {
		dispatchTime += waitTime;
	}

	public Node getSource() {
		return source;
	}

	public void setSource(Node newSource) {
		this.source = newSource;
	}


	public Node getDestination() {
		return destination;
	}

	public void setDestination(Node newDestination) {
		this.destination = newDestination;
	}

	public String toString() {
		//returns schedule in same style as input
		String output = "";
		output += "(";
		output += source.toString() + ", ";
		output += destination.toString() + ", ";
		output += dispatchTime + ")";
		return output;
	}

	/**
	 * Compare to another train based on dispatchTime
	 * @param s schedule to compare to this
	 */
	@Override
	public int compareTo( Schedule s ) {
		return Integer.compare( this.dispatchTime, s.dispatchTime );
	}

	/**
	 * Creates a randomized file and returns a PriorityQueue of it
	 * Each line represents a train and should be formatted
	 * <sourceNode> <destinationNode> <dispatchTime>
	 * Note that the PriorityQueue is sorted by the dispatchTimes of each train
	 * @param maxNodes the maximum number of nodes in the graph
	 * @param maxTime the last time at which trains will be ready to dispatch
	 * @param scheduleLength the number of trains in the queue
	 * @param graph the graph to load the the nodes from
	 * @param filename the filename of the file to load trains from
	 * @return a PriorityQueue of trains
	 */
	public static String createRandomTrainFile (int maxTime,
			int scheduleLength, MyGraph graph) throws IOException {
		//a random number generator given a seed
		Random rand = new Random(Long.getLong("seed", System.nanoTime()));
		int maxNodes = graph.getNodeCount();

		//the filename "Random_maxNodes_maxTime_scheduleLength"
		String filename = "schedules/RandSchedule_" + maxNodes + "_" + maxTime + "_" + scheduleLength + ".txt";

		//a new bufferedwriter to write to file
		BufferedWriter bwtemp = new BufferedWriter(new FileWriter(filename));

		for (int i = 0; i < scheduleLength; i++) {
			//string to be written once assigned
			String line = null;

			//create source, destination, and time
			int a = rand.nextInt(maxNodes) + 1;
			int b = rand.nextInt(maxNodes) + 1;
			int c = rand.nextInt(maxTime);

			//if source and destination are same, resolve the conflict
			if (a == b){
				if (a == maxNodes) {
					b--;
				} 
				else {
					b++;
				}
			}
			//write these in file
			line = a + " " + b + " " + c;
			bwtemp.write(line);
			bwtemp.newLine();
		}
		//close buffered writer
		bwtemp.close();

		//return a trainQueue given the graph and newly created file
		return filename;
	}
	/**
	 * Returns a PriorityQueue of Trains loaded from a file
	 * Each line of the file represents a train and should be formatted
	 * <sourceNode> <destinationNode> <dispatchTime>
	 * Note that the PriorityQueue is sorted by the dispatchTimes of each train
	 * @param graph the graph to load the the nodes from
	 * @param filename the filename of the file to load trains from
	 * @return a PriorityQueue of trains
	 */
	public static PriorityQueue<Schedule> loadTrainsfromFile( MyGraph graph, String filename )
			throws FileNotFoundException {

		Scanner inputScanner = new Scanner( new File( filename ) );

		//a priority queue to store the trains
		PriorityQueue<Schedule> trainQueue = new PriorityQueue<Schedule>();

		while ( inputScanner.hasNextInt() ) {
			//create Node objects for source and destination
			MultiNode source = graph.getNode(Integer.toString(inputScanner.nextInt()));
			MultiNode destination = graph.getNode(Integer.toString(inputScanner.nextInt()));
			//grab the dispatch time
			int dispatchTime = inputScanner.nextInt();

			//make a new train and add it to the queue
			trainQueue.add( new Schedule( source, destination, dispatchTime ) );
		}
		inputScanner.close();
		return trainQueue;
	}

}
