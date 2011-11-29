package org.git4j.core.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.git4j.core.ConflictResolution;
import org.git4j.core.GitException;
import org.git4j.core.objs.Blob;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.UploadPack;
import org.git4j.core.repo.Repository;

public abstract class ObjectUtils {

	public static boolean equals(Object a, Object b) {
		if (a == b) {
			return true;
		}

		if ((a == null) || (b == null)) {
			return false;
		}

		return a.equals(b);
	}

	public static void validateBlobContent(Object content) {
		if (content == null) {
			throw new NullPointerException("content");
		}

		if (!Serializable.class.isInstance(content)
				&& !byte[].class.isInstance(content)
				&& !String.class.isInstance(content)) {
			throw new ClassCastException(
					"content MUST be a serializable or byte[] or String object");
		}
	}

	/**
	 * do reverse walk for fast-forward checking
	 * 
	 * @param repo
	 *            the repository
	 * @param fromId
	 *            from object
	 * @param toId
	 *            to object
	 * @return <code>true</code> if fast-forward, <code>false</code> otherwise
	 * @throws GitException
	 *             if unable to find commit object
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public static boolean canFastForward(Repository repo, String fromId,
			String toId) throws GitException, IOException {
		boolean fastForward = false;
		String currentId = fromId;

		while ((currentId != null) && !(fastForward = currentId.equals(toId))) {
			Commit current = repo.find(Commit.class, currentId);
			if (current == null) {
				throw new GitException("unable to find commit " + currentId);
			}

			// walk
			currentId = current.getParent();
		}

		return (toId == null) || fastForward;
	}

	/**
	 * do reverse walk to collect objects
	 * 
	 * @param repo
	 *            the repository
	 * @param fromId
	 *            from object
	 * @param toId
	 *            to object
	 * @param commits
	 *            where collected commit should be stored, <code>null</code> if
	 *            don't want to collect COMMITs
	 * @param blobs
	 *            where collected blob should be stored, <code>null</code> if
	 *            don't want to collect BLOBs
	 * @return <code>true</code> if fast-forward, <code>false</code> otherwise
	 * @throws GitException
	 *             if unable to find commit or blob object
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public static boolean collectObjects(Repository repo, String fromId,
			String toId, Map<String, Commit> commits, Map<String, Blob> blobs)
			throws GitException, IOException {

		boolean collectCOMMITs = commits != null;
		boolean collectBLOBs = blobs != null;

		boolean fastForward = false;
		String currentId = fromId;

		while ((currentId != null) && !(fastForward = currentId.equals(toId))) {
			Commit current = repo.find(Commit.class, currentId);
			if (current == null) {
				throw new GitException("unable to find commit " + currentId);
			}

			// collect COMMIT
			if (collectCOMMITs) {
				commits.put(currentId, current);
			}

			// collect BLOBs
			if (collectBLOBs) {
				for (String oid : current.index().values()) {
					if (!blobs.containsKey(oid)) {
						Blob blob = repo.find(Blob.class, oid);

						if (blob == null) {
							throw new GitException("unable to find blob " + oid);
						}

						blobs.put(oid, blob);
					}
				}
			}

			// walk
			currentId = current.getParent();
		}

		return (toId == null) || fastForward;
	}

	/**
	 * do reverse walk to collect objects from upload pack
	 * 
	 * @param pack
	 *            the upload pack
	 * @param fromId
	 *            from object
	 * @param toId
	 *            to object
	 * @param commits
	 *            where collected commit should be stored, <code>null</code> if
	 *            don't want to collect COMMITs
	 * @param blobs
	 *            where collected blob should be stored, <code>null</code> if
	 *            don't want to collect BLOBs
	 * @return <code>true</code> if fast-forward, <code>false</code> otherwise
	 * @throws GitException
	 *             if unable to find commit or blob object
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public static boolean collectObjects(UploadPack pack, String fromId,
			String toId, Map<String, Commit> commits, Map<String, Blob> blobs)
			throws GitException, IOException {

		Map<String, Commit> packedCommits = pack.getCommits();
		Map<String, Blob> packedBlobs = pack.getBlobs();

		boolean collectCOMMITs = commits != null;
		boolean collectBLOBs = blobs != null;

		boolean fastForward = false;
		String currentId = fromId;

		while ((currentId != null) && !(fastForward = currentId.equals(toId))) {
			Commit current = packedCommits.get(currentId);
			if (current == null) {
				throw new GitException("unable to find commit " + currentId);
			}

			// collect COMMIT
			if (collectCOMMITs) {
				commits.put(currentId, current);
			}

			// collect BLOBs
			if (collectBLOBs) {
				for (String oid : current.index().values()) {
					if (!blobs.containsKey(oid)) {
						Blob blob = packedBlobs.get(oid);

						if (blob == null) {
							throw new GitException("unable to find blob " + oid);
						}

						blobs.put(oid, blob);
					}
				}
			}

			// walk
			currentId = current.getParent();
		}

		return (toId == null) || fastForward;
	}

	/**
	 * do auto-merge from <code>from</code> to <code>to</code> and return the
	 * new index
	 * 
	 * @param from
	 * @param to
	 * @param resolution
	 * @return
	 * @throws GitException
	 * @throws IOException
	 */
	public static AutoMergeResult autoMerge(Commit from, Commit to,
			ConflictResolution resolution) throws GitException, IOException {

		// find diff
		Set<String> conflicts = new HashSet<String>();

		Map<String, String> fromIndex = from.index();
		Map<String, String> toIndex = to.index();

		Map<String, String> mergedIndex = new HashMap<String, String>();

		for (Map.Entry<String, String> e : fromIndex.entrySet()) {
			String fromObjectName = e.getKey();
			String fromObjectId = e.getValue();

			String toObjectId = toIndex.get(fromObjectName);

			// "from" add new file
			if (toObjectId == null) {
				mergedIndex.put(fromObjectName, fromObjectId);
			} else
			// file remains same
			if (fromObjectId.equals(toObjectId)) {
				mergedIndex.put(fromObjectName, fromObjectId);
			} else
			// CONFLICT, same name different content
			{
				conflicts.add(fromObjectName);

				switch (resolution) {
				case LEAVE:
					// do nothing for merged index
					break;
				case USE_COMMIT:
					mergedIndex.put(fromObjectName, fromObjectId);
					break;
				case USE_BRANCH:
					mergedIndex.put(fromObjectName, toObjectId);
					break;
				}
			}
		}

		for (Map.Entry<String, String> e : toIndex.entrySet()) {
			String toObjectName = e.getKey();
			String toObjectId = e.getValue();

			// "to" add new file
			if (!fromIndex.containsKey(toObjectName)) {
				mergedIndex.put(toObjectName, toObjectId);
			}
		}

		return new AutoMergeResult(mergedIndex, conflicts);
	}

	/**
	 * find intersection between commit A and B and return the previous point in
	 * A path
	 * 
	 * @param repo
	 *            repository
	 * @param idA
	 *            object id of commit A
	 * @param idB
	 *            object id of commit B
	 * @return previous point (commit object id) in A path or <code>null</code>
	 *         if idA and idB are equals
	 * @throws GitException
	 *             if unable to find commit object
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public static String findPreIntersection(Repository repo, String idA,
			String idB) throws GitException, IOException {

		Set<String> revpathA = new HashSet<String>();
		Set<String> revpathB = new HashSet<String>();

		// <id [can be null], previous id>
		Map<String, String> prevA = new HashMap<String, String>();

		while ((idA != null) || (idB != null)) {
			if ((idA != null) && (idB != null) && idA.equals(idB)) {
				break;
			}

			if (idA != null) {
				if (revpathB.contains(idA)) {
					break;
				}

				Commit pointA = repo.find(Commit.class, idA);
				if (pointA == null) {
					throw new GitException("unable to find commit " + idA);
				}

				revpathA.add(idA);

				prevA.put(pointA.getParent(), idA);
				idA = pointA.getParent();
			}

			if (idB != null) {
				if (revpathA.contains(idB)) {
					break;
				}

				Commit pointB = repo.find(Commit.class, idB);
				if (pointB == null) {
					throw new GitException("unable to find commit " + idB);
				}

				revpathB.add(idB);
				idB = pointB.getParent();
			}
		}

		// case: no intersection
		if ((idA == null) && (idB == null)) {
			return prevA.get(null);
		}

		// case: intersection found, same level
		if ((idA != null) && idA.equals(idB)) {
			return prevA.get(idA);
		}

		// case: intersection found, A shorter than B
		if ((idB != null) && revpathA.contains(idB)) {
			return prevA.get(idB);
		}

		// case: intersection found, B shorter than A
		if ((idA != null) && revpathB.contains(idA)) {
			return prevA.get(idA);
		}

		// no other case found
		return null;
	}

	public static class AutoMergeResult {
		private Map<String, String> index;

		private Set<String> conflicts;

		private AutoMergeResult(Map<String, String> index, Set<String> conflicts) {
			this.index = index;
			this.conflicts = conflicts;
		}

		public Map<String, String> index() {
			return index;
		}

		public Set<String> conflicts() {
			return conflicts;
		}
	}
}
