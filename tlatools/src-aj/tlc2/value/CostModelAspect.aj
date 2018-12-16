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

import tlc2.TLCGlobals;
import tlc2.tool.Action;
import tlc2.tool.ActionItemList;
import tlc2.tool.StateVec;
import tlc2.tool.TLCState;
import tlc2.tool.coverage.CostModel;
import tla2sany.semantic.OpApplNode;
import tla2sany.semantic.SemanticNode;

@Aspect
public class CostModelAspect {
	
	// -------------------------------- //
	
/*
  private final void getInitStates(ActionItemList acts, TLCState ps, IStateFunctor states, CostModel cm) {
		if (acts.isEmpty()) {
			states.addElement(ps.copy());
			return;
		} else if (ps.allAssigned()) {
			while (!acts.isEmpty()) {
				final Value bval = this.eval(acts.carPred(), acts.carContext(), ps, TLCState.Empty, EvalControl.Init, acts.cm);
				if (!(bval instanceof BoolValue)) {
					//TODO Choose more fitting error message.
					Assert.fail(EC.TLC_EXPECTED_EXPRESSION_IN_COMPUTING,
							new String[] { "initial states", "boolean", bval.toString(), acts.pred.toString() });
				}
				if (!((BoolValue) bval).val) {
					return;
				}
				// Move on to the next action in the ActionItemList.
				acts = acts.cdr();
			}
			states.addElement(ps.copy());
			return;
		}
		// Assert.check(act.kind > 0 || act.kind == -1);
		ActionItemList acts1 = acts.cdr();
		this.getInitStates(acts.carPred(), acts1, acts.carContext(), ps, states, acts.cm);
	  }
 */

	@Pointcut("execution(private void tlc2.tool.Tool.getInitStates(tlc2.tool.ActionItemList, tlc2.tool.TLCState, ..))"
			 + "&& !within(tlc2.value.CostModelAspect) && if()")
	public static boolean getInitStates() {
		return TLCGlobals.isCoverageEnabled();
	}
	
	@Around("getInitStates()")
	public void getInitStates(final ProceedingJoinPoint call) throws Throwable {
		final ActionItemList acts = (ActionItemList) call.getArgs()[0];

		final TLCState ps = (TLCState) call.getArgs()[1];

		final CostModel cm = (CostModel) call.getArgs()[3];

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
			 + "&& !within(tlc2.value.CostModelAspect) && if()")
	public static boolean getNextStatesAction() {
		return TLCGlobals.isCoverageEnabled();
	}
	
	@Around("getNextStatesAction()")
	public Object getNextStatesAction(final ProceedingJoinPoint call) throws Throwable {
		final Action action = ((Action) call.getArgs()[0]);
		
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
			 + "&& !within(tlc2.value.CostModelAspect) && if()")
	public static boolean getNextStates() {
		return TLCGlobals.isCoverageEnabled();
	}
	
	@Around("getNextStates()")
	public Object getNextStates(final ProceedingJoinPoint call) throws Throwable {
		final CostModel cm = (CostModel) call.getArgs()[4];
		
		final TLCState copy = (TLCState) call.proceed(call.getArgs());
		if (copy != call.getArgs()[2]) {
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
			 + "&& !within(tlc2.value.CostModelAspect) && if()")
	public static boolean setSource() {
		return TLCGlobals.isCoverageEnabled();
	}
	
	@Around("setSource()")
	public Object setSource(final ProceedingJoinPoint call) throws Throwable {
		final Object[] args = call.getArgs();
		
		final OpApplNode oan = (OpApplNode) args[0];
		final Value v = (Value) call.getArgs()[1];
		final CostModel cm = (CostModel) args[2];
		final CostModel cmNew = cm.incInvocations();
		
		v.setCostModel(cmNew);
		return call.proceed(new Object[] {oan, v, cmNew});
	}

	// -------------------------------- //
	
/*
	private final Value evalAppl(final OpApplNode expr, Context c, TLCState s0, TLCState s1, final int control, CostModel cm) {
		cm = cm.get(expr);
		cm.incInvocations();
*/

	@Pointcut("execution(private tlc2.value.Value tlc2.tool.Tool.evalAppl(tla2sany.semantic.OpApplNode, ..))"
			 + "&& !within(tlc2.value.CostModelAspect) && if()")
	public static boolean evalsAppl() {
		return TLCGlobals.isCoverageEnabled();
	}
	
	@Around("evalsAppl()")
	public Object evalsAppl(final ProceedingJoinPoint call) throws Throwable {
		final Object[] args = call.getArgs();
		final SemanticNode oan = (SemanticNode) args[0];
		final CostModel cm = (CostModel) args[5];
		final CostModel cmNew = cm.incInvocations();
		return call.proceed(new Object[] {oan, args[1], args[2], args[3], args[4], cmNew});
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
			 + "&& !within(tlc2.value.CostModelAspect) && if()")
	public static boolean evals6() {
		return TLCGlobals.isCoverageEnabled();
	}
	
	@Around("evals6()")
	public Object beforeEvals6(final ProceedingJoinPoint call) throws Throwable {
		final Object[] args = call.getArgs();
		final SemanticNode oan = (SemanticNode) args[0];
		final CostModel cm = (CostModel) args[6];
		return call.proceed(new Object[] {oan, args[1], args[2], args[3], args[4], args[5], cm.get(oan)});
	}

	// -------------------------------- //
	
/*
  private final void getInitStatesAppl(OpApplNode init, ActionItemList acts, Context c, TLCState ps, IStateFunctor states, CostModel cm) {
    cm = cm.get(init);

  private final TLCState enabledAppl(OpApplNode pred, ActionItemList acts, Context c, TLCState s0, TLCState s1, CostModel cm) {
    cm = cm.get(pred);

  public final Value eval(SemanticNode expr, Context c, TLCState s0, TLCState s1, final int control, CostModel cm) {
	 cm = cm.get(expr)
*/

	@Pointcut("(execution(private void tlc2.tool.Tool.getInitStatesAppl(tla2sany.semantic.OpApplNode, .., tlc2.tool.coverage.CostModel)) ||"
			 + "execution(private tlc2.tool.TLCState tlc2.tool.Tool.enabledAppl(tla2sany.semantic.OpApplNode, .., tlc2.tool.coverage.CostModel)) || "
			 + "execution(public tlc2.value.Value tlc2.tool.Tool.eval(tla2sany.semantic.SemanticNode, tlc2.util.Context, tlc2.tool.TLCState, tlc2.tool.TLCState, int, tlc2.tool.coverage.CostModel)))"
			 + "&& !within(tlc2.value.CostModelAspect) && if()")
	public static boolean evals5() {
		return TLCGlobals.isCoverageEnabled();
	}
	
	@Around("evals5()")
	public Object beforeEvals5(final ProceedingJoinPoint call) throws Throwable {
		final Object[] args = call.getArgs();
		final SemanticNode oan = (SemanticNode) args[0];
		final CostModel cm = (CostModel) args[5];
		return call.proceed(new Object[] {oan, args[1], args[2], args[3], args[4], cm.get(oan)});
	}

	// -------------------------------- //
	
	@Pointcut("call(tlc2.value.Value+.new(..)) && !within(tlc2.value.CostModelAspect) && if()")
	public static boolean newValueCtor() {
		return TLCGlobals.isCoverageEnabled();
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

	@Pointcut("execution(public tlc2.value.ValueEnumeration tlc2.value.Enumerable+.elements(..)) && !within(tlc2.value.CostModelAspect) && if()")
	public static boolean elementsExec() {
		return TLCGlobals.isCoverageEnabled();
	}
	
	@Around("elementsExec()")
	public Object procedeElements(final ProceedingJoinPoint call) throws Throwable {
		return new WrappingValueEnumeration(((EnumerableValue) call.getTarget()).getCostModel(), (ValueEnumeration) call.proceed());
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
