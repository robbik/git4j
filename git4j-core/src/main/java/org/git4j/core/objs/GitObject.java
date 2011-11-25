package org.git4j.core.objs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.git4j.core.util.ObjectUtils;

public abstract class GitObject implements Serializable {

	private static final long serialVersionUID = 5709035767104159032L;

	protected transient String id;

	public GitObject(String id) {
		this.id = id;
	}

	public GitObject() {
		// do nothing
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (o == this) {
			return true;
		}

		if (o instanceof GitObject) {
			GitObject another = (GitObject) o;
			return ObjectUtils.equals(id, another.id);
		}

		if (o instanceof String) {
			return ObjectUtils.equals(id, o);
		}

		return false;
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		id = in.readUTF();
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeUTF(id);
	}
}
