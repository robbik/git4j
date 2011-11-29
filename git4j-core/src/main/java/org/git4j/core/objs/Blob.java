package org.git4j.core.objs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import org.git4j.core.gen.ObjectIdGenerator;
import org.git4j.core.util.IOUtils;

public class Blob implements Serializable {

	private static final long serialVersionUID = 2969936514443253631L;

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private transient Object content;

	private transient AtomicReference<String> idRef;

	private transient ObjectIdGenerator idgen;

	public Blob(ObjectIdGenerator idgen, Object content) {
		idRef = new AtomicReference<String>(null);

		this.idgen = idgen;
		this.content = content;
	}

	public Blob() {
		this(ObjectIdGenerator.DEFAULT, null);
	}

	public Blob(Object content) {
		this(ObjectIdGenerator.DEFAULT, content);
	}

	private static void writeObject(Object content, OutputStream out)
			throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		// content type
		buf.write(getContentType(content).getBytes(UTF8));
		buf.write(0);

		// content
		if (content instanceof String) {
			buf.write(((String) content).getBytes(UTF8));
		} else if (content instanceof byte[]) {
			buf.write((byte[]) content);
		} else if (content instanceof Serializable) {
			ObjectOutputStream oos = new ObjectOutputStream(buf);
			oos.writeObject(content);

			oos.flush();
			oos = null;
		}

		byte[] bytes = buf.toByteArray();
		buf = null;

		// TYPE
		out.write(Types.BLOB.toString().getBytes(UTF8));
		out.write(' ');

		// LENGTH
		out.write(String.valueOf(bytes.length).getBytes(UTF8));
		out.write(0);

		// BUFFER
		out.write(bytes);
	}
	
	private static String getContentType(Object content) {
		if (content == null) {
			return "application/x-java-serialized-object";
		}

		if (content instanceof String) {
			return "text/plain";
		}

		if (content instanceof byte[]) {
			return "application/octet";
		}

		if (content instanceof Serializable) {
			return "application/x-java-serialized-object";
		}

		return "application/x-java-serialized-object";
	}

	public static String getId(ObjectIdGenerator idgen, Object content) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try {
			writeObject(content, baos);
		} catch (Throwable t) {
			throw new Error(t);
		}

		String id = idgen.generate(baos.toByteArray());
		baos = null;

		return id;
	}
	
	public static String getId(Object content) {
		return getId(ObjectIdGenerator.DEFAULT, content);
	}

	public String getId() {
		String id = idRef.get();

		if (id == null) {
			id = getId(idgen, content);
			idRef.set(id);
		}

		return id;
	}

	public String getContentType() {
		return Blob.getContentType(content);
	}

	public void setContent(Object content) {
		this.content = content;

		idRef.set(null);
	}

	public void setContent(byte[] bytes, String contentType) {
		if (bytes == null) {
			content = null;
		} else {
			if ("application/x-java-serialized-object"
					.equalsIgnoreCase(contentType)) {

				try {
					content = new ObjectInputStream(new ByteArrayInputStream(
							bytes)).readObject();
				} catch (Throwable t) {
					throw new IllegalArgumentException(
							"unable to deserialize bytes", t);
				}
			} else if ("text/plain".equalsIgnoreCase(contentType)) {
				content = new String(bytes, UTF8);
			} else if ("application/octet".equalsIgnoreCase(contentType)) {
				content = bytes;
			} else {
				throw new IllegalArgumentException("content-type "
						+ contentType + " is unsupported");
			}
		}

		idRef.set(null);
	}

	public Object getContent() {
		return content;
	}

	public byte[] getContentAsBytes() {
		if (content == null) {
			return null;
		}

		if (content instanceof String) {
			return ((String) content).getBytes(UTF8);
		}

		if (content instanceof byte[]) {
			return (byte[]) content;
		}

		if (content instanceof char[]) {
			return String.valueOf((char[]) content).getBytes(UTF8);
		}

		if (content instanceof Serializable) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try {
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(content);

				oos.flush();
				oos.close();
			} catch (IOException e) {
				// do nothing
			}

			return baos.toByteArray();
		}

		return null;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();

		PrintWriter pw = new PrintWriter(sw);
		pw.println("blob " + getId());
		pw.println();
		if (content instanceof byte[]) {
			pw.println(new String((byte[]) content, UTF8));
		} else if (content instanceof char[]) {
			pw.println((char[]) content);
		} else if (content instanceof String) {
			pw.print((String) content);
		} else {
			pw.println(content);
		}
		pw.println();
		pw.close();

		return sw.toString();
	}

	public Blob readObject(InputStream in) throws ClassNotFoundException,
			IOException {
		String type = new String(IOUtils.nextToken(in, ' '), UTF8);
		int length = Integer
				.parseInt(new String(IOUtils.nextToken(in, 0), UTF8));

		if (!Types.BLOB.toString().equals(type)) {
			IOUtils.skip(in, length);
			throw new IllegalArgumentException("type " + type + " is not blob");
		}

		byte[] bytes = IOUtils.readFully(in, length);

		int remaining = length;

		int eos = IOUtils.findNextToken(bytes, 0, remaining, 0);
		if (eos == -1) {
			throw new IllegalArgumentException("no content type specified");
		}

		String contentType = new String(bytes, 0, eos, UTF8);
		remaining -= eos + 1;

		if ("application/x-java-serialized-object"
				.equalsIgnoreCase(contentType)) {
			content = new ObjectInputStream(new ByteArrayInputStream(bytes,
					eos + 1, remaining)).readObject();
		} else if ("text/plain".equalsIgnoreCase(contentType)) {
			content = new String(bytes, eos + 1, remaining, UTF8);
		} else if ("application/octet".equalsIgnoreCase(contentType)) {
			content = IOUtils.sub(bytes, eos + 1, remaining);
		} else {
			throw new IllegalArgumentException("unsupported content type "
					+ contentType);
		}
		
		return this;
	}

	public void writeObject(OutputStream out) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		// content type
		buf.write(getContentType().getBytes(UTF8));
		buf.write(0);

		// content
		if (content instanceof String) {
			buf.write(((String) content).getBytes(UTF8));
		} else if (content instanceof byte[]) {
			buf.write((byte[]) content);
		} else if (content instanceof Serializable) {
			ObjectOutputStream oos = new ObjectOutputStream(buf);
			oos.writeObject(content);

			oos.flush();
			oos = null;
		}

		byte[] bytes = buf.toByteArray();
		buf = null;

		// TYPE
		out.write(Types.BLOB.toString().getBytes(UTF8));
		out.write(' ');

		// LENGTH
		out.write(String.valueOf(bytes.length).getBytes(UTF8));
		out.write(0);

		// BUFFER
		out.write(bytes);
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		readObject((InputStream) in);
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		writeObject((OutputStream) out);
	}
}
