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

package org.springframework.boot.cli.util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.junit.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceUtils}.
 *
 * @author Dave Syer
 */
public class ResourceUtilsTests {

	@Test
	public void explicitClasspathResource() {
		List<String> urls = ResourceUtils.getUrls("classpath:init.groovy", ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void duplicateResource() throws Exception {
		URLClassLoader loader = new URLClassLoader(new URL[] { new URL("file:./src/test/resources/"),
				new File("src/test/resources/").getAbsoluteFile().toURI().toURL() });
		List<String> urls = ResourceUtils.getUrls("classpath:init.groovy", loader);
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void explicitClasspathResourceWithSlash() {
		List<String> urls = ResourceUtils.getUrls("classpath:/init.groovy", ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void implicitClasspathResource() {
		List<String> urls = ResourceUtils.getUrls("init.groovy", ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void implicitClasspathResourceWithSlash() {
		List<String> urls = ResourceUtils.getUrls("/init.groovy", ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void nonexistentClasspathResource() {
		List<String> urls = ResourceUtils.getUrls("classpath:nonexistent.groovy", null);
		assertThat(urls).isEmpty();
	}

	@Test
	public void explicitFile() {
		List<String> urls = ResourceUtils.getUrls("file:src/test/resources/init.groovy",
				ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void implicitFile() {
		List<String> urls = ResourceUtils.getUrls("src/test/resources/init.groovy", ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void nonexistentFile() {
		List<String> urls = ResourceUtils.getUrls("file:nonexistent.groovy", null);
		assertThat(urls).isEmpty();
	}

	@Test
	public void recursiveFiles() {
		List<String> urls = ResourceUtils.getUrls("src/test/resources/dir-sample", ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void recursiveFilesByPatternWithPrefix() {
		List<String> urls = ResourceUtils.getUrls("file:src/test/resources/dir-sample/**/*.groovy",
				ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void recursiveFilesByPattern() {
		List<String> urls = ResourceUtils.getUrls("src/test/resources/dir-sample/**/*.groovy",
				ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void directoryOfFilesWithPrefix() {
		List<String> urls = ResourceUtils.getUrls("file:src/test/resources/dir-sample/code/*",
				ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

	@Test
	public void directoryOfFiles() {
		List<String> urls = ResourceUtils.getUrls("src/test/resources/dir-sample/code/*",
				ClassUtils.getDefaultClassLoader());
		assertThat(urls).hasSize(1);
		assertThat(urls.get(0).startsWith("file:")).isTrue();
	}

}
