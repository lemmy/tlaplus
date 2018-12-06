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
import tlc2.tool.coverage.CostModel;

@Aspect
public class CostModelAspect {
	
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
