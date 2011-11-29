package org.git4j.core.repo;

import java.io.IOException;
import java.util.Collection;

import org.git4j.core.GitException;
import org.git4j.core.objs.Blob;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.UploadPack;

public interface Repository {

	/**
	 * store blob object into this repository.
	 * 
	 * @param blob
	 *            the blob object
	 * @return object id
	 * @throws IOException
	 *             if an IO error occurred
	 */
	String store(Blob blob) throws IOException;

	/**
	 * store commit object into this repository.
	 * 
	 * @param commit
	 *            the commit object
	 * @return object id
	 * @throws IOException
	 *             if an IO error occurred
	 */
	String store(Commit commit) throws IOException;

	/**
	 * store packed object (COMMITs and/or BLOBs) into this repository.
	 * 
	 * @param pack
	 *            packed object
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void store(UploadPack pack) throws IOException;

	/**
	 * Load git object by it's id.
	 * 
	 * @param type
	 *            git object type
	 * @param id
	 *            git object id
	 * @return git object, <code>null</code> if not found
	 * @throws IOException
	 *             if an IO error occurred
	 */
	<T> T find(Class<T> type, String id) throws IOException;

	/**
	 * Get head for a local branch.
	 * 
	 * @param branch
	 *            the local branch
	 * @return commit, <code>null</code> if not found
	 */
	Commit getLocalHead(String branch) throws IOException;

	/**
	 * Get head for a local branch (ref only).
	 * 
	 * @param branch
	 *            the local branch
	 * @return commit object id, <code>null</code> if not found
	 * @throws IOException
	 *             if an IO error occurred
	 */
	String getLocalHeadRef(String branch) throws IOException;

	/**
	 * Set head for a local branch. If the branch doesn't exist, the new branch
	 * will be created using name <code>branch</code>.
	 * 
	 * @param branch
	 *            the local branch
	 * @param headRef
	 *            current HEAD, used for integrity checking. can be
	 *            <code>null</code>
	 * @param newHeadRef
	 *            new HEAD
	 * @throws GitException
	 *             if current branch head is not equals to headRef
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void setLocalHeadRef(String branch, String headRef, String newHeadRef)
			throws GitException, IOException;

	/**
	 * Get head for a remote branch (object id only).
	 * 
	 * @param branch
	 *            the remote branch
	 * @return commit id, <code>null</code> if not found
	 * @throws IOException
	 *             if an IO error occurred
	 */
	String getRemoteHeadRef(String branch) throws IOException;

	/**
	 * Set head for a remote branch. If the branch doesn't exist, the new branch
	 * will be created using name <code>branch</code>.
	 * 
	 * @param branch
	 *            the remote branch
	 * @param headRef
	 *            current HEAD, used for integrity checking. can be
	 *            <code>null</code>
	 * @param newHeadRef
	 *            new HEAD
	 * @throws GitException
	 *             if current branch head is not equals to headRef
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void setRemoteHeadRef(String branch, String headRef, String newHeadRef)
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
	 * wipe repository
	 * 
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void wipe() throws IOException;
}
