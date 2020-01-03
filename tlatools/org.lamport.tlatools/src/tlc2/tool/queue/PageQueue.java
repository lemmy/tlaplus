/*******************************************************************************
 * Copyright (c) 2019 Microsoft Research. All rights reserved. 
 *
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/
package tlc2.tool.queue;

import java.io.File;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import tlc2.TLCGlobals;
import tlc2.tool.TLCState;
import tlc2.tool.Worker;
import util.FileUtil;
import util.ToolIO;

public final class PageQueue {
	
	private static final long FINISH = -1L;
	
	private static final long InMemoryPages = Integer.getInteger(PageQueue.class.getName() + ".InMemoryPages",
			1 + new Random().nextInt(1000));

	private static final int PageSize = Integer.getInteger(PageQueue.class.getName() + ".PageSize",
			1 + new Random().nextInt(8196));
	
	// For the moment represent the disk with an in-memory hash map. 
	private final Map<Long, Page> pages = new ConcurrentHashMap<>();

	private final int numWorkers = TLCGlobals.getNumWorkers();
	
	private final AtomicLong head = new AtomicLong(0L);
	
	private final AtomicLong tail = new AtomicLong(0L);

	private final String diskdir;
	
	public PageQueue(String diskdir) {
		this.diskdir = diskdir;
		ToolIO.out.println(String.format("Loaded PageQueue (MemLimit=%s, PageSize=%s)\n", InMemoryPages, PageSize));
	}

	public final Page claim() {
		return new Page(head.incrementAndGet(), pageSize());
	}
	
	public final void enqueue(final TLCState initialState) {
		final Page page = claim();
		page.add(initialState);
		this.enqueue(page);
	}

	public final void enqueue(final Page page) {
		/** wrt-action **/
		if (page.id() > InMemoryPages) {
			page.write(this.diskdir);
		} else {
			this.pages.put(page.id(), page);
		}
	}

	public final Page dequeue(final Worker worker) {
		/**
            (****************************************************************)
            (* 1. Stage: Dequeue an unexplored page iff one is available.   *)
            (****************************************************************)
		 */
		/** deq-action **/
		long t = tail.get();
		if (t == FINISH) {
			return null;
		}
		/** casA-action **/
		if (!tail.compareAndSet(t, t + 1L)) {
			// Slow path: Page t is assigned to a different worker.
			return dequeue(worker);
		}
		t = t + 1;
		
		/**
            (***************************************************************)
            (* Spin until a page is available and can be read. In case all *)
            (* other workers spin here too, the workers will eventually    *)
            (* terminate once one of the worker CASes "fin".               *)
            (***************************************************************)
		 */
		/** wt-action **/
		Page page = null;
		int	retries = 0;
		LOOP: while ((page = getPage(t)) == null) {
			// Slow path:
			/** wt1-action: **/
			final long t2 = tail.get();
			final long h2 = head.get();
			
			if (t2 == FINISH) {
				return null;
			} else if (h2 == t2 - numWorkers) {
				assert !worker.hasPage();
				finishAll();
				return null;
			} else if (worker.hasPage()) {
				// Either release local page when pages run low globally or after number of
				// retries. Retries takes care of cyclic wait-for graphs where one or more
				// workers spin to read the other worker's page (i.e. a worker with local page
				// 42 gets assigned page 42).
				if (h2 <= t2 || retries++ == 10) {
					this.enqueue(worker.releasePage());
					continue LOOP;
				}
			}
		}
		/** rd-action **/
		return page;
	}

	private final Page getPage(final long t) {
		if (t > InMemoryPages) {
			final File f = new File(diskdir + FileUtil.separator + Long.toString(t) + ".pq");
			if (f.exists()) {
				return new Page(f, t, pageSize(t));
			} else {
				// Slow path:
				if (numWorkers == 1) {
					// Don't be schizophrenic and wait for others when there is nobody else out
					// there.
					return null;
				} else {
					try {
						Thread.sleep(500L);
					} catch (InterruptedException whyDoWeIgnoreThis) {
						whyDoWeIgnoreThis.printStackTrace();
					}
				}
				return null;
			}
		} else {
			return this.pages.remove(t);
		}
	}
	
	public final boolean isEmpty() {
		return head.get() == 0;
	}

	public final long size() {
		// Approximate...
		long low = tail.get();
		if (t < 0) {
			low = t;
		}
		
		final long high = head.get();
		final long numPages = high - low;
		if (numPages <= 0) {
			return 0;
		}
		
		return numPages * pageSize(high);
	}
	
	// TODO t could be the return value of suspendAll and the parameter to
	// resumeAll. Logically, kind of like a ticket number.
	private long t = 0L;

	public final void finishAll() {
		this.tail.set(FINISH);
	}
	
	public final boolean suspendAll() {
		throw new UnsupportedOperationException("suspendAll not yet implemented");
	}

	public final void resumeAll() {
		throw new UnsupportedOperationException("suspendAll not yet implemented");
	}

	public final int pageSize() {
		return PageSize;
//		return pageSize(head.get());
	}

	private static final int pageSize(final long h) {
		if (h < 10) {
			return 1;
		} else if (h < 100) {
			return 10;
		} else if (h < 1000) {
			return 10000;
		} else if (h < 10000) {
			return 100000;
		}
		return 1000000;
	}
}
