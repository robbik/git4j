package org.git4j.core.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.git4j.core.GitException;
import org.git4j.core.objs.Blob;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.GitObject;
import org.git4j.core.objs.UploadPack;
import org.git4j.core.util.ObjectUtils;

public class InMemoryRepository implements Repository {

	// <id, content>
	private Map<String, Object> objects;

	// <name, head-id>
	private Map<String, String> heads;

	// <name, head-id>
	private Map<String, String> remotes;

	public InMemoryRepository() {
		objects = Collections.synchronizedMap(new HashMap<String, Object>());

		heads = Collections.synchronizedMap(new HashMap<String, String>());
		remotes = Collections.synchronizedMap(new HashMap<String, String>());
	}

	public void store(Blob blob) throws IOException {
		objects.put(blob.getId(), blob);
	}

	public void store(Commit commit) throws IOException {
		objects.put(commit.getId(), commit);
	}

	public void store(UploadPack pack) throws IOException {
		// store BLOBs
		for (Blob blob : pack.getBlobs().values()) {
			store(blob);
		}

		// store COMMITs
		for (Commit commit : pack.getCommits().values()) {
			store(commit);
		}
	}

	public <T extends GitObject> T load(String id, Class<T> type)
			throws IOException {
		Object obj = objects.get(id);
		if (obj == null) {
			return null;
		}

		return type.cast(obj);
	}

	public Commit getLocalHead(String branch) throws IOException {
		String headRef = heads.get(branch);
		if (headRef == null) {
			return null;
		}

		return load(headRef, Commit.class);
	}

	public String getLocalHeadRef(String branch) throws IOException {
		return heads.get(branch);
	}

	public void setLocalHeadRef(String branch, String headRef, String newHeadRef)
			throws GitException, IOException {
		synchronized (heads) {
			if (!ObjectUtils.equals(heads.get(branch), headRef)) {
				throw new GitException();
			}

			heads.put(branch, newHeadRef);
		}
	}

	public String getRemoteHeadRef(String branch) throws IOException {
		return remotes.get(branch);
	}

	public void setRemoteHeadRef(String branch, String headRef,
			String newHeadRef) throws GitException, IOException {
		synchronized (remotes) {
			if (!ObjectUtils.equals(remotes.get(branch), headRef)) {
				throw new GitException();
			}

			remotes.put(branch, newHeadRef);
		}
	}

	public Collection<BranchAndHead> getLocalBranches() throws IOException {
		List<BranchAndHead> list = new ArrayList<BranchAndHead>();

		synchronized (heads) {
			for (Map.Entry<String, String> e : heads.entrySet()) {
				list.add(new BranchAndHead(e.getKey(), e.getValue()));
			}
		}

		return list;
	}

	public Collection<BranchAndHead> getRemoteBranches() throws IOException {
		List<BranchAndHead> list = new ArrayList<BranchAndHead>();

		synchronized (remotes) {
			for (Map.Entry<String, String> e : remotes.entrySet()) {
				list.add(new BranchAndHead(e.getKey(), e.getValue()));
			}
		}

		return list;
	}

	public void removeLocalBranch(String branch) throws IOException {
		heads.remove(branch);
	}

	public void removeRemoteBranch(String branch) throws IOException {
		remotes.remove(branch);
	}

	public void wipe() throws IOException {
		remotes.clear();
		heads.clear();
		objects.clear();
	}
}
