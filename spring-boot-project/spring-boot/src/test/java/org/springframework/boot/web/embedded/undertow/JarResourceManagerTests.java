/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarResourceManager}.
 *
 * @author Andy Wilkinson
 */
class JarResourceManagerTests {

	@TempDir
	static File tempDir;

	@ResourceManagersTest
	void emptyPathIsHandledCorrectly(String filename, ResourceManager resourceManager) throws IOException {
		Resource resource = resourceManager.getResource("");
		assertThat(resource).isNotNull();
		assertThat(resource.isDirectory()).isTrue();
	}

	@ResourceManagersTest
	void rootPathIsHandledCorrectly(String filename, ResourceManager resourceManager) throws IOException {
		Resource resource = resourceManager.getResource("/");
		assertThat(resource).isNotNull();
		assertThat(resource.isDirectory()).isTrue();
	}

	@ResourceManagersTest
	void resourceIsFoundInJarFile(String filename, ResourceManager resourceManager) throws IOException {
		Resource resource = resourceManager.getResource("/hello.txt");
		assertThat(resource).isNotNull();
		assertThat(resource.isDirectory()).isFalse();
		assertThat(resource.getContentLength()).isEqualTo(5);
	}

	@ResourceManagersTest
	void resourceIsFoundInJarFileWithoutLeadingSlash(String filename, ResourceManager resourceManager)
			throws IOException {
		Resource resource = resourceManager.getResource("hello.txt");
		assertThat(resource).isNotNull();
		assertThat(resource.isDirectory()).isFalse();
		assertThat(resource.getContentLength()).isEqualTo(5);
	}

	static List<Arguments> resourceManagers() throws IOException {
		File jar = new File(tempDir, "test.jar");
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
			out.putNextEntry(new ZipEntry("hello.txt"));
			out.write("hello".getBytes());
		}
		File troublesomeNameJar = new File(tempDir, "test##1.0.jar");
		FileCopyUtils.copy(jar, troublesomeNameJar);
		return Arrays.asList(Arguments.of(jar.getName(), new JarResourceManager(jar)),
				Arguments.of(troublesomeNameJar.getName(), new JarResourceManager(troublesomeNameJar)));
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("resourceManagers")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface ResourceManagersTest {

	}

}
