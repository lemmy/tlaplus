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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.lamport.tla.toolbox.editor.basic.TLAEditor;
import org.lamport.tla.toolbox.editor.basic.TLAEditorReadOnly;
import org.lamport.tla.toolbox.editor.basic.TLASourceViewerConfiguration;
import org.lamport.tla.toolbox.tool.tlc.output.data.CoverageInformationItem;
import org.lamport.tla.toolbox.tool.tlc.output.data.ModuleCoverageInformation;
import org.lamport.tla.toolbox.tool.tlc.output.data.LegendItem;
import org.lamport.tla.toolbox.util.UIHelper;

public class TLACoverageEditor extends TLAEditorReadOnly {

	static {
		JFaceResources.getColorRegistry().put("LIGHT_YELLOW", new RGB(245,245,245));
	}
	
	private static final Color lightYellow = JFaceResources.getColorRegistry().get("LIGHT_YELLOW");

	private static class ResizeListener implements Listener {
		
		private final Point size = new Point(1024,768);

		@Override
		public void handleEvent(final Event event) {
			final Widget widget = event.widget;
			if (widget instanceof Composite) {
				final Composite c = (Composite) widget;
				size.x = c.getSize().x;
				size.y = c.getSize().y;
			}
		}
		
		public int getWidth() {
			return size.x;
		}
	}
	
	/* TLACoverageEditor */

	private final ResizeListener resizeListener = new ResizeListener();
	
	private ModuleCoverageInformation coverage;

	private Composite heatMapComposite;

	private TLACoveragePainter painter;

	public TLACoverageEditor(final ModuleCoverageInformation coverage) {
		this.coverage = coverage;
	}

	@Override
	public void dispose() {
		painter.queue.offer(TERMINATE);
		super.dispose();
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
		composite.addListener(SWT.Resize, resizeListener);

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
		painter = new TLACoveragePainter(this, heatMapComposite);
		((TextViewer) viewer).addTextPresentationListener(painter);
		
		return super.getSourceViewerDecorationSupport(viewer);
	}

	public void resetInput(final ModuleCoverageInformation ci) throws PartInitException {
		if (this.coverage == ci) {
			// The CoverageInformation from which the FileCoverageInformation has been
			// projected, is identical to the one already open. No need to update the ui.
			// This case occurs when the TLCModelLaunchDataProvider parses the MC.out of a
			// finished model with more than one block of coverage statistics. For each it
			// notifies ResultPage but - due to TLCModelLaunchDataProvider sending strings
			// instead of the actual values and threading - we read a newer/more up-to-date
			// instance of CoverageInformation before a notification reaches us.
			return;
		}
		this.coverage = ci;
		// Trigger the editor's coverage painter.
		painter.queue.offer(ALL);
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

	private static final int ALL = -1;
	
	private static final int TERMINATE = -42;
	
	public class TLACoveragePainter implements ITextPresentationListener {
		
		private final TLACoverageEditor editor;
		
		private final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
		
		private final Job annotator = new Job("Coverage Painter") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				while (true) {
					final int offset = getOffset();
					if (offset == TERMINATE || monitor.isCanceled()) {
						return Status.OK_STATUS;
					} else {
						monitor.beginTask(String.format("Painting coverage for %s", offset), 1);
					}
					
					// Do not clear old style ranges before creating new ones as clear removes TLA+ syntax highlighting.
					//textPresentation.clear(); 
					
					// Create new style ranges (and the legend)
					Set<LegendItem> legend = new HashSet<>(0);
					if (offset == ALL) {
						coverage.getRoot().style(textPresentation);
						legend = coverage.getLegend();
					} else {
						final CoverageInformationItem node = coverage.getNode(offset);
						if (node != null) {
							// Style all unrelated parts gray.
							coverage.getRoot().style(textPresentation, JFaceResources.getColorRegistry().get(ModuleCoverageInformation.GRAY));
							
							node.style(textPresentation);
							legend = node.getLegend();
						}
					}
					
					if (monitor.isCanceled()) {
						return Status.OK_STATUS;
					}
					
					final List<LegendItem> fLegend = new ArrayList<>(legend);
					UIHelper.runUISync(new Runnable() {
						@Override
						public void run() {
							final TextViewer viewer = editor.getViewer();
							// viewer might have been disposed by the time the outer thread styled the presentation.
							if (viewer == null || viewer.getTextWidget() == null
									|| viewer.getTextWidget().isDisposed()) {
								return;
							}
							viewer.getTextWidget().removeListener(SWT.MouseDown, listener);

							viewer.changeTextPresentation(textPresentation, true);
							updateLegend(fLegend);
							
							viewer.getTextWidget().addListener(SWT.MouseDown, listener);
						}
					});
					
					monitor.done();
				}
			}
			
			private int getOffset() {
				try {
					return queue.take();
				} catch (InterruptedException notExpectedException) {
					notExpectedException.printStackTrace();
					return TERMINATE;
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
					final int numLabel = resizeListener.getWidth() / 47; // 47 pixel per label seems to fit most text and still looks pleasant.
					if (legend.size() > numLabel) {
						final int nth = legend.size() / numLabel;
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
		};
		private final Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				final int offset = JFaceTextUtil.getOffsetForCursorLocation(editor.getViewer());
				Integer peek = queue.peek();
				if (peek == null || peek != offset) {
					//System.out.println(String.format("Scheduling offset %s after %s", offset, peek));
					queue.offer(offset);
				} else {
					//System.out.println("Skipping redundant offset " + offset);
				}
			}
		};
		
		private TextPresentation textPresentation;
		private Composite heatMapComposite;

		public TLACoveragePainter(TLACoverageEditor editor, Composite heatMapComposite) {
			this.editor = editor;
			this.heatMapComposite = heatMapComposite;
			
			this.annotator.setPriority(Job.LONG);
			this.annotator.setRule(null);
			this.annotator.setSystem(true);
		}

		@Override
		public synchronized void applyTextPresentation(final TextPresentation textPresentation) {
			// Unregister this to not rerun the initialization again.
			editor.getViewer().removeTextPresentationListener(this);
			
			this.textPresentation = textPresentation;
			
			editor.getViewer().addTextInputListener(new ITextInputListener() {
				@Override
				public synchronized void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
					editor.getViewer().removeTextInputListener(this);
					
					// Register listener to update coverage information based on mouse clicks.
					final StyledText textWidget = editor.getViewer().getTextWidget();
					textWidget.addListener(SWT.MouseDown, listener);

					// Color the editor with coverage information initially.
					queue.add(ALL);
					annotator.schedule();
				}
				
				@Override
				public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
				}
			});
		}
	}
}
