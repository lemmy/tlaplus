// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon 30 Apr 2007 at 14:01:40 PST by lamport  
//      modified on Wed Dec  5 15:35:42 PST 2001 by yuanyu   

package tlc2.tool;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import tlc2.output.EC;
import tlc2.output.MP;
import tlc2.tool.TLCTrace.UID;
import tlc2.util.BufferedRandomAccessFile;
import tlc2.util.IdThread;
import tlc2.util.ObjLongTable;
import tlc2.util.statistics.FixedSizedBucketStatistics;
import tlc2.util.statistics.IBucketStatistics;
import util.FileUtil;

public class Worker extends IdThread implements IWorker {
	
	/**
	 * Multi-threading helps only when running on multiprocessors. TLC can
	 * pretty much eat up all the cycles of a processor running single threaded.
	 * We expect to get linear speedup with respect to the number of processors.
	 */
	private final ModelChecker tlc;
	private final ObjLongTable astCounts;
	private final IBucketStatistics outDegree;
	private final CyclicBarrier finalization;
	private volatile long generated = 0L;
	private volatile long processed = 0L;
	private TLCState head;
	private TLCState tail;
	private long statesGenerated;

	private final BufferedRandomAccessFile raf;
	private long lastPtr;
	private volatile int maxLevel = 0;

	// SZ Feb 20, 2009: changed due to super type introduction
	public Worker(int id, CyclicBarrier finalization, AbstractChecker tlc, String metadir, String specFile) throws IOException {
		super(id);
		// Worker threads are daemon threads because TLC does NOT stop workers,
		// when one of the workers discovers a (safety) violation. Let w be the
		// worker from the set of workers that discovery a safety violation. w
		// will shift from state graph exploration to counterexample
		// reconstruction.
		// In the meantime, the remaining workers will continue state graph
		// exploration until the JVM terminates. This special form of eventual
		// consistency suffices for TLC. Its only downside is that we burn CPU
		// cycles when N-1 workers continue the state graph exploration even
		// though w has already discovered a violation. On the other hand, the
		// vast majority of time is spent on state graph exploration when no
		// violation has yet been found. Thus, the reduced thread contention
		// probably makes up for the extra CPU cycles at the end.
		this.setDaemon(true);
		
		this.setName("TLC Worker " + id);
		this.tlc = (ModelChecker) tlc;
		this.astCounts = new ObjLongTable(10);
		this.outDegree = new FixedSizedBucketStatistics(this.getName(), 32); // maximum outdegree of 32 appears sufficient for now.
		this.setName("TLCWorkerThread-" + String.format("%03d", id));

		final String filename = metadir + FileUtil.separator + specFile + "-" + myGetId() + TLCTrace.EXT;
		this.raf = new BufferedRandomAccessFile(filename, "rw");
		this.finalization = finalization;
	}

   public Worker(TLCState predErrState, CyclicBarrier finalization, AbstractChecker tlc, String metadir, String specFile) throws IOException {
	   this(predErrState.uid.wid, finalization, tlc, metadir, specFile);
	   this.head = predErrState;
	   this.astCounts.clear();
   }

   public final ObjLongTable getCounts() { return this.astCounts; }

	/**
     * This method gets a state from the queue, generates all the
     * possible next states of the state, checks the invariants, and
     * updates the state set and state queue.
	 */
	public final void run() {
		try {
			while (true) {
				if (head == null) {
					return;
				}
				if (this.tlc.doNext(this.astCounts, this)) {
					return;
				}
				dequeue();
			}
		} catch (Throwable e) {
			// Something bad happened. Quit ...
			// Assert.printStack(e);
			if (this.tlc.setErrState(head, null, true)) {
				MP.printError(EC.GENERAL, e); // LL changed call 7 April
												// 2012
			}
			return;
		} finally {
			try {
				finalization.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				e.printStackTrace();
			}
		}
	}

	private void dequeue() {
		// Done with the state. Unlink the current head and make its
		// successor the new head of the list.
		final TLCState oldHead = head;
		head = head.next;
		oldHead.next = null;
		processed++;
	}

	public void enqueueTop(TLCState state) {
		assert state != null;
		if (head == null) {
			assert tail == null;
			head = state;
			tail = state;
		} else {
			head.next = state;
			tail = state;
		}
		generated++;
	}

	public void enqueue(TLCState state) {
		assert state != null;
		tail.next = state;
		tail = state;
		// received additional work
		generated++;
	}

	/**
	 * The length of the linked list of {@link TLCState}s started at head and
	 * ended at tail. This value is an approximation.
	 */
	public long unexplored() {
		return generated - processed;
	}

	public TLCState getHead() {
		return this.head;
	}

	void incrementStatesGenerated(long l) {
		this.statesGenerated += l;		
	}
	
	long getStatesGenerated() {
		return this.statesGenerated;
	}

	void setOutDegree(final int numOfSuccessors) {
		this.outDegree.addSample(numOfSuccessors);
	}

	public IBucketStatistics getOutDegree() {
		return this.outDegree;
	}
	
	public UID writeState(long fp) throws IOException {
		this.lastPtr = this.raf.getFilePointer();
		this.raf.writeShortNat(myGetId());
		this.raf.writeLongNat(1L);
		this.raf.writeLong(fp);
		return new TLCTrace.UID(myGetId(), this.lastPtr);
	}

	public UID writeState(final TLCState curState, final long sucStateFp) throws IOException {
		maxLevel = Math.max(curState.level + 1, maxLevel);
		this.lastPtr = this.raf.getFilePointer();
		this.raf.writeShortNat(curState.uid.wid);
		this.raf.writeLongNat(curState.uid.sid);
		this.raf.writeLong(sucStateFp);
		return new TLCTrace.UID(myGetId(), this.lastPtr);
	}

	public UID getPrev(long loc) throws IOException {
		this.raf.seek(loc);
		return new TLCTrace.UID(raf.readShortNat(), raf.readLongNat());
	}

	public long getFP(long loc) throws IOException {
		this.raf.seek(loc);
		this.raf.readShortNat(); /* drop */
		this.raf.readLongNat(); /* drop */
		return this.raf.readLong();
	}
	
	public int getMaxLevel() {
		return maxLevel;
	}
}
