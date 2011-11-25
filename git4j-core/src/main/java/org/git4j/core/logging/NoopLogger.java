package org.git4j.core.logging;

public class NoopLogger implements Logger {
	
	public static final Logger INSTANCE = new NoopLogger();
	
	private NoopLogger() {
		// do nothing
	}

	public boolean isTraceEnabled() {
		return false;
	}

	public void trace(String msg) {
		// do nothing
	}

	public void info(String msg) {
		// do nothing
	}

	public void warning(String msg) {
		// do nothing
	}

	public void warning(String msg, Throwable t) {
		// do nothing
	}

	public void error(String msg) {
		// do nothing
	}

	public void error(String msg, Throwable t) {
		// do nothing
	}
}
