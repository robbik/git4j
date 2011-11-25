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
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

@Sharable
public class NettyServerTransport extends SimpleChannelUpstreamHandler {

	private Transport transport;

	public NettyServerTransport(Transport transport) {
		this.transport = transport;
	}

	private void sendOK(Channel channel) throws Exception {
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);

		// TYPE
		out.writeByte(Transport.TYPE_OK);

		// flush
		out.flush();

		// send!
		Channels.write(channel, buffer);
	}

	private void sendError(Channel channel, String code, String msg)
			throws Exception {
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);

		// TYPE
		out.writeByte(Transport.TYPE_ERROR_MSG);

		// ERROR CODE
		out.writeUTF(code);

		// ERROR MESSAGE
		out.writeUTF(msg);

		// flush
		out.flush();

		// send!
		Channels.write(channel, buffer);
	}

	private void handleFetch(Channel channel, ChannelBufferInputStream in)
			throws Exception {
		// read request

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

		UploadPack[] packs;

		try {
			packs = transport.fetch(branches);
		} catch (GitException e) {
			sendError(channel, Transport.ERROR_CODE_GIT, e.getMessage());
			return;
		} catch (IOException e) {
			sendError(channel, Transport.ERROR_CODE_IO, e.getMessage());
			return;
		} catch (Throwable t) {
			sendError(channel, Transport.ERROR_CODE_UNKNOWN, t.getMessage());
			return;
		}

		len = packs.length;

		// write response
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();

		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);
		ObjectOutputStream oos = new ObjectOutputStream(out);

		// TYPE
		out.write(Transport.TYPE_UPLOAD_PACK);

		// NUMBER OF PACKs
		out.write(len);

		// PACKs
		for (int i = 0; i < len; ++i) {
			packs[i].serialize(oos);
		}

		// flush
		oos.flush();
		out.flush();

		// send
		Channels.write(channel, buffer);
	}

	private void handleUploadPack(Channel channel, ChannelBufferInputStream in)
			throws Exception {
		// number of packs
		int len = in.readInt();
		if (len <= 0) {
			sendOK(channel);
			return;
		}

		ObjectInputStream ois = new ObjectInputStream(in);
		boolean error = false;

		for (int i = 0; i < len; ++i) {
			UploadPack pack = new UploadPack();

			try {
				pack.deserialize(ois);
			} catch (ClassNotFoundException e) {
				throw (IOException) new IOException().initCause(e);
			}

			try {
				transport.push(pack);
			} catch (GitException e) {
				sendError(channel, Transport.ERROR_CODE_GIT, e.getMessage());

				error = true;
				break;
			} catch (IOException e) {
				sendError(channel, Transport.ERROR_CODE_IO, e.getMessage());

				error = true;
				break;
			} catch (Throwable t) {
				sendError(channel, Transport.ERROR_CODE_UNKNOWN, t.getMessage());

				error = true;
				break;
			}
		}

		if (!error) {
			sendOK(channel);
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();

		if (!(msg instanceof ChannelBuffer)) {
			super.messageReceived(ctx, e);
			return;
		}

		Channel channel = ctx.getChannel();

		// read request
		ChannelBufferInputStream in = new ChannelBufferInputStream(
				(ChannelBuffer) msg);

		// TYPE
		int type = in.readUnsignedByte();

		if (type == Transport.TYPE_FETCH) {
			handleFetch(channel, in);
		} else if (type == Transport.TYPE_UPLOAD_PACK) {
			handleUploadPack(channel, in);
		}

		super.messageReceived(ctx, e);
	}
}
