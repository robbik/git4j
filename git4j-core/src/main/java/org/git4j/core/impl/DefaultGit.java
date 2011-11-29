package org.git4j.core.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.git4j.core.ConflictResolution;
import org.git4j.core.Git;
import org.git4j.core.GitException;
import org.git4j.core.Workspace;
import org.git4j.core.logging.Logger;
import org.git4j.core.logging.LoggerFactory;
import org.git4j.core.objs.Blob;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.Status;
import org.git4j.core.objs.UploadPack;
import org.git4j.core.repo.Repository;
import org.git4j.core.transport.Transport;
import org.git4j.core.util.ObjectUtils;
import org.git4j.core.util.StringUtils;

/**
 * Default Git Implementation
 * 
 * @author robbi.kurniawan
 * 
 */
public class DefaultGit implements Git {

	private static final Logger log = LoggerFactory.getLogger(DefaultGit.class);

	private Repository repo;

	/**
	 * Create a new instance of GitImpl
	 * 
	 * @param repo
	 *            repository
	 */
	public DefaultGit(Repository repo) {
		this.repo = repo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#commit(org.git4j.core.Workspace,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	public String commit(Workspace workspace, String branch, String author,
			String msg) throws IOException {

		if (StringUtils.isEmpty(msg)) {
			msg = "NO COMMIT MESSAGE";
		} else {
			msg = msg.trim();
		}

		Commit commit = new Commit();
		commit.setAuthor(author);
		commit.setMessage(msg);

		Map<String, String> index = commit.index();

		Commit head = repo.getLocalHead(branch);

		if (head != null) {
			if (!head.getId().equals(workspace.getCommitId())) {
				throw new GitException("workspace (" + workspace.getCommitId()
						+ ") is not checked out for HEAD (" + head.getId()
						+ ")");
			}

			commit.setParent(head.getId());
			index.putAll(head.index());
		}

		for (Map.Entry<String, Object> e : workspace.added().entrySet()) {
			Blob blob = new Blob();
			blob.setContent(e.getValue());

			index.put(e.getKey(), repo.store(blob));
		}

		for (Map.Entry<String, Object> e : workspace.modified().entrySet()) {
			Blob blob = new Blob();
			blob.setContent(e.getValue());

			index.put(e.getKey(), repo.store(blob));
		}

		for (String name : workspace.removed()) {
			index.remove(name);
		}

		// store COMMIT
		String commitId = repo.store(commit);

		// set branch HEAD
		repo.setLocalHeadRef(branch, head == null ? null : head.getId(),
				commitId);

		workspace.update(commit);
		return commitId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#fetch(org.git4j.core.GitTransport,
	 * java.lang.String[])
	 */
	public void fetch(Transport transport, String... remoteBranches)
			throws GitException, IOException {

		List<BranchAndHead> bnhs = new ArrayList<BranchAndHead>();

		if ((remoteBranches == null) || (remoteBranches.length == 0)) {
			bnhs.addAll(repo.getRemoteBranches());
		}

		Map<String, String> remoteHeadRefs = new HashMap<String, String>();

		for (int i = 0, len = remoteBranches.length; i < len; ++i) {
			String remoteBranch = remoteBranches[i];
			String headRef = repo.getRemoteHeadRef(remoteBranch);

			bnhs.add(new BranchAndHead(remoteBranch, headRef));
			remoteHeadRefs.put(remoteBranch, headRef);
		}

		// case: no remote branches
		if (bnhs.isEmpty()) {
			return;
		}

		// fetch!
		UploadPack[] packs = transport
				.fetch(bnhs.toArray(new BranchAndHead[0]));

		if (log.isTraceEnabled()) {
			log.trace("fetching remote branches " + bnhs + ": " + packs.length
					+ " will be fetched");
		}

		for (int i = 0, len = packs.length; i < len; ++i) {
			UploadPack pack = packs[i];

			String branch = pack.getBranch();
			String newHeadRef = pack.getHeadRef();

			// store COMMITs and BLOBs
			repo.store(pack);

			// set new HEAD
			repo.setRemoteHeadRef(pack.getBranch(), remoteHeadRefs.get(branch),
					newHeadRef);

			if (log.isTraceEnabled()) {
				if (log.isTraceEnabled()) {
					log.trace("fetch remote branch " + branch + " "
							+ newHeadRef + ": OK");
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#merge(java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String, org.git4j.core.ConflictResolution)
	 */
	public Set<String> merge(String commitId, String branch, String author,
			String msg, ConflictResolution resolution) throws GitException,
			IOException {

		switch (resolution) {
		case LEAVE:
		case USE_BRANCH:
		case USE_COMMIT:
			break;
		default:
			throw new IllegalArgumentException(
					"resolution must be LEAVE or USE_BRANCH or USE_COMMIT");
		}

		Commit commit = repo.find(Commit.class, commitId);
		if (commit == null) {
			throw new GitException("commit " + commitId + " cannot be found");
		}

		String branchHeadRef = repo.getLocalHeadRef(branch);

		// case: empty branch
		if (branchHeadRef == null) {
			// set new HEAD
			repo.setLocalHeadRef(branch, null, commitId);

			// no conflicts
			return Collections.emptySet();
		}

		// case: fast-forward
		if (ObjectUtils.canFastForward(repo, commitId, branchHeadRef)) {
			// set new HEAD
			repo.setLocalHeadRef(branch, branchHeadRef, commitId);

			// no conflicts
			return Collections.emptySet();
		}

		// auto-merge
		ObjectUtils.AutoMergeResult result = ObjectUtils.autoMerge(commit,
				repo.find(Commit.class, branchHeadRef), resolution);

		if (result.conflicts().isEmpty()
				|| !ConflictResolution.LEAVE.equals(resolution)) {

			// find pre intersection A between commitId and branchHeadRef in
			// path branch
			String idA = ObjectUtils.findPreIntersection(repo, branchHeadRef,
					commitId);

			// construct new commit path, commit Id -> A -> branch HEAD
			Commit reparentA = repo.find(Commit.class, idA);
			reparentA.setParent(commitId);

			Commit merged = new Commit();
			merged.setAuthor(author);
			merged.setParent2(commitId);
			merged.setMessage(msg);

			merged.index().putAll(result.index());
			merged.setParent(branchHeadRef);

			// store COMMITs
			repo.store(merged);
			repo.store(reparentA);

			// set new HEAD
			repo.setLocalHeadRef(branch, branchHeadRef, merged.getId());
		}

		return result.conflicts();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#mergeBranch(java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String, org.git4j.core.ConflictResolution)
	 */
	public Set<String> mergeBranch(String remoteBranch, String localBranch,
			String author, String msg, ConflictResolution resolution)
			throws GitException, IOException {

		switch (resolution) {
		case USE_LOCAL_BRANCH:
			resolution = ConflictResolution.USE_BRANCH;
			break;
		case USE_REMOTE_BRANCH:
			resolution = ConflictResolution.USE_COMMIT;
			break;
		case LEAVE:
			break;
		default:
			throw new IllegalArgumentException(
					"resolution must be USE_LOCAL_BRANCH or USE_REMOTE_BRANCH or LEAVE_");
		}

		String remoteHeadRef = repo.getRemoteHeadRef(remoteBranch);
		if (remoteHeadRef == null) {
			throw new GitException("remote branch " + remoteBranch
					+ " cannot be found or empty");
		}

		return merge(remoteHeadRef, localBranch, author, msg, resolution);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#push(org.git4j.core.GitTransport,
	 * java.lang.String, java.lang.String)
	 */
	public void push(Transport transport, String commitId, String remoteBranch)
			throws GitException, IOException {

		Map<String, Blob> blobs = new HashMap<String, Blob>();
		Map<String, Commit> commits = new HashMap<String, Commit>();

		String remoteHeadRef = repo.getRemoteHeadRef(remoteBranch);

		// collect objects from commitId to remote branch HEAD
		if (!ObjectUtils.collectObjects(repo, commitId, remoteHeadRef, commits,
				blobs)) {
			if (log.isTraceEnabled()) {
				log.trace("push " + commitId + " into remote branch "
						+ remoteBranch + " " + remoteHeadRef
						+ ": REJECTED, non fast-forward push");
			}

			throw new GitException("non fast-forward push");
		}

		// case: nothing to commit
		if (commits.isEmpty()) {
			if (log.isTraceEnabled()) {
				log.trace("push " + commitId + " into remote branch "
						+ remoteBranch + " " + remoteHeadRef
						+ ": REJECTED, nothing to commit");
			}

			return;
		}

		// prepare upload pack
		UploadPack pack = new UploadPack();
		pack.setBranch(remoteBranch);
		pack.setHeadRef(commitId);
		pack.setCommits(commits);
		pack.setBlobs(blobs);

		// push!
		transport.push(pack);

		// set new HEAD
		repo.setRemoteHeadRef(remoteBranch, remoteHeadRef, commitId);

		if (log.isTraceEnabled()) {
			log.trace("push " + commitId + " into remote branch "
					+ remoteBranch + " " + remoteHeadRef + ": OK");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#getLocalBranches()
	 */
	public Collection<BranchAndHead> getLocalBranches() throws IOException {
		return repo.getLocalBranches();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#getRemoteBranches()
	 */
	public Collection<BranchAndHead> getRemoteBranches() throws IOException {
		return repo.getRemoteBranches();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#removeLocalBranch(java.lang.String)
	 */
	public void removeLocalBranch(String branch) throws IOException {
		repo.removeLocalBranch(branch);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#removeRemoteBranch(java.lang.String)
	 */
	public void removeRemoteBranch(String branch) throws IOException {
		repo.removeRemoteBranch(branch);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#status(org.git4j.core.Workspace,
	 * java.lang.String)
	 */
	public Status status(Workspace workspace, String branch)
			throws GitException, IOException {
		Set<String> added = new HashSet<String>();
		Set<String> modified = new HashSet<String>();
		Set<String> removed = new HashSet<String>();

		// <name, content>
		Map<String, Object> wadded = workspace.added();

		// <name, content>
		Map<String, Object> wmodified = workspace.modified();

		// <name>
		Set<String> wremoved = workspace.removed();

		Commit head = repo.getLocalHead(branch);

		if (head == null) {
			added.addAll(wadded.keySet());
		} else {
			Map<String, String> index = head.index();

			// recheck removed
			for (String name : wremoved) {
				if (index.containsKey(name)) {
					removed.add(name);
				}
			}

			// recheck added
			for (Map.Entry<String, Object> e : wadded.entrySet()) {
				String name = e.getKey();
				String wid = new Blob(e.getValue()).getId();

				String id = index.get(name);

				// case: not in branch
				if (id == null) {
					added.add(name);
				} else
				// case: content modified
				if (!wid.equals(id)) {
					modified.add(name);
				}
			}

			// recheck modified
			for (Map.Entry<String, Object> e : wmodified.entrySet()) {
				String name = e.getKey();
				String wid = new Blob(e.getValue()).getId();

				String id = index.get(name);

				// case: not in branch
				if (id == null) {
					added.add(name);
				} else
				// case: content modified
				if (!wid.equals(id)) {
					modified.add(name);
				}
			}
		}

		Status status = new Status();
		status.setAdded(added);
		status.setModified(modified);
		status.setRemoved(removed);

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#checkout(org.git4j.core.Workspace,
	 * java.lang.String)
	 */
	public void checkout(Workspace workspace, String commitId)
			throws GitException, IOException {

		Commit commit = repo.find(Commit.class, commitId);
		if (commit == null) {
			throw new GitException("commit " + commitId + " cannot be found");
		}

		Map<String, Object> cobjects = new HashMap<String, Object>();

		for (Map.Entry<String, String> e : commit.index().entrySet()) {
			Blob blob = repo.find(Blob.class, e.getValue());
			if (blob == null) {
				throw new GitException("blob " + e.getValue()
						+ " cannot be found");
			}

			cobjects.put(e.getKey(), blob.getContent());
		}

		workspace.update(commit, cobjects);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#checkoutLocalBranchHead(org.git4j.core.Workspace,
	 * java.lang.String)
	 */
	public void checkoutLocalBranchHead(Workspace workspace, String branch)
			throws GitException, IOException {

		String headRef = repo.getLocalHeadRef(branch);
		if (headRef == null) {
			throw new GitException("local branch " + branch
					+ " cannot be found or is empty");
		}

		checkout(workspace, headRef);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.git4j.core.Git#checkoutRemoteBranchHead(org.git4j.core.Workspace,
	 * java.lang.String)
	 */
	public void checkoutRemoteBranchHead(Workspace workspace, String branch)
			throws GitException, IOException {

		String headRef = repo.getRemoteHeadRef(branch);
		if (headRef == null) {
			throw new GitException("remote branch " + branch
					+ " cannot be found or is empty");
		}

		checkout(workspace, headRef);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#getRemoteBranchHeadRef(java.lang.String)
	 */
	public String getRemoteBranchHeadRef(String branch) throws GitException,
			IOException {

		String headRef = repo.getRemoteHeadRef(branch);
		if (headRef == null) {
			throw new GitException("remote branch " + branch
					+ " cannot be found or is empty");
		}

		return headRef;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#getLocalBranchHeadRef(java.lang.String)
	 */
	public String getLocalBranchHeadRef(String branch) throws GitException,
			IOException {

		String headRef = repo.getLocalHeadRef(branch);
		if (headRef == null) {
			throw new GitException("local branch " + branch
					+ " cannot be found or is empty");
		}

		return headRef;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.Git#dumpLog(java.lang.String, java.io.PrintStream)
	 */
	public void dumpLog(String branch, PrintStream out) throws GitException,
			IOException {

		String currentId = repo.getLocalHeadRef(branch);
		if (currentId == null) {
			throw new GitException("local branch " + branch
					+ " cannot be found or is empty");
		}

		List<String> lines = new ArrayList<String>();

		// reverse walk!
		while (currentId != null) {
			Commit commit = repo.find(Commit.class, currentId);
			if (commit == null) {
				throw new GitException("unable to find commit " + currentId);
			}

			lines.add(commit.toString());

			currentId = commit.getParent();
		}

		for (int i = lines.size() - 1; i >= 0; --i) {
			out.print(lines.get(i));

			if (i > 0) {
				out.println("--------");
			}
		}

		lines.clear();
	}
}
