/* 
Author: Drew Kwak
Date: 4/27/2019
Description: Project 2 Scheduler. Implementation of Multilevel Feedback Queue Scheduler. 
*/
import java.util.*;

public class Scheduler extends Thread {
	// Create 3 queues.
	private Vector queue0;
	private Vector queue1;
	private Vector queue2;
	private int timeSlice;
	// New quantum times. zeroSlice is half of timeSlice. No need for the double of timeSlice.
	// I basically just use zeroSlice for everything. 
	private int zeroSlice;
	private boolean[] tids; // Indicate which ids have been used
	private static final int DEFAULT_TIME_SLICE 	= 1000;
	private static final int DEFAULT_MAX_THREADS 	= 10000;

	// Allocate an ID array, each element indicating if that id has been used
	private int nextId = 0;

	// Allocates the tid[] array with a maxThreads number of elements
	private void initTid(int maxThreads) {
		tids = new boolean[maxThreads];
		for (int i = 0; i < maxThreads; i++)
			tids[i] = false;
	}

	// Search an available thread ID and provide a new thread with this ID
	// Finds an tid[] array element whose value is false, and returns its index as a new thread ID.
	private int getNewTid() {
		// Will run just once because of the if statement and tids being all false.
		for (int i = 0; i < tids.length; i++) {
			int tentative = (nextId + i) % tids.length;
			if (tids[tentative] == false) {
				tids[tentative] = true;
				nextId = (tentative + 1) % tids.length;
				return tentative;
			}
		}
		return -1;
	}

	// Return the thread ID and set the corresponding tids element to be unused
	// Sets the corresponding tid[] element, (i.e., tid[tid]) false. The return value is false if tid[tid] is already
	// false, (i.e., if this tid has not been used), otherwise true.
	private boolean returnTid(int tid) {
		if (tid >= 0 && tid < tids.length && tids[tid] == true) {
			tids[tid] = false;
			return true;
		}
		return false;
	}

	// Modify. Have it search through one of the active queues to find and return it's TCB.
	// Retrieve the current thread's TCB from the queue
	// Finds the current thread's TCB from the active thread queue and returns it.
	public TCB getMyTcb() {
		Thread myThread = Thread.currentThread(); // Get my thread object
		synchronized (queue0) {
			for (int i = 0; i < queue0.size(); i++) {
				TCB tcb = (TCB) queue0.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
		synchronized (queue1) {
			for (int i = 0; i < queue1.size(); i++) {
				TCB tcb = (TCB) queue1.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
		synchronized (queue2) {
			for (int i = 0; i < queue2.size(); i++) {
				TCB tcb = (TCB) queue2.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
		return null;
	}

	// Return the maximal number of threads to be spawned in the system
	// Returns the length of the tid[] array, (i.e., the available number of threads).
	public int getMaxThreads() {
		return tids.length;
	}

	// Constructors =====================================================
	// Modify.
	public Scheduler() {
		this.zeroSlice 	= DEFAULT_TIME_SLICE / 2;
		this.timeSlice 	= DEFAULT_TIME_SLICE;
		this.queue0 	= new Vector();
		this.queue1 	= new Vector();
		this.queue2 	= new Vector();
		initTid(DEFAULT_MAX_THREADS);
	}

	public Scheduler(int quantum) {
		this.zeroSlice 	= quantum / 2;
		this.timeSlice 	= quantum;
		this.queue0 	= new Vector();
		this.queue1 	= new Vector();
		this.queue2 	= new Vector();
		initTid(DEFAULT_MAX_THREADS);
	}

	// A constructor to receive the max number of threads to be spawned
	// Receives two arguments: (1) the time slice allocated to each thread execution and (2) the maximal number of
	// threads to be spawned, (namely the length of tid[]). It creates an active thread queue and initializes
	// the tid[] array.
	public Scheduler(int quantum, int maxThreads) {
		this.zeroSlice 	= quantum / 2;
		this.timeSlice 	= quantum;
		this.queue0 	= new Vector();
		this.queue1 	= new Vector();
		this.queue2 	= new Vector();
		initTid(maxThreads);
	}

	// Changed to use zeroSlice as the basic unit of sleep. 
	private void schedulerSleep() {
		try {
			Thread.sleep(zeroSlice);
		} catch (InterruptedException e) {
		}
	}

	// Modify.
	// Allocates a new TCB to this thread t and adds the TCB to the active thread queue. This new TCB receives the
	// calling thread's id as its parent id.
	public TCB addThread(Thread t) {
		// Removed. 
		// t.setPriority( 2 );
		TCB parentTcb = getMyTcb(); // get my TCB and find my TID
		int pid = (parentTcb != null) ? parentTcb.getTid() : -1;
		int tid = getNewTid(); // get a new TID
		if (tid == -1)
			return null;
		TCB tcb = new TCB(t, tid, pid); // create a new TCB
		queue0.add(tcb);
		return tcb;
	}

	// Removing the TCB of a terminating thread
	// Finds the current thread's TCB from the active thread queue and marks its TCB as terminated.
	// The actual deletion of a terminated TCB is performed inside the run( ) method, (in order to
	// prevent race conditions).
	public boolean deleteThread() {
		TCB tcb = getMyTcb();
		if (tcb != null)
			return tcb.setTerminated();
		else
			return false;
	}

	// Puts the calling thread to sleep for a given time quantum.
	public void sleepThread(int milliseconds) {
		try {
			sleep(milliseconds);
		} catch (InterruptedException e) {
		}
	}

	// This is the heart of Scheduler. The difference from the lecture slide includes:
	// (1) retrieving a next available TCB rather than a thread from the active thread list,
	// (2) deleting it if it has been marked as "terminated", and
	// (3) starting the thread if it has not yet been started.
	// Other than this difference, the Scheduler repeats retrieving a next available TCB from the list,
	// raising up the corresponding thread's priority,
	// yielding CPU to this thread with sleep( ), and lowering the thread's priority.

	// Ugly long nested if statement, would have been nice to make helper methods for everything, 
	// but... alas. 
	// Structure basically used what was there before, removing the first if conditional of the queue being
	// empty. Added some sleeps between checking the queues, always checking in 500ms slices. 
	public void run( ) {
		Thread current = null;
		while ( true ) {
		    try {
				if ( queue0.size() != 0) {
					TCB currentTCB = (TCB)queue0.firstElement( );
					if ( currentTCB.getTerminated( ) == true ) {
					    queue0.remove( currentTCB );
					    returnTid( currentTCB.getTid( ) );
					    continue;
					}
					current = currentTCB.getThread( );
					if ( current != null ) {
						// Check if thread was previously being processed.
					    if ( current.isAlive( ) ) {
							current.resume(); 
						}
					    else {
						// Spawn must be controlled by Scheduler
						// Scheduler must start a new thread
						current.start( ); 
					    }
					}
					// Sleep for a slice. 
					this.schedulerSleep();

					synchronized (this.queue0) {
						// Check if the thread is still being processed.
						if (current != null && current.isAlive()) {
							current.suspend();
						}
						this.queue0.remove(currentTCB); // Took too long, remove and move to queue1.
						this.queue1.add(currentTCB);
					}
				}	 
				else if (queue1.size() != 0) {
					// Set current TCB to the first element in the queue1.
					TCB currentTCB = (TCB) queue1.firstElement();
					// If that TCB is set to be terminated, remove it from the queue1.
					if (currentTCB.getTerminated() == true) {
						this.queue1.remove(currentTCB);
						this.returnTid(currentTCB.getTid());
						continue;
					}
					current = currentTCB.getThread();
					if (current != null) {
						// Check if anyuthing is still processing in queue1. 
						if (current.isAlive()) {
							current.resume();				
						} else {
							// Spawn must be controlled by Scheduler
							// Scheduler must start a new thread
							current.start();
						}
					}
					// Sleep for a slice. 
					this.schedulerSleep();
					// If queue0 has something inside, continue the while loop again to process that thread. 
					if (queue0.size() > 0) {
						current.suspend();
						continue;
					}
					// If nothing is in queue0, start keep checking for 500ms, for a total of 1000. 
					this.schedulerSleep();

					synchronized (this.queue1) {
						// Check if the thread is still being processed.
						if (current != null && current.isAlive()){
							current.suspend();
						}						
						this.queue1.remove(currentTCB); // Took too long, remove and move to queue2.
						this.queue2.add(currentTCB);
					}
				}
				// If queue2 is not empty, do some work. 
				else if (queue2.size() != 0) {
					// Set current TCB to the first element in the queue.
					TCB currentTCB = (TCB) queue2.firstElement();
					// If that TCB is set to be terminated, remove it from the queue.
					if (currentTCB.getTerminated() == true) {
						this.queue2.remove(currentTCB);
						this.returnTid(currentTCB.getTid());
						continue;
					}
					current = currentTCB.getThread();
					if (current != null) {
						// Check if any threads still are active. 
						if (current.isAlive()) {
							current.resume();
						} else {
							// Spawn must be controlled by Scheduler
							// Scheduler must start a new thread
							current.start();
						}
					}
					// Sleep and check in intervals of 500ms if queue0 or 1 have a thread, continue while loop if so. 
					for (int i = 0; i < 4; i ++) {
						this.schedulerSleep();
						if (queue0.size() > 0) {
							current.suspend();
							continue;
						}
						if (queue1.size() > 0) {
							current.suspend();
							continue;
						}
					}
					synchronized (this.queue2) {
						// Check if the thread is still being processed.
						if (current != null && current.isAlive()){
							current.suspend();
						}
						this.queue2.remove(currentTCB); // Took too long, throw it in the back of queue2. 
						this.queue2.add(currentTCB);
					}
				}	
		    } catch ( NullPointerException e3 ) { };
		}
    }
}
