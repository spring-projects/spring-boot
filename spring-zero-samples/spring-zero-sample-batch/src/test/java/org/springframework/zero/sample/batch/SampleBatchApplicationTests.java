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

package org.springframework.zero.sample.batch;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.zero.SpringApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SampleBatchApplicationTests {

	private PrintStream savedOutput;
	private ByteArrayOutputStream output;

	@Before
	public void init() {
		this.savedOutput = System.err;
		this.output = new ByteArrayOutputStream();
		// jdk logging goes to syserr by default
		System.setErr(new PrintStream(this.output));
	}

	@After
	public void after() {
		System.setErr(this.savedOutput);
	}

	private String getOutput() {
		return this.output.toString();
	}

	@Test
	public void testDefaultSettings() throws Exception {
		assertEquals(0, SpringApplication.exit(SpringApplication
				.run(SampleBatchApplication.class)));
		String output = getOutput();
		assertTrue("Wrong output: " + output,
				output.contains("completed with the following parameters"));
	}

}
