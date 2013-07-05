package org.springframework.zero.sample.simple;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SampleSimpleApplication}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class SampleSimpleApplicationTests {

	private PrintStream savedOutput;

	private ByteArrayOutputStream output;

	private String profiles;

	@Before
	public void init() {
		this.savedOutput = System.out;
		this.output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(this.output));
		this.profiles = System.getProperty("spring.profiles.active");
	}

	@After
	public void after() {
		if (this.profiles != null) {
			System.setProperty("spring.profiles.active", this.profiles);
		} else {
			System.clearProperty("spring.profiles.active");
		}
		System.setOut(this.savedOutput);
	}

	private String getOutput() {
		return this.output.toString();
	}

	@Test
	public void testDefaultSettings() throws Exception {
		SampleSimpleApplication.main(new String[0]);
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello Phil"));
	}

	@Test
	public void testCommandLineOverrides() throws Exception {
		SampleSimpleApplication.main(new String[] { "--name=Gordon" });
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello Gordon"));
	}

}
