package org.git4j.core;

import org.git4j.core.impl.DefaultGit;
import org.git4j.core.repo.Repository;

public abstract class GitFactory {

	public static Git getGit(Repository repo) throws Exception {
		return new DefaultGit(repo);
	}
}
