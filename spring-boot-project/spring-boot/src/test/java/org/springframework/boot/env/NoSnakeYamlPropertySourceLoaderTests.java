/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.env;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.core.io.ByteArrayResource;

/**
 * Tests for {@link YamlPropertySourceLoader} when snakeyaml is not available.
 *
 * @author Madhura Bhave
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("snakeyaml-*.jar")
public class NoSnakeYamlPropertySourceLoaderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

	@Test
	public void load() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Attempted to load resource but snakeyaml was not found on the classpath");
		ByteArrayResource resource = new ByteArrayResource("foo:\n  bar: spam".getBytes());
		this.loader.load("resource", resource);
	}

}
