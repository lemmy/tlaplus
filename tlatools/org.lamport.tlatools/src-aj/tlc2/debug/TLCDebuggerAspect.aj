/*******************************************************************************
 * Copyright (c) 2020 Microsoft Research. All rights reserved. 
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
package tlc2.debug;

import tla2sany.semantic.SemanticNode;
import tlc2.debug.IDebugTarget;
import tlc2.tool.TLCState;
import tlc2.tool.coverage.CostModel;
import tlc2.tool.impl.Tool;
import tlc2.util.Context;
import tlc2.value.IValue;
import tlc2.value.impl.Value;

public aspect TLCDebuggerAspect {
	
	private int level = 0;
	private IDebugTarget target = IDebugTarget.Factory.getInstance();
	
	/*
	  @Override
	  public final IValue eval(SemanticNode expr, Context c, TLCState s0) {
		    return this.eval(expr, c, s0, TLCState.Empty, EvalControl.Clear, CostModel.DO_NOT_RECORD);
	  }
     */
	
	pointcut eval(SemanticNode expr, Context c, TLCState s0) : execution(public final IValue Tool.eval(SemanticNode, Context, TLCState))
	&& args(expr, c, s0)
	&& !within(TLCDebuggerAspect) 
	&& !(cflow(execution(IDebugTarget pushFrame(Tool, int, SemanticNode, Context, int))) || cflow(execution(IDebugTarget popFrame(Tool, Value, int, SemanticNode, Context, int))) );

	IValue around(SemanticNode expr, Context c, TLCState s0): (eval(expr, c, s0)) {
		target = IDebugTarget.Factory.getInstance();
		level = 0;
		IValue v = proceed(expr, c, s0);
		return v;
	}
	
	/*
  @ExpectInlined
  protected final Value evalImpl(final SemanticNode expr, final Context c, final TLCState s0,
          final TLCState s1, final int control, CostModel cm) {
	 */
	pointcut evalImpl(SemanticNode expr, Context c, TLCState s0, TLCState s1, int control, CostModel cm) : execution(protected final Value Tool.evalImpl(SemanticNode, Context, TLCState, TLCState, int, CostModel))
	&& args(expr, c, s0, s1, control, cm) && !within(TLCDebuggerAspect)
	&& !(cflow(execution(IDebugTarget pushFrame(Tool, int, SemanticNode, Context, int))) || cflow(execution(IDebugTarget popFrame(Tool, Value, int, SemanticNode, Context, int))) );
	
	Value around(SemanticNode expr, Context c, TLCState s0, TLCState s1, int control, CostModel cm): (evalImpl(expr, c, s0, s1, control, cm)) {
		level++;

		final Tool t = (Tool) thisJoinPoint.getThis();
		target = target.pushFrame(t, level, expr, c, control);
		
		final Value v = proceed(expr, c, s0, s1, control, cm);
		
		target = target.popFrame(t, v, level, expr, c, control);
		level--;
		return v;
	}
}
