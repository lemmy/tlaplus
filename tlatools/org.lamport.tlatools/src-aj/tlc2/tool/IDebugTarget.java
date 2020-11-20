package tlc2.tool;

import tla2sany.semantic.SemanticNode;
import tlc2.debug.TLCDebugger;
import tlc2.util.Context;

public interface IDebugTarget {

	IDebugTarget frame(int level, SemanticNode expr, Context c, int control);
	
	public static class Factory {

		static IDebugTarget getInstance() {
			return inst;
		}

		static IDebugTarget inst = null;

		public static void set(TLCDebugger idt) {
			inst = idt;
		}
	}
}
