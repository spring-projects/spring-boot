/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link InputArgumentsJavaAgentDetector}
 * 
 * @author Andy Wilkinson
 */
public class InputArgumentsJavaAgentDetectorTests {

	@Test
	public void nonAgentJarsDoNotProduceFalsePositives() throws MalformedURLException,
			IOException {
		InputArgumentsJavaAgentDetector detector = new InputArgumentsJavaAgentDetector(
				Arrays.asList("-javaagent:my-agent.jar"));
		assertFalse(detector.isJavaAgentJar(new File("something-else.jar")
				.getCanonicalFile().toURI().toURL()));
	}

	@Test
	public void singleJavaAgent() throws MalformedURLException, IOException {
		InputArgumentsJavaAgentDetector detector = new InputArgumentsJavaAgentDetector(
				Arrays.asList("-javaagent:my-agent.jar"));
		assertTrue(detector.isJavaAgentJar(new File("my-agent.jar").getCanonicalFile()
				.toURI().toURL()));
	}

	@Test
	public void singleJavaAgentWithOptions() throws MalformedURLException, IOException {
		InputArgumentsJavaAgentDetector detector = new InputArgumentsJavaAgentDetector(
				Arrays.asList("-javaagent:my-agent.jar=a=alpha,b=bravo"));
		assertTrue(detector.isJavaAgentJar(new File("my-agent.jar").getCanonicalFile()
				.toURI().toURL()));
	}

	@Test
	public void multipleJavaAgents() throws MalformedURLException, IOException {
		InputArgumentsJavaAgentDetector detector = new InputArgumentsJavaAgentDetector(
				Arrays.asList("-javaagent:my-agent.jar", "-javaagent:my-other-agent.jar"));
		assertTrue(detector.isJavaAgentJar(new File("my-agent.jar").getCanonicalFile()
				.toURI().toURL()));
		assertTrue(detector.isJavaAgentJar(new File("my-other-agent.jar")
				.getCanonicalFile().toURI().toURL()));
	}

}
