package tlc2.tool;

import tla2sany.semantic.SemanticNode;

public interface CostModel {

	CostModel DO_NOT_RECORD = new CostModel() {

		@Override
		public void increment(SemanticNode oan) {
			// no-op
		}

		@Override
		public void report() {
			// no-op
		}

		@Override
		public CostModel get(final SemanticNode eon) {
			return this;
		}

		@Override
		public void add(long size) {
			// no-op
		}

		@Override
		public void increment() {
			// no-op
		}

		@Override
		public boolean matches(SemanticNode expr) {
			return true;
		}
	};
	void increment();

	void increment(SemanticNode ast);

	void report();

	CostModel get(final SemanticNode cmpts);

	void add(long size);

	boolean matches(SemanticNode expr);
}
