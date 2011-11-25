package org.git4j.core.objs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Commit extends GitObject {

	private static final long serialVersionUID = -8734222820487895699L;

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"EEE MMM dd HH:mm:ss yyyy Z");

	private transient String author;

	private transient String date;

	private transient String merged;

	private transient String message;

	// Name, BLOB ID
	private transient Map<String, String> index;

	private transient String parent;

	public Commit() {
		author = "Unknown <unknown@unknown.unknown>";
		date = sdf.format(new Date());

		merged = null;
		message = "NO COMMIT MESSAGE";

		index = new HashMap<String, String>();
		parent = null;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getDate() {
		try {
			return sdf.parse(date);
		} catch (ParseException e) {
			return null;
		}
	}

	public void setDate(Date date) {
		this.date = sdf.format(date);
	}

	public String getMerged() {
		return merged;
	}

	public void setMerged(String merged) {
		this.merged = merged;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void addToIndex(String uuid, String name) {
		index.put(uuid, name);
	}

	public void removeFromIndex(String uuid) {
		index.remove(uuid);
	}

	public Map<String, String> index() {
		return index;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();

		PrintWriter pw = new PrintWriter(sw);
		pw.println("commit " + id);
		pw.println("Author: " + author);
		pw.println("Date: " + date);
		if (merged != null) {
			pw.println("Merged: " + merged);
		}
		pw.println();
		pw.println(message);
		pw.println();
		pw.close();

		return sw.toString();
	}

	public Commit deserialize(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		readObject(in);
		return this;
	}

	public void serialize(ObjectOutputStream out) throws IOException {
		writeObject(out);
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		author = in.readUTF();
		date = in.readUTF();

		merged = in.readUTF();
		if (merged.length() == 0) {
			merged = null;
		}

		message = in.readUTF();

		index = new HashMap<String, String>();
		for (int i = 0, len = in.readInt(); i < len; ++i) {
			index.put(in.readUTF(), in.readUTF());
		}

		parent = in.readUTF();
		if (parent.length() == 0) {
			parent = null;
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeUTF(author);
		out.writeUTF(date);

		if (merged == null) {
			out.writeUTF("");
		} else {
			out.writeUTF(merged);
		}

		out.writeUTF(message);

		out.writeInt(index.size());
		for (Map.Entry<String, String> e : index.entrySet()) {
			out.writeUTF(e.getKey());
			out.writeUTF(e.getValue());
		}

		if (parent == null) {
			out.writeUTF("");
		} else {
			out.writeUTF(parent);
		}
	}
}
