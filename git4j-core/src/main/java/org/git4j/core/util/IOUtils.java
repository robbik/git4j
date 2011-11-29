package org.git4j.core.util;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public abstract class IOUtils {

	public static void readFully(InputStream in, byte[] bbuf, int length)
			throws IOException {
		int offset = 0;
		int remaining = length;

		int nbread;

		while (remaining > 0) {
			nbread = in.read(bbuf, offset, remaining);
			if (nbread == -1) {
				throw new EOFException();
			}

			remaining -= nbread;
			offset += nbread;
		}
	}

	public static void readFully(InputStream in, byte[] bbuf, int off,
			int length) throws IOException {
		int offset = off;
		int remaining = length;

		int nbread;

		while (remaining > 0) {
			nbread = in.read(bbuf, offset, remaining);
			if (nbread == -1) {
				throw new EOFException();
			}

			remaining -= nbread;
			offset += nbread;
		}
	}

	public static byte[] readFully(InputStream in, int length)
			throws IOException {
		int offset = 0;
		int remaining = length;

		int nbread;
		byte[] bbuf = new byte[length];

		while (remaining > 0) {
			nbread = in.read(bbuf, offset, remaining);
			if (nbread == -1) {
				throw new EOFException();
			}

			remaining -= nbread;
			offset += nbread;
		}

		return bbuf;
	}

	public static byte[] readFully(InputStream in) throws IOException {
		int nbread;

		byte[] bbuf = new byte[16384];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		while ((nbread = in.read(bbuf, 0, bbuf.length)) >= 0) {
			baos.write(bbuf, 0, nbread);
		}

		return baos.toByteArray();
	}
	
	public static byte[] sub(byte[] bytes, int off, int length) {
		byte[] bbuf = new byte[length];
		System.arraycopy(bytes, off, bbuf, 0, length);
		
		return bbuf;
	}

	public static byte[] nextToken(InputStream in, int delimiter)
			throws IOException {
		int readb;
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		while ((readb = in.read()) >= 0) {
			if (readb == delimiter) {
				break;
			}

			buf.write(readb);
		}

		return buf.toByteArray();
	}

	public static int findNextToken(byte[] bytes, int off, int len,
			int delimiter) throws IOException {

		for (int i = 0; i < len; ++i) {
			int val = bytes[i] & 0xFF;
			
			if (val == delimiter) {
				return i;
			}
		}
		
		return -1;
	}

	public static void skip(InputStream in, int length) throws IOException {
		in.skip(length);
	}
}
