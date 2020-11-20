package tlc2.debug;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Exchanger;
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
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.GotoArguments;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.LoadedSourcesArguments;
import org.eclipse.lsp4j.debug.LoadedSourcesResponse;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.PauseArguments;
import org.eclipse.lsp4j.debug.RestartArguments;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetVariableArguments;
import org.eclipse.lsp4j.debug.SetVariableResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceArguments;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.SourceResponse;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepBackArguments;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.TerminateArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import tla2sany.semantic.ModuleNode;
import tla2sany.semantic.OpDefNode;
import tla2sany.semantic.SemanticNode;
import tla2sany.st.Location;
import tlc2.tool.IDebugTarget;
import tlc2.tool.impl.FastTool;
import tlc2.tool.impl.Tool;
import tlc2.util.Context;
import util.SimpleFilenameToStream;

public class TLCDebugger extends AbstractDebugger implements IDebugTarget {

	public static void main(String[] args) throws IOException {
		try (ServerSocket serverSocket = new ServerSocket(4712)) {
			final Socket socket = serverSocket.accept();
			final InputStream inputStream = socket.getInputStream();
			final OutputStream outputStream = socket.getOutputStream();

			final TLCDebugger server = new TLCDebugger();
			final Launcher<IDebugProtocolClient> launcher = DSPLauncher.createServerLauncher(server, inputStream,
					outputStream);
			server.setLauncher(launcher);

			launcher.startListening();

			final IDebugProtocolClient remoteProxy = launcher.getRemoteProxy();
			remoteProxy.initialized();
		}
	}

	private Launcher<IDebugProtocolClient> launcher;
	private int line;
	private Tool tool;

	@Override
	public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
		System.out.println("initialize");
		Capabilities cap = new Capabilities();
//		cap.setSupportsSetExpression(true);
//		cap.setSupportsRestartRequest(true);
//		cap.setSupportsBreakpointLocationsRequest(true);
//		cap.setSupportsCancelRequest(true);
//		cap.setSupportsConditionalBreakpoints(true);
		cap.setSupportsConfigurationDoneRequest(true);
		cap.setSupportsLoadedSourcesRequest(true);
//		cap.setSupportsStepBack(true);
//		cap.setSupportsSteppingGranularity(true);
		return CompletableFuture.completedFuture(cap);
	}

	@Override
	public CompletableFuture<Void> launch(Map<String, Object> args) {
		System.out.println("launch"); 
		return CompletableFuture.completedFuture(null);
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
	public CompletableFuture<Void> attach(Map<String, Object> args) {
		System.out.println("attach");
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> restart(RestartArguments args) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> disconnect(DisconnectArguments args) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> terminate(TerminateArguments args) {
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
	public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
		System.out.println("continue_");
		return CompletableFuture.completedFuture(new ContinueResponse());
	}

	@Override
	public CompletableFuture<Void> next(NextArguments args) {
		System.out.println("next");
		line++;
		Executors.newSingleThreadExecutor().submit(() -> {
			System.err.println("next -> stopped");
			StoppedEventArguments eventArguments = new StoppedEventArguments();
			eventArguments.setThreadId(0);
			eventArguments.setReason("step");
			launcher.getRemoteProxy().stopped(eventArguments);
		});
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> stepIn(StepInArguments args) {
		System.out.println("stepIn");
		line++;
		Executors.newSingleThreadExecutor().submit(() -> {
			System.err.println("stepIn -> stopped");
			StoppedEventArguments eventArguments = new StoppedEventArguments();
			eventArguments.setThreadId(0);
			eventArguments.setReason("step");
			launcher.getRemoteProxy().stopped(eventArguments);
		});
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> stepOut(StepOutArguments args) {
		System.out.println("stepOut");
		line++;
		Executors.newSingleThreadExecutor().submit(() -> {
			System.err.println("stepOut -> stopped");
			StoppedEventArguments eventArguments = new StoppedEventArguments();
			eventArguments.setThreadId(0);
			eventArguments.setReason("step");
			launcher.getRemoteProxy().stopped(eventArguments);
		});
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> stepBack(StepBackArguments args) {
		System.out.println("stepBack");
		line--;
		Executors.newSingleThreadExecutor().submit(() -> {
			System.err.println("stepBack -> stopped");
			StoppedEventArguments eventArguments = new StoppedEventArguments();
			eventArguments.setThreadId(0);
			eventArguments.setReason("step");
			launcher.getRemoteProxy().stopped(eventArguments);
		});
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> goto_(GotoArguments args) {
		System.out.println("goto_");
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
	public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
		System.out.println("stackTrace");
		
        ModuleNode module = tool.getSpecProcessor().getRootModule();
        OpDefNode valueNode = module.getOpDef("replvalue");
		
		StackFrame stackFrame = new StackFrame();
		stackFrame.setId(0);
		
		Location location = valueNode.getLocation();
		stackFrame.setLine(location.beginLine());
		stackFrame.setEndLine(location.endLine());
		stackFrame.setColumn(location.beginColumn());
		stackFrame.setEndColumn(location.endColumn());
		
		stackFrame.setName(valueNode.getHumanReadableImage());
		
		Source source = new Source();
		source.setPath("/home/markus/src/TLA/_meta/vscode-mock-debug/sampleworkspace/Debug.tla");
		source.setName("Debug.tla");
		stackFrame.setSource(source);

		StackFrame[] stackFrames = new StackFrame[] { stackFrame };

		final StackTraceResponse res = new StackTraceResponse();
		res.setStackFrames(stackFrames);
		return CompletableFuture.completedFuture(res);
	}

	@Override
	public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
		System.out.println("scopes");
		return CompletableFuture.completedFuture(new ScopesResponse());
	}

	@Override
	public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
		System.out.println("variables");
		return CompletableFuture.completedFuture(new VariablesResponse());
	}

	@Override
	public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
		System.out.println("setVariable");
		return CompletableFuture.completedFuture(new SetVariableResponse());
	}

	@Override
	public CompletableFuture<SourceResponse> source(SourceArguments args) {
		System.out.println("SourceResponse");
		SourceResponse response = new SourceResponse();
		String fileName = "/home/markus/src/TLA/_meta/vscode-mock-debug/sampleworkspace/test.md";
		try {
			response.setContent(new String(Files.readAllBytes(Paths.get(fileName))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(response);
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
	public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
		System.out.println("loadedSources");
		
		IDebugTarget.Factory.set(this);
		
		final Path p = Paths.get("/home/markus/src/TLA/_meta/vscode-mock-debug/sampleworkspace/Debug.tla");
		
		final SimpleFilenameToStream resolver = 
				new SimpleFilenameToStream(p.getParent().toAbsolutePath().toString());
		final String spec = p.getFileName().toFile().toString();
		tool = new FastTool(spec.replaceFirst(".tla$", ""), spec, resolver);
		
		final LoadedSourcesResponse response = new LoadedSourcesResponse();
		final Source source = new Source();
		source.setName(p.getFileName().toString());
		source.setPath(p.toAbsolutePath().toString());
		final Source[] sources = new Source[] { source };
		response.setSources(sources);
		
		Executors.newSingleThreadExecutor().submit(() -> {
			System.err.println("loadSource -> stopped");
			StoppedEventArguments eventArguments = new StoppedEventArguments();
			eventArguments.setThreadId(0);
			launcher.getRemoteProxy().stopped(eventArguments);
		});
		
		return CompletableFuture.completedFuture(response);
	}

	public void setLauncher(Launcher<IDebugProtocolClient> launcher) {
		this.launcher = launcher;
	}


	private final Exchanger<Object> exchange = new Exchanger<>();
	
	@Override
	public IDebugTarget frame(int level, SemanticNode expr, Context c, int control) {
		// TODO Auto-generated method stub
		return null;
	}
}
