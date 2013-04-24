package org.springframework.bootstrap.sample.simple;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ProfileBootstrapApplicationTests {

	private static PrintStream savedOutput;
	private static ByteArrayOutputStream output;
	private String profiles;

	@BeforeClass
	public static void init() {
		savedOutput = System.out;
		output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output));
	}

	@Before
	public void before() {
		this.profiles = System.getProperty("spring.profiles.active");
	}

	@After
	public void after() {
		if (this.profiles != null) {
			System.setProperty("spring.profiles.active", this.profiles);
		} else {
			System.clearProperty("spring.profiles.active");
		}
	}

	@AfterClass
	public static void clear() {
		System.setOut(savedOutput);
	}

	private static String getOutput() {
		return output.toString();
	}

	@Test
	public void testDefaultProfile() throws Exception {
		ProfileBootstrapApplication.main(new String[0]);
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello Phil"));
	}

	@Test
	public void testGoodbyeProfile() throws Exception {
		System.setProperty("spring.profiles.active", "goodbye");
		ProfileBootstrapApplication.main(new String[0]);
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Goodbye Everyone"));
	}

	@Test
	public void testGoodbyeProfileFromCommandline() throws Exception {
		ProfileBootstrapApplication
				.main(new String[] { "--spring.profiles.active=goodbye" });
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Goodbye Everyone"));
	}

}
