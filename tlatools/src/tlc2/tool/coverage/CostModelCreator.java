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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import tla2sany.explorer.ExploreNode;
import tla2sany.explorer.ExplorerVisitor;
import tla2sany.semantic.ExprNode;
import tla2sany.semantic.OpApplNode;
import tla2sany.semantic.OpDefNode;
import tla2sany.semantic.SemanticNode;
import tla2sany.semantic.SymbolNode;
import tlc2.TLCGlobals;
import tlc2.tool.Action;
import tlc2.tool.Tool;
import tlc2.util.ObjLongTable;

public class CostModelCreator extends ExplorerVisitor {

	private final Deque<CostModelNode> stack = new ArrayDeque<>();
	private final Set<OpDefNode> opDefNodes = new HashSet<>();
	// OpAppNode does not implement equals/hashCode which causes problem when added
	// to sets or maps. E.g. for a test, an OpApplNode instance belonging to
	// Sequences.tla showed up in coverage output.
	private final Set<OpApplNodeWrapper> nodes = new HashSet<>();
	
	private CostModelCreator(final SemanticNode root) {
		this.stack.push(new OpApplNodeWrapper());
		root.walkGraph(new CoverageHashTable(opDefNodes), this);
	}

	// root cannot be type OpApplNode but has to be SemanticNode (see Test216).
	public CostModelCreator(final Tool tool) {
		// MAK 10/08/2018: Annotate OApplNodes in the semantic tree that correspond to
		// primed vars. It is unclear why OpApplNodes do not get marked as primed when
		// instantiated. The logic in Tool#getPrimedLocs is too obscure to tell.
		final ObjLongTable<SemanticNode>.Enumerator<SemanticNode> keys = tool.getPrimedLocs().keys();
		SemanticNode sn;
		while ((sn = keys.nextElement()) != null) {
			this.nodes.add(new OpApplNodeWrapper((OpApplNode) sn));
		}
	}
	
	public CostModel getCM(final Action act) {
		this.opDefNodes.clear();
		this.stack.clear();
		
		this.stack.push(new ActionWrapper(act));
		act.pred.walkGraph(new CoverageHashTable(opDefNodes), this);
		
		assert this.stack.peek().isRoot();
		return this.stack.peek().getRoot();
	}

	@Override
	public void preVisit(final ExploreNode exploreNode) {
		if (exploreNode instanceof OpApplNode) {
			if (((OpApplNode) exploreNode).isStandardModule()) {
				return;
			}
			final OpApplNodeWrapper oan = new OpApplNodeWrapper((OpApplNode) exploreNode);
			if (nodes.contains(oan)) {
				assert !oan.isPrimed();
				oan.setPrimed();
			}
			
			// CONSTANT operators (this is similar to the lookups in Tool#evalAppl on e.g.
			// line 1442), except that we lookup ToolObject only.
			final Object val = ((OpApplNode) exploreNode).getOperator().getToolObject(TLCGlobals.ToolId);// tool.lookup(((OpApplNode)
			if (val instanceof OpDefNode) {
				final OpDefNode odn = (OpDefNode) val;
				final ExprNode body = odn.getBody();
				if (body instanceof OpApplNode) {
					final CostModelCreator substitution = new CostModelCreator(body);
					oan.addChild((OpApplNodeWrapper) substitution.getModel());
				}
			}			
			
			// RECURSIVE
			final SymbolNode operator = oan.getNode().getOperator();
			if (operator instanceof OpDefNode) {
				final OpDefNode odn = (OpDefNode) operator;
				if (odn.getInRecursive()) {
					final OpApplNodeWrapper recursive = (OpApplNodeWrapper) stack.stream()
							.filter(w -> w.getNode() != null && ((OpApplNode) w.getNode()).getOperator() == odn).findFirst()
							.orElse(null);
					if (recursive != null) {
						oan.setRecursive(recursive);
					}
				}
			}
			
			final CostModelNode parent = stack.peek();
			parent.addChild(oan.setLevel(parent.getLevel() + 1));
			stack.push(oan);
		} else if (exploreNode instanceof OpDefNode) {
			//TODO Might suffice to just keep RECURSIVE ones.
			opDefNodes.add((OpDefNode) exploreNode);
		}
	}

	@Override
	public void postVisit(final ExploreNode exploreNode) {
		if (exploreNode instanceof OpApplNode) {
			if (((OpApplNode) exploreNode).isStandardModule()) {
				return;
			}
			final CostModelNode pop = stack.pop();
			assert pop.getNode() == exploreNode;
		} else if (exploreNode instanceof OpDefNode) {
			final boolean removed = opDefNodes.remove((OpDefNode) exploreNode);
			assert removed;
		}
	}

	public CostModel getModel() {
		assert this.stack.peek().isRoot();
		return this.stack.peek().getRoot();
	}
}
