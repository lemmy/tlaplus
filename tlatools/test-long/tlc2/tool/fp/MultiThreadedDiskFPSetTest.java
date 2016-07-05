// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp;

import tlc2.tool.fp.management.DiskFPSetMXWrapper;

public class MultiThreadedDiskFPSetTest extends MultiThreadedFPSetTest {

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.AbstractFPSetTest#getFPSet(long)
	 */
	@Override
	protected FPSet getFPSet(final long freeMemory) throws Exception {
		final DiskFPSet diskFPSet = new DiskFPSet(freeMemory);
		new DiskFPSetMXWrapper(diskFPSet);
		return diskFPSet;
	}
}
