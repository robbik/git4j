package org.git4j.core;

public class GitException extends RuntimeException {

	private static final long serialVersionUID = 7356164150925939701L;

	public GitException() {
		// do nothing
	}

	public GitException(String msg) {
		super(msg);
	}

	public GitException(Throwable cause) {
		super(cause);
	}

	public GitException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
