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

import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import tla2sany.st.Location;
import tlc2.tool.coverage.ActionWrapper.Relation;

public class ActionInformationItem extends CoverageInformationItem {

	public static ActionInformationItem parseInit(String outputMessage, String modelName) {
		final Pattern pattern = Pattern.compile("^<(.*?) (.*)>: ([0-9]+)$");
		final Matcher matcher = pattern.matcher(outputMessage);
		matcher.find();

		final Location location = Location.parseLocation(matcher.group(2));
		final long generatedStates = Long.parseLong(matcher.group(3));
		
		return new ActionInformationItem(matcher.group(1), location, modelName, generatedStates);
	}
	
	public static ActionInformationItem parseNext(String outputMessage, String modelName) {
		final Pattern pattern = Pattern.compile("^<(.*?) (line .*)>: ([0-9]+):([0-9]+)$");
		final Matcher matcher = pattern.matcher(outputMessage);
		matcher.find();

		final Location location = Location.parseLocation(matcher.group(2));
		final long distinctStates = Long.parseLong(matcher.group(3));
		final long generatedStates = Long.parseLong(matcher.group(4));
		
		return new ActionInformationItem(matcher.group(1), location, modelName, generatedStates, distinctStates);
	}
	
	public static ActionInformationItem parseProp(String outputMessage, String modelName) {
		final Pattern pattern = Pattern.compile("^<(.*?) (line .*)>$");
		final Matcher matcher = pattern.matcher(outputMessage);
		matcher.find();

		final Location location = Location.parseLocation(matcher.group(2));
		
		return new ActionInformationItem(matcher.group(1), location, modelName);
	}
	
	// ---- ---- //
	
	private final Relation relation;
	private final String name;
	private long unseen = 0L;
	private long sum;

	public ActionInformationItem(final String name, Location loc, final String modelName) {
		super(loc, 0, modelName, 0);
		this.name = name;
		this.relation = Relation.PROP;
	}
	
	public ActionInformationItem(final String name, Location loc, final String modelName, long generated) {
		super(loc, generated, modelName, 0);
		this.name = name;
		this.relation = Relation.INIT;
	}

	public ActionInformationItem(final String name, Location loc, final String modelName, long generated, long unseen) {
		super(loc, generated, modelName, 0);
		this.name = name;
		this.unseen = unseen;
		this.relation = Relation.NEXT;
	}

	public String getName() {
		return name;
	}
	
	public Relation getRelation() {
		return relation;
	}

	public long getUnseen() {
		return unseen;
	}
	
	@Override
	public void style(final TextPresentation textPresentation, final Color c) {
		// Do not unstyle AII when specific CostModel tree gets selected.
		for (CoverageInformationItem child : super.getChildren()) {
			child.style(textPresentation, c);
		}
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.tool.tlc.output.data.CoverageInformationItem#colorItem(java.util.TreeSet, int)
	 */
	@Override
	void colorItem(TreeSet<Long> counts, final int ignored) {
		final int hue = CoverageInformation.getHue(getUnseen(), counts);
		final String key = Integer.toString(hue);
		if (!JFaceResources.getColorRegistry().hasValueFor(key)) {
			JFaceResources.getColorRegistry().put(key, new RGB(hue, .25f, 1f));
		}
		setColor(JFaceResources.getColorRegistry().get(key), null);
	}

	public String getHover() {
		if (relation != Relation.PROP){
			final double ratio = (unseen * 1d / sum) * 100d;
			if (relation == Relation.NEXT) {
				final double overhead = (unseen * 1d / getCount()) * 100d;
				return String.format(
						"Action %s:\n%,d state(s) generated with %,d of them distinct (%.2f%%).\nContributes %.2f%% to total number of distinct states across all actions.",
						name, getCount(), unseen, overhead, ratio);
			} else {
				return String.format("Action %s (Init):\n%,d state(s) generated.", name, getCount());
			}
		}
		return "";
	}

	public void setSum(final long sum) {
		this.sum = sum;
	}
}
