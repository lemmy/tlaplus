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
package org.lamport.tla.toolbox.tool.tlc.output.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import tla2sany.st.Location;

public class CoverageInformationTest {

	@Test
	public void test() {
		final String input = "  line 10, col 1 to line 10, col 4 of module MC: 1\n" + 
				"  line 12, col 6 to line 12, col 17 of module CostMetrics: 3\n" + 
				"  line 14, col 6 to line 14, col 18 of module CostMetrics: 3\n" + 
				"  |line 14, col 11 to line 14, col 18 of module CostMetrics: 3\n" + 
				"  ||line 10, col 12 to line 10, col 47 of module CostMetrics: 33\n" + 
				"  |||line 10, col 15 to line 10, col 19 of module CostMetrics: 33\n" + 
				"  |||line 10, col 33 to line 10, col 47 of module CostMetrics: 30\n";
		
		final CoverageInformation ci = new CoverageInformation();
		for (String line : input.split("\n")) {
			ci.add(CoverageInformationItem.parse(line, "modelName"));
		}

		CoverageInformationItem root = ci.getRoot("MC");
		List<CoverageInformationItem> childs = root.getChildren();
		assertEquals(1, childs.size());
		
		CoverageInformationItem coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 10, col 1 to line 10, col 4 of module MC"), coverageInformationItem.getModuleLocation());
		assertEquals(1, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(coverageInformationItem.getChildren().isEmpty());

		root = ci.getRoot("CostMetrics");
		childs = root.getChildren();
		assertEquals(2, childs.size());

		coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 12, col 6 to line 12, col 17 of module CostMetrics"), coverageInformationItem.getModuleLocation());
		assertEquals(3, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(childs.get(0).getChildren().isEmpty());

		coverageInformationItem = childs.get(1);
		assertEquals(Location.parseLocation("line 14, col 6 to line 14, col 18 of module CostMetrics"), coverageInformationItem.getModuleLocation());
		assertEquals(3, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		
		childs = coverageInformationItem.getChildren();
		assertEquals(1, childs.size());
		
		coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 14, col 11 to line 14, col 18 of module CostMetrics"), coverageInformationItem.getModuleLocation());
		assertEquals(3, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());

		childs = coverageInformationItem.getChildren();
		assertEquals(1, childs.size());

		coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 10, col 12 to line 10, col 47 of module CostMetrics"), coverageInformationItem.getModuleLocation());
		assertEquals(33, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());

		childs = coverageInformationItem.getChildren();
		assertEquals(2, childs.size());

		coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 10, col 15 to line 10, col 19 of module CostMetrics"), coverageInformationItem.getModuleLocation());
		assertEquals(33, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(coverageInformationItem.getChildren().isEmpty());
		
		coverageInformationItem = childs.get(1);
		assertEquals(Location.parseLocation("line 10, col 33 to line 10, col 47 of module CostMetrics"), coverageInformationItem.getModuleLocation());
		assertEquals(30, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(coverageInformationItem.getChildren().isEmpty());
	}
	
	@Test
	public void test2() {
		final String input = "  line 5, col 9 to line 5, col 13 of module D: 1\n" + 
				"  line 11, col 6 to line 11, col 17 of module D: 3\n" + 
				"  |line 11, col 11 to line 11, col 17 of module D: 3\n" + 
				"  ||line 9, col 12 to line 9, col 47 of module D: 9\n" + 
				"  |||line 9, col 15 to line 9, col 19 of module D: 9\n" + 
				"  |||line 9, col 33 to line 9, col 47 of module D: 6\n" + 
				"  line 13, col 6 to line 13, col 17 of module D: 3\n" + 
				"  |line 13, col 11 to line 13, col 17 of module D: 3\n" + 
				"  |line 9, col 33 to line 9, col 47 of module D: 24\n";
		
		final CoverageInformation ci = new CoverageInformation();
		for (String line : input.split("\n")) {
			ci.add(CoverageInformationItem.parse(line, "modelName"));
		}

		CoverageInformationItem root = ci.getRoot("D");
		List<CoverageInformationItem> childs = root.getChildren();
		assertEquals(3, childs.size());
		
		CoverageInformationItem coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 5, col 9 to line 5, col 13 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(1, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(coverageInformationItem.getChildren().isEmpty());
		
		coverageInformationItem = childs.get(1);
		assertEquals(Location.parseLocation("line 11, col 6 to line 11, col 17 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(3, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		
		childs = coverageInformationItem.getChildren();
		assertEquals(1, childs.size());

		coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 11, col 11 to line 11, col 17 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(3, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		
		childs = coverageInformationItem.getChildren();
		assertEquals(1, childs.size());

		coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 9, col 12 to line 9, col 47 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(9, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());

		childs = coverageInformationItem.getChildren();
		assertEquals(2, childs.size());

		coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 9, col 15 to line 9, col 19 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(9, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(coverageInformationItem.getChildren().isEmpty());
		
		coverageInformationItem = childs.get(1);
		assertEquals(Location.parseLocation("line 9, col 33 to line 9, col 47 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(6, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(coverageInformationItem.getChildren().isEmpty());
		
		
		coverageInformationItem = root.getChildren().get(2);
		assertEquals(Location.parseLocation("line 13, col 6 to line 13, col 17 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(3, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		
		childs = coverageInformationItem.getChildren();
		assertEquals(2, childs.size());

		coverageInformationItem = childs.get(0);
		assertEquals(Location.parseLocation("line 13, col 11 to line 13, col 17 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(3, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(coverageInformationItem.getChildren().isEmpty());
		
		coverageInformationItem = childs.get(1);
		assertEquals(Location.parseLocation("line 9, col 33 to line 9, col 47 of module D"), coverageInformationItem.getModuleLocation());
		assertEquals(24, coverageInformationItem.getCount());
		assertEquals("modelName", coverageInformationItem.getModelName());
		assertTrue(coverageInformationItem.getChildren().isEmpty());
	}
}
