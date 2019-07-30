package org.springframework.boot.configurationprocessor.json;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JSONTokenerTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testBack() {
		JSONTokener jsonTokener = new JSONTokener(null);
		jsonTokener.back();
		Assert.assertEquals(" at character 0 of null", jsonTokener.toString());
	}

	@Test
	public void testConstructor1() {
		JSONTokener jsonTokener = new JSONTokener("fooBar");
		Assert.assertEquals(" at character 0 of fooBar", jsonTokener.toString());
	}

	@Test
	public void testConstructor2() {
		JSONTokener jsonTokener = new JSONTokener("\ufeff foo");
		Assert.assertEquals(' ', jsonTokener.next());
		Assert.assertEquals('f', jsonTokener.next());
		Assert.assertEquals(" at character 2 of  foo", jsonTokener.toString());
	}

	@Test
	public void testDehexchar() {
		Assert.assertEquals(0, JSONTokener.dehexchar('0'));
		Assert.assertEquals(15, JSONTokener.dehexchar('f'));
		Assert.assertEquals(15, JSONTokener.dehexchar('F'));
		Assert.assertEquals(-1, JSONTokener.dehexchar('i'));
		Assert.assertEquals(-1, JSONTokener.dehexchar('!'));
	}

	@Test
	public void testMoreOutputFalse() {
		JSONTokener jsonTokener = new JSONTokener("");
		Assert.assertFalse(jsonTokener.more());
	}

	@Test
	public void testMoreOutputTrue() {
		JSONTokener jsonTokener = new JSONTokener("fooBar");
		Assert.assertTrue(jsonTokener.more());
	}

	@Test
	public void testNext1() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("foo");
		thrown.expect(JSONException.class);
		jsonTokener.next('\u0001');
		Assert.assertFalse(true);
	}

	@Test
	public void testNext2() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("foo");
		Assert.assertEquals(102, jsonTokener.next('f'));
	}

	@Test
	public void testNextInputPositive() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("foo");
		thrown.expect(JSONException.class);
		jsonTokener.next(524_288);
	}

	@Test
	public void testNextInputZero() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("foo");
		Assert.assertEquals("", jsonTokener.next(0));
	}

	@Test
	public void testNextOutput0() {
		JSONTokener jsonTokener = new JSONTokener("foo");
		Assert.assertEquals('f', jsonTokener.next());
		Assert.assertEquals('o', jsonTokener.next());
		Assert.assertEquals('o', jsonTokener.next());
		Assert.assertEquals(" at character 3 of foo", jsonTokener.toString());
	}

	@Test
	public void testNextString1() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("IT\\\\b");
		String actual = jsonTokener.nextString('\\');

		Assert.assertEquals("IT", actual);
		Assert.assertEquals(" at character 3 of IT\\\\b", jsonTokener.toString());
	}

	@Test
	public void testNextString2() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("X\\uabb8\\");
		try {
			thrown.expect(JSONException.class);
			jsonTokener.nextString('a');
		} catch (JSONException ex) {
			Assert.assertEquals(" at character 8 of X\\uabb8\\", jsonTokener.toString());
			throw ex;
		}
	}

	@Test
	public void testNextString3() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("X\\\\abb");
		String actual = jsonTokener.nextString('a');

		Assert.assertEquals("X\\", actual);
		Assert.assertEquals(" at character 4 of X\\\\abb", jsonTokener.toString());
	}

	@Test
	public void testNextString4() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("((((\\uabb8");
		try {
			thrown.expect(JSONException.class);
			jsonTokener.nextString('h');
		} catch (JSONException ex) {
			Assert.assertEquals(" at character 10 of ((((\\uabb8", jsonTokener.toString());
			throw ex;
		}
	}

	@Test
	public void testNextTo1() {
		JSONTokener jsonTokener = new JSONTokener("? >>>>>>>");
		String string = jsonTokener.nextTo(' ');
		Assert.assertEquals("?", string);
		Assert.assertEquals(" at character 1 of ? >>>>>>>", jsonTokener.toString());
	}

	@Test
	public void testNextTo2() {
		JSONTokener jsonTokener = new JSONTokener("");
		Assert.assertEquals("", jsonTokener.nextTo("eee"));
	}

	@Test
	public void testNextTo3() {
		JSONTokener jsonTokener = new JSONTokener(null);
		thrown.expect(NullPointerException.class);
		jsonTokener.nextTo(null);
	}

	@Test
	public void testReadEscapeCharacter1() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("\\ta");
		Assert.assertEquals("\t", jsonTokener.nextString('a'));
		Assert.assertEquals(" at character 3 of \\ta", jsonTokener.toString());
	}

	@Test
	public void testReadEscapeCharacter2() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("\\ba");
		Assert.assertEquals("\b", jsonTokener.nextString('a'));
		Assert.assertEquals(" at character 3 of \\ba", jsonTokener.toString());
	}

	@Test
	public void testReadEscapeCharacter3() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("\\na");
		Assert.assertEquals("\n", jsonTokener.nextString('a'));
		Assert.assertEquals(" at character 3 of \\na", jsonTokener.toString());
	}

	@Test
	public void testReadEscapeCharacter4() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("\\ra");
		Assert.assertEquals("\r", jsonTokener.nextString('a'));
		Assert.assertEquals(" at character 3 of \\ra", jsonTokener.toString());
	}

	@Test
	public void testReadEscapeCharacter5() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("\\fa");
		Assert.assertEquals("\f", jsonTokener.nextString('a'));
		Assert.assertEquals(" at character 3 of \\fa", jsonTokener.toString());
	}

	@Test
	public void testReadEscapeCharacter6() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("\\ua");
		thrown.expect(JSONException.class);
		Assert.assertEquals("\u0000", jsonTokener.nextString('a'));
		Assert.assertEquals(" at character 3 of \\fa", jsonTokener.toString());
	}

	@Test
	public void testNextCleanInternal1() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("#foo\nBar");
		Assert.assertEquals('B', jsonTokener.nextClean());
		Assert.assertEquals(" at character 6 of #foo\nBar", jsonTokener.toString());
	}

	@Test
	public void testNextCleanInternal2() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("/foo\nBar");
		Assert.assertEquals('/', jsonTokener.nextClean());
		Assert.assertEquals(" at character 1 of /foo\nBar", jsonTokener.toString());
	}

	@Test
	public void testNextCleanInternal3() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("/");
		Assert.assertEquals('/', jsonTokener.nextClean());
		Assert.assertEquals(" at character 1 of /", jsonTokener.toString());
	}

	@Test
	public void testNextCleanInternal4() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("\nfoo");
		Assert.assertEquals('f', jsonTokener.nextClean());
		Assert.assertEquals(" at character 2 of \nfoo", jsonTokener.toString());
	}

	@Test
	public void testNextCleanInternal5() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("//foo\nBar");
		Assert.assertEquals('B', jsonTokener.nextClean());
		Assert.assertEquals(" at character 7 of //foo\nBar", jsonTokener.toString());
	}

	@Test
	public void testNextCleanInternal6() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("/*foo*/");
		Assert.assertEquals(0, jsonTokener.nextClean());
		Assert.assertEquals(" at character 7 of /*foo*/", jsonTokener.toString());
	}

	@Test
	public void testNextCleanInternal7() throws JSONException {
		JSONTokener jsonTokener = new JSONTokener("/*foo");
		thrown.expect(JSONException.class);
		Assert.assertEquals(0, jsonTokener.nextClean());
	}

	@Test
	public void testSkipTo1() {
		JSONTokener jsonTokener = new JSONTokener("pppppppppa");
		Assert.assertEquals('a', jsonTokener.skipTo('a'));
	}

	@Test
	public void testSkipTo2() {
		JSONTokener jsonTokener = new JSONTokener("???????");
		Assert.assertEquals(0, jsonTokener.skipTo('a'));
	}
}
