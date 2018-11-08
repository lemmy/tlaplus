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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tla2sany.semantic.OpApplNode;
import tla2sany.semantic.SemanticNode;
import tla2sany.st.Location;
import tlc2.TLCGlobals;
import tlc2.output.EC;
import tlc2.output.MP;
import tlc2.tool.CostModel;
import tlc2.util.statistics.CounterStatistic;

public class OpApplNodeWrapper implements Comparable<OpApplNodeWrapper>, CostModel {

	// children has to preserve order to later traverse tree in the module location
	// order.
//	private final Set<OpApplNodeWrapper> children = new TreeSet<>();
	private final List<OpApplNodeWrapper> children = new ArrayList<>();
	private final OpApplNode node;
	private final CounterStatistic stats = CounterStatistic.getInstance(() -> TLCGlobals.isCoverageEnabled());
	private boolean primed = false;
	private int level;
	private OpApplNodeWrapper recursive;

	public OpApplNodeWrapper(OpApplNode node) {
		this.node = node;
	}

	public OpApplNodeWrapper() {
		this.node = null;
		this.level = 0;
	}

	OpApplNodeWrapper(long samples) {
		// For testing only.
		this();
		this.add(samples);
	}

	// ---------------- Identity... ---------------- //
	
	@Override
	public int compareTo(OpApplNodeWrapper arg0) {
		return this.getLocation().compareTo(arg0.getLocation());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node.getLocation() == null) ? 0 : node.getLocation().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		OpApplNodeWrapper other = (OpApplNodeWrapper) obj;
		if (node.getLocation() == null) {
			if (other.node.getLocation() != null)
				return false;
		} else if (!node.getLocation().equals(other.node.getLocation())) {
			return false;
		}
		return true;

	}

	@Override
	public String toString() {
		if (this.node == null) {
			return "root";
		}
		return node.toString();
	}
	
	// ----------------  ---------------- //

	private Location getLocation() {
		return this.node != null ? this.node.getLocation() : Location.nullLoc;
	}

	public OpApplNode getNode() {
		return this.node;
	}

	// ---------------- Parent <> Child ---------------- //

	public OpApplNodeWrapper setRecursive(OpApplNodeWrapper recursive) {
		assert this.recursive == null;
		this.recursive = recursive;
		return this;
	}

	public boolean isRoot() {
		return this.node == null;
	}

	public void addChild(final OpApplNodeWrapper child) {
		this.children.add(child);
	}
	
	public List<OpApplNodeWrapper> getChildren() {
		return this.children;
	}

	@Override
	public CostModel get(final SemanticNode eon) {
		if (eon == this.node) {
			return this;
		}
		if (eon instanceof OpApplNode) {
			// TODO certainly too slow for actual impl!
			for (OpApplNodeWrapper child : children) {
				if (child.node == eon) {
					return child;
				}
			}
			
			if (recursive != null) {
				for (OpApplNodeWrapper child : recursive.children) {
					if (child.node == eon) {
						return child;
					}
				}
			}
			// TODO Not all places in Tool lookup the correct CM yet. This should only be an
			// engineering effort but no fundamental problem expect that SubstInNode has not
			// been looked into yet.
			throw new RuntimeException("Couldn't find child where one should be!");
		}
		return this;
	}

	@Override
	public boolean matches(final SemanticNode expr) {
		if (expr instanceof OpApplNode) {
			return expr == node;
		}
		return true;
	}

	// ---------------- Level ---------------- //

	public int getLevel() {
		return this.level;
	}

	public OpApplNodeWrapper setLevel(int level) {
		this.level = level;
		return this;
	}

	// ---------------- Primed ---------------- //
	
	public OpApplNodeWrapper setPrimed() {
		this.primed = true;
		return this;
	}

	public boolean isPrimed() {
		return this.primed;
	}
	
	// ---------------- Statistics ---------------- //

	private long getEvalCount() {
		return this.stats.getCount();
	}

	private long getCount(Set<Long> collectWeights) {
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

	public void increment(final SemanticNode oan) {
		if (oan != node) {
			throw new RuntimeException("Reporting cost metrics into wrong node.");
		}
		increment();
	}

	public void increment() {
		this.stats.increment();
	}

	// ---------------- Print ---------------- //
	
	public void report() {
		print(0, Calculate.FRESH);
	}

	public void print(int level, final OpApplNodeWrapper.Calculate fresh) {
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
	
	private void printChildren(final int level) {
		for (OpApplNodeWrapper opApplNodeWrapper : children) {
			opApplNodeWrapper.print(level, Calculate.CACHED);
		}
	}

	private void printSelf(final int level, final long count) {
		MP.printMessage(EC.TLC_COVERAGE_VALUE, new String[] {
				indentBegin(level, TLCGlobals.coverageIndent, getLocation().toString()), String.valueOf(count) });
	}

	private void printSelf(final int level) {
		printSelf(level, getEvalCount());
	}

	private static String indentBegin(final int n, final char c, final String str) {
		assert n >= 0;
		final String whitespaces = new String(new char[n]).replace('\0', c);
		return whitespaces + str;
	}
	
	// ---------------- Child counts ---------------- //

	private enum Calculate {
		FRESH, CACHED;
	}

	private final Set<Long> childCounts = new HashSet<>();

	private void collectChildren(final Set<Long> result, OpApplNodeWrapper.Calculate c) {
		for (OpApplNodeWrapper opApplNodeWrapper : children) {
			opApplNodeWrapper.collectEvalCounts(result, c);
		}
	}

	public void collectEvalCounts(final Set<Long> result, OpApplNodeWrapper.Calculate c) {
		if (c == Calculate.FRESH) {
			childCounts.clear();
			if (getEvalCount() > 0 || this.isPrimed()) {
				childCounts.add(this.getEvalCount());
			}
			collectChildren(childCounts, c);
		}
		result.addAll(childCounts);
	}
}