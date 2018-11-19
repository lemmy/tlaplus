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

import tla2sany.semantic.OpApplNode;
import tla2sany.semantic.SemanticNode;
import tla2sany.semantic.SubstInNode;
import tla2sany.st.Location;

public class OpApplNodeWrapper extends CostModelNode implements Comparable<OpApplNodeWrapper>, CostModel {

	private final OpApplNode node;
	private boolean primed = false;
	private int level;
	private OpApplNodeWrapper recursive;

	OpApplNodeWrapper(OpApplNode node) {
		super();
		this.node = node;
		this.level = 0;
	}

	OpApplNodeWrapper() {
		this(null);
	}

	// For unit testing only.
	OpApplNodeWrapper(OpApplNode node, long samples) {
		this(node);
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

	@Override
	protected Location getLocation() {
		return this.node != null ? this.node.getLocation() : Location.nullLoc;
	}

	public OpApplNode getNode() {
		return this.node;
	}
	
	public boolean isRoot() {
		return this.node == null;
	}

	@Override
	public void increment(final SemanticNode oan) {
		if (oan != node) {
			throw new RuntimeException("Reporting cost metrics into wrong node.");
		}
		increment();
	}

	// ---------------- Parent <> Child ---------------- //

	public OpApplNodeWrapper setRecursive(OpApplNodeWrapper recursive) {
		assert this.recursive == null;
		this.recursive = recursive;
		return this;
	}
	
	public CostModelNode getRoot() {
		return this.children.values().iterator().next();
	}
	
	@Override
	public CostModel get(final SemanticNode eon) {
		if (eon == this.node || !(eon instanceof OpApplNode)) {
			return this;
		}
		
		CostModelNode child = children.get(eon);
		if (child != null) {
			return child;
		}
		
		if (recursive != null) {
			child = recursive.children.get(eon);
			if (child != null) {
				return child;
			}
		}
		
		// TODO Not all places in Tool lookup the correct CM yet. This should only be an
		// engineering effort but no fundamental problem expect that SubstInNode has not
		// been looked into yet.
		throw new RuntimeException("Couldn't find child where one should be!");
	}

	@Override
	public boolean matches(final SemanticNode expr) {
		if (expr instanceof OpApplNode) {
			return expr == node;
		} else if (expr instanceof SubstInNode) {
			final SubstInNode sin = (SubstInNode) expr;
			return sin.getBody() == node;
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
}