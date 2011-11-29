package org.git4j.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.git4j.core.ConflictResolution;
import org.git4j.core.Git;
import org.git4j.core.Workspace;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.repo.InMemoryRepository;
import org.git4j.core.repo.Repository;
import org.git4j.core.transport.DirectTransport;
import org.git4j.core.transport.Transport;
import org.git4j.core.util.ObjectUtils;
import org.junit.Before;
import org.junit.Test;

public class DefaultGitMergeTest {

	private Repository repo;

	private Git git;

	private Git remote;

	private Transport transport;

	private String branch;

	private String remoteBranch;

	private Workspace workspace;

	@Before
	public void before() throws Exception {
		branch = "L" + UUID.randomUUID().toString().replace("-", "");
		remoteBranch = "R" + UUID.randomUUID().toString().replace("-", "");

		repo = new InMemoryRepository();
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
	public void emptyLocal() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		String remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		assertTrue(git.mergeBranch(remoteBranch, branch, "MERGER", "MERGE!",
				ConflictResolution.LEAVE).isEmpty());

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

		// check local heads
		assertEquals(remoteHeadRef, git.getLocalBranchHeadRef(branch));
		assertEquals(remoteHeadRef, git.getRemoteBranchHeadRef(remoteBranch));

		// check remote heads
		assertEquals(remoteHeadRef, remote.getLocalBranchHeadRef(remoteBranch));
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

		assertTrue(git.getLocalBranchHeadRef(branch).equals(remoteHeadRef));

		// commit to remote
		workspace.add("a", "A2");
		remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE 2");

		// fetch to local
		git.fetch(transport, remoteBranch);

		String headRef = git.getLocalBranchHeadRef(branch);

		// merge
		Set<String> conflicts = git.merge(remoteHeadRef, branch, "MERGER",
				"MERGE!", ConflictResolution.LEAVE);

		assertTrue(conflicts.toString(), conflicts.isEmpty());

		String newHeadRef = git.getLocalBranchHeadRef(branch);

		assertFalse(newHeadRef.equals(headRef));
		assertTrue(newHeadRef.equals(remoteHeadRef));

		// remote should be fast-forward to local
		ObjectUtils.canFastForward(repo, remoteHeadRef, headRef);

		// remote should be fast-forward to new HEAD
		ObjectUtils.canFastForward(repo, remoteHeadRef, newHeadRef);

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(1, workspace.list().size());
		assertEquals("A2", workspace.get("a"));
	}

	@Test
	public void nonFastForwardWithoutConflicts() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		String remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE");

		// commit to local
		workspace.add("b", "A2");
		String headRef = git.commit(workspace, branch, "LOCAL",
				"COMMITTED BY LOCAL");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		Set<String> conflicts = git.mergeBranch(remoteBranch, branch, "MERGER",
				"MERGE!", ConflictResolution.LEAVE);

		assertTrue(conflicts.isEmpty());

		String newHeadRef = git.getLocalBranchHeadRef(branch);

		assertFalse(newHeadRef.equals(headRef));
		assertFalse(newHeadRef.equals(remoteHeadRef));

		// remote should be fast-forward to local
		ObjectUtils.canFastForward(repo, remoteHeadRef, headRef);

		// remote should be fast-forward to new HEAD
		ObjectUtils.canFastForward(repo, remoteHeadRef, newHeadRef);

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(2, workspace.list().size());
		assertEquals("A1", workspace.get("a"));
		assertEquals("A2", workspace.get("b"));
	}

	@Test
	public void nonFastForwardWithConflictsLeft() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		String remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE");

		// commit to local
		workspace.add("a", "A2");
		String headRef = git.commit(workspace, branch, "LOCAL",
				"COMMITTED BY LOCAL");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		Set<String> conflicts = git.mergeBranch(remoteBranch, branch, "MERGER",
				"MERGE!", ConflictResolution.LEAVE);

		assertFalse(conflicts.isEmpty());
		assertTrue(conflicts.contains("a"));

		String newHeadRef = git.getLocalBranchHeadRef(branch);

		assertTrue(newHeadRef.equals(headRef));
		assertFalse(newHeadRef.equals(remoteHeadRef));

		// remote should be fast-forward to local
		ObjectUtils.canFastForward(repo, remoteHeadRef, headRef);

		// remote should be fast-forward to new HEAD
		ObjectUtils.canFastForward(repo, remoteHeadRef, newHeadRef);

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(1, workspace.list().size());
		assertEquals("A2", workspace.get("a"));
	}

	@Test
	public void nonFastForwardWithConflictsUseRemoteBranch() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		String remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE");

		// commit to local
		workspace.add("a", "A2");
		String headRef = git.commit(workspace, branch, "LOCAL",
				"COMMITTED BY LOCAL");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		Set<String> conflicts = git.mergeBranch(remoteBranch, branch, "MERGER",
				"MERGE!", ConflictResolution.USE_REMOTE_BRANCH);

		assertFalse(conflicts.isEmpty());
		assertTrue(conflicts.contains("a"));

		String newHeadRef = git.getLocalBranchHeadRef(branch);

		assertFalse(newHeadRef.equals(headRef));
		assertFalse(newHeadRef.equals(remoteHeadRef));

		// remote should be fast-forward to local
		ObjectUtils.canFastForward(repo, remoteHeadRef, headRef);

		// remote should be fast-forward to new HEAD
		ObjectUtils.canFastForward(repo, remoteHeadRef, newHeadRef);

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(1, workspace.list().size());
		assertEquals("A1", workspace.get("a"));
	}

	@Test
	public void nonFastForwardWithConflictsUseLocalBranch() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		String remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE");

		// commit to local
		workspace.add("a", "A2");
		String headRef = git.commit(workspace, branch, "LOCAL",
				"COMMITTED BY LOCAL");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		Set<String> conflicts = git.mergeBranch(remoteBranch, branch, "MERGER",
				"MERGE!", ConflictResolution.USE_LOCAL_BRANCH);

		assertFalse(conflicts.isEmpty());
		assertTrue(conflicts.contains("a"));

		String newHeadRef = git.getLocalBranchHeadRef(branch);

		assertFalse(newHeadRef.equals(headRef));
		assertFalse(newHeadRef.equals(remoteHeadRef));

		// remote should be fast-forward to local
		ObjectUtils.canFastForward(repo, remoteHeadRef, headRef);

		// remote should be fast-forward to new HEAD
		ObjectUtils.canFastForward(repo, remoteHeadRef, newHeadRef);

		git.checkoutLocalBranchHead(workspace, branch);
		assertEquals(1, workspace.list().size());
		assertEquals("A2", workspace.get("a"));
	}

	@Test
	public void alreadyUpToDate() throws Exception {
		// commit to remote
		workspace.add("a", "A1");
		String remoteHeadRef = remote.commit(workspace, remoteBranch, "REMOTE",
				"COMMITTED BY REMOTE");

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		assertTrue(git.mergeBranch(remoteBranch, branch, "MERGER", "MERGE!",
				ConflictResolution.LEAVE).isEmpty());

		// fetch to local
		git.fetch(transport, remoteBranch);

		// merge
		assertTrue(git.mergeBranch(remoteBranch, branch, "MERGER", "MERGE!",
				ConflictResolution.LEAVE).isEmpty());

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

		// check local heads
		assertEquals(remoteHeadRef, git.getLocalBranchHeadRef(branch));
		assertEquals(remoteHeadRef, git.getRemoteBranchHeadRef(remoteBranch));

		// check remote heads
		assertEquals(remoteHeadRef, remote.getLocalBranchHeadRef(remoteBranch));
	}
}
