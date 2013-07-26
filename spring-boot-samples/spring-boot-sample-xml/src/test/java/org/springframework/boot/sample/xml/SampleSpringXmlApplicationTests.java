/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.sample.xml;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.sample.xml.SampleSpringXmlApplication;

import static org.junit.Assert.assertTrue;

public class SampleSpringXmlApplicationTests {

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
		SampleSpringXmlApplication.main(new String[0]);
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

}
