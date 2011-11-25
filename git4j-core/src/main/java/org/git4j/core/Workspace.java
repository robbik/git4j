package org.git4j.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.git4j.core.gen.ObjectIdGenerator;
import org.git4j.core.gen.SHA256Generator;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.Status;
import org.git4j.core.util.ObjectUtils;

public class Workspace {

	private String cid;

	private Map<String, String> cindex;

	private Map<String, Object> cobjects;

	private ObjectIdGenerator idgen;

	// changes (add) <name, content>
	private Map<String, Object> added;

	// changes (modify) <name, content>
	private Map<String, Object> modified;

	// changes (removed) <name>
	private Set<String> removed;

	public Workspace(Commit commit, Map<String, Object> cobjects,
			ObjectIdGenerator idgen) {
		if (commit == null) {
			cid = null;
			cindex = Collections.emptyMap();
		} else {
			cid = commit.getId();
			cindex = commit.index();
		}

		this.cobjects = cobjects == null ? new HashMap<String, Object>()
				: cobjects;
		this.idgen = idgen;

		added = Collections.synchronizedMap(new HashMap<String, Object>());
		modified = Collections.synchronizedMap(new HashMap<String, Object>());
		removed = Collections.synchronizedSet(new HashSet<String>());
	}

	public Workspace(Commit commit, Map<String, Object> cobjects) {
		this(commit, cobjects, new SHA256Generator());
	}

	public Workspace() {
		this(null, null, new SHA256Generator());
	}

	public synchronized String getCommitId() {
		return cid;
	}

	public synchronized Workspace update(Commit commit,
			Map<String, Object> cobjects) {
		if (commit == null) {
			cid = null;
			cindex = Collections.emptyMap();
		} else {
			cid = commit.getId();
			cindex = commit.index();
		}

		this.cobjects = cobjects == null ? new HashMap<String, Object>()
				: cobjects;

		added.clear();
		modified.clear();
		removed.clear();

		return this;
	}

	public synchronized Workspace update(Commit commit) {
		if (commit == null) {
			throw new NullPointerException("commit");
		}

		cid = commit.getId();
		cindex = commit.index();

		cobjects.putAll(added);
		cobjects.putAll(modified);

		for (String name : removed) {
			cobjects.remove(name);
		}

		added.clear();
		modified.clear();
		removed.clear();

		return this;
	}

	public Map<String, Object> added() {
		return added;
	}

	public Map<String, Object> modified() {
		return modified;
	}

	public Set<String> removed() {
		return removed;
	}

	/**
	 * Add object to workspace.
	 * 
	 * @param name
	 *            object name
	 * @param content
	 *            object content, MUST be {@link java.io.Serializable} or
	 *            <code>byte[]</code> or <code>char[]</code> or {@link String}
	 * @throws NullPointerException
	 *             if content is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if content is NOT {@link java.io.Serializable} nor
	 *             <code>byte[]</code> nor <code>char[]</code> nor
	 *             {@link String}
	 */
	public synchronized Workspace add(String name, Object content) {
		ObjectUtils.validateBlobContent(content);

		String id = idgen.generate(content);
		String ccontentId = cindex.get(name);

		// case: new name
		if (ccontentId == null) {
			added.put(name, content);
		} else
		// case: same name, different content
		if (!id.equals(ccontentId)) {
			modified.put(name, content);
		}

		removed.remove(name);

		return this;
	}

	/**
	 * retrieve object content
	 * 
	 * @param name
	 *            object name
	 * @return object content or <code>null</code> if not exists
	 */
	public synchronized Object get(String name) {
		if (removed.contains(name)) {
			return null;
		}

		if (added.containsKey(name)) {
			return added.get(name);
		}

		if (modified.containsKey(name)) {
			return modified.get(name);
		}

		return cobjects.get(name);
	}

	/**
	 * Remove object from workspace.
	 * 
	 * @param name
	 *            object name
	 */
	public synchronized Workspace remove(String name) {
		// case: added object
		if (added.remove(name) != null) {
			removed.remove(name);
		} else
		// case: modified object
		if (modified.remove(name) != null) {
			removed.add(name);
		} else
		// case: committed object
		if (cindex.containsKey(name)) {
			removed.add(name);
		}

		return this;
	}

	public synchronized Collection<String> list() {
		HashSet<String> s = new HashSet<String>();

		s.addAll(cobjects.keySet());
		s.addAll(added.keySet());
		s.addAll(modified.keySet());
		s.removeAll(removed);

		return s;
	}

	/**
	 * clear changes.
	 */
	public synchronized Workspace stash() {
		added.clear();
		modified.clear();
		removed.clear();

		return this;
	}

	/**
	 * reset
	 */
	public synchronized Workspace reset() {
		cid = null;
		cindex = Collections.emptyMap();
		cobjects = new HashMap<String, Object>();

		added.clear();
		modified.clear();
		removed.clear();

		return this;
	}

	public synchronized Status status() {
		Status status = new Status();
		status.setAdded(added.keySet());
		status.setModified(modified.keySet());
		status.setRemoved(removed);

		return status;
	}
}
