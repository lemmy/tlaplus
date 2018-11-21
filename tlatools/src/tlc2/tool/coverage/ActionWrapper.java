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

import tla2sany.semantic.OpDefNode;
import tla2sany.semantic.SemanticNode;
import tla2sany.semantic.SubstInNode;
import tla2sany.st.Location;
import tla2sany.st.SyntaxTreeConstants;
import tla2sany.st.TreeNode;
import tlc2.TLCGlobals;
import tlc2.output.EC;
import tlc2.output.MP;
import tlc2.tool.Action;
import tlc2.util.statistics.CounterStatistic;

public class ActionWrapper extends CostModelNode {

	public enum Relation {
		INIT, NEXT, PROP;
	}
	
	private final CounterStatistic unseen = CounterStatistic.getInstance(() -> TLCGlobals.isCoverageEnabled());
	private final Action action;
	private final Relation relation;
	
	public ActionWrapper(final Action action, Relation rel) {
		this.action = action;
		this.relation = rel;
	}
	
	/* (non-Javadoc)
	 * @see tlc2.tool.coverage.CostModelNode#getLocation()
	 */
	@Override
	protected Location getLocation() {
		if (this.action.getOpDef() != null) {
			final OpDefNode opDef = this.action.getOpDef();
			final TreeNode tn = opDef.getTreeNode();
			if (tn != null && tn.one() != null && tn.one().length >= 1) {
				final TreeNode treeNode = tn.one()[0];
				assert treeNode.isKind(SyntaxTreeConstants.N_IdentLHS);
				return treeNode.getLocation();
			}
//			return opDef.getLocation();
		}
		return this.action.pred.getLocation();
	}
	
	/* (non-Javadoc)
	 * @see tlc2.tool.coverage.CostModelNode#incUnseen()
	 */
	@Override
	public void incUnseen() {
		this.unseen.increment();
	}
	
	/* (non-Javadoc)
	 * @see tlc2.tool.coverage.CostModelNode#incUnseen(long)
	 */
	@Override
	public void incUnseen(final long value) {
		this.unseen.add(value);
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.coverage.CostModelNode#getRoot()
	 */
	@Override
	public CostModelNode getRoot() {
		return this;
	}
	
	/* (non-Javadoc)
	 * @see tlc2.tool.CostModel#get(tla2sany.semantic.SemanticNode)
	 */
	@Override
	public CostModel get(final SemanticNode eon) {
		if (eon instanceof SubstInNode) {
			final SubstInNode sin = (SubstInNode) eon;
			return this.children.get(sin.getBody());
		}
		return this.children.get(eon);
	}

	@Override
	SemanticNode getNode() {
		return action.pred;
	}

	@Override
	boolean isRoot() {
		return true;
	}

	public void report() {
		// Report count for action itself.
		if (relation == Relation.PROP) {
			assert getEvalCount() == 0L && this.unseen.getCount() == 0L;
			MP.printMessage(EC.TLC_COVERAGE_PROPERTY, new String[] { getLocation().toString() });
		} else if (relation == Relation.INIT) {
			assert this.unseen.getCount() == 0L;
			MP.printMessage(EC.TLC_COVERAGE_INIT,
					new String[] { getLocation().toString(), String.valueOf(getEvalCount()) });
		} else {
			MP.printMessage(EC.TLC_COVERAGE_NEXT, new String[] { getLocation().toString(),
					String.valueOf(this.unseen.getCount()), String.valueOf(getEvalCount()) });
		}

		// An action has single child which is the OpApplNodeWrapper with the OpApplNode
		// for this OpDefNode unless the action's pred is a substitution.
		assert !(this.action.pred instanceof SubstInNode) ? this.children.size() == 1 : !this.children.isEmpty();
		// Let children report.
		this.children.values().forEach(c -> c.report());
	}
}
