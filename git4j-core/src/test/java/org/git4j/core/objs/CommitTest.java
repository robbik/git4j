package org.git4j.core.objs;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.git4j.core.util.StringUtils;
import org.junit.Test;

public class CommitTest {

	@Test
	public void shouldHaveSameIdWithSerializedOne() throws Exception {
		Commit a = new Commit();
		a.setAuthor("robbi.kurniawan <robbi.kurniawan@sigma.co.id>");
		a.setMessage("initial commit!");
		a.index().put("b", "sw326GWwqeih34v");

		String aId = a.getId();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		a.writeObject(out);

		a = new Commit();
		a.readObject(new ByteArrayInputStream(out.toByteArray()));

		assertEquals(aId, a.getId());
	}

	@Test
	public void shouldGenerateSameObjectInSerialization() throws Exception {
		System.out.println(new String(StringUtils.fromHexString("74726565205558333467467a59373169712b2e6a38436d2b563932754b677568414356646346396d70365478694b356b000a617574686f7220726f6262692e6b75726e696177616e203c726f6262692e6b75726e696177616e407369676d612e636f2e69643e2031333232353539343033202b303730300a636f6d6d697474657220556e6b6e6f776e203c756e6b6e6f776e40756e6b6e6f776e2e756e6b6e6f776e3e2031333232353539343033202b303730300a0a696e697469616c20636f6d6d69742100620076576e76547634676c66664b303054414f6248504f755a456673546532784a75637a6a4e707274466a6d3000"), "UTF-8"));
	}
}
