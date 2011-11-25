package org.git4j.core.objs;

import java.io.Serializable;
import java.util.Collection;

public class Status implements Serializable {

	private static final long serialVersionUID = 8149366643701366425L;

	// <Name>
	private Collection<String> modified;

	// <Name>
	private Collection<String> added;

	// <Name>
	private Collection<String> removed;

	public Collection<String> getModified() {
		return modified;
	}

	public void setModified(Collection<String> modified) {
		this.modified = modified;
	}

	public Collection<String> getAdded() {
		return added;
	}

	public void setAdded(Collection<String> added) {
		this.added = added;
	}

	public Collection<String> getRemoved() {
		return removed;
	}

	public void setRemoved(Collection<String> removed) {
		this.removed = removed;
	}
}
