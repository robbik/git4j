package org.git4j.core.transport;

import java.io.IOException;

import org.git4j.core.GitException;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.UploadPack;
import org.git4j.core.repo.Repository;
import org.git4j.core.util.TransportHelper;
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

	public NettyServerTransport(Repository repo) {
		this.transport = new DirectTransport(repo);
	}

	private void sendOK(Channel channel) throws Exception {
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);

		// data
		TransportHelper.sendOK(out);

		// flush
		out.flush();

		// send!
		Channels.write(channel, buffer);
	}

	private void sendError(Channel channel, String code, String msg)
			throws Exception {
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);

		// data
		TransportHelper.sendError(out, code, msg);

		// flush
		out.flush();

		// send!
		Channels.write(channel, buffer);
	}

	private void handleFetch(Channel channel, ChannelBufferInputStream in)
			throws Exception {
		// read request
		BranchAndHead[] branches = TransportHelper.receiveFetch(in, false);

		// process
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

		// write response
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);

		TransportHelper.sendUploadPack(out, out, packs);
		out.flush();

		// send
		Channels.write(channel, buffer);
	}

	private void handleUploadPack(Channel channel, ChannelBufferInputStream in)
			throws Exception {

		// read request
		UploadPack[] packs = TransportHelper.receiveUploadPack(in, in, false);

		// process
		boolean error = false;

		for (int i = 0, len = packs.length; i < len; ++i) {
			try {
				transport.push(packs[i]);
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
