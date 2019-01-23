/*******************************************************************************
 * Copyright (c) 2019 Microsoft Research. All rights reserved. 
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

package tlc2.tool.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import tlc2.tool.ITool;

public class ToolImplActivator implements BundleActivator {

	private ServiceRegistration<ITool> registerService;

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext ctxt) throws Exception {
		final ServiceReference<ITool> sr = ctxt.getServiceReference(ITool.class);
		if (sr == null) {
			System.err.printf("Found no ITool service reference\n");
			return;
		}
		final ITool other = ctxt.getService(sr);
		if (other == null) {
			System.err.printf("Found no ITool instance\n");
			return;
		}
		
		ITool tool = new Tool(other.getRootFile(), other.getConfigFile());
		if (tool == null) {
			System.err.printf("Tool instantiation failed\n");
			return;
		}
		
		registerService = ctxt.registerService(ITool.class, tool, null);
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext ctxt) throws Exception {
		if (registerService != null) {
			ctxt.ungetService(registerService.getReference());
		}
	}
}
