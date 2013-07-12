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

package org.springframework.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SpringApplication} main method.
 * 
 * @author Dave Syer
 */
@Configuration
public class SimpleMainTests {

	private PrintStream savedOutput;
	private ByteArrayOutputStream output;

	@Before
	public void open() {
		this.savedOutput = System.out;
		this.output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(this.output));
	}

	@After
	public void after() {
		System.setOut(this.savedOutput);
		System.out.println(getOutput());
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyApplicationContext() throws Exception {
		SpringApplication.main(getArgs());
		assertTrue(getOutput().contains("Pre-instantiating singletons"));
	}

	@Test
	public void basePackageScan() throws Exception {
		SpringApplication.main(getArgs(ClassUtils.getPackageName(getClass())
				+ ".sampleconfig"));
		assertTrue(getOutput().contains("Pre-instantiating singletons"));
	}

	@Test
	public void configClassContext() throws Exception {
		SpringApplication.main(getArgs(getClass().getName()));
		assertTrue(getOutput().contains("Pre-instantiating singletons"));
	}

	@Test
	public void xmlContext() throws Exception {
		SpringApplication.main(getArgs("org/springframework/bootstrap/sample-beans.xml"));
		assertTrue(getOutput().contains("Pre-instantiating singletons"));
	}

	@Test
	public void mixedContext() throws Exception {
		SpringApplication.main(getArgs(getClass().getName(),
				"org/springframework/bootstrap/sample-beans.xml"));
		assertTrue(getOutput().contains("Pre-instantiating singletons"));
	}

	private String[] getArgs(String... args) {
		List<String> list = new ArrayList<String>(Arrays.asList(
				"--spring.main.webEnvironment=false", "--spring.main.showBanner=false"));
		if (args.length > 0) {
			list.add("--spring.main.sources="
					+ StringUtils.arrayToCommaDelimitedString(args));
		}
		return list.toArray(new String[list.size()]);
	}

	private String getOutput() {
		return this.output.toString();
	}

}
