package org.git4j.core.gen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.git4j.core.util.StringUtils;

public class SHA256Generator implements ObjectIdGenerator {

	private static final Charset UTF8;

	static {
		UTF8 = Charset.forName("UTF-8");
	}

	private MessageDigest md;

	public SHA256Generator() {
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("SHA-256 not supported");
		}
	}

	public String generate(Object content) {
		md.reset();

		if (content instanceof byte[]) {
			return StringUtils.toString64(md.digest((byte[]) content));
		}

		if (content instanceof String) {
			return StringUtils.toString64(md.digest(((String) content)
					.getBytes(UTF8)));
		}

		if (content instanceof char[]) {
			return StringUtils.toString64(md.digest(String.valueOf(
					(char[]) content).getBytes(UTF8)));
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(content);
			oos.close();
		} catch (IOException ex) {
			// SHOULD NOT BE HERE
		}

		return StringUtils.toHexString(md.digest(out.toByteArray()));
	}
}
