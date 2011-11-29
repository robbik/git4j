package org.git4j.core.repo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.git4j.core.GitException;
import org.git4j.core.objs.Blob;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.Commit;
import org.git4j.core.objs.UploadPack;
import org.git4j.core.util.StringUtils;

public class JDBCRepository implements Repository {

	private DataSource ds;

	public JDBCRepository(DataSource ds) {
		this.ds = ds;
	}

	private Commit loadCommit(String id) throws IOException {
		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		Commit commit = new Commit();

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			conn.setReadOnly(true);

			pstmt = conn
					.prepareStatement("SELECT cauthor, cdate, cparent2, cmessage, cparent FROM git_commits WHERE id = ?");

			pstmt.setString(1, id);

			rs = pstmt.executeQuery();
			if (rs.next()) {
				commit.setAuthor(rs.getString(1));
				commit.setDate(rs.getString(2));
				commit.setParent2(rs.getString(3));
				commit.setMessage(rs.getString(4));
				commit.setParent(rs.getString(5));

				rs.close();
				rs = null;
				
				pstmt.close();
				pstmt = null;
				
				Map<String, String> index = commit.index();
				
				pstmt = conn.prepareStatement("SELECT obj_name, blob_id FROM git_index WHERE commit_id = ?");
				pstmt.setString(1, id);
				
				rs = pstmt.executeQuery();
				while (rs.next()) {
					index.put(rs.getString(1), rs.getString(2));
				}

				if (!commit.getId().equals(id)) {
					throw new SQLException("confusing because of inconsistent object");
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

		return commit;
	}

	private Blob loadBlob(String id) throws IOException {
		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		Blob blob = new Blob();

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			conn.setReadOnly(true);

			pstmt = conn
					.prepareStatement("SELECT content_type, content FROM git_blobs WHERE id = ?");
			pstmt.setString(1, id);

			rs = pstmt.executeQuery();
			if (rs.next()) {
				blob.setContent(rs.getBytes(2), rs.getString(1));
			}
			
			if (!blob.getId().equals(id)) {
				throw new SQLException("confusing because of inconsistent object");
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

		return blob;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#store(org.git4j.core.objs.Blob)
	 */
	public String store(Blob blob) throws IOException {
		byte[] contentAsBytes = blob.getContentAsBytes();
		if (contentAsBytes == null) {
			throw new IllegalArgumentException("blob content MUST NOT be NULL");
		}

		String id = blob.getId();

		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			pstmt = conn
					.prepareStatement("SELECT id FROM git_blobs WHERE id = ?");

			pstmt.setString(1, id);

			rs = pstmt.executeQuery();
			boolean found = rs.next();

			rs.close();
			pstmt.close();

			if (!found) {
				pstmt = conn
						.prepareStatement("INSERT INTO git_blobs (id, content_type, content) VALUES (?, ?, ?)");

				pstmt.setString(1, id);
				pstmt.setString(2, blob.getContentType());
				pstmt.setBytes(3, contentAsBytes);

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

		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#store(org.git4j.core.objs.Commit)
	 */
	public String store(Commit commit) throws IOException {
		String id = commit.getId();

		Connection conn = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);

			pstmt = conn
					.prepareStatement("SELECT id FROM git_commits WHERE id = ?");

			pstmt.setString(1, id);

			rs = pstmt.executeQuery();
			boolean found = rs.next();

			rs.close();
			pstmt.close();

			if (!found) {
				pstmt = conn
						.prepareStatement("INSERT INTO git_commits (id, cauthor, cdate, cparent2, cmessage, cparent) VALUES (?, ?, ?, ?, ?, ?)");

				pstmt.setString(1, commit.getId());
				StringUtils.setStringOrNull(pstmt, 2, commit.getAuthor());
				pstmt.setString(3, commit.getDateAsString());
				StringUtils.setStringOrNull(pstmt, 4, commit.getParent2());
				StringUtils.setStringOrNull(pstmt, 5, commit.getMessage());
				StringUtils.setStringOrNull(pstmt, 6, commit.getParent());

				int eu;
				if ((eu = pstmt.executeUpdate()) != 1) {
					throw new SQLException(
							"insert statement return unexpected result " + eu);
				}
				
				pstmt.close();
				pstmt = null;
				
				pstmt = conn.prepareStatement("INSERT INTO git_index (commit_id, obj_name, blob_id) VALUES (?, ?, ?)");
				pstmt.setString(1, id);
				
				for (Map.Entry<String, String> entry : commit.index().entrySet()) {
					pstmt.setString(2, entry.getKey());
					pstmt.setString(3, entry.getValue());

					if ((eu = pstmt.executeUpdate()) != 1) {
						throw new SQLException(
								"insert statement return unexpected result " + eu);
					}
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

		return id;
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
	 * @see org.git4j.core.repo.Repository#find(java.lang.Class,
	 * java.lang.String)
	 */
	public <T> T find(Class<T> type, String id) throws IOException {
		if (Commit.class.isAssignableFrom(type)) {
			return type.cast(loadCommit(id));
		}

		if (Blob.class.isAssignableFrom(type)) {
			return type.cast(loadBlob(id));
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.git4j.core.repo.Repository#getLocalHead(java.lang.String)
	 */
	public Commit getLocalHead(String branch) throws IOException {
		String headRef = getLocalHeadRef(branch);
		if (headRef == null) {
			return null;
		}

		return find(Commit.class, headRef);
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

			stmt.executeUpdate("TRUNCATE TABLE git_refs_remotes");
			stmt.executeUpdate("TRUNCATE TABLE git_refs_heads");
			stmt.executeUpdate("TRUNCATE TABLE git_commits");
			stmt.executeUpdate("TRUNCATE TABLE git_blobs");
			stmt.executeUpdate("TRUNCATE TABLE git_index");

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
