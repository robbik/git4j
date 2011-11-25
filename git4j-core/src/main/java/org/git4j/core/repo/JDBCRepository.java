package org.git4j.core.repo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.git4j.core.GitException;
import org.git4j.core.objs.Blob;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.GitObject;
import org.git4j.core.objs.UploadPack;

public class JDBCRepository implements Repository {

	private DataSource ds;

	public JDBCRepository(DataSource ds) {
		this.ds = ds;
	}

	private void store(String id, byte[] content) throws IOException {
		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			pstmt = conn
					.prepareStatement("SELECT id FROM git_objects WHERE id = ?");

			pstmt.setString(1, id);

			rs = pstmt.executeQuery();
			boolean found = rs.next();

			rs.close();
			pstmt.close();

			if (!found) {
				pstmt = conn
						.prepareStatement("INSERT INTO git_objects (id, content) VALUES (?, ?)");

				pstmt.setString(1, id);
				pstmt.setBytes(2, content);

				int eu;
				if ((eu = pstmt.executeUpdate()) != 1) {
					throw new SQLException(
							"insert statement return unexpected result " + eu);
				}
			}

			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}
	}

	private byte[] load(String id) throws IOException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		byte[] content = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			conn.setReadOnly(true);

			pstmt = conn
					.prepareStatement("SELECT content FROM git_objects WHERE id = ?");

			pstmt.setString(1, id);

			rs = pstmt.executeQuery();
			if (rs.next()) {
				content = rs.getBytes(1);
			}

			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}

		return content;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#store(org.git4j.core.objs.Blob)
	 */
	public void store(Blob blob) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.write(1);
		blob.serialize(oos);
		oos.close();

		store(blob.getId(), baos.toByteArray());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#store(org.git4j.core.objs.Commit)
	 */
	public void store(Commit commit) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.write(0);
		commit.serialize(oos);
		oos.close();

		store(commit.getId(), baos.toByteArray());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#store(org.git4j.core.objs.UploadPack)
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
	 * @see org.git4j.core.repo.Repository#load(java.lang.String,
	 * java.lang.Class)
	 */
	public <T extends GitObject> T load(String id, Class<T> type)
			throws IOException {

		byte[] content = load(id);
		if (content == null) {
			return null;
		}

		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
				content));

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
			throw (IOException) new IOException().initCause(e);
		} finally {
			try {
				ois.close();
			} catch (Throwable t) {
				// do nothing
			}
		}

		return type.cast(o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#getLocalHead(java.lang.String)
	 */
	public Commit getLocalHead(String branch) throws IOException {
		return load(getLocalHeadRef(branch), Commit.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#getLocalHeadRef(java.lang.String)
	 */
	public String getLocalHeadRef(String branch) throws IOException {
		String head = null;

		Connection conn = null;

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			stmt = conn
					.prepareStatement("SELECT head FROM git_refs_heads WHERE branch = ?");

			stmt.setString(1, branch);

			rs = stmt.executeQuery();
			if (rs.next()) {
				head = rs.getString(1);
			}

			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}

		return head;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#setLocalHeadRef(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	public void setLocalHeadRef(String branch, String headRef, String newHeadRef)
			throws GitException, IOException {
		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			pstmt = conn
					.prepareStatement("SELECT head FROM git_refs_heads WHERE branch = ?");

			pstmt.setString(1, branch);

			rs = pstmt.executeQuery();

			boolean found = rs.next();
			if (found && !rs.getString(1).equals(headRef)) {
				throw new GitException();
			}

			rs.close();
			pstmt.close();

			if (found) {
				pstmt = conn
						.prepareStatement("UPDATE git_refs_heads SET head = ? WHERE branch = ?");

				pstmt.setString(1, newHeadRef);
				pstmt.setString(2, branch);
			} else {
				pstmt = conn
						.prepareStatement("INSERT INTO git_refs_heads (branch, head) VALUES (?, ?)");

				pstmt.setString(1, branch);
				pstmt.setString(2, newHeadRef);
			}

			int eu;
			if ((eu = pstmt.executeUpdate()) != 1) {
				throw new SQLException(
						"insert statement return unexpected result " + eu);
			}

			conn.commit();
		} catch (GitException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw e;
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#getRemoteHeadRef(java.lang.String)
	 */
	public String getRemoteHeadRef(String branch) throws IOException {
		String head = null;

		Connection conn = null;

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			stmt = conn
					.prepareStatement("SELECT head FROM git_refs_remotes WHERE branch = ?");

			stmt.setString(1, branch);

			rs = stmt.executeQuery();
			if (rs.next()) {
				head = rs.getString(1);
			}

			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}

		return head;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#setRemoteHeadRef(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	public void setRemoteHeadRef(String branch, String headRef,
			String newHeadRef) throws GitException, IOException {
		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			pstmt = conn
					.prepareStatement("SELECT head FROM git_refs_remotes WHERE branch = ?");

			pstmt.setString(1, branch);

			rs = pstmt.executeQuery();

			boolean found = rs.next();
			if (found && !rs.getString(1).equals(headRef)) {
				throw new GitException();
			}

			rs.close();
			pstmt.close();

			if (found) {
				pstmt = conn
						.prepareStatement("UPDATE git_refs_remotes SET head = ? WHERE branch = ?");

				pstmt.setString(1, newHeadRef);
				pstmt.setString(2, branch);
			} else {
				pstmt = conn
						.prepareStatement("INSERT INTO git_refs_remotes (branch, head) VALUES (?, ?)");

				pstmt.setString(1, branch);
				pstmt.setString(2, newHeadRef);
			}

			int eu;
			if ((eu = pstmt.executeUpdate()) != 1) {
				throw new SQLException(
						"insert statement return unexpected result " + eu);
			}

			conn.commit();
		} catch (GitException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw e;
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#getLocalBranches()
	 */
	public Collection<BranchAndHead> getLocalBranches() throws IOException {
		List<BranchAndHead> list = new ArrayList<BranchAndHead>();

		Connection conn = null;

		Statement stmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT branch, head FROM git_refs_heads");

			while (rs.next()) {
				list.add(new BranchAndHead(rs.getString(1), rs.getString(2)));
			}

			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}

		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#getRemoteBranches()
	 */
	public Collection<BranchAndHead> getRemoteBranches() throws IOException {
		List<BranchAndHead> list = new ArrayList<BranchAndHead>();

		Connection conn = null;

		Statement stmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT branch, head FROM git_refs_remotes");

			while (rs.next()) {
				list.add(new BranchAndHead(rs.getString(1), rs.getString(2)));
			}

			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}

		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#removeLocalBranch(java.lang.String)
	 */
	public void removeLocalBranch(String branch) throws IOException {
		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			pstmt = conn
					.prepareStatement("DELETE FROM git_refs_heads WHERE branch = ?");

			pstmt.setString(1, branch);

			pstmt.executeUpdate();

			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#removeRemoteBranch(java.lang.String)
	 */
	public void removeRemoteBranch(String branch) throws IOException {
		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			pstmt = conn
					.prepareStatement("DELETE FROM git_refs_remotes WHERE branch = ?");

			pstmt.setString(1, branch);

			pstmt.executeUpdate();

			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#wipe()
	 */
	public void wipe() throws IOException {
		Connection conn = null;

		Statement stmt = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			stmt = conn.createStatement();

			stmt.executeUpdate("TRUNCATE TABLE git_refs_remoted");
			stmt.executeUpdate("TRUNCATE TABLE git_refs_heads");
			stmt.executeUpdate("TRUNCATE TABLE git_objects");

			stmt.close();
			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (Throwable t) {
					// do nothing
				}
			}

			throw (IOException) new IOException().initCause(e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Throwable t) {
					// do nothing
				}
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}
	}
}
