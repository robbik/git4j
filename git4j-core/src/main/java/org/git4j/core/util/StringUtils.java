package org.git4j.core.util;

public abstract class StringUtils {

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private static final char[] AN = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+."
			.toCharArray();

	public static boolean isEmpty(String value) {
		if (value == null) {
			return true;
		}

		return value.trim().length() == 0;
	}

	public static String toHexString(byte[] bytes) {
		char[] hexs = new char[bytes.length << 1];

		for (int i = 0, j = 0, len = bytes.length; i < len; ++i) {
			int val = bytes[i] & 0xFF;

			hexs[j++] = HEX[val >> 4];
			hexs[j++] = HEX[val & 0x0F];
		}

		return new String(hexs);
	}

	public static String toString32(byte[] bytes) {
		int i = 0, index = 0, digit = 0;
		int currByte, nextByte;

		StringBuffer base32 = new StringBuffer((bytes.length + 7) * 8 / 5);

		while (i < bytes.length) {
			currByte = (bytes[i] >= 0) ? bytes[i] : (bytes[i] + 256); // unsign

			/* Is the current digit going to span a byte boundary? */
			if (index > 3) {
				if ((i + 1) < bytes.length) {
					nextByte = (bytes[i + 1] >= 0) ? bytes[i + 1]
							: (bytes[i + 1] + 256);
				} else {
					nextByte = 0;
				}

				digit = currByte & (0xFF >> index);
				index = (index + 5) % 8;
				digit <<= index;
				digit |= nextByte >> (8 - index);
				i++;
			} else {
				digit = (currByte >> (8 - (index + 5))) & 0x1F;
				index = (index + 5) % 8;

				if (index == 0) {
					i++;
				}
			}

			base32.append(AN[digit]);
		}

		return base32.toString();
	}

	public static String toString64(byte[] bytes) {
		return toString64(bytes, 0, bytes.length);
	}

	public static String toString64(byte[] bytes, int offset, int length) {
		int oDataLen = (length * 4 + 2) / 3; // output length without padding
		int oLen = ((length + 2) / 3) * 4; // output length including padding

		char[] out = new char[oLen];
		int i = offset;
		int end = offset + length;
		int op = 0;

		while (i < end) {
			int i0 = bytes[i++] & 0xff;
			int i1 = i < end ? bytes[i++] & 0xff : 0;
			int i2 = i < end ? bytes[i++] & 0xff : 0;
			int o0 = i0 >>> 2;
			int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
			int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
			int o3 = i2 & 0x3F;

			out[op++] = AN[o0];
			out[op++] = AN[o1];

			if (op < oDataLen) {
				out[op] = AN[o2];
			}
			++op;

			if (op < oDataLen) {
				out[op] = AN[o3];
			}
			++op;
		}

		return String.valueOf(out, 0, op);
	}

	public static void main(String[] args) throws Exception {
		System.out.println(AN.length);
	}
}
