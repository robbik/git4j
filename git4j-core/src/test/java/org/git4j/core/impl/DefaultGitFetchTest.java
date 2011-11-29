package org.git4j.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.UUID;

import org.git4j.core.Workspace;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.repo.InMemoryRepository;
import org.git4j.core.repo.Repository;
import org.git4j.core.transport.DirectTransport;
import org.git4j.core.transport.Transport;
import org.junit.Before;
import org.junit.Test;

public class DefaultGitFetchTest {

	private DefaultGit git;

	private DefaultGit remote;

	private Transport transport;

	private String branch;

	private String remoteBranch;

	private Workspace workspace;

	@Before
	public void before() throws Exception {
		branch = "L" + UUID.randomUUID().toString().replace("-", "");
		remoteBranch = "R" + UUID.randomUUID().toString().replace("-", "");

		Repository repo = new InMemoryRepository();
		Repository repoRemote = new InMemoryRepository();

		git = new DefaultGit(repo);
		remote = new DefaultGit(repoRemote);

		transport = new DirectTransport(repoRemote);
		workspace = new Workspace();
	}

	private static boolean contains(String branch, Collection<BranchAndHead> c) {
		for (BranchAndHead e : c) {
			if (e.getBranch().equals(branch)) {
				return true;
			}
		}

		return false;
	}

	@Test
	public void emptyRemoteEmptyLocal() throws Exception {
		git.fetch(transport, remoteBranch);

		assertEquals(0, git.getRemoteBranches().size());
		assertEquals(0, git.getLocalBranches().size());
	}

	@Test
	public void emptyLocal() throws Exception {
		// commit remote
		workspace.add("a", "ABCD");
		remote.commit(workspace, remoteBranch, "AUTHOR", "INITIAL COMMIT");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// check remote branches in local
		assertTrue(contains(remoteBranch, git.getRemoteBranches()));
		assertFalse(contains(branch, git.getRemoteBranches()));

		// check local branches in local
		assertFalse(contains(remoteBranch, git.getLocalBranches()));
		assertFalse(contains(branch, git.getLocalBranches()));

		// check remote branches in remote
		assertFalse(contains(remoteBranch, remote.getRemoteBranches()));
		assertFalse(contains(branch, remote.getRemoteBranches()));

		// check local branches in remote
		assertTrue(contains(remoteBranch, remote.getLocalBranches()));
		assertFalse(contains(branch, remote.getLocalBranches()));

		// check just fetched workspace
		git.checkoutRemoteBranchHead(workspace, remoteBranch);
		assertEquals(1, workspace.list().size());
		assertEquals("ABCD", workspace.get("a"));
	}

	@Test
	public void emptyRemote() throws Exception {
		// commit local
		workspace.add("a", "ABCD");
		git.commit(workspace, branch, "AUTHOR", "INITIAL COMMIT");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// check remote branches in local
		assertFalse(contains(remoteBranch, git.getRemoteBranches()));
		assertFalse(contains(branch, git.getRemoteBranches()));

		// check local branches in local
		assertFalse(contains(remoteBranch, git.getLocalBranches()));
		assertTrue(contains(branch, git.getLocalBranches()));

		// check remote branches in remote
		assertFalse(contains(remoteBranch, remote.getRemoteBranches()));
		assertFalse(contains(branch, remote.getRemoteBranches()));

		// check local branches in remote
		assertFalse(contains(remoteBranch, remote.getLocalBranches()));
		assertFalse(contains(branch, remote.getLocalBranches()));

		// check workspace
		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(1, workspace.list().size());
		assertEquals("ABCD", workspace.get("a"));
	}

	@Test
	public void bothNotEmpty() throws Exception {
		// commit local
		workspace.add("a", "ABCD");
		git.commit(workspace, branch, "AUTHOR", "INITIAL COMMIT");

		workspace.reset();

		// commit remote
		workspace.add("a", "ABCD2");
		remote.commit(workspace, remoteBranch, "AUTHOR", "INITIAL COMMIT");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// check remote branches in local
		assertTrue(contains(remoteBranch, git.getRemoteBranches()));
		assertFalse(contains(branch, git.getRemoteBranches()));

		// check local branches in local
		assertFalse(contains(remoteBranch, git.getLocalBranches()));
		assertTrue(contains(branch, git.getLocalBranches()));

		// check remote branches in remote
		assertFalse(contains(remoteBranch, remote.getRemoteBranches()));
		assertFalse(contains(branch, remote.getRemoteBranches()));

		// check local branches in remote
		assertTrue(contains(remoteBranch, remote.getLocalBranches()));
		assertFalse(contains(branch, remote.getLocalBranches()));

		// check just fetched workspace
		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(1, workspace.list().size());
		assertEquals("ABCD", workspace.get("a"));

		// check just fetched workspace
		git.checkoutRemoteBranchHead(workspace, remoteBranch);
		assertEquals(1, workspace.list().size());
		assertEquals("ABCD2", workspace.get("a"));
	}
}
