package main;

import java.io.IOException;

import graph.MyGraph;
import schedule.Schedule;

/**
 * Runs the program using a graph and a schedule
 */
public class Driver {

   public static void main(String[] args) throws IOException {
       MyGraph graph = MyGraph.loadGraphFromFile("graphs/" + args[0], "Map");

       boolean base = true;
       boolean second = true;
       
       // Schedule takes parameters <maxTime>, <scheduleLength>, graph
       // where maxTime is the latest time a train will be dispatched
       // and scheduleLength is the number of trains in the schedule 
       String schedule = Schedule.createRandomTrainFile(10, 10, graph); 

       if(base) {
    	   TrainDispatch baseCase = new TrainDispatch(graph);
    	   baseCase.dispatch(schedule);
    	   System.out.println(baseCase.getAverageCost());
       }
       
       if(second) {
    	   TrainDispatch secondCase = new ImprovedDispatch(graph);
    	   secondCase.dispatch(schedule);
    	   System.out.println(secondCase.getAverageCost());
       }

   }
}
