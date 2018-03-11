import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
//import com.google.gson.GsonBuilder;
import java.util.Arrays;
import static java.util.concurrent.TimeUnit.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;


public class Driver {
	
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");//date formattter
	
	public static BufferedWriter out;
	
	
	/**
	 * Goal is to create a new block with pulled arduino value every 30 minutes (1800 seconds). Instantaneously AFTER PULLING (in this case printing), we want to 
	 * PULL website data.  WE get web data(for that same iteration...hour) put in new block THAT WILL FIRST be compared to Arduinos RECENTLY CREATED BLOCK
	 * 
	 * 1. Store system time i.e. 2:30 pm
	 * 2. Retrieve arduino value once we store that time (instantly) - PRINT IT immediately
	 * 		a. When we retrieve above, we are putting into block no matter what
	 * 3. During same iteration...aka.. within BEEPER function, WITH the delay of 30 minute increments, we want to pull from website. 
	 * 		a. Retrieve website values, and add a new block attached to previous ARDUINO block....(2 values to compare are now side by side)
	 * 			i. Compare the data in this block with Ardunios (n-2) block...2nd to last block if starting from 0...IF(withinRange, overrwrite arduino's block data with
	 * 				websites pulled data, NOTE THAT WE HAVE WESBITE data(accurate web), delete all info in last block (websites) since it has now been stored in 2nd to last block)
	 * 			ii. This last block now has no info and is dangling (DELETE IT....in other words, delete the n-1 block in chain), awaiting for next Arduino value...adds new block and repeats steps 1-3
	 * 		b. once were done pulling there are maybe 25 minutes left, where program will know, then repeat
	 * @author Mario Garcia
	 * @throws IOException 
	 *
	 */
	public static void main(String[] args) throws IOException {
		out = new BufferedWriter(new FileWriter("datareceived.txt"));
		
		//each block will have its respective info.  New block created every 30 min for 12 hours
		//as we add the new block (using arduino value) during a specific time, we will then retrieve website temperature, and attempt to add a new block at which we will only compare temperature values within range
		
		weatherOperation();
	}
	
	
	/**
	 * Actual operation being performed (does the entire purpose of the project)
	 * @throws IOException
	 */
	public static void weatherOperation() throws IOException {
		
		double [] arduinoTemp = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
		double [] websiteTemp = {3, 5, 5, 7 ,5, 8, 10, 6 ,14, 16, 19, 18};
		Boolean [] webValid = {false, false, false, false, false, false, false, false, false, false, false, false};
		
		 final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		 ArrayList<Block> arduinoChain = new ArrayList<Block>();
		 ArrayList<Block> webChain = new ArrayList<Block>();
		 
		 
		 final Runnable getArduino = new Runnable() {//assigning the variable beeper to be a function...where the function run is ran			 
			 int counter = 0;//check that this is being incremented correctly
			 
			 public void run() {//grabbing arduino new value and printing it -- should add a block every 10 seconds starting instantly 
				 System.out.println("getting Arduino's value"); 
				 LocalDateTime currentTime = LocalDateTime.now();
				 String timeNow = dtf.format(currentTime);//should closely match upcoming blocks time within nanoseconds
				 if(counter == 0) {//for the first element
					 //get Arduino values, store into array then
					 arduinoChain.add(new Block(arduinoTemp[counter], "0"));
					 //grab website data function, store in array so that you can do the following
					 webChain.add(new Block(websiteTemp[counter], "0"));//hashes dont really matter now
					 //overwrite data in arduinoChain with webChain if webChain has temp value of +/- 2 degrees (accurate)
					 if((webChain.get(counter).getData() == arduinoChain.get(counter).getData())  ||  (webChain.get(counter).getData() - arduinoChain.get(counter).getData() == 2)  ||  (webChain.get(counter).getData() - arduinoChain.get(counter).getData() == -2)){	//if website & arduino equal each other "1 degree" . equals "1. degree"
						 //we can either leave arduinos value or put web data...lets put web data's
						 arduinoChain.get(counter).setData(webChain.get(counter).getData());
						 webValid[counter] = true;//websource was valid ...for later report print out
					 }
					 //other wise, arduino chain remains the same and webchain[counter] is kept at false
					 
				 }
				 else { //for all other elements
					 arduinoChain.add(new Block(arduinoTemp[counter], arduinoChain.get(arduinoChain.size()-1).getHash()));
					//grab website data function, store in array so that you can do the following
					 webChain.add(new Block(websiteTemp[counter], webChain.get(webChain.size()-1).getHash()));//hashes dont really matter now
					 //overrwrite data in arduinoChain with webChain if webChain has temp value of +/- 2 degrees (accurate)
					 if((webChain.get(counter).getData() == arduinoChain.get(counter).getData())  ||  (webChain.get(counter).getData() - arduinoChain.get(counter).getData() == 2)  ||  (webChain.get(counter).getData() - arduinoChain.get(counter).getData() == -2)){	//if website & arduino equal each other "1 degree" . equals "1. degree"
						 webValid[counter] = true;//websource was valid ...for later report print out
						 //we can either leave arduinos value or put web data...lets put web data's
						 arduinoChain.get(counter).setData(webChain.get(counter).getData());
					 }
				 }
				 arduinoChain.get(counter).printBlock();//has final values of the chain
				 
				 try {
					 out.write(arduinoChain.get(counter).getPrevHash());
					 out.newLine();
					 out.write(arduinoChain.get(counter).getHash());
					 out.newLine();
					 out.write(Double.toString(arduinoChain.get(counter).getData()) + "º degrees");
					 out.newLine();
					 out.write(arduinoChain.get(counter).getTimeStamp());
				 }catch(IOException ioe) {
					 ioe.printStackTrace();
				 }
				 
				 counter ++;
				 if(counter == 12) {//or whatever the period is
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					//print validity of websource values per index (time)
					for (int i = 0; i< counter; i++)
						System.out.println("Web source valid? " + "[" + i +  "] " + webValid[i]);
				 }
			 }
			 
		 };//end runnable command
		 
		 //continue doing getArduino using the following increments -- eventually change to minutes
		 final ScheduledFuture<?> operationHandler = scheduler.scheduleAtFixedRate(getArduino, 0, 10, SECONDS);//runnable command beeper, intialDelay, period, unit
		 
		 scheduler.schedule(new Runnable() {			//Runnable command function, long delay, TimeUnit unit
			 public void run() { 
				 //Thread.currentThread().interrupt();
				 System.out.println(operationHandler.cancel(true));
				 System.out.print(" -> Exitting...");
				 System.exit(0); 
			 }//cancel within a time period
		 }, 110, SECONDS);//cancels after -- 110 seconds---will allow 12 items to print since first prints instantly
	
	}
}