package org.git4j.core.objs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.git4j.core.util.IOUtils;

public class Blob extends GitObject {

	private static final long serialVersionUID = 2969936514443253631L;

	private transient Object content;

	public Blob(String id) {
		super(id);
	}

	public Blob() {
		super();
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();

		PrintWriter pw = new PrintWriter(sw);
		pw.println("blob " + id);
		pw.println();
		if (content instanceof byte[]) {
			try {
				pw.println(new String((byte[]) content, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				pw.println(new String((byte[]) content));
			}
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

	public Blob deserialize(ObjectInputStream in) throws ClassNotFoundException,
			IOException {
		readObject(in);
		return this;
	}

	public void serialize(ObjectOutputStream out) throws IOException {
		writeObject(out);
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		switch (in.read()) {
		case 1: // String
			content = new String(IOUtils.readFully(in), "UTF-8");
			break;
		case 2: // byte[]
			content = IOUtils.readFully(in);
			break;
		case 3: // char[]
			content = new String(IOUtils.readFully(in), "UTF-8").toCharArray();
			break;
		case 4: // Serializable
			content = in.readObject();
			break;
		default:
			content = null;
			break;
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		if (content instanceof String) {
			out.write(1);
			out.write(((String) content).getBytes("UTF-8"));
		} else if (content instanceof byte[]) {
			out.write(2);
			out.write((byte[]) content);
		} else if (content instanceof char[]) {
			out.write(3);
			out.write(String.valueOf((char[]) content).getBytes("UTF-8"));
		} else if (content instanceof Serializable) {
			out.write(4);
			out.writeObject(content);
		} else {
			out.write(0);
		}
	}
}
