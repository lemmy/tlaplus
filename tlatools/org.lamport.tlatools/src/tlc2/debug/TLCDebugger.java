/*******************************************************************************
 * Copyright (c) 2020 Microsoft Research. All rights reserved. 
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
package tlc2.debug;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.BreakpointLocation;
import org.eclipse.lsp4j.debug.BreakpointLocationsArguments;
import org.eclipse.lsp4j.debug.BreakpointLocationsResponse;
import org.eclipse.lsp4j.debug.CancelArguments;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.ContinueResponse;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.PauseArguments;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetVariableArguments;
import org.eclipse.lsp4j.debug.SetVariableResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.Variable;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import tla2sany.semantic.ModuleNode;
import tla2sany.semantic.NumeralNode;
import tla2sany.semantic.OpDefNode;
import tla2sany.semantic.SemanticNode;
import tla2sany.st.Location;
import tlc2.tool.EvalControl;
import tlc2.tool.TLCState;
import tlc2.tool.impl.FastTool;
import tlc2.tool.impl.Tool;
import tlc2.util.Context;
import tlc2.util.FP64;
import tlc2.value.impl.FcnRcdValue;
import tlc2.value.impl.LazyValue;
import tlc2.value.impl.RecordValue;
import tlc2.value.impl.SetEnumValue;
import tlc2.value.impl.Value;
import tlc2.value.impl.ValueEnumeration;
import util.SimpleFilenameToStream;
import util.ToolIO;

public class TLCDebugger extends AbstractDebugger implements IDebugTarget {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		new TLCDebugger();
	}

	private Launcher<IDebugProtocolClient> launcher;

	public TLCDebugger() throws IOException, InterruptedException, ExecutionException {
		try (ServerSocket serverSocket = new ServerSocket(4712)) {
			while (true) {
				System.out.printf("Debugger is listening on localhost:%s", serverSocket.getLocalPort());
				final Socket socket = serverSocket.accept();
				final InputStream inputStream = socket.getInputStream();
				final OutputStream outputStream = socket.getOutputStream();

				launcher = DSPLauncher.createServerLauncher(this, inputStream, outputStream);
				launcher.startListening().get();
			}
		}
	}

	@Override
	public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
		System.out.println("initialize");

		Executors.newSingleThreadExecutor().submit(() -> {
			System.err.println("initialize -> initialized");
			// Signal the debugger that we are ready. It seem not relevant in what order the
			// response below and the initialized signal arrive at the debugger.
			launcher.getRemoteProxy().initialized();
		});

		// The capabilities define customizations how the debugger will interact with
		// this debuggee. Declaring no capabilities causes the most simple protocol to
		// be used.
		return CompletableFuture.completedFuture(new Capabilities());
	}

	@Override
	public CompletableFuture<Void> cancel(CancelArguments args) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
		System.out.println("configurationDone");
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<BreakpointLocationsResponse> breakpointLocations(BreakpointLocationsArguments args) {
		System.out.println("breakpointLocations");
		final BreakpointLocationsResponse response = new BreakpointLocationsResponse();
		BreakpointLocation breakpoint = new BreakpointLocation();
		breakpoint.setColumn(args.getColumn());
		breakpoint.setEndColumn(args.getEndColumn());
		breakpoint.setEndLine(args.getEndLine());
		breakpoint.setLine(args.getLine());
		BreakpointLocation[] breakpoints = new BreakpointLocation[] { breakpoint };
		response.setBreakpoints(breakpoints);
		return CompletableFuture.completedFuture(response);
	}

	@Override
	public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
		System.out.println("setBreakpoints");
		SourceBreakpoint[] sbps = args.getBreakpoints();
		Breakpoint[] breakpoints = new Breakpoint[sbps.length];
		for (int j = 0; j < sbps.length; j++) {
			breakpoints[j] = new Breakpoint();
			breakpoints[j].setColumn(sbps[j].getColumn());
			breakpoints[j].setLine(sbps[j].getLine());
			breakpoints[j].setId(j);
			breakpoints[j].setVerified(true);
		}
		SetBreakpointsResponse response = new SetBreakpointsResponse();
		response.setBreakpoints(breakpoints);
		return CompletableFuture.completedFuture(response);
	}

	@Override
	public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
		System.out.printf("stackTrace frame: %s, levels: %s\n", args.getStartFrame(), args.getLevels());

		final List<StackFrame> frames = new ArrayList<>(stack.size());
		for (int i = stack.size() - 1; i >= 0; i--) {
			final TLCStackFrame stackFrame = stack.elementAt(i);
			frames.add(stackFrame);
		}

		final StackTraceResponse res = new StackTraceResponse();
		res.setStackFrames(frames.toArray(new StackFrame[frames.size()]));
		return CompletableFuture.completedFuture(res);
	}

	@Override
	public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
		System.out.printf("scopes frame %s\n", args.getFrameId());

		final ScopesResponse response = new ScopesResponse();

		final Optional<TLCStackFrame> findFirst = stack.stream().filter(s -> s.node.myUID == args.getFrameId())
				.findFirst();
		if (findFirst.isPresent()) {
			TLCStackFrame frame = findFirst.get();
			response.setScopes(frame.getScopes());
		}

		return CompletableFuture.completedFuture(response);
	}

	@Override
	public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
		final int vr = args.getVariablesReference();
		System.out.println("variables " + vr);

		final TLCStackFrame frame = this.stack.peek();
		final Variable[] variables = frame.getVariables(vr);

		VariablesResponse value = new VariablesResponse();
		value.setVariables(variables);
		return CompletableFuture.completedFuture(value);
	}

	@Override
	public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
		System.out.println("setVariable");
		return CompletableFuture.completedFuture(new SetVariableResponse());
	}

	@Override
	public CompletableFuture<ThreadsResponse> threads() {
		System.out.println("threads");
		Thread thread = new Thread();
		thread.setId(0);
		thread.setName("worker");
		ThreadsResponse res = new ThreadsResponse();
		res.setThreads(new Thread[] { thread });
		return CompletableFuture.completedFuture(res);
	}

	@Override
	public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
		System.out.println("continue_");
		targetLevel = -1;
		step = Step.Out;
		queue.offer(this);
		return CompletableFuture.completedFuture(new ContinueResponse());
	}

	@Override
	public CompletableFuture<Void> next(NextArguments args) {
		System.out.println("next/stepOver");
		step = Step.Over;
		queue.offer(this);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> stepIn(StepInArguments args) {
		System.out.println("stepIn");
		targetLevel++;
		step = Step.In;
		queue.offer(this);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> stepOut(StepOutArguments args) {
		System.out.println("stepOut");
		targetLevel--;
		step = Step.Out;
		queue.offer(this);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> pause(PauseArguments args) {
		System.out.println("pause");
		Executors.newSingleThreadExecutor().submit(() -> {
			System.err.println("pause -> stopped");
			StoppedEventArguments eventArguments = new StoppedEventArguments();
			eventArguments.setThreadId(0);
			eventArguments.setReason("pause");
			launcher.getRemoteProxy().stopped(eventArguments);
		});
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> launch(Map<String, Object> args) {
		System.out.println("launch");

		final Path p = Paths.get((String) args.get("program"));
		final String specPath = p.getParent().toAbsolutePath().toString();
		final String specName = p.getFileName().toFile().toString();
		final String moduleName = specName.replaceFirst(".tla$", "");

		// IValue#hashCode calls below require fingerprints to be correctly initialized.
		FP64.Init();

		// Listen to that SANY and TLC have to say, and what gets written with TLC!Print*.
		ToolIO.out = new PrintStream(System.out) {
			@Override
			public void println(String str) {
				this.print(str + "\n");
			}
			@Override
			public void print(String str) {
				super.print(str);
				final OutputEventArguments oea = new OutputEventArguments();
				oea.setOutput(str);
				launcher.getRemoteProxy().output(oea);
			}
		};
		ToolIO.reset();
		
		final Tool tool = new FastTool(moduleName, specName, new SimpleFilenameToStream(specPath));
		final ModuleNode module = tool.getSpecProcessor().getRootModule();
		// The spec has to have an "debugMe" operator.
		final OpDefNode valueNode = module.getOpDef("debugMe");

		// Register this instance at the debug target of the aspect that is woven into
		// Tool. Do *not* register before processing the spec above because TLC eagerly
		// tries to evaluate expressions that we don't want to debug.
		IDebugTarget.Factory.set(this);

		// Make sure we pause/stop debugging initially.
		targetLevel = Integer.MIN_VALUE;
		step = Step.In;

		// Kick off the evaluation of the expression in Tool in a different thread.
		Executors.newSingleThreadExecutor().submit(() -> {
			tool.eval(valueNode.getBody(), Context.Empty, TLCState.Empty);
		});

		return CompletableFuture.completedFuture(null);
	}

	// 8888888888888888888888888888888888888888888888888888888888888888888888888//

	private static class TLCStackFrame extends StackFrame {

		private transient final SemanticNode node;
		private transient final Context ctxt;

		private transient final Map<Integer, Variable[]> variableValues = new HashMap<>();

		private static final int CONTEXT_SCOPE = 2913847;

		public TLCStackFrame(SemanticNode node, Context ctxt, final Tool tool) {
			this.node = node;
			this.ctxt = ctxt;

			if (node instanceof NumeralNode) {
				setName(Integer.toString(((NumeralNode)node).val()));
			} else {
				setName(node.getHumanReadableImage());
			}
			setId(node.myUID);

			final Location location = node.getLocation();
			setLine(location.beginLine());
			setEndLine(location.endLine());
			setColumn(location.beginColumn());
			setEndColumn(location.endColumn()+1);

			final Source source = new Source();
			final File moduleFile = tool.getResolver().resolve(node.getTreeNode().getFilename(), true);
			source.setPath(moduleFile.getAbsolutePath().toString());
			setSource(source);

			final List<Variable> vars = new ArrayList<>();
			Context c = this.ctxt;
			while (c.hasNext()) {
				final Variable variable = new Variable();

				final String name = c.getName().getName().toString();
				variable.setName(name);

				Object val = c.getValue();
				if (val instanceof LazyValue) {
					// unlazy/eval LazyValues
					final LazyValue lv = (LazyValue) c.getValue();
					val = tool.eval(lv.expr, lv.con, TLCState.Empty, null, EvalControl.Clear, lv.getCostModel());
				}
				variable.setValue(val.toString());

				variable.setType(val.getClass().getSimpleName());

				if (val instanceof Value) {
					final Value value = (Value) val;
					if (value.toSetEnum() != null) {
						SetEnumValue setEnum = (SetEnumValue) value.toSetEnum();
						final List<Variable> nestedVars = new ArrayList<>(setEnum.size());
						ValueEnumeration elements = setEnum.elements();
						Value elem;
						while ((elem = elements.nextElement()) != null) {
							final Variable nested = new Variable();
							nested.setName(elem.toString());
							nested.setValue(elem.toString());
							nestedVars.add(nested);
						}
						variableValues.put(Math.abs(setEnum.hashCode()), nestedVars.toArray(new Variable[nestedVars.size()]));
						variable.setVariablesReference(Math.abs(setEnum.hashCode()));
					} else if (value.toFcnRcd() != null) {
						FcnRcdValue fcnRcd = (FcnRcdValue) value.toFcnRcd();
						Value[] values = fcnRcd.getDomainAsValues();
						final List<Variable> nestedVars = new ArrayList<>(values.length);
						for (int i = 0; i < values.length; i++) {
							for (Value v : values) {
								final Variable nested = new Variable();
								nested.setName(values[i].toString());
								nested.setValue(fcnRcd.values[i].toString());
								nested.setType(fcnRcd.values[i].getClass().getSimpleName());
								nestedVars.add(nested);
							}
						}
						variableValues.put(Math.abs(fcnRcd.hashCode()), nestedVars.toArray(new Variable[nestedVars.size()]));
						variable.setVariablesReference(Math.abs(fcnRcd.hashCode()));
					} else if (value.toRcd() != null) {
						RecordValue rcd = (RecordValue) value.toRcd();
						//TODO
					}
				}
				vars.add(variable);

				c = c.next();
			}
			variableValues.put(CONTEXT_SCOPE, vars.toArray(new Variable[vars.size()]));
		}

		public Variable[] getVariables(final int vr) {
			return variableValues.get(vr);
		}

		public Scope[] getScopes() {

			final List<Scope> scopes = new ArrayList<>();

			if (!variableValues.isEmpty()) {
				final Scope scope = new Scope();
				scope.setName("Context");
				scope.setVariablesReference(CONTEXT_SCOPE);
				scopes.add(scope);
			}

			return scopes.toArray(new Scope[scopes.size()]);
		}
	}

	private final Stack<TLCStackFrame> stack = new Stack<>();

	//TODO: This is a clutch; it's working but should be simplified!
	private final ArrayBlockingQueue<IDebugTarget> queue = new ArrayBlockingQueue<>(1);
	private volatile int targetLevel;
	private volatile Step step;

	@Override
	public IDebugTarget pushFrame(Tool tool, int level, SemanticNode expr, Context c, int control) {
		System.out.printf("%s Call pushFrame: [%s], level: %s\n", new String(new char[level]).replace('\0', '#'), expr,
				level);

		TLCStackFrame frame = new TLCStackFrame(expr, c, tool);
		stack.push(frame);

		if (matches(step, targetLevel, level)) {
			System.err.println("loadSource -> stopped");
			StoppedEventArguments eventArguments = new StoppedEventArguments();
			eventArguments.setThreadId(0);
			launcher.getRemoteProxy().stopped(eventArguments);

			try {
				queue.take();
			} catch (InterruptedException notExpectedToHappen) {
				notExpectedToHappen.printStackTrace();
				java.lang.Thread.currentThread().interrupt();
			}
		}
		return this;
	}

	@Override
	public IDebugTarget popFrame(Tool tool, Value v, int level, SemanticNode expr, Context c, int control) {
		System.out.printf("%s Call popFrame: [%s], level: %s\n", new String(new char[level]).replace('\0', '#'), expr,
				level);
		TLCStackFrame pop = stack.pop();
		assert expr == pop.node;
		return this;
	}

	//TODO: This is only working more or less for step.in.
	private static boolean matches(Step dir, int targetLevel, int currentLevel) {
		if (dir == Step.In) {
			if (currentLevel >= targetLevel) {
				return true;
			}
		} else if (dir == Step.Over) {
			if (currentLevel == targetLevel) {
				return true;
			}
		} else {
			// When stepping out, level has to greater than or zero/0;
			if (currentLevel < targetLevel || currentLevel == 0) {
				return true;
			}
		}
		return false;
	}
}
