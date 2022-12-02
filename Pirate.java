package hw7;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

/***************************************************/
/* CS-350 Fall 2022 - Homework 7 - Code Solution   */
/* Author: Renato Mancuso (BU)                     */
/*                                                 */
/* Description: This class implements the logic of */
/*   a super-entity, called Pirate that uses the   */
/*   Dispatcher class multiple times to create     */
/*   two stages of computation. In the first stage */
/*   the system cracks simple hashes, while in the */
/*   second, compound hashes are cracked.          */
/*                                                 */
/***************************************************/

public class Pirate {

    String fileName;
    int numCPUs;
    int timeoutMillis;
    Dispatcher dispatcher;

    /* Queue for inputs to be processed */
    protected LinkedList<WorkUnit> workQueue;

    /* Queue for processed outputs */
	protected LinkedList<WorkUnit> resQueue;

    /* Mutex to protect input queue */   
    Semaphore wqMutex;
    
    /* Mutex to protect output queue */
    Semaphore rsMutex;

	Boolean isDone = false;

	Integer uncrackedCounter = 1;

	Integer crackedCounter = 1;

	HashSet<Integer> kVals = new HashSet<Integer>();

    
    public Pirate (String fileName, int N, int timeout) {
	this.fileName = fileName;
	this.numCPUs = N;
	this.timeoutMillis = timeout;

	/* Now build the other data structures */
	workQueue = new LinkedList<WorkUnit>();
	resQueue = new LinkedList<WorkUnit>();	

	wqMutex = new Semaphore(1);
	rsMutex = new Semaphore(1);
	
	/* Construct the dispatcher which will also start the worker threads */
        this.dispatcher = new Dispatcher(workQueue, resQueue, wqMutex, rsMutex, N, timeout);	
    }

    private void __initWorkQueue() throws InterruptedException {
        /* The fileName parameter contains the full path to the input file */
        Path inputFile = Paths.get(fileName);

	/* Attempt to open the input file, if it exists */
        if (Files.exists(inputFile)) {
	    /* It appears to exists, so open file */
            File fHandle = inputFile.toFile();
            /* Use a buffered reader to be a bit faster on the I/O */
            try (BufferedReader in = new BufferedReader(new FileReader(fHandle)))
            {

                String line;
		
		/* Pass each line read in input to the dehasher */
                while((line = in.readLine()) != null){
		    WorkUnit work = new WorkUnit(line);
		    dispatcher.addWork(work);
                }
		
            } catch (FileNotFoundException e) {
                System.err.println("Input file does not exist.");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Unable to open input file for read operation.");
                e.printStackTrace();
            }	    
	    
        } else {
            System.err.println("Input file does not exist. Exiting.");        	
        }
	
    }

	private int __getUncrackedCount() {
		int count = 0;
		for (WorkUnit w : resQueue) {
			if (!(w.getResult() != null && w.isSimple())) {
				count++;
			}
		}
		return count;
	}

	private int __getCrackedCount() {
		int count = 0;
		for (WorkUnit w : resQueue) {
			if (w.getResult() != null && w.isSimple()) {
				count++;
			}
		}
		return count;
	}
    private void __prepareCompoundWork() throws InterruptedException { /// change
	/* This function will execute when no pending work exists. But
	 * as it goes along, worker threads might start crunching
	 * data. Hence, copy over all the result so far. */

	ArrayList<Integer> L = new ArrayList<Integer>();
	ArrayList<String> uncracked = new ArrayList<String>();

	uncrackedCounter = 0;
	crackedCounter = 0;
	
	for (WorkUnit w : resQueue) {
	    String res = w.getResult();
	    
	    if (res != null && w.isSimple()){
			L.add(Integer.parseInt(res));
			kVals.add(Integer.parseInt(res));
			crackedCounter++;
			System.out.println(res);
		}
		
		/* We might as well print this first round of results already */

		else if (res != null && !w.isSimple()) {
			L.add(Integer.parseInt(res.split(";")[1]));
			kVals.add(Integer.parseInt(res.split(";")[1]));
		}

	    else {
			uncracked.add(w.getHash());
			uncrackedCounter++;
	    }
	}

	/* Done splitting result -- we can clean up the result queue */
	resQueue.clear();

	/* Sort list L of integers */
	Collections.sort(L);

	/* Possibly the worst way of doing this. Generate all the
	 * possible tuples of the form <a, b, hash> to be cracked. The
	 * work queue might explode after this. A work-queue pruning
	 * strategy is required to meet some reasonable timing
	 * constraints. */
	int len = L.size();
	for (int i = 0; i < len-1; ++i) {
	    for (int j = i + 1; j < len; ++j) {
			for (String h : uncracked) {
				WorkUnit work = new WorkUnit(h, L.get(i), L.get(j));
				dispatcher.addWork(work);
			}
	    }
	}

	System.out.println("Uncracked: " + uncrackedCounter);
	System.out.println("Cracked: " + crackedCounter);
	System.out.println("Size of work queue: " + workQueue.size());
	System.out.println("Size of result queue: " + resQueue.size());
    }

    private void __postProcessResult() throws InterruptedException, IOException { //// change
	HashMap<String, Boolean> uncrackable = new HashMap<String, Boolean>();

	File map = new File("HW8_island.txt");
	byte[] mapBytes = Files.readAllBytes(map.toPath());
	String mapString = new String(mapBytes, StandardCharsets.UTF_8);
	String output = "";

//	System.out.println(mapString);

	for (WorkUnit w : resQueue) {
	    String res = w.getResult();
	    String h = w.getHash();

	    /* Add all of them for now */
	    uncrackable.put(h, true);
	}
	
	/* Done! Print result */
	for (WorkUnit w : resQueue) {
	    String res = w.getResult();
	    String h = w.getHash();
	    
	    if (res != null) {
			kVals.add(Integer.parseInt((res.split(";")[1])));
			System.out.println(res);

			/* Remove what we know has been cracked*/
			uncrackable.remove(h);
	    }
	}

	/* Print the uncrackable hashes */
	for (String h : uncrackable.keySet()) {
	    System.out.println(h);
	}

//	if (uncrackable.size() == 0) {
//		isDone = true;
//	}
	System.out.println(kVals + " " + kVals.size());

	// treasure map

//	ArrayList<Integer> kArr = new ArrayList<Integer>(kVals);
//	Collections.sort(kArr);
//
//	for (int i = 0; i < mapString.length() + 1; i++) {
//		if (i == kArr.get(i)) {
//			output += mapString.charAt(i);
//		}
//	}
//	System.out.println(output);
    }
    
    public void findTreasure () throws InterruptedException, IOException ///// change
	{
		boolean firstCall = true;

		/* Read the input file and initialize the input queue */

		__initWorkQueue();

		/* Dispatch work and wait for completion of current stage */
		dispatcher.dispatch();

		while (__getUncrackedCount() != 0 || firstCall) {
			System.out.println("CRACKKING");
			firstCall = false;

			rsMutex.acquire();
			/* We have the result. Generate the new work queue to crack compound hashes */
			__prepareCompoundWork();
//			uncrackedCounter = __getUncrackedCount();
//			crackedCounter = __getCrackedCount();
			rsMutex.release();

			/* Dispatch work and wait for completion of current stage */
			dispatcher.dispatch();

			/* Use a hash map to prune the output result */
			rsMutex.acquire();
			__postProcessResult();
			rsMutex.release();

			}
		/* Done! Terminate the dispatcher (and any worker thread with that) */
		dispatcher.terminate();

		ArrayList<Integer> kArr = new ArrayList<Integer>(kVals);
		Collections.sort(kArr);

		System.out.println(kArr);
		System.out.println(kArr.size());

	}

    /* Entry point of the code */
    public static void main(String[] args) throws InterruptedException, IOException {
	/* Read path of input file */       
//  	String inputFile = args[0];
	String inputFile = "HW8_public_test_easy.txt";

	/* Read number of available CPUs */
//	int N = Integer.parseInt(args[1]);
	int N = 4;

	/* If it exists, read in timeout, default to 10 seconds otherwise */
	int timeoutMillis = 1000;
	if (args.length > 2) {
	    timeoutMillis = Integer.parseInt(args[2]);
	}

	/* Construct the dispatcher with all the necessary parameters */
        Pirate thePirate = new Pirate(inputFile, N, timeoutMillis);

	/* Start the work */
        thePirate.findTreasure();
	}

}

/* END -- Q1BSR1QgUmVuYXRvIE1hbmN1c28= */
