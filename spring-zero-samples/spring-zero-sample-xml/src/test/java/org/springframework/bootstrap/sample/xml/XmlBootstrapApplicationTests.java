package org.springframework.bootstrap.sample.xml;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class XmlBootstrapApplicationTests {

	private static PrintStream savedOutput;
	private static ByteArrayOutputStream output;

	@BeforeClass
	public static void init() {
		savedOutput = System.out;
		output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output));
	}

	@AfterClass
	public static void clear() {
		System.setOut(savedOutput);
	}

	private static String getOutput() {
		return output.toString();
	}

	@Test
	public void testDefaultSettings() throws Exception {
		XmlBootstrapApplication.main(new String[0]);
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

}
