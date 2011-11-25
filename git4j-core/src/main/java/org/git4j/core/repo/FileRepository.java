package org.git4j.core.repo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.git4j.core.GitException;
import org.git4j.core.objs.Blob;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.GitObject;
import org.git4j.core.objs.UploadPack;

public class FileRepository implements Repository {

	private File refsHeads;

	private File refsRemotes;

	private File objects;

	public FileRepository(File base) {
		refsHeads = new File(base, "refs" + File.separator + "heads");
		if (!refsHeads.exists()) {
			if (!refsHeads.mkdirs()) {
				throw new SecurityException("unable to create directory "
						+ refsHeads.getAbsolutePath());
			}
		}

		refsRemotes = new File(base, "refs" + File.separator + "remotes");
		if (!refsRemotes.exists()) {
			if (!refsRemotes.mkdirs()) {
				throw new SecurityException("unable to create directory "
						+ refsRemotes.getAbsolutePath());
			}
		}

		objects = new File(base, "objects");
		if (!objects.exists()) {
			if (!objects.mkdirs()) {
				throw new SecurityException("unable to create directory "
						+ objects.getAbsolutePath());
			}
		}
	}

	private void write(Object o, File target) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(target)));

		try {
			if (o instanceof Commit) {
				oos.write(0);
				((Commit) o).serialize(oos);
			} else if (o instanceof Blob) {
				oos.write(1);
				((Blob) o).serialize(oos);
			}
		} finally {
			try {
				oos.close();
			} catch (Throwable t) {
				// do nothing
			}
		}
	}

	private <T> T read(File source, Class<T> type) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(
				new FileInputStream(source)));

		Object o;

		try {
			switch (ois.readByte()) {
			case 0:
				o = new Commit().deserialize(ois);
				break;
			case 1:
				o = new Blob().deserialize(ois);
				break;
			default:
				o = null;
				break;
			}
		} catch (ClassNotFoundException e) {
			throw (IOException) new IOException(
					"unable to read object from file " + source).initCause(e);
		} finally {
			try {
				ois.close();
			} catch (Throwable t) {
				// do nothing
			}
		}

		return type.cast(o);
	}

	private void rm(File dir) {
		File[] files = dir.listFiles();

		for (int i = 0, len = files.length; i < len; ++i) {
			File f = files[i];

			if (f.isDirectory()) {
				rm(f);
			}

			if (!f.delete()) {
				f.deleteOnExit();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#store(org.git4j.core.impl.Blob)
	 */
	public void store(Blob blob) throws IOException {
		File target = new File(objects, blob.getId());

		if (!target.canRead()) {
			write(blob, target);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#store(org.git4j.core.impl.Commit)
	 */
	public void store(Commit commit) throws IOException {
		File target = new File(objects, commit.getId());

		if (!target.canRead()) {
			write(commit, target);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#store(org.git4j.core.GitUploadPack)
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#load(java.lang.String, java.lang.Class)
	 */
	public <T extends GitObject> T load(String id, Class<T> type)
			throws IOException {
		if (id == null) {
			return null;
		}

		File source = new File(objects, id);
		if (!source.canRead()) {
			return null;
		}

		T o = read(source, type);
		o.setId(id);

		return o;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#getLocalHead(java.lang.String)
	 */
	public Commit getLocalHead(String branch) throws IOException {
		return load(getLocalHeadRef(branch), Commit.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#getLocalHeadRef(java.lang.String)
	 */
	public String getLocalHeadRef(String branch) throws IOException {
		File source = new File(refsHeads, branch);
		if (!source.canRead()) {
			return null;
		}

		BufferedReader br = new BufferedReader(new FileReader(source));
		String id;

		try {
			id = br.readLine();
		} finally {
			try {
				br.close();
			} catch (Throwable t) {
				// do nothing
			}
		}

		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#getRemoteHeadRef(java.lang.String)
	 */
	public String getRemoteHeadRef(String branch) throws IOException {
		File source = new File(refsRemotes, branch);
		if (!source.canRead()) {
			return null;
		}

		BufferedReader br = new BufferedReader(new FileReader(source));
		String id;

		try {
			id = br.readLine();
		} finally {
			try {
				br.close();
			} catch (Throwable t) {
				// do nothing
			}
		}

		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#setLocalHeadRef(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	public void setLocalHeadRef(String branch, String headRef, String newHeadRef)
			throws GitException, IOException {
		PrintStream ps = new PrintStream(new BufferedOutputStream(
				new FileOutputStream(new File(refsHeads, branch))));

		try {
			ps.println(newHeadRef);
		} finally {
			try {
				ps.close();
			} catch (Throwable t) {
				// do nothing
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#setRemoteHeadRef(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	public void setRemoteHeadRef(String branch, String headRef,
			String newHeadRef) throws IOException {
		PrintStream ps = new PrintStream(new BufferedOutputStream(
				new FileOutputStream(new File(refsRemotes, branch))));

		try {
			ps.println(newHeadRef);
		} finally {
			try {
				ps.close();
			} catch (Throwable t) {
				// do nothing
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#getLocalBranches()
	 */
	public Collection<BranchAndHead> getLocalBranches() throws IOException {
		List<BranchAndHead> bnhs = new ArrayList<BranchAndHead>();

		File[] files = refsHeads.listFiles();
		if (files != null) {
			for (int i = 0, len = files.length; i < len; ++i) {
				File f = files[i];

				if (f.canRead()) {
					String branch = f.getName();
					bnhs.add(new BranchAndHead(branch, getLocalHeadRef(branch)));
				}
			}
		}

		return bnhs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#getRemoteBranches()
	 */
	public Collection<BranchAndHead> getRemoteBranches() throws IOException {
		List<BranchAndHead> bnhs = new ArrayList<BranchAndHead>();

		File[] files = refsRemotes.listFiles();
		if (files != null) {
			for (int i = 0, len = files.length; i < len; ++i) {
				File f = files[i];

				if (f.canRead()) {
					String branch = f.getName();
					bnhs.add(new BranchAndHead(branch, getRemoteHeadRef(branch)));
				}
			}
		}

		return bnhs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.GitRepository#removeLocalBranch(java.lang.String)
	 */
	public void removeLocalBranch(String branch) throws IOException {
		File target = new File(refsHeads, branch);

		if (target.canRead()) {
			if (!target.delete()) {
				target.deleteOnExit();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#removeRemoteBranch(java.lang.String)
	 */
	public void removeRemoteBranch(String branch) throws IOException {
		File target = new File(refsRemotes, branch);

		if (target.canRead()) {
			if (!target.delete()) {
				target.deleteOnExit();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#wipe()
	 */
	public void wipe() throws IOException {
		rm(refsRemotes);
		rm(refsHeads);
		rm(objects);
	}
}
