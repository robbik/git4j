package org.git4j.core.transport;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.git4j.core.GitException;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.UploadPack;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.queue.BlockingReadHandler;

public class NettyClientTransport implements Transport {

	private Channel channel;

	private BlockingReadHandler<ChannelBuffer> reader;

	public NettyClientTransport(Channel channel,
			BlockingReadHandler<ChannelBuffer> reader) {
		if (channel == null) {
			throw new NullPointerException("channel");
		}

		if (reader == null) {
			throw new NullPointerException("reader");
		}

		this.channel = channel;
		this.reader = reader;
	}

	public UploadPack[] fetch(BranchAndHead... branches) throws GitException,
			IOException {

		if (branches == null) {
			branches = new BranchAndHead[0];
		}

		// send request
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);

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

		// flush
		out.flush();

		// send!
		try {
			channel.write(buffer).await();
		} catch (InterruptedException e) {
			throw (IOException) new IOException().initCause(e);
		}

		// receive response
		try {
			buffer = reader.read();
		} catch (InterruptedException e) {
			throw (IOException) new IOException().initCause(e);
		}

		// read response
		ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);
		ObjectInputStream ois = new ObjectInputStream(in);

		// TYPE
		int type = in.readUnsignedByte();

		if (type == Transport.TYPE_ERROR_MSG) {
			// ERROR CODE
			String code = in.readUTF();

			// ERROR MESSAGE
			String message = in.readUTF();

			throw new GitException("ERROR " + code + ": " + message);
		}

		if (type != Transport.TYPE_UPLOAD_PACK) {
			throw new GitException("Fetch receive non UPLOAD_PACK response ("
					+ type + ")");
		}

		// NUMBER OF PACKs
		int len = in.readInt();

		UploadPack[] packs = new UploadPack[len];

		// PACKs
		for (int i = 0; i < len; ++i) {
			UploadPack pack = new UploadPack();

			try {
				pack.deserialize(ois);
			} catch (ClassNotFoundException e) {
				throw (IOException) new IOException().initCause(e);
			}

			packs[i] = pack;
		}

		return packs;
	}

	public void push(UploadPack pack) throws GitException, IOException {
		// send request
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();

		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);
		ObjectOutputStream oos = new ObjectOutputStream(out);

		// TYPE
		out.writeByte(Transport.TYPE_UPLOAD_PACK);

		// NUMBER OF PACKs
		out.writeInt(1);

		// PACKs
		pack.serialize(oos);

		// flush
		oos.flush();
		out.flush();

		// send!
		try {
			channel.write(buffer).await();
		} catch (InterruptedException e) {
			throw (IOException) new IOException().initCause(e);
		}

		// receive response
		try {
			buffer = reader.read();
		} catch (InterruptedException e) {
			throw (IOException) new IOException().initCause(e);
		}

		// read response
		ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);

		// TYPE
		int type = in.readUnsignedByte();

		if (type == Transport.TYPE_ERROR_MSG) {
			// ERROR CODE
			String code = in.readUTF();

			// ERROR MESSAGE
			String message = in.readUTF();

			throw new GitException("ERROR " + code + ": " + message);
		}

		if (type != Transport.TYPE_OK) {
			throw new GitException("Fetch receive non OK response (" + type
					+ ")");
		}
	}
}
