package org.git4j.core.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.git4j.core.GitException;
import org.git4j.core.objs.Blob;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.UploadPack;
import org.git4j.core.repo.Repository;
import org.git4j.core.util.ObjectUtils;

public class DirectTransport implements Transport {

	private Repository repo;

	public DirectTransport(Repository repo) {
		this.repo = repo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitTransport#fetch(org.git4j.core.BranchAndHeadRef[])
	 */
	public UploadPack[] fetch(BranchAndHead... branches) throws GitException,
			IOException {

		if ((branches == null) || (branches.length == 0)) {
			return new UploadPack[0];
		}

		List<UploadPack> packs = new ArrayList<UploadPack>(branches.length);

		for (int i = 0, len = branches.length; i < len; ++i) {
			String branch = branches[i].getBranch();
			String advHeadRef = branches[i].getHeadRef();

			String headRef = repo.getLocalHeadRef(branch);

			// case: empty branch
			if (headRef == null) {
				continue;
			}

			Map<String, Blob> blobs = new HashMap<String, Blob>();
			Map<String, Commit> commits = new HashMap<String, Commit>();

			// case: non fast-forward
			if (!ObjectUtils.collectObjects(repo, headRef, advHeadRef, commits,
					blobs)) {
				continue;
			}

			// construct upload pack
			UploadPack pack = new UploadPack();
			pack.setBranch(branch);
			pack.setHeadRef(headRef);
			pack.setCommits(commits);
			pack.setBlobs(blobs);

			// add to fetched list
			packs.add(pack);
		}

		return packs.toArray(new UploadPack[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitTransport#push(org.git4j.core.GitUploadPack)
	 */
	public void push(UploadPack pack) throws GitException, IOException {
		String branch = pack.getBranch();

		String advHeadRef = pack.getHeadRef();
		String headRef = repo.getLocalHeadRef(branch);

		if (!pack.getCommits().containsKey(advHeadRef)) {
			throw new GitException(
					"upload pack is corrupt, unable to find commit "
							+ advHeadRef);
		}

		// case: empty branch
		if (headRef == null) {
			// store BLOBs
			for (Blob blob : pack.getBlobs().values()) {
				repo.store(blob);
			}

			// store COMMITs
			for (Commit commit : pack.getCommits().values()) {
				repo.store(commit);
			}

			// set new HEAD
			repo.setLocalHeadRef(branch, null, advHeadRef);
			return;
		}

		// case: up-to-date
		if (headRef.equals(advHeadRef)) {
			return;
		}

		// reversal walk for fast-forward checking and collecting objects
		Map<String, Blob> blobs = new HashMap<String, Blob>();
		Map<String, Commit> commits = new HashMap<String, Commit>();

		// case: non fast-forward
		if (!ObjectUtils.collectObjects(pack, advHeadRef, headRef, commits,
				blobs)) {
			throw new GitException("non fast-forward push");
		}

		// store BLOBs
		for (Blob blob : blobs.values()) {
			repo.store(blob);
		}

		// store COMMITs
		for (Commit commit : commits.values()) {
			repo.store(commit);
		}

		// set new HEAD
		repo.setLocalHeadRef(branch, headRef, advHeadRef);
	}
}
