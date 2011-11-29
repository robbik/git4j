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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.git4j.core.gen.ObjectIdGenerator;
import org.git4j.core.util.IOUtils;

public class Commit implements Serializable {

	private static final long serialVersionUID = -8734222820487895699L;

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"EEE MMM dd HH:mm:ss yyyy Z");

	private static final SimpleDateFormat Z = new SimpleDateFormat("Z");

	private transient ObjectIdGenerator idgen;

	private transient AtomicReference<String> idRef;

	private String author;

	private String committer;

	private Calendar calendar;

	private String parent;

	private String parent2;

	private String message;

	// Name, BLOB ID
	private Map<String, String> index;

	public Commit(ObjectIdGenerator idgen) {
		this.idgen = idgen;

		idRef = new AtomicReference<String>(null);

		author = "Unknown <unknown@unknown.unknown>";
		committer = "Unknown <unknown@unknown.unknown>";

		calendar = Calendar.getInstance();

		parent = null;
		parent2 = null;

		message = "NO COMMIT MESSAGE";

		index = new HashMap<String, String>();
	}

	public Commit() {
		this(ObjectIdGenerator.DEFAULT);
	}

	public String getId() {
		String id = idRef.get();

		if (id == null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try {
				writeObject(baos);
			} catch (Throwable t) {
				throw new Error(t);
			}

			id = idgen.generate(baos.toByteArray());
			baos = null;

			idRef.set(id);
		}

		return id;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getCommitter() {
		return committer;
	}

	public void setCommitter(String committer) {
		this.committer = committer;
	}

	public Date getDate() {
		return calendar.getTime();
	}

	public String getDateAsString() {
		return String.format("%1$s %2$tz",
				String.valueOf(calendar.getTimeInMillis() / 1000L), calendar);
	}

	public void setDate(Date date) {
		calendar.setTime(date);
	}

	public void setDate(String date) {
		String[] sp = date.split(" ");

		try {
			calendar.setTime(Z.parse(sp[1]));
		} catch (Throwable t) {
			throw new IllegalArgumentException("date", t);
		}

		calendar.setTimeInMillis(Long.parseLong(sp[0]) * 1000);
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getParent2() {
		return parent2;
	}

	public void setParent2(String parent2) {
		this.parent2 = parent2;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Map<String, String> index() {
		return index;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();

		PrintWriter pw = new PrintWriter(sw);
		pw.println("commit " + getId());
		pw.println("Author: " + author);
		pw.println("Date: " + sdf.format(calendar.getTime()));
		if (parent2 != null) {
			pw.println("Merge: " + parent + " " + parent2);
		}
		pw.println();
		pw.println(message);
		pw.println();

		pw.println("index = " + index);

		pw.close();

		return sw.toString();
	}

	private void parseAuthor(String data) {
		int sep = data.lastIndexOf(' ', data.lastIndexOf(' ') - 1);
		
		setDate(data.substring(sep).trim());
		author = data.substring(0, sep).trim();
	}

	private void parseCommitter(String data) {
		int sep = data.lastIndexOf(' ', data.lastIndexOf(' ') - 1);

		committer = data.substring(0, sep).trim();
	}

	public Commit readObject(InputStream in) throws ClassNotFoundException,
			IOException {
		String type = new String(IOUtils.nextToken(in, ' '), "UTF-8");
		int length = Integer.parseInt(new String(IOUtils.nextToken(in, 0), "UTF-8"));

		if (!Types.COMMIT.toString().equals(type)) {
			IOUtils.skip(in, length);
			throw new IllegalArgumentException("type " + type
					+ " is not commit");
		}

		byte[] bytes = IOUtils.readFully(in, length);
		InputStream is = new ByteArrayInputStream(bytes);

		// tree
		String line = new String(IOUtils.nextToken(is, '\n'), "UTF-8");

		line = new String(IOUtils.nextToken(is, '\n'), "UTF-8");

		// parent 1
		if (line.startsWith("parent ")) {
			parent = line.substring(7).trim();

			line = new String(IOUtils.nextToken(is, '\n'), "UTF-8");
		} else {
			parent = null;
		}

		// parent 2
		if (line.startsWith("parent ")) {
			parent2 = line.substring(7).trim();

			line = new String(IOUtils.nextToken(is, '\n'), "UTF-8");
		} else {
			parent2 = null;
		}

		// author
		parseAuthor(line.substring(7).trim());
		line = new String(IOUtils.nextToken(is, '\n'), "UTF-8");

		// committer
		parseCommitter(line.substring(10).trim());

		// end of header
		IOUtils.nextToken(is, '\n');

		// message
		message = new String(IOUtils.nextToken(is, 0), "UTF-8");

		// index
		index = new HashMap<String, String>();

		for (;;) {
			String key = new String(IOUtils.nextToken(is, 0), "UTF-8");
			if (key.length() == 0) {
				break;
			}

			String value = new String(IOUtils.nextToken(is, 0), "UTF-8");
			index.put(key, value);
		}

		idRef.set(null);
		return this;
	}

	public void writeObject(OutputStream out) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		// tree
		buf.write(String.format("tree %1$s\n", idgen.generate(new byte[0]))
				.getBytes("UTF-8"));

		// parent 1
		if (parent != null) {
			buf.write(String.format("parent %1$s\n", parent).getBytes("UTF-8"));
		}

		// parent 2
		if (parent2 != null) {
			buf.write(String.format("parent %1$s\n", parent2).getBytes("UTF-8"));
		}

		String dateAsString = getDateAsString();

		// author
		buf.write(String.format("author %1$s %2$s\n", author, dateAsString)
				.getBytes("UTF-8"));

		// committer
		buf.write(String.format("committer %1$s %2$s\n", committer,
				dateAsString).getBytes("UTF-8"));

		// end of header
		buf.write('\n');

		// message
		buf.write(message.getBytes("UTF-8"));
		
		// end of message
		buf.write(0);

		// index
		for (Map.Entry<String, String> e : index.entrySet()) {
			buf.write(e.getKey().getBytes("UTF-8"));
			buf.write(0);

			buf.write(e.getValue().getBytes("UTF-8"));
			buf.write(0);
		}

		buf.flush();

		byte[] bytes = buf.toByteArray();
		buf = null;

		// TYPE
		out.write(Types.COMMIT.toString().getBytes("UTF-8"));
		out.write(' ');

		// LENGTH
		out.write(String.valueOf(bytes.length).getBytes("UTF-8"));
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
