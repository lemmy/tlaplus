package tlc2.tool;

import java.util.Scanner;
import java.util.concurrent.Exchanger;

import tla2sany.semantic.ExprNode;
import tla2sany.semantic.LetInNode;
import tla2sany.semantic.NumeralNode;
import tla2sany.semantic.OpDefNode;
import tla2sany.semantic.SemanticNode;
import tlc2.util.Context;

public class DebugTarget implements IDebugTarget {

	public enum Step {
		In, Out, Over
	};

	private final Step dir;
	private final int level;

	public DebugTarget(int l, Step d) {
		this.level = l;
		this.dir = d;
	}

	public static DebugTarget getInitial(SemanticNode expr) {
		if (expr instanceof LetInNode) {
			LetInNode lin = (LetInNode) expr;
			OpDefNode[] lets = lin.getLets();
			for (int i = 0; i < lets.length; i++) {
				if (lets[i].getName().equals("target")) {
					ExprNode body = lets[i].getBody();
					if (body instanceof NumeralNode) {
						NumeralNode nn = (NumeralNode) body;
						return new DebugTarget(nn.val(), Step.In);
					}
				}
			}
		}
		return new DebugTarget(-1, Step.Out);
	}

	public boolean matches(int currentLevel) {
		if (dir == Step.In) {
			if (currentLevel >= level) {
				return true;
			}
		} else if (dir == Step.Over) {
			if (currentLevel == level) {
				return true;
			}
		} else {
			// When stepping out, level has to greater than or zero/0;
			if (currentLevel < level || currentLevel == 0) {
				return true;
			}
		}
		return false;
	}
	
	private final Scanner scanner = new Scanner(System.in);
	
	public DebugTarget frame(int level, SemanticNode expr, Context c, int control) throws InterruptedException {
		if (matches(level)) {
			// (This) thread A: remember current frame in a (concurrent) data-structure
			
			// thread A: send launcher.getRemoteProxy().stopped(eventArguments)
			// thread A; await
			
			// Some thread; debugger pulls stackFrame
			
			// Some thread; receives new step over/in/out target and sets it in a queue
			// Some thread; notifies waiting/blocked thread A
			
			// Thread A; returns new debugtarget
			DebugTarget ex = (DebugTarget) exchange.exchange(expr);
			
			String indent = new String(new char[level]).replace('\0', '#');
			System.out.printf("%s(%s/%s): loc: (%s) ctxt: (%s)\n", indent, level, this.level, expr, c);
			final String nextLine = scanner.nextLine();
			if (nextLine.trim().startsWith("o")) {
				return new DebugTarget(level, DebugTarget.Step.Over);
			} else if (nextLine.trim().startsWith("i")) {
				return new DebugTarget(level, DebugTarget.Step.In);
			} else {
				return new DebugTarget(level, DebugTarget.Step.Out);
			}
		}
		return this;
	}
}
