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

import org.junit.Before;
import org.junit.Test;

import tlc2.TLCGlobals;
import util.TestPrintStream;
import util.ToolIO;

public class OpApplNodeWrapperTest {

	@Before
	public void setup() {
		TLCGlobals.coverageInterval = 1;
		ToolIO.out = new TestPrintStream();
	}
	
	@Test
	public void testReportCoverage01() {
		final OpApplNodeWrapper root = new OpApplNodeWrapper();
		root.report();
		((TestPrintStream) ToolIO.out).assertEmpty();
		
		root.addChild(new OpApplNodeWrapper());
	}
	
	@Test
	public void testReportCoverage02() {
		final OpApplNodeWrapper root = new OpApplNodeWrapper();
		root.add(42);
		
		root.addChild(new OpApplNodeWrapper(23));
		root.addChild(new OpApplNodeWrapper(24));
		root.addChild(new OpApplNodeWrapper(0)); // Not reported

		root.report();
		((TestPrintStream) ToolIO.out)
				.assertContains("  Unknown location: 42\n"
						+ "  |Unknown location: 23\n"
						+ "  |Unknown location: 24");
	}
	
	@Test
	public void testReportCoverage03() {
		final OpApplNodeWrapper root = new OpApplNodeWrapper();
		root.add(42);
		
		OpApplNodeWrapper childA = new OpApplNodeWrapper(23);
		childA.addChild(new OpApplNodeWrapper(546));
		root.addChild(childA);
		
		OpApplNodeWrapper childB = new OpApplNodeWrapper(24);
		root.addChild(childB);
		childB.addChild(new OpApplNodeWrapper(0)); // Not reported because 0
		
		OpApplNodeWrapper childC = new OpApplNodeWrapper(0);
		root.addChild(childC); // Not reported

		childC.addChild(new OpApplNodeWrapper(17)); // Must be reported despite C being 0
		
		root.report();
		((TestPrintStream) ToolIO.out)
				.assertContains("  Unknown location: 42\n"
						+ "  |Unknown location: 23\n"
						+ "  ||Unknown location: 546\n" 
						+ "  |Unknown location: 24\n"
						+ "  |Unknown location: 17");
	}
	
	/*
  line 8, col 12 to line 8, col 21 of module A: 1
  |line 5, col 11 to line 5, col 49 of module A: 1
  ||line 5, col 31 to line 5, col 49 of module A: 131072
  ||line 5, col 20 to line 5, col 27 of module A: 131072
  |||line 5, col 27 to line 5, col 27 of module A: 1
  |line 8, col 16 to line 8, col 20 of module A: 1
	 */
	@Test
	public void testReportCoverage04() {
		final OpApplNodeWrapper root = new OpApplNodeWrapper();
		root.add(1);
		
		OpApplNodeWrapper childA = new OpApplNodeWrapper(1);
		root.addChild(childA);
		
		childA.addChild(new OpApplNodeWrapper(131072));
		
		OpApplNodeWrapper cChildA = new OpApplNodeWrapper(131072);
		childA.addChild(cChildA);
		
		cChildA.addChild(new OpApplNodeWrapper(1));
		
		OpApplNodeWrapper childB = new OpApplNodeWrapper(1);
		root.addChild(childB);
		
		root.report();
		((TestPrintStream) ToolIO.out)
				.assertContains("  Unknown location: 1\n" + 
						"  |Unknown location: 1\n" + 
						"  ||Unknown location: 131072\n" + 
						"  ||Unknown location: 131072\n" + 
						"  |||Unknown location: 1\n" + 
						"  |Unknown location: 1");
	}
}
