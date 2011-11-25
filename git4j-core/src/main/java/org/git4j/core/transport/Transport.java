package org.git4j.core.transport;

import java.io.IOException;

import org.git4j.core.GitException;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.UploadPack;

public interface Transport {

	public static final int TYPE_FETCH = 1;

	public static final int TYPE_UPLOAD_PACK = 2;

	public static final int TYPE_OK = 3;

	public static final int TYPE_ERROR_MSG = 4;
	
	public static final String ERROR_CODE_GIT = "01";
	
	public static final String ERROR_CODE_IO = "02";
	
	public static final String ERROR_CODE_UNKNOWN = "FF";

	/**
	 * fetch objects from given remote branches started from <code>tip</code> to
	 * HEAD.
	 * 
	 * @param branches
	 *            contains list of <branch, head> to be fetched
	 * @return upload packs, one for each remote branch
	 * @throws GitException
	 * @throws IOException
	 */
	UploadPack[] fetch(BranchAndHead... branches) throws GitException,
			IOException;

	/**
	 * push objects to remote
	 * 
	 * @param pack
	 *            packed objects
	 * @throws GitException
	 *             if a git error occurred (i.e. non fast-forward)
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void push(UploadPack pack) throws GitException, IOException;
}
