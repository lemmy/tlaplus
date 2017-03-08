// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon 30 Apr 2007 at 14:01:40 PST by lamport  
//      modified on Wed Dec  5 15:35:42 PST 2001 by yuanyu   

package tlc2.tool;

import java.io.IOException;

import tlc2.output.EC;
import tlc2.output.MP;
import tlc2.tool.TLCTrace.UID;
import tlc2.tool.queue.IStateQueue;
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
	private final IStateQueue squeue;
	private final ObjLongTable astCounts;
	private final IBucketStatistics outDegree;
	private long statesGenerated;

	private final BufferedRandomAccessFile raf;
	private long lastPtr;
	private volatile int maxLevel = 0;

	// SZ Feb 20, 2009: changed due to super type introduction
	public Worker(int id, AbstractChecker tlc, String metadir, String specFile) throws IOException {
		super(id);
		// SZ 12.04.2009: added thread name
		this.setName("TLC Worker " + id);
		this.tlc = (ModelChecker) tlc;
		this.squeue = this.tlc.theStateQueue;
		this.astCounts = new ObjLongTable(10);
		this.outDegree = new FixedSizedBucketStatistics(this.getName(), 32); // maximum outdegree of 32 appears sufficient for now.
		this.setName("TLCWorkerThread-" + String.format("%03d", id));

		final String filename = metadir + FileUtil.separator + specFile + "-" + myGetId() + TLCTrace.EXT;
		this.raf = new BufferedRandomAccessFile(filename, "rw");
	}

  public final ObjLongTable getCounts() { return this.astCounts; }

	/**
   * This method gets a state from the queue, generates all the
   * possible next states of the state, checks the invariants, and
   * updates the state set and state queue.
	 */
	public final void run() {
		TLCState curState = null;
		try {
			while (true) {
				curState = (TLCState) this.squeue.sDequeue();
				if (curState == null) {
					synchronized (this.tlc) {
						this.tlc.setDone();
						this.tlc.notify();
					}
					this.squeue.finishAll();
					return;
				}
				if (this.tlc.doNext(curState, this.astCounts, this))
					return;
			}
		} catch (Throwable e) {
			// Something bad happened. Quit ...
			// Assert.printStack(e);
			synchronized (this.tlc) {
				if (this.tlc.setErrState(curState, null, true)) {
					MP.printError(EC.GENERAL, e); // LL changed call 7 April
													// 2012
				}
				this.squeue.finishAll();
				this.tlc.notify();
			}
			return;
		}
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
