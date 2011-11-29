package org.git4j.core.gen;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.git4j.core.util.StringUtils;

public class SHA256Generator implements ObjectIdGenerator {

	private MessageDigest md;

	public SHA256Generator() {
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("SHA-256 not supported");
		}
	}

	public String generate(byte[] content) {
		md.reset();
		return StringUtils.toString64(md.digest((byte[]) content));
	}
}
