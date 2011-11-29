package org.git4j.core.objs;

public enum Types {

	NULL(0, "null"), COMMIT(1, "commit"), TREE(2, "tree"), BLOB(3, "blob"), TAG(
			4, "tag");

	private final int num;

	private final String name;

	private Types(int num, String name) {
		this.num = num;
		this.name = name;
	}

	public int intValue() {
		return num;
	}

	@Override
	public String toString() {
		return name;
	}

	public static Types valueOf(int num) {
		switch (num) {
		case 0:
			return NULL;
		case 1:
			return COMMIT;
		case 2:
			return TREE;
		case 3:
			return BLOB;
		case 4:
			return TAG;
		default:
			throw new IllegalArgumentException("unknown value " + num);
		}
	}
}
