/*******************************************************************************
 * Copyright (c) 2018 Microsoft Research. All rights reserved. 
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
package tlc2.tool.coverage;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import tla2sany.semantic.SemanticNode;
import tla2sany.st.Location;
import tlc2.TLCGlobals;
import tlc2.output.EC;
import tlc2.output.MP;
import tlc2.util.statistics.CounterStatistic;

public abstract class CostModelNode implements CostModel {
	
	// children has to preserve order to later traverse tree in the module location
	// order. Thus, use LinkedHashMap here.
	protected final Map<SemanticNode, CostModelNode> children = new LinkedHashMap<>();

	protected final CounterStatistic stats = CounterStatistic.getInstance(() -> TLCGlobals.isCoverageEnabled());
	
	// ---------------- Statistics ---------------- //

	protected long getEvalCount() {
		return this.stats.getCount();
	}

	protected long getCount(Set<Long> collectWeights) {
		assert collectWeights.size() == 1;
		for (Long l : collectWeights) {
			return l;
		}
		return -1l; // make compiler happy
	}

	@Override
	public void add(long size) {
		this.stats.add(size);
	}

	@Override
	public void increment() {
		this.stats.increment();
	}
	
	// -- -- //

	protected boolean isPrimed() {
		return false;
	}

	protected abstract Location getLocation();

	// ---------------- Print ---------------- //
	
	public void report() {
		print(0, Calculate.FRESH);
	}

	protected void print(int level, final Calculate fresh) {
		final Set<Long> collectedEvalCounts = new HashSet<>();
		this.collectChildren(collectedEvalCounts, fresh);
		if (collectedEvalCounts.isEmpty()) {
			// Subtree has nothing to report.
			if (getEvalCount() == 0l && !isPrimed()) {
				// ..this node neither.
				return;
			} else {
				printSelf(level++);
				return; // Do not invoke printSelf(...) again below.
			}
		}

		if (collectedEvalCounts.size() == 1) {
			final long count = getCount(collectedEvalCounts);

			if (count < getEvalCount()) {
				// Cannot collapse subtree because inconsistent with this node.
				printSelf(level++);
				printChildren(level);
				return;
			}
			if (!isPrimed() && getEvalCount() == 0l && count != 0l) {
				// Collapse consistent subtree into this node unless this node is primed.
				printSelf(level++, count);
				return;
			}
			if (getEvalCount() == count && count == 0l) {
				if (isPrimed()) {
					printSelf(level++);
				}
				// Have a primed in subtree.
				printChildren(level);
				return;
			}
			if (getEvalCount() == count) {
				// Have a primed in subtree.
				printSelf(level++);
				return;
			}
		}

		// Subtree is inconsistent and needs to report itself.
		if (getEvalCount() > 0 || isPrimed()) {
			printSelf(level++);
		}
		printChildren(level);
	}
	
	protected void printChildren(final int level) {
		for (CostModelNode cmn : children.values()) {
			cmn.print(level, Calculate.CACHED);
		}
	}

	protected void printSelf(final int level, final long count) {
		MP.printMessage(EC.TLC_COVERAGE_VALUE, new String[] {
				indentBegin(level, TLCGlobals.coverageIndent, getLocation().toString()), String.valueOf(count) });
	}

	protected void printSelf(final int level) {
		printSelf(level, getEvalCount());
	}

	protected static String indentBegin(final int n, final char c, final String str) {
		assert n >= 0;
		final String whitespaces = new String(new char[n]).replace('\0', c);
		return whitespaces + str;
	}
	
	// ---------------- Child counts ---------------- //

	private enum Calculate {
		FRESH, CACHED;
	}

	protected final Set<Long> childCounts = new HashSet<>();

	protected void collectChildren(final Set<Long> result, Calculate c) {
		for (CostModelNode cmn : children.values()) {
			cmn.collectEvalCounts(result, c);
		}
	}

	protected void collectEvalCounts(final Set<Long> result, Calculate c) {
		if (c == Calculate.FRESH) {
			childCounts.clear();
			if (getEvalCount() > 0 || this.isPrimed()) {
				childCounts.add(this.getEvalCount());
			}
			collectChildren(childCounts, c);
		}
		result.addAll(childCounts);
	}
	
	// -- --//
	
	void addChild(final CostModelNode child) {
		final boolean newlyInserted = this.children.put(child.getNode(), child) == null;
		assert newlyInserted;
	}

	abstract SemanticNode getNode();
	
	abstract CostModelNode getRoot();
	
	boolean isRoot() {
		return false;
	}

	int getLevel() {
		return 0;
	}
}
