package org.git4j.core.transport;

import java.io.IOException;

import org.git4j.core.GitException;
import org.git4j.core.objs.BranchAndHead;
import org.git4j.core.objs.UploadPack;
import org.git4j.core.util.TransportHelper;
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

		TransportHelper.sendFetch(out, branches);
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

		if (type != Transport.TYPE_UPLOAD_PACK) {
			throw new GitException("Fetch receive non UPLOAD_PACK response ("
					+ type + ")");
		}

		// data
		return TransportHelper.receiveUploadPack(in, in, false);
	}

	public void push(UploadPack pack) throws GitException, IOException {
		// send request
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);

		// data
		TransportHelper.sendUploadPack(out, out, pack);

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
