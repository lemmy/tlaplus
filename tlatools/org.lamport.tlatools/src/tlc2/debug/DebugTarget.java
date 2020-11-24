package tlc2.debug;
///*******************************************************************************
// * Copyright (c) 2020 Microsoft Research. All rights reserved. 
// *
// * The MIT License (MIT)
// * 
// * Permission is hereby granted, free of charge, to any person obtaining a copy 
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// * of the Software, and to permit persons to whom the Software is furnished to do
// * so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software. 
// * 
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
// * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// *
// * Contributors:
// *   Markus Alexander Kuppe - initial API and implementation
// ******************************************************************************/
//package tlc2.tool;
//
//import java.util.Scanner;
//
//import tla2sany.semantic.ExprNode;
//import tla2sany.semantic.LetInNode;
//import tla2sany.semantic.NumeralNode;
//import tla2sany.semantic.OpDefNode;
//import tla2sany.semantic.SemanticNode;
//import tlc2.util.Context;
//
//public class DebugTarget implements IDebugTarget {
//
//	private final Step dir;
//	private final int level;
//
//	public DebugTarget(int l, Step d) {
//		this.level = l;
//		this.dir = d;
//	}
//
//	public static DebugTarget getInitial(SemanticNode expr) {
//		if (expr instanceof LetInNode) {
//			LetInNode lin = (LetInNode) expr;
//			OpDefNode[] lets = lin.getLets();
//			for (int i = 0; i < lets.length; i++) {
//				if (lets[i].getName().equals("target")) {
//					ExprNode body = lets[i].getBody();
//					if (body instanceof NumeralNode) {
//						NumeralNode nn = (NumeralNode) body;
//						return new DebugTarget(nn.val(), Step.In);
//					}
//				}
//			}
//		}
//		return new DebugTarget(-1, Step.Out);
//	}
//
//	public boolean matches(int currentLevel) {
//		if (dir == Step.In) {
//			if (currentLevel >= level) {
//				return true;
//			}
//		} else if (dir == Step.Over) {
//			if (currentLevel == level) {
//				return true;
//			}
//		} else {
//			// When stepping out, level has to greater than or zero/0;
//			if (currentLevel < level || currentLevel == 0) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private final Scanner scanner = new Scanner(System.in);
//	public DebugTarget popFrame(int level, SemanticNode expr, Context c, int control) {
//		return this;
//	}
//
//	public DebugTarget pushFrame(Tool tool, int level, SemanticNode expr, Context c, int control) {
//		if (matches(level)) {
//			String indent = new String(new char[level]).replace('\0', '#');
//			System.out.printf("%s(%s/%s): loc: (%s) ctxt: (%s)\n", indent, level, this.level, expr, c);
//			final String nextLine = scanner.nextLine();
//			if (nextLine.trim().startsWith("o")) {
//				return new DebugTarget(level, DebugTarget.Step.Over);
//			} else if (nextLine.trim().startsWith("i")) {
//				return new DebugTarget(level, DebugTarget.Step.In);
//			} else {
//				return new DebugTarget(level, DebugTarget.Step.Out);
//			}
//		}
//		return this;
//	}
//}
