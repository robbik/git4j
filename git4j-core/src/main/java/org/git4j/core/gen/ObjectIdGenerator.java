package org.git4j.core.gen;

public interface ObjectIdGenerator {
	
	public static final ObjectIdGenerator DEFAULT = new SHA256Generator();

	String generate(byte[] content);
}
