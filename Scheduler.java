/*
Round Robin Variation

*/
import java.util.*;

public class Scheduler extends Thread
{
    private Vector queue;
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

    // New data added to p161 
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;

    // Allocates the tid[] array with a maxThreads number of elements
    private void initTid( int maxThreads ) {
		tids = new boolean[maxThreads];
		for ( int i = 0; i < maxThreads; i++ )
		    tids[i] = false;
    }

    // Search an available thread ID and provide a new thread with this ID
    // Finds an tid[] array element whose value is false, and returns its index as a new thread ID.
    private int getNewTid( ) {
    	// Will run just once because of the if statement and tids being all false. 
		for ( int i = 0; i < tids.length; i++ ) {
		    int tentative = ( nextId + i ) % tids.length;
		    if ( tids[tentative] == false ) {
				tids[tentative] = true;
				nextId = ( tentative + 1 ) % tids.length;
				return tentative;
		    }
		}
		return -1;
    }

    // Return the thread ID and set the corresponding tids element to be unused
    // Sets the corresponding tid[] element, (i.e., tid[tid]) false. The return value is false if tid[tid] is already false, (i.e., if this tid has not been used), otherwise true.
    private boolean returnTid( int tid ) {
		if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
		    tids[tid] = false;
		    return true;
		}
		return false;
    }

    // Modify. 
    // Retrieve the current thread's TCB from the queue
    // Finds the current thread's TCB from the active thread queue and returns it.
    public TCB getMyTcb( ) {
		Thread myThread = Thread.currentThread( ); // Get my thread object
		synchronized( queue ) {
		    for ( int i = 0; i < queue.size( ); i++ ) {
				TCB tcb = ( TCB )queue.elementAt( i );
				Thread thread = tcb.getThread( );
				
				if ( thread == myThread ) // if this is my TCB, return it
			    	return tcb;
		    }
		}
		return null;
    }

    // Return the maximal number of threads to be spawned in the system
    // Returns the length of the tid[] array, (i.e., the available number of threads).
    public int getMaxThreads( ) {
		return tids.length;
    }

    // Constructors =====================================================
    // Modify. 
    public Scheduler( ) {
		timeSlice = DEFAULT_TIME_SLICE;
		queue = new Vector( );
		initTid( DEFAULT_MAX_THREADS );
    }

    public Scheduler( int quantum ) {
		timeSlice = quantum;
		queue = new Vector( );
		initTid( DEFAULT_MAX_THREADS );
    }

    // A constructor to receive the max number of threads to be spawned
    // Receives two arguments: (1) the time slice allocated to each thread execution and (2) the maximal number of threads to be spawned, (namely the length of tid[]). It creates an active thread queue and initializes the tid[] array.
    public Scheduler( int quantum, int maxThreads ) {
		timeSlice = quantum;
		queue = new Vector( );
		initTid( maxThreads );
    }

    private void schedulerSleep( ) {
		try {
		    Thread.sleep( timeSlice );
		} catch ( InterruptedException e ) {
		}
    }

    // A modified addThread of p161 example
    // Modify. 
    // Allocates a new TCB to this thread t and adds the TCB to the active thread queue. This new TCB receives the calling thread's id as its parent id.
    public TCB addThread( Thread t ) {
		// Removed. 
		// t.setPriority( 2 );
		TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
		int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
		int tid = getNewTid( ); // get a new TID
		if ( tid == -1)
		    return null;
		TCB tcb = new TCB( t, tid, pid ); // create a new TCB
		queue.add( tcb );
		return tcb;
    }

    // Removing the TCB of a terminating thread
    // Finds the current thread's TCB from the active thread queue and marks its TCB as terminated. 
    // The actual deletion of a terminated TCB is performed inside the run( ) method, (in order to prevent race conditions).
    public boolean deleteThread( ) {
		TCB tcb = getMyTcb( ); 
		if ( tcb!= null )
		    return tcb.setTerminated( );
		else
		    return false;
    }

    // Puts the calling thread to sleep for a given time quantum.
    public void sleepThread( int milliseconds ) {
		try {
		    sleep( milliseconds );
		} catch ( InterruptedException e ) { }
    }
    
    // A modified run of p161
    // This is the heart of Scheduler. The difference from the lecture slide includes: 
    // (1) retrieving a next available TCB rather than a thread from the active thread list, 
    // (2) deleting it if it has been marked as "terminated", and 
    // (3) starting the thread if it has not yet been started. 
    // Other than this difference, the Scheduler repeats retrieving a next available TCB from the list, raising up the corresponding thread's priority, 
    // yielding CPU to this thread with sleep( ), and lowering the thread's priority.
    public void run( ) {
		Thread current = null;
		// Removed. 
		// this.setPriority( 6 );
		
		while ( true ) {
		    try {
			// get the next TCB and its thrad
			if ( queue.size( ) == 0 )
			    continue;
			TCB currentTCB = (TCB)queue.firstElement( );
			if ( currentTCB.getTerminated( ) == true ) {
			    queue.remove( currentTCB );
			    returnTid( currentTCB.getTid( ) );
			    continue;
			}
			current = currentTCB.getThread( );
			if ( current != null ) {
			    if ( current.isAlive( ) )
				current.resume(); 
				// current.setPriority( 4 );
			    else {
				// Spawn must be controlled by Scheduler
				// Scheduler must start a new thread
				current.start( ); 
				// Removed. 
				//current.setPriority( 4 );
			    }
			}
			
			schedulerSleep( );
			// System.out.println("* * * Context Switch * * * ");

			synchronized ( queue ) {
			    if ( current != null && current.isAlive( ) )
				current.suspend(); 
				// current.setPriority( 2 );
			    queue.remove( currentTCB ); // rotate this TCB to the end
			    queue.add( currentTCB );
			}
		    } catch ( NullPointerException e3 ) { };
		}
    }
}
