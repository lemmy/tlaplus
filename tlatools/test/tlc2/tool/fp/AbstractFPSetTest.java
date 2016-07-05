// Copyright (c) 2011 Microsoft Corporation.  All rights reserved.

package tlc2.tool.fp;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;

import junit.framework.TestCase;
import tlc2.TLC;
import util.TLCRuntime;

public abstract class AbstractFPSetTest extends TestCase {

	protected static final String tmpdir = System.getProperty("java.io.tmpdir") + File.separator + "FPSetTest"
					+ System.currentTimeMillis();
	protected static final String filename = "FPSetTestTest";
	
	protected static final DecimalFormat df = new DecimalFormat("###,###.###");
	protected static final DecimalFormat pf = new DecimalFormat("#.##");

	protected long previousTimestamp;
	protected long previousSize;
	protected Date endTimeStamp;
	protected int cnt;

	private File dir;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	
		// create temp folder
		dir = new File(tmpdir);
		dir.mkdirs();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	public void tearDown() {
		// delete all nested files
		final File[] listFiles = dir.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			final File aFile = listFiles[i];
			aFile.delete();
		}
		dir.delete();
	}

	/**
	 * @param freeMemory
	 * @return A new {@link FPSet} instance
	 * @throws IOException
	 */
	protected abstract FPSet getFPSet(long freeMemoryInBytes) throws Exception;

	/**
	 * Implementation based on {@link TLC#handleParameters(String[])}
	 * @return
	 */
	protected long getFreeMemoryInBytes() {
		return TLCRuntime.getInstance().getFPMemSize(.9d);
	}
	
	protected FPSet getFPSetInitialized(int numThreads) throws Exception {
		final FPSet fpSet = getFPSet(getFreeMemoryInBytes());
		fpSet.init(numThreads, tmpdir, filename);
		
		if (fpSet instanceof DiskFPSet) {
			final DiskFPSet diskFPSet = (DiskFPSet) fpSet;
			long maxTblCnt = diskFPSet.getMaxTblCnt();
			System.out.println("Maximum FPSet bucket count is: "
					+ df.format(maxTblCnt) + " (approx: "
					+ df.format(maxTblCnt * FPSet.LongSize >> 20) + " GiB)");
			System.out.println("FPSet lock count is: " + diskFPSet.getLockCnt());
		}
		return fpSet;
	}

	// insertion speed
	public void printInsertionSpeed(final FPSet fpSet) {
		final long currentSize = fpSet.size();
		final long currentTimestamp = System.currentTimeMillis();
		// print every minute
		final double factor = (currentTimestamp - previousTimestamp) / 60000d;
		if (factor >= 1d) {
			long insertions = (long) ((currentSize - previousSize) * factor);
			if (fpSet instanceof DiskFPSet) {
				DiskFPSet diskFPSet = (DiskFPSet) fpSet;
				System.out.println(cnt++ + "; " + System.currentTimeMillis() + " s; " + df.format(insertions)
						+ " insertions/min; " + pf.format(diskFPSet.getLoadFactor()) + " load factor");
			} else {
				System.out.println(cnt++ + "; " + System.currentTimeMillis() + " s (epoch); " + df.format(insertions)
						+ " insertions/min");
			}
			previousTimestamp = currentTimestamp;
			previousSize = currentSize;
		}
	}
}
