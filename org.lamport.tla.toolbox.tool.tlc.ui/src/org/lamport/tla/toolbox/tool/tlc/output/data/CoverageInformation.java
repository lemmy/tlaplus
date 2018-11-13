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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.lamport.tla.toolbox.util.AdapterFactory;

import tla2sany.st.Location;

public class CoverageInformation implements Iterable<CoverageInformationItem> {
	
	private final List<CoverageInformationItem> items = new ArrayList<>();

	private Map<Location, List<CoverageInformationItem>> loc2cci = new HashMap<>();
	
	private final TreeMap<Integer, List<CoverageInformationItem>> offset2cii = new TreeMap<>();

	public void add(final CoverageInformationItem item) {
		this.items.add(item);
		
		this.loc2cci.computeIfAbsent(item.getModuleLocation(), c -> new ArrayList<>()).add(item);
	}

	private int numSiblings(CoverageInformationItem item) {
		return this.loc2cci.get(item.getModuleLocation()).size();
	}
	
	@Override
	public Iterator<CoverageInformationItem> iterator() {
		return this.items.iterator();
	}

	public boolean isEmpty() {
		return this.items.isEmpty();
	}

	public Object[] toArray() {
		return this.items.toArray();
	}

	private static final int BLUE = 240;

	private int getHue(final long count, final TreeSet<Long> counts) {
		final int size = counts.size();
		final float r = 240f / size;
		final SortedSet<Long> headSet = counts.headSet(count);
		return BLUE - Math.round(r * (headSet.size() + 1));
	}
	
	private CoverageInformationItem root;

	public CoverageInformationItem getRoot(final String filename) {
		final CoverageInformationItem root = new CoverageInformationItem();
		
		final Deque<CoverageInformationItem> stack = new ArrayDeque<>();
		stack.push(root.setLayer(-1));
		
		for (CoverageInformationItem item : items) {
			if (filename.equals(item.getModuleLocation().source())) {
				int layer = item.getLayer();
				while (layer <= stack.peek().getLayer()) {
					stack.pop();
				}
				stack.peek().addChild(item);
				stack.push(item);
				
				// Set siblings if any:
				item.addSiblings(loc2cci.get(item.getModuleLocation()));
			}
		}
		
		return root;
	}
	
	public CoverageInformationItem getRoot() {
		if (root == null) {
			root = getRoot(items.get(0).getModuleLocation().source());
		}
		return root;
	}
	
	public CoverageInformationItem getNode(final int offset) {
		return getNodes(offset).stream().findFirst().orElse(null);
	}
	
	public List<CoverageInformationItem> getNodes(final int offset) {
		final Entry<Integer, List<CoverageInformationItem>> entry = this.offset2cii.floorEntry(offset);
		if (entry != null) {
			return entry.getValue().stream()
					.filter(cii -> offset <= cii.getRegion().getOffset() + cii.getRegion().getLength())
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public static final String GRAY = "GRAY";

	public static final String RED = "RED";

	public CoverageInformation prepare(final IDocument document) throws BadLocationException {
		final TreeSet<Long> counts = new TreeSet<>();
		
		for (CoverageInformationItem item : items) {
			// Convert Location to IRegion
			final IRegion region = AdapterFactory.locationToRegion(document, item.getModuleLocation());
			item.setRegion(region);
			
			offset2cii.computeIfAbsent(region.getOffset(), c -> new ArrayList<>()).add(item);
			
			counts.add(item.getCount());
			counts.add(item.getCount() * numSiblings(item));
		}

		JFaceResources.getColorRegistry().put(RED, new RGB(255,0,0));
		JFaceResources.getColorRegistry().put(GRAY, new RGB(211,211,211));
		
		for (CoverageInformationItem item : items) {
			// Calculate colors.
			int hue = getHue(item.getCount(), counts);
			String key = Integer.toString(hue);
			if (!JFaceResources.getColorRegistry().hasValueFor(key)) {
				JFaceResources.getColorRegistry().put(key, new RGB(hue, .25f, 1f));
			}
			Color color = JFaceResources.getColorRegistry().get(key);

			// Aggregated color (might be identical to color).
			hue = getHue(item.getCount() * numSiblings(item), counts);
			key = Integer.toString(hue);
			if (!JFaceResources.getColorRegistry().hasValueFor(key)) {
				JFaceResources.getColorRegistry().put(key, new RGB(hue, .25f, 1f));
			}
			Color aggregate = JFaceResources.getColorRegistry().get(key);

			item.setColor(color, aggregate);
		}

		return this;
	}
}
