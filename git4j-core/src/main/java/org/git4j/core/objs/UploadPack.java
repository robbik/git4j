package org.git4j.core.objs;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;



public class UploadPack implements Externalizable {

	private static final long serialVersionUID = -2897044870639459633L;

	private String branch;

	private String headRef;

	private boolean canFastForward;

	private Map<String, Commit> commits;

	private Map<String, Blob> blobs;

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getHeadRef() {
		return headRef;
	}

	public void setHeadRef(String headRef) {
		this.headRef = headRef;
	}

	public boolean canFastForward() {
		return canFastForward;
	}

	public void canFastForward(boolean canFastForward) {
		this.canFastForward = canFastForward;
	}

	public Map<String, Commit> getCommits() {
		return commits;
	}

	public void setCommits(Map<String, Commit> commits) {
		this.commits = commits;
	}

	public Map<String, Blob> getBlobs() {
		return blobs;
	}

	public void setBlobs(Map<String, Blob> blobs) {
		this.blobs = blobs;
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {

		branch = in.readUTF();
		headRef = in.readUTF();

		canFastForward = in.readBoolean();

		int commitLen = in.readInt();

		commits = new HashMap<String, Commit>(commitLen);
		for (int i = 0; i < commitLen; ++i) {
			Commit commit = (Commit) in.readObject();
			commits.put(commit.getId(), commit);
		}

		int blobLen = in.readInt();

		blobs = new HashMap<String, Blob>(blobLen);
		for (int i = 0; i < blobLen; ++i) {
			Blob blob = (Blob) in.readObject();
			blobs.put(blob.getId(), blob);
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(branch);
		out.writeUTF(headRef);

		out.writeBoolean(canFastForward);

		out.writeInt(commits.size());
		for (Commit commit : commits.values()) {
			out.writeObject(commit);
		}

		out.writeInt(blobs.size());
		for (Blob blob : blobs.values()) {
			out.writeObject(blob);
		}
	}
}
