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
package tlc2.value;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import tlc2.tool.Action;
import tlc2.tool.ActionItemList;
import tlc2.tool.IStateFunctor;
import tlc2.tool.StateVec;
import tlc2.tool.TLCState;
import tlc2.tool.coverage.CostModel;
import tlc2.util.Context;
import tla2sany.semantic.OpApplNode;
import tla2sany.semantic.SemanticNode;

@Aspect
public class CostModelAspect {
	
	// -------------------------------- //
	
/*
  private final void getInitStates(ActionItemList acts, TLCState ps, IStateFunctor states, CostModel cm) {
 */

	@Pointcut("execution(private void tlc2.tool.Tool.getInitStates(tlc2.tool.ActionItemList, tlc2.tool.TLCState, ..))"
			 + "&& args(acts, ps, .., cm)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void getInitStates(ActionItemList acts, TLCState ps, CostModel cm) {
	}
	
	@Around("getInitStates(acts, ps, cm)")
	public void getInitStates(final ActionItemList acts, final TLCState ps, final CostModel cm, final ProceedingJoinPoint call) throws Throwable {
		if (acts.isEmpty() || ps.allAssigned()) {
			cm.incInvocations();
			cm.getRoot().incInvocations();
		}
		
		call.proceed();
	}

	// -------------------------------- //
	
/*
  public StateVec getNextStates(Action action, TLCState state) {
    action.cm.incInvocations(nss.size());
  }
*/

	@Pointcut("execution(public tlc2.tool.StateVec tlc2.tool.Tool.getNextStates(tlc2.tool.Action, tlc2.tool.TLCState))"
			 + "&& args(action, state)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void getNextStatesAction(Action action, TLCState state) {
	}
	
	@Around("getNextStatesAction(action, state)")
	public Object getNextStatesAction(final Action action, final TLCState state, final ProceedingJoinPoint call) throws Throwable {
		final StateVec nss = (StateVec) call.proceed();
		action.cm.incInvocations(nss.size());
		
		return nss;
	}

	// -------------------------------- //
	
/*
  private final TLCState getNextStates(ActionItemList acts, final TLCState s0, final TLCState s1, final StateVec nss, CostModel cm) {
	  final TLCState copy = getNextStates0(acts, s0, s1, nss, cm);
	  if (copy != s1) {
		  cm.incInvocations();
	  }
	  return copy;
  }
*/

	@Pointcut("execution(private tlc2.tool.TLCState tlc2.tool.Tool.getNextStates(tlc2.tool.ActionItemList, tlc2.tool.TLCState, ..))"
			 + "&& args(acts, s0, s1, nss, cm)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void getNextStates(ActionItemList acts, TLCState s0, TLCState s1, StateVec nss, CostModel cm) {
	}
	
	@Around("getNextStates(acts, s0, s1, nss, cm)")
	public Object getNextStates(final ActionItemList acts, final TLCState s0, final TLCState s1, final StateVec nss, final CostModel cm, final ProceedingJoinPoint call) throws Throwable {
		final TLCState copy = (TLCState) call.proceed();
		if (copy != s1) {
			cm.incInvocations();
		}
		return copy;
	}

	// -------------------------------- //.get(sn)
	
/*
  private final Value setSource(final SemanticNode expr, final Value value, CostModel cm) {
    value.setCostModel(cm.get(expr));
*/

	@Pointcut("execution(private tlc2.value.Value tlc2.tool.Tool.setSource(tla2sany.semantic.SemanticNode, ..))"
			 + "&& args(expr, value, cm)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void setSource(SemanticNode expr, final Value value, CostModel cm) {
	}
	
	@Around("setSource(expr, value, cm)")
	public Object setSource(final SemanticNode expr, final Value value, final CostModel cm, final ProceedingJoinPoint call) throws Throwable {
		value.setCostModel(cm.get(expr));
		return call.proceed();
	}

	// -------------------------------- //
	
/*
	private final Value evalAppl(final OpApplNode expr, Context c, TLCState s0, TLCState s1, final int control, CostModel cm) {
		cm = cm.get(expr);
		cm.incInvocations();
*/

	@Pointcut("execution(private tlc2.value.Value tlc2.tool.Tool.evalAppl(tla2sany.semantic.OpApplNode, ..))"
			 + "&& args(expr, c, s0, s1, control, cm)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void evalsAppl(OpApplNode expr, Context c, TLCState s0, TLCState s1, int control, CostModel cm) {
	}
	
	@Around("evalsAppl(expr, c, s0, s1, control, cm)")
	public Object evalsAppl(final OpApplNode expr, final Context c, final TLCState s0, final TLCState s1, final int control, final CostModel cm, final ProceedingJoinPoint call) throws Throwable {
		final CostModel cmNew = cm.get(expr).incInvocations();
		return call.proceed(new Object[] {expr, c, s0, s1, control, cmNew});
	}
	
	// -------------------------------- //
	
/*
    
  private final TLCState getNextStates(SemanticNode pred, ActionItemList acts, Context c, TLCState s0, TLCState s1, StateVec nss, CostModel cm) {
    cm = cm.get(pred);
    
  private final TLCState processUnchanged(SemanticNode expr, ActionItemList acts, Context c, TLCState s0, TLCState s1, StateVec nss, CostModel cm) {
    cm = cm.get(expr);
*/

	@Pointcut("(execution(private tlc2.tool.TLCState tlc2.tool.Tool.getNextStates(tla2sany.semantic.SemanticNode, ..)) ||"
			 + "execution(private tlc2.tool.TLCState tlc2.tool.Tool.processUnchanged(tla2sany.semantic.SemanticNode, ..)))"
			 + "&& args(expr, acts, c, s0, s1, nss, cm)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void evals6(SemanticNode expr, ActionItemList acts, Context c, TLCState s0, TLCState s1, StateVec nss, CostModel cm) {
	}
	
	@Around("evals6(expr, acts, c, s0, s1, nss, cm)")
	public Object evals6(SemanticNode expr, ActionItemList acts, Context c, TLCState s0, TLCState s1, StateVec nss, CostModel cm, final ProceedingJoinPoint call) throws Throwable {
		return call.proceed(new Object[] {expr, acts, c, s0, s1, nss, cm.get(expr)});
	}

	// -------------------------------- //
	
/*
  private final void getInitStatesAppl(OpApplNode init, ActionItemList acts, Context c, TLCState ps, IStateFunctor states, CostModel cm) {
    cm = cm.get(init);
*/
	
	@Pointcut("execution(private void tlc2.tool.Tool.getInitStatesAppl(tla2sany.semantic.OpApplNode, .., tlc2.tool.coverage.CostModel))"
			 + "&& args(expr, acts, c, s0, states, cm)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void getInitStatesAppl(OpApplNode expr, ActionItemList acts, Context c, TLCState s0, IStateFunctor states, CostModel cm) {
	}
	
	@Around("getInitStatesAppl(expr, acts, c, s0, states, cm)")
	public Object getInitStatesAppl(final OpApplNode expr, final ActionItemList acts, final Context c, final TLCState s0, final IStateFunctor states, final CostModel cm, final ProceedingJoinPoint call) throws Throwable {
		return call.proceed(new Object[] {expr, acts, c, s0, states, cm.get(expr)});
	}
	
	// -------------------------------- //
	
/*
  private final TLCState enabledAppl(OpApplNode pred, ActionItemList acts, Context c, TLCState s0, TLCState s1, CostModel cm) {
    cm = cm.get(pred);
*/
	
	@Pointcut("execution(private tlc2.tool.TLCState tlc2.tool.Tool.enabledAppl(tla2sany.semantic.OpApplNode, .., tlc2.tool.coverage.CostModel))"
			 + "&& args(expr, acts, c, s0, s1, cm)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void enableAppl(OpApplNode expr, ActionItemList acts, Context c, TLCState s0, TLCState s1, CostModel cm) {
	}
	
	@Around("enableAppl(expr, acts, c, s0, s1, cm)")
	public Object enableAppl(final OpApplNode expr, final ActionItemList acts, final Context c, final TLCState s0, final TLCState s1, final CostModel cm, final ProceedingJoinPoint call) throws Throwable {
		return call.proceed(new Object[] {expr, acts, c, s0, s1, cm.get(expr)});
	}
	
	// -------------------------------- //
	
/*
  public final Value eval(SemanticNode expr, Context c, TLCState s0, TLCState s1, final int control, CostModel cm) {
	 cm = cm.get(expr)
*/
	
	@Pointcut("execution(public tlc2.value.Value tlc2.tool.Tool.eval(tla2sany.semantic.SemanticNode, tlc2.util.Context, tlc2.tool.TLCState, tlc2.tool.TLCState, int, tlc2.tool.coverage.CostModel))"
			 + "&& args(expr, c, s0, s1, control, cm)"
			 + "&& !within(tlc2.value.CostModelAspect)")
	public static void evals5(SemanticNode expr, Context c, TLCState s0, TLCState s1, int control, CostModel cm) {
	}
	
	@Around("evals5(expr, c, s0, s1, control, cm)")
	public Object evals5(final SemanticNode expr, final Context c, final TLCState s0, final TLCState s1, final int control, final CostModel cm, final ProceedingJoinPoint call) throws Throwable {
		return call.proceed(new Object[] {expr, c, s0, s1, control, cm.get(expr)});
	}

	// -------------------------------- //
	
	@Pointcut("call(tlc2.value.Value+.new(..)) && !within(tlc2.value.CostModelAspect)")
	public static void newValueCtor() {
	}
	
	@AfterReturning(pointcut="newValueCtor()", returning="newValue")
	public void afterNewValue(final Value newValue, final JoinPoint jp) {
		afterNewValueImpl(newValue, jp);
	}

	private void afterNewValueImpl(final Value newValue, final JoinPoint jp) {
		if (jp.getThis() instanceof Value) {
			// Get CostModel instance from existing Value and attach to new one.
			final Value existingValue = (Value) jp.getThis();
			newValue.setCostModel(existingValue.getCostModel());
		}
	}

	// -------------------------------- //

	@Pointcut("execution(public tlc2.value.ValueEnumeration tlc2.value.Enumerable+.elements(..))"
			+ " && target(en)"
			+ " && !within(tlc2.value.CostModelAspect)")
	public static void elementsExec(Enumerable en) {
	}
	
	@Around("elementsExec(en)")
	public Object procedeElements(final Enumerable en, final ProceedingJoinPoint call) throws Throwable {
		return new WrappingValueEnumeration(((EnumerableValue) en).getCostModel(), (ValueEnumeration) call.proceed());
	}
	
	private static class WrappingValueEnumeration implements ValueEnumeration {

		private final CostModel cm;
		private final ValueEnumeration ve;
		
		public WrappingValueEnumeration(CostModel costModel, ValueEnumeration ve) {
			this.cm = costModel.incInvocations(1);
			this.ve = ve;
		}

		@Override
		public void reset() {
			ve.reset();
		}

		@Override
		public Value nextElement() {
			cm.incSecondary();
			return ve.nextElement();
		}
	}
}
