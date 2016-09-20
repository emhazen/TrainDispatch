# README #

#Group 6#
Emily Hazen: 901620674
Rachel Froling: 901913758
Taylor Carter: 901753744

Train Dispatch dispatches trains in two cases.
The first case dispatches using dijkstra's and locking the entire 
train's path.
The second case dispatches using an improved version of dijkstra's that 
considers all paths, locked or unlocked. It also has a reservation 
system that allows trains to only lock one edge at a time.

### How do I get set up? ###

To compile, run in src: ./compile.sh
To execute, run in src:  java -cp "*:." main.Driver <args[0]>

Where <args[0]> is a string indicating the graph you would like to run
Options include:
 * Paris.txt
 * Athens.txt
 * Berlin.txt

###How to run tests###
 * To run the base case the boolean variable base in 
src/main/Driver.java 
needs to be  true, if the base case should not be run set the boolean 
base to false. 
 * To run the improvement case the boolean variable second in 
Driver.java needs to be true, if the improvement should not be run set 
the boolean second to false.  
 * Randomly generated schedules are sent as files to the src/schedules/ 
directory.

###Understanding Output###
The program prints to the console each schedule as it dispatches. When 
the program has finished, it prints the average cost of the trains.
