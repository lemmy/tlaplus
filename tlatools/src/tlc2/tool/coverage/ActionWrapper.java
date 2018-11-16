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

import tla2sany.semantic.SemanticNode;
import tla2sany.st.Location;
import tlc2.tool.Action;

public class ActionWrapper extends CostModelNode {

	private final Action action;
	
	public ActionWrapper(final Action action) {
		this.action = action;
	}
	
	/* (non-Javadoc)
	 * @see tlc2.tool.coverage.CostModelNode#getLocation()
	 */
	@Override
	protected Location getLocation() {
		return this.action.pred.getLocation();
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.CostModel#increment(tla2sany.semantic.SemanticNode)
	 */
	@Override
	public void increment(final SemanticNode ast) {
		increment();
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.CostModel#get(tla2sany.semantic.SemanticNode)
	 */
	@Override
	public CostModel get(final SemanticNode cmpts) {
		assert action.pred == cmpts;
		return this;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.CostModel#matches(tla2sany.semantic.SemanticNode)
	 */
	@Override
	public boolean matches(final SemanticNode expr) {
		return true;
	}

	@Override
	SemanticNode getNode() {
		return action.pred;
	}

	@Override
	CostModelNode getRoot() {
		return this;
	}

	@Override
	boolean isRoot() {
		return true;
	}
}
