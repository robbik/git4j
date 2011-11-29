package org.git4j.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.UUID;

import org.git4j.core.ConflictResolution;
import org.git4j.core.GitException;
import org.git4j.core.Workspace;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.repo.InMemoryRepository;
import org.git4j.core.repo.Repository;
import org.git4j.core.transport.PipedTransport;
import org.git4j.core.transport.Transport;
import org.junit.Before;
import org.junit.Test;

public class DefaultGitPushTest {

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

		repo.wipe();

		git = new DefaultGit(repo);
		remote = new DefaultGit(repoRemote);

		transport = new PipedTransport(repoRemote);
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
	public void emptyRemote() throws Exception {
		// commit to local
		workspace.add("a", "A1");
		String headRef = git.commit(workspace, branch, "AUTHOR",
				"COMMIT MESSAGE");

		// push to remote
		git.push(transport, headRef, remoteBranch);

		// check local branches in local
		assertTrue(contains(branch, git.getLocalBranches()));
		assertFalse(contains(remoteBranch, git.getLocalBranches()));

		// check remote branches in local
		assertFalse(contains(branch, git.getRemoteBranches()));
		assertTrue(contains(remoteBranch, git.getRemoteBranches()));

		// check local branches in remote
		assertFalse(contains(branch, remote.getLocalBranches()));
		assertTrue(contains(remoteBranch, remote.getLocalBranches()));

		// check remote branches in remote
		assertFalse(contains(branch, remote.getRemoteBranches()));
		assertFalse(contains(remoteBranch, remote.getRemoteBranches()));

		// check local workspace
		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(1, workspace.list().size());
		assertEquals("A1", workspace.get("a"));

		// check remote workspace
		remote.checkoutLocalBranchHead(workspace, remoteBranch);
		assertEquals(1, workspace.list().size());
		assertEquals("A1", workspace.get("a"));
	}

	@Test
	public void fastForward() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		String remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		assertTrue(git.merge(remoteHeadRef, branch, "MERGER", "MERGE!",
				ConflictResolution.LEAVE).isEmpty());

		// checkout
		git.checkout(workspace, remoteHeadRef);

		// commit to local
		workspace.add("a", "A2");
		String headRef = git.commit(workspace, branch, "LOCAL",
				"COMMITTED BY LOCAL");

		// push to remote
		git.push(transport, headRef, remoteBranch);

		// check local branches in local
		assertTrue(contains(branch, git.getLocalBranches()));
		assertFalse(contains(remoteBranch, git.getLocalBranches()));

		// check remote branches in local
		assertFalse(contains(branch, git.getRemoteBranches()));
		assertTrue(contains(remoteBranch, git.getRemoteBranches()));

		// check local branches in remote
		assertFalse(contains(branch, remote.getLocalBranches()));
		assertTrue(contains(remoteBranch, remote.getLocalBranches()));

		// check remote branches in remote
		assertFalse(contains(branch, remote.getRemoteBranches()));
		assertFalse(contains(remoteBranch, remote.getRemoteBranches()));

		// check local workspace
		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(1, workspace.list().size());
		assertEquals("A2", workspace.get("a"));

		// check remote workspace
		remote.checkoutLocalBranchHead(workspace, remoteBranch);
		assertEquals(1, workspace.list().size());
		assertEquals("A2", workspace.get("a"));
	}

	@Test(expected = GitException.class)
	public void nonFastForward() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		remote.commit(workspace, remoteBranch, "REMOTE", "COMMITTED BY REMOTE");

		workspace.reset();

		// commit to local
		workspace.add("a", "A2");
		String headRef = git.commit(workspace, branch, "LOCAL",
				"COMMITTED BY LOCAL");

		workspace.reset();

		// push to remote
		git.push(transport, headRef, remoteBranch);
	}

	@Test
	public void alreadyUpToDate() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		String remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE");

		workspace.reset();

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		assertTrue(git.merge(remoteHeadRef, branch, "MERGER", "MERGE!",
				ConflictResolution.LEAVE).isEmpty());

		String headRef = git.getLocalBranchHeadRef(branch);

		// push to remote
		git.push(transport, headRef, remoteBranch);

		// check local branches in local
		assertTrue(contains(branch, git.getLocalBranches()));
		assertFalse(contains(remoteBranch, git.getLocalBranches()));

		// check remote branches in local
		assertFalse(contains(branch, git.getRemoteBranches()));
		assertTrue(contains(remoteBranch, git.getRemoteBranches()));

		// check local branches in remote
		assertFalse(contains(branch, remote.getLocalBranches()));
		assertTrue(contains(remoteBranch, remote.getLocalBranches()));

		// check remote branches in remote
		assertFalse(contains(branch, remote.getRemoteBranches()));
		assertFalse(contains(remoteBranch, remote.getRemoteBranches()));

		// check heads in local
		assertEquals(headRef, git.getLocalBranchHeadRef(branch));
		assertEquals(remoteHeadRef, git.getRemoteBranchHeadRef(remoteBranch));

		// check heads in remote
		assertEquals(remoteHeadRef, remote.getLocalBranchHeadRef(remoteBranch));
	}
}
