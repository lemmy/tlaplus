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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import tlc2.output.EC;
import tlc2.tool.TLCState;
import tlc2.value.ValueInputStream;
import tlc2.value.ValueOutputStream;
import util.Assert;
import util.FileUtil;

public final class Page {

	private final TLCState[] l;
	private final long id;
	private int idx;
	
	Page(final long id, final int length) {
		this.id = id;
		this.l = new TLCState[length];
	}

	public Page(final File f, final long id, final int states) {
		this.id = id;
		TLCState[] buf = null;
		try {
			final ValueInputStream vis = new ValueInputStream(f);
			this.idx = vis.readInt();
			assert this.idx > 0;
			buf = new TLCState[this.idx];
			for (int i = 0; i < buf.length; i++) {
				buf[i] = TLCState.Empty.createEmpty();
				buf[i].read(vis);
			}
			vis.close();
			f.delete();
		} catch (Exception e) {
			Assert.fail(EC.SYSTEM_ERROR_READING_STATES, new String[] { f.getName(),
					(e.getMessage() == null) ? e.toString() : e.getMessage() });
		}
		this.l = buf;
	}

	public void write(final String diskdir) {
		final String tmpFileName = diskdir + FileUtil.separator + Long.toString(id) + ".pq.tmp";
		try {
			final ValueOutputStream vos = new ValueOutputStream(tmpFileName);
			vos.writeInt(this.idx);
			for (int i = 0; i < this.idx; i++) {
				this.l[i].write(vos);
			}
			vos.close();
			
			Files.move(Paths.get(tmpFileName), Paths.get(tmpFileName.replace(".tmp", "")),
					StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			Assert.fail(EC.SYSTEM_ERROR_WRITING_STATES,
					new String[] { tmpFileName, (e.getMessage() == null) ? e.toString() : e.getMessage() });
		}
	}

	public void add(final TLCState state) {
		this.l[idx++] = state;
	}
	
	public boolean isFull() {
		return this.idx == this.l.length;
	}

	public long size() {
		return this.idx;
	}

	public TLCState get(final int idx) {
		return this.l[idx];
	}

	public long id() {
		return this.id;
	}

	@Override
	public String toString() {
		return Long.toString(id);
	}
}
