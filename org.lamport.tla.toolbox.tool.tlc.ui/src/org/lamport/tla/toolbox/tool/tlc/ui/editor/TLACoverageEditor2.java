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
package org.lamport.tla.toolbox.tool.tlc.ui.editor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.lamport.tla.toolbox.editor.basic.TLAEditor;
import org.lamport.tla.toolbox.editor.basic.TLAEditorReadOnly;
import org.lamport.tla.toolbox.editor.basic.TLASourceViewerConfiguration;
import org.lamport.tla.toolbox.tool.tlc.output.data.CoverageInformationItem;
import org.lamport.tla.toolbox.tool.tlc.output.data.FileCoverageInformation;
import org.lamport.tla.toolbox.tool.tlc.output.data.LegendItem;

public class TLACoverageEditor2 extends TLAEditorReadOnly {

	static {
		JFaceResources.getColorRegistry().put("LIGHT_YELLOW", new RGB(245,245,245));
	}
	
	/* TLACoverageEditor */

	private static final Color lightYellow = JFaceResources.getColorRegistry().get("LIGHT_YELLOW");
	
	private FileCoverageInformation coverage;

	private Composite heatMapComposite;

	public TLACoverageEditor2(final FileCoverageInformation coverage) {
		this.coverage = coverage;
	}

	@Override
	protected ISourceViewer createSourceViewer(final Composite parent, IVerticalRuler ruler, int styles) {
		// Create composite inside of parent (of which we don't control the layout) to
		// place heatMap with a fixed height below the editor.
		
		final Composite composite = new Composite(parent, SWT.BORDER);
		final GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);

		// Inside editor use again a FillLayout to let super.create... use all available
		// space.
		final Composite editorComposite = new Composite(composite, SWT.NONE);
		editorComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.marginHeight = 0;
		fillLayout.marginWidth = 0;
		fillLayout.spacing = 0;
		editorComposite.setLayout(fillLayout);

		heatMapComposite = new Composite(composite, SWT.BORDER);
		final GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
		heatMapComposite.setLayoutData(layoutData);

		// Inside of heatMap, use a horizontally FillLayout to place individuals heat
		// map item next to each other.
		heatMapComposite.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		final ISourceViewer createSourceViewer = super.createSourceViewer(editorComposite, ruler, styles);
		
		// Make TLACoverageEditor distinguishable from regular TLAEditor.
		final StyledText textWidget = createSourceViewer.getTextWidget();
		textWidget.setBackground(lightYellow);
		textWidget.setCursor(new Cursor(textWidget.getDisplay(), SWT.CURSOR_HAND));
		
		return createSourceViewer;
	}

	@Override
    protected TLASourceViewerConfiguration getTLASourceViewerConfiguration(IPreferenceStore preferenceStore) {
    	return new TLACoverageSourceViewerConfiguration(preferenceStore, this); 
    }

	@Override
	protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
		//TODO Initialize painter after editor input has been set.
		final TextViewer textViewer = (TextViewer) viewer;
		textViewer.addTextPresentationListener(new TLACoveragePainter(viewer, coverage, heatMapComposite));
		
		return super.getSourceViewerDecorationSupport(viewer);
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setInput(coverage);
	}

	public void setInput(final FileCoverageInformation ci) throws PartInitException {
		this.coverage = ci;
	}

	/* TLASourceViewerConfiguration */

	private class TLACoverageSourceViewerConfiguration extends TLASourceViewerConfiguration {

		public TLACoverageSourceViewerConfiguration(IPreferenceStore preferenceStore,
				TLAEditor tlaCoverageEditor) {
			super(preferenceStore, tlaCoverageEditor);
		}
		
		@Override
		public ITextHover getTextHover(final ISourceViewer sourceViewer, String contentType) {
			return new DefaultTextHover(sourceViewer) {
				@Override
				public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
					return coverage.getHoverInfo(hoverRegion.getOffset());
				}
			};
		}
    }
    
	private static final DecimalFormat df = new DecimalFormat("0.0E0");
	
	public static class TextPresentationListener implements Listener {

		private final ISourceViewer viewer;
		private final TextPresentation textPresentation;
		private final FileCoverageInformation coverage;
		private Composite heatMapComposite;

		public TextPresentationListener(FileCoverageInformation coverage, ISourceViewer viewer, TextPresentation textPresentation, Composite heatMapComposite) {
			this.coverage = coverage;
			this.viewer = viewer;
			this.textPresentation = textPresentation;
			this.heatMapComposite = heatMapComposite;
		}

		@Override
		public void handleEvent(Event event) {
			final int offset = JFaceTextUtil.getOffsetForCursorLocation(viewer);
			if (offset == -1) {
				textPresentation.clear();
				coverage.getRoot().style(textPresentation);
				viewer.changeTextPresentation(textPresentation, true);
				updateLegend(this.coverage.getLegend());
			} else {
				final CoverageInformationItem node = coverage.getNode(offset);
				if (node != null) {
					coverage.getRoot().style(textPresentation, JFaceResources.getColorRegistry().get(FileCoverageInformation.GRAY));
					node.style(textPresentation);
					viewer.changeTextPresentation(textPresentation, true);
					updateLegend(node.getLegend());
				}
			}
		}
		
			private void updateLegend(List<LegendItem> legend) {
				final Composite parent = heatMapComposite.getParent();
				if (legend.isEmpty()) {
					heatMapComposite.setVisible(false);
				} else {
					heatMapComposite.dispose();
					
					heatMapComposite = new Composite(parent, SWT.BORDER);
					final GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
					heatMapComposite.setLayoutData(layoutData);
					
					// Inside of heatMap, use a horizontally FillLayout to place individuals heat
					// map item next to each other.
					heatMapComposite.setLayout(new FillLayout(SWT.HORIZONTAL));
					
					Label label = new Label(heatMapComposite, SWT.BORDER);
					label.setText("Invocations:");
					
					// Cannot fit more than N labels into the legend. Thus, only take N elements
					// out of legend (even distribution).
					//TODO take composites's width into account to determine actual number of possible labels. 20 just a wild guess.
					if (legend.size() > 20) {
						final int nth = legend.size() / 20;
						legend = IntStream.range(0, legend.size()).filter(n -> n % nth == 0).mapToObj(legend::get)
								.collect(Collectors.toList());
					}
					
					for (LegendItem cii : legend) {
						label = new Label(heatMapComposite, SWT.BORDER);
						label.setAlignment(SWT.CENTER);
						if (cii.getValue() > 1000) {
							label.setText(df.format(cii.getValue()));
						} else {
							label.setText(String.format("%,d", cii.getValue()));
						}
						label.setToolTipText(String.format("%,d", cii.getValue()));
						label.setBackground(cii.getColor());
						label.addMouseListener(new MouseAdapter() {
							@Override
							public void mouseDoubleClick(MouseEvent e) {
								final IRegion region = cii.getRegion();
								editor.selectAndReveal(region.getOffset(), cii.getRegion().getLength());
							}
						});
					}
				}
				parent.layout();
			}
	}
	
	public class TLACoveragePainter implements ITextPresentationListener {

		private final FileCoverageInformation coverage;
		private final ISourceViewer viewer;
		private final Composite heatMapComposite;

		public TLACoveragePainter(ISourceViewer viewer, FileCoverageInformation coverage, Composite heatMapComposite) {
			this.viewer = viewer;
			this.coverage = coverage;
			this.heatMapComposite = heatMapComposite;
		}
		
		@Override
		public void applyTextPresentation(final TextPresentation textPresentation) {
			final StyledText textWidget = viewer.getTextWidget();
			
			// Attach a listener to react to mouse clicks to reveal coverage for selected expressions.
			final TextPresentationListener listener = new TextPresentationListener(coverage, viewer, textPresentation, heatMapComposite);
			textWidget.addListener(SWT.MouseDown, listener);
			
			// Unregister this to not rerun the initialization above.
			((TextViewer) viewer).removeTextPresentationListener(this);
			
			// Color the editor with coverage information initially.
			listener.handleEvent(null);
		}
	}
}
