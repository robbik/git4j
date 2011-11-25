package org.git4j.core;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Set;

import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.Status;
import org.git4j.core.transport.Transport;

public interface Git {

	/**
	 * Commit workspace to local branch and clear the stage.
	 * 
	 * @param workspace
	 *            workspace
	 * @param branch
	 *            local branch
	 * @param author
	 *            commit author
	 * @param msg
	 *            commit message
	 * @return commit object id or <code>null</code> if nothing was committed
	 * @throws IOException
	 *             if an IO error occurred
	 */
	String commit(Workspace workspace, String branch, String author, String msg)
			throws IOException;

	/**
	 * Fetch objects from remote using a transporter for selected branches
	 * 
	 * @param transport
	 *            the transporter
	 * @param remoteBranches
	 *            the selected branches. If not specified, all remote branches
	 *            will be fetched
	 * @throws GitException
	 *             if a Git error occurred (i.e. non fast-forward)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void fetch(Transport transport, String... remoteBranches)
			throws GitException, IOException;

	/**
	 * Merge a commit into selected local branch. The selected local branch will
	 * be updated.
	 * 
	 * @param commitId
	 *            the commit object id will be merged
	 * @param branch
	 *            the selected local branch where the merge will be merged into
	 * @param author
	 *            merge author
	 * @param msg
	 *            merge message
	 * @param resolution
	 *            conflict resolution
	 * @return conflict objects
	 * @throws GitException
	 *             if a git error occurred
	 * @throws IOException
	 *             if an IO error occurred
	 */
	Set<String> merge(String commitId, String branch, String author,
			String msg, ConflictResolution resolution) throws GitException,
			IOException;

	/**
	 * Merge remote branch into selected local branch. The selected local branch
	 * will be updated.
	 * 
	 * @param remoteBranch
	 *            the remote branch
	 * @param localBranch
	 *            the selected local branch where the merge will be merged into
	 * @param author
	 *            merge author
	 * @param msg
	 *            merge message
	 * @param resolution
	 *            conflict resolution
	 * @return conflict objects
	 * @throws GitException
	 *             if a git error occurred
	 * @throws IOException
	 *             if an IO error occurred
	 */
	Set<String> mergeBranch(String remoteBranch, String localBranch,
			String author, String msg, ConflictResolution resolution)
			throws GitException, IOException;

	/**
	 * Push a commit to remote using a transporter.
	 * 
	 * @param transport
	 *            the transporter
	 * @param commitId
	 *            the commit object id
	 * @param remoteBranch
	 *            remote branch
	 * @throws GitException
	 *             if a Git error occurred (i.e. non fast-forward)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void push(Transport transport, String commitId, String remoteBranch)
			throws GitException, IOException;

	/**
	 * retrieve local branches.
	 * 
	 * @return local branches and its head ref
	 * @throws IOException
	 *             if an IO error occurred
	 */
	Collection<BranchAndHead> getLocalBranches() throws IOException;

	/**
	 * retrieve remote branches.
	 * 
	 * @return remote branches and its head ref
	 * @throws IOException
	 *             if an IO error occurred
	 */
	Collection<BranchAndHead> getRemoteBranches() throws IOException;

	/**
	 * remove local branch
	 * 
	 * @param branch
	 *            the local branch
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void removeLocalBranch(String branch) throws IOException;

	/**
	 * remove remote branch
	 * 
	 * @param branch
	 *            the remote branch
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void removeRemoteBranch(String branch) throws IOException;

	/**
	 * List workspace changes relative to local branch
	 * 
	 * @param workspace
	 *            workspace
	 * @param branch
	 *            local branch name
	 * @return changes
	 * @throws GitException
	 *             if a Git error occurred (i.e. branch not found)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	Status status(Workspace workspace, String branch) throws GitException,
			IOException;

	/**
	 * checkout a commit into a workspace
	 * 
	 * @param workspace
	 *            workspace
	 * @param commitId
	 *            the commit object id
	 * @throws GitException
	 *             if a Git error occurred (i.e. branch not found)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void checkout(Workspace workspace, String commitId) throws GitException,
			IOException;

	/**
	 * checkout local branch head into a workspace
	 * 
	 * @param workspace
	 *            workspace
	 * @param branch
	 *            local branch name
	 * @throws GitException
	 *             if a Git error occurred (i.e. branch not found)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void checkoutLocalBranchHead(Workspace workspace, String branch)
			throws GitException, IOException;

	/**
	 * checkout remote branch head into a workspace
	 * 
	 * @param workspace
	 *            workspace
	 * @param branch
	 *            remote branch name
	 * @throws GitException
	 *             if a Git error occurred (i.e. branch not found)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void checkoutRemoteBranchHead(Workspace workspace, String branch)
			throws GitException, IOException;

	/**
	 * retrieve remote branch head
	 * 
	 * @param branch
	 *            remote branch name
	 * @return head ref
	 * @throws GitException
	 *             if a Git error occurred (i.e. branch not found)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	String getRemoteBranchHeadRef(String branch) throws GitException,
			IOException;

	/**
	 * retrieve local branch head
	 * 
	 * @param branch
	 *            local branch name
	 * @return head ref
	 * @throws GitException
	 *             if a Git error occurred (i.e. branch not found)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	String getLocalBranchHeadRef(String branch) throws GitException,
			IOException;

	/**
	 * dump local branch log into a stream
	 * 
	 * @param branch
	 *            local branch
	 * @param out
	 *            the stream
	 * @throws GitException
	 *             if a Git error occurred (i.e. branch not found)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void dumpLog(String branch, PrintStream out) throws GitException,
			IOException;
}
