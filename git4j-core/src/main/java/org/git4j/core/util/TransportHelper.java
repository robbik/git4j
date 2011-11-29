package org.git4j.core.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.UploadPack;
import org.git4j.core.transport.Transport;

public abstract class TransportHelper {

	public static void sendFetch(DataOutput out, BranchAndHead... branches)
			throws IOException {
		// TYPE
		out.writeByte(Transport.TYPE_FETCH);

		// number of branches
		out.writeInt(branches.length);

		for (int i = 0, len = branches.length; i < len; ++i) {
			BranchAndHead branch = branches[i];

			// branch name
			out.writeUTF(branch.getBranch());

			// branch head
			out.writeUTF(branch.getHeadRef());
		}
	}

	public static BranchAndHead[] receiveFetch(DataInput in, boolean readType)
			throws IOException {
		if (readType) {
			int type = in.readUnsignedByte();

			if (type != Transport.TYPE_FETCH) {
				throw new IllegalArgumentException("data is not FETCH");
			}
		}

		// number of branches
		int len = in.readInt();
		if (len <= 0) {
			len = 0;
		}

		BranchAndHead[] branches = new BranchAndHead[len];

		for (int i = 0; i < len; ++i) {
			BranchAndHead branch = new BranchAndHead();

			// branch name
			branch.setBranch(in.readUTF());

			// branch head
			branch.setHeadRef(in.readUTF());

			branches[i] = branch;
		}

		return branches;
	}

	public static void sendOK(DataOutput out) throws IOException {
		// TYPE
		out.writeByte(Transport.TYPE_OK);
	}

	public static void sendError(DataOutput out, String code, String msg)
			throws IOException {
		// TYPE
		out.writeByte(Transport.TYPE_ERROR_MSG);

		// ERROR CODE
		out.writeUTF(code);

		// ERROR MESSAGE
		out.writeUTF(msg);
	}

	public static void sendUploadPack(DataOutput dout, OutputStream out,
			UploadPack... packs) throws IOException {
		int len = packs.length;

		// TYPE
		out.write(Transport.TYPE_UPLOAD_PACK);

		// NUMBER OF PACKs
		dout.writeInt(len);

		// PACKs
		ObjectOutputStream oos = new ObjectOutputStream(out);

		for (int i = 0; i < len; ++i) {
			packs[i].serialize(oos);
		}

		oos.flush();
	}

	public static UploadPack[] receiveUploadPack(DataInput din, InputStream in,
			boolean readType) throws IOException {
		if (readType) {
			int type = din.readUnsignedByte();

			if (type != Transport.TYPE_UPLOAD_PACK) {
				throw new IllegalArgumentException("data is not UPLOAD_PACK");
			}
		}

		int len = din.readInt();
		if (len <= 0) {
			return new UploadPack[0];
		}

		UploadPack[] packs = new UploadPack[len];
		ObjectInputStream ois = new ObjectInputStream(in);

		for (int i = 0; i < len; ++i) {
			UploadPack pack = new UploadPack();

			try {
				pack.deserialize(ois);
			} catch (ClassNotFoundException e) {
				throw (IOException) new IOException().initCause(e);
			}
		}

		return packs;
	}
}
