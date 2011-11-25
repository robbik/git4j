package org.git4j.core.impl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.UUID;

import org.git4j.core.Git;
import org.git4j.core.Workspace;
import org.git4j.core.objs.Status;
import org.git4j.core.repo.FileRepository;
import org.git4j.core.repo.Repository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkspaceTest {

	private Git git;

	private String branch;

	private Workspace workspace;

	@BeforeClass
	public static void beforeClass() throws Exception {
		new FileRepository(new File("target" + File.separator + "test-repo"))
				.wipe();
	}

	@Before
	public void before() throws Exception {
		branch = "L" + UUID.randomUUID().toString().replace("-", "");

		Repository repo = new FileRepository(new File("target" + File.separator
				+ "test-repo"));

		git = new DefaultGit(repo);

		workspace = new Workspace();
	}

	@Test
	public void add() throws Exception {
		workspace.add("a", "ABCDEFGHIJKL");
		workspace.add("b", "ABCDEFGHIJKL");

		Status status = workspace.status();
		assertEquals(2, status.getAdded().size());
		assertEquals(0, status.getModified().size());
		assertEquals(0, status.getRemoved().size());
	}

	@Test
	public void removeIfEmpty() throws Exception {
		workspace.remove("a");

		Status status = workspace.status();
		assertEquals(0, status.getAdded().size());
		assertEquals(0, status.getModified().size());
		assertEquals(0, status.getRemoved().size());
	}

	@Test
	public void removeIfExists() throws Exception {
		workspace.add("a", "test1");
		workspace.remove("a");

		Status status = workspace.status();
		assertEquals(0, status.getAdded().size());
		assertEquals(0, status.getModified().size());
		assertEquals(0, status.getRemoved().size());
	}

	@Test
	public void removeIfNotExists() throws Exception {
		workspace.add("a", "test1");
		workspace.remove("b");

		Status status = workspace.status();
		assertEquals(1, status.getAdded().size());
		assertEquals(0, status.getModified().size());
		assertEquals(0, status.getRemoved().size());
	}

	@Test
	public void addDifferentContentWithCommittedOne() throws Exception {
		workspace.add("a", "test1");
		git.commit(workspace, branch, "ME", "COMMIT 1");

		workspace.add("a", "test2");

		Status status = workspace.status();
		assertEquals(0, status.getAdded().size());
		assertEquals(1, status.getModified().size());
		assertEquals(0, status.getRemoved().size());

		status = git.status(workspace, branch);
		assertEquals(0, status.getAdded().size());
		assertEquals(1, status.getModified().size());
		assertEquals(0, status.getRemoved().size());
	}

	@Test
	public void addSameContentWithCommittedOne() throws Exception {
		workspace.add("a", "test1");
		git.commit(workspace, branch, "ME", "COMMIT 1");

		workspace.add("a", "test1");

		Status status = workspace.status();
		assertEquals(0, status.getAdded().size());
		assertEquals(0, status.getModified().size());
		assertEquals(0, status.getRemoved().size());

		status = git.status(workspace, branch);
		assertEquals(0, status.getAdded().size());
		assertEquals(0, status.getModified().size());
		assertEquals(0, status.getRemoved().size());
	}

	@Test
	public void removeIfHasCommitted() throws Exception {
		workspace.add("a", "test1");
		git.commit(workspace, branch, "ME", "COMMIT 1");

		workspace.remove("a");

		Status status = workspace.status();
		assertEquals(0, status.getAdded().size());
		assertEquals(0, status.getModified().size());
		assertEquals(1, status.getRemoved().size());

		status = git.status(workspace, branch);
		assertEquals(0, status.getAdded().size());
		assertEquals(0, status.getModified().size());
		assertEquals(1, status.getRemoved().size());
	}
}
