package org.git4j.core.objs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class BranchAndHead implements Serializable {
	private static final long serialVersionUID = 5280052782018048240L;

	private String branch;

	private String headRef;

	public BranchAndHead() {
		// do nothing
	}

	public BranchAndHead(String branch, String headRef) {
		this.branch = branch;
		this.headRef = headRef;
	}

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

	@Override
	public String toString() {
		return branch + " " + headRef;
	}

	public BranchAndHead read(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		readObject(in);
		return this;
	}

	public void write(ObjectOutputStream out) throws IOException {
		writeObject(out);
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		branch = in.readUTF();

		headRef = in.readUTF();
		if (headRef.length() == 0) {
			headRef = null;
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeUTF(branch);
		out.writeUTF(headRef == null ? "" : headRef);
	}
}
