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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.lamport.tla.toolbox.editor.basic.TLAEditor;
import org.lamport.tla.toolbox.editor.basic.TLAEditorReadOnly;
import org.lamport.tla.toolbox.editor.basic.TLASourceViewerConfiguration;
import org.lamport.tla.toolbox.tool.tlc.output.data.CoverageInformation;
import org.lamport.tla.toolbox.tool.tlc.output.data.CoverageInformationItem;

public class TLACoverageEditor2 extends TLAEditorReadOnly {

	static {
		JFaceResources.getColorRegistry().put("LIGHT_YELLOW", new RGB(245,245,245));
	}
	
	/* TLACoverageEditor */

	private static final Color lightYellow = JFaceResources.getColorRegistry().get("LIGHT_YELLOW");
	
	private CoverageInformation coverage;

	public TLACoverageEditor2(final CoverageInformation coverage) {
		this.coverage = coverage;
	}

	@Override
    protected TLASourceViewerConfiguration getTLASourceViewerConfiguration(IPreferenceStore preferenceStore) {
    	return new TLACoverageSourceViewerConfiguration(preferenceStore, this); 
    }

	@Override
	protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
		//TODO Initialize painter after editor input has been set.
		final TextViewer textViewer = (TextViewer) viewer;
		textViewer.addTextPresentationListener(new TLACoveragePainter(viewer, coverage));
		
		return super.getSourceViewerDecorationSupport(viewer);
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setInput(coverage);
	}

	public void setInput(final CoverageInformation ci) throws PartInitException {
		this.coverage = ci;
		final IDocument document = getDocumentProvider().getDocument(getEditorInput());
		try {
			this.coverage.prepare(document);
		} catch (BadLocationException e) {
			throw new PartInitException(e.getMessage(), e);
		}
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
	
	public static class TextPresentationListener implements Listener {

		private final ISourceViewer viewer;
		private final TextPresentation textPresentation;
		private final CoverageInformation coverage;

		public TextPresentationListener(CoverageInformation coverage, ISourceViewer viewer, TextPresentation textPresentation) {
			this.coverage = coverage;
			this.viewer = viewer;
			this.textPresentation = textPresentation;
		}

		@Override
		public void handleEvent(Event event) {
			final int offset = JFaceTextUtil.getOffsetForCursorLocation(viewer);
			if (offset == -1) {
				coverage.getRoot().style(textPresentation);
				viewer.changeTextPresentation(textPresentation, true);
			} else {
				final CoverageInformationItem node = coverage.getNode(offset);
				if (node != null) {
					coverage.getRoot().style(textPresentation, JFaceResources.getColorRegistry().get(CoverageInformation.GRAY));
					node.style(textPresentation);
					viewer.changeTextPresentation(textPresentation, true);
				}
			}
		}
	}
	
	public class TLACoveragePainter implements ITextPresentationListener {

		private final CoverageInformation coverage;
		private final ISourceViewer viewer;

		public TLACoveragePainter(ISourceViewer viewer, CoverageInformation coverage) {
			this.viewer = viewer;
			this.coverage = coverage;
		}
		
		@Override
		public void applyTextPresentation(final TextPresentation textPresentation) {
			final StyledText textWidget = viewer.getTextWidget();
			
			// Make TLACoverageEditor distinguishable from regular TLAEditor.
			textWidget.setBackground(lightYellow);
			textWidget.setCursor(new Cursor(textWidget.getDisplay(), SWT.CURSOR_HAND));
			
			// Attach a listener to react to mouse clicks to reveal coverage for selected expressions.
			final TextPresentationListener listener = new TextPresentationListener(coverage, viewer, textPresentation);
			textWidget.addListener(SWT.MouseDown, listener);
			
			// Unregister this to not rerun the initialization above.
			((TextViewer) viewer).removeTextPresentationListener(this);
			
			// Color the editor with coverage information initially.
			listener.handleEvent(null);
		}
	}
}
