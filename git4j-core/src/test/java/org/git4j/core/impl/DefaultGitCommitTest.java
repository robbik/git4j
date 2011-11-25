package org.git4j.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.git4j.core.Git;
import org.git4j.core.Workspace;
import org.git4j.core.repo.InMemoryRepository;
import org.git4j.core.repo.Repository;
import org.junit.Before;
import org.junit.Test;

public class DefaultGitCommitTest {

	private Git git;

	private String branch;

	private Workspace workspace;

	@Before
	public void before() throws Exception {
		branch = "L" + UUID.randomUUID().toString().replace("-", "");

		Repository repo = new InMemoryRepository();

		git = new DefaultGit(repo);
		workspace = new Workspace();
	}

	@Test
	public void simple() throws Exception {
		String content = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		git.commit(workspace.add("b", content), branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>",
				"initial commit!");

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(content, workspace.get("b"));
		assertNull(workspace.get("b2"));
	}

	@Test
	public void add() throws Exception {
		String content = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		git.commit(workspace.add("b", content), branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>",
				"initial commit!");

		git.commit(workspace.add("b2", content), branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>", "commit 2");

		workspace = new Workspace();

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(content, workspace.get("b"));
		assertEquals(content, workspace.get("b2"));
	}

	@Test
	public void modify() throws Exception {
		String content1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String content2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ2";

		String commit1 = git.commit(workspace.add("b", content1), branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>",
				"initial commit!");

		git.commit(workspace.add("b", content2), branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>", "commit 2");

		workspace = new Workspace();

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(content2, workspace.get("b"));

		git.checkout(workspace, commit1);
		assertEquals(content1, workspace.get("b"));
	}

	@Test
	public void remove() throws Exception {
		String content1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String content2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ2";

		String commit1 = git.commit(workspace.add("b", content1), branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>",
				"initial commit!");

		workspace.add("b2", content2);
		workspace.remove("b");

		git.commit(workspace, branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>", "commit 2");

		git.checkoutLocalBranchHead(workspace, branch);
		assertNull(workspace.get("b"));
		assertEquals(content2, workspace.get("b2"));
		assertEquals(1, workspace.list().size());

		git.checkout(workspace, commit1);
		assertEquals(content1, workspace.get("b"));
		assertNull(workspace.get("b2"));
	}

	@Test
	public void addAfterRemove() throws Exception {
		String content1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String content2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ2";

		workspace.add("b", content1);
		workspace.add("b2", content2);

		git.commit(workspace, branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>",
				"initial commit!");

		workspace.remove("b");
		git.commit(workspace, branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>", "commit 2");

		workspace.add("b", content2);
		git.commit(workspace, branch,
				"robbi.kurniawan <robbi.kurniawan@sigma.co.id>", "commit 3");

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(content2, workspace.get("b"));
		assertEquals(content2, workspace.get("b2"));
	}
}
