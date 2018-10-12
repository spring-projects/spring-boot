/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context;

import java.net.URL;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link FilteredClassLoader}.
 *
 * @author Phillip Webb
 * @author Roy Jacobs
 */
public class FilteredClassLoaderTests {

	private static ClassPathResource TEST_RESOURCE = new ClassPathResource(
			"org/springframework/boot/test/context/FilteredClassLoaderTestsResource.txt");

	@Test
	public void loadClassWhenFilteredOnPackageShouldThrowClassNotFound()
			throws Exception {
		try (FilteredClassLoader classLoader = new FilteredClassLoader(
				FilteredClassLoaderTests.class.getPackage().getName())) {
			assertThatExceptionOfType(ClassNotFoundException.class)
					.isThrownBy(() -> classLoader.loadClass(getClass().getName()));
		}
	}

	@Test
	public void loadClassWhenFilteredOnClassShouldThrowClassNotFound() throws Exception {
		try (FilteredClassLoader classLoader = new FilteredClassLoader(
				FilteredClassLoaderTests.class)) {
			assertThatExceptionOfType(ClassNotFoundException.class)
					.isThrownBy(() -> classLoader.loadClass(getClass().getName()));
		}
	}

	@Test
	public void loadClassWhenNotFilteredShouldLoadClass() throws Exception {
		FilteredClassLoader classLoader = new FilteredClassLoader((className) -> false);
		Class<?> loaded = classLoader.loadClass(getClass().getName());
		assertThat(loaded.getName()).isEqualTo(getClass().getName());
		classLoader.close();
	}

	@Test
	public void loadResourceWhenFilteredOnResourceShouldReturnNotFound()
			throws Exception {
		try (FilteredClassLoader classLoader = new FilteredClassLoader(TEST_RESOURCE)) {
			final URL loaded = classLoader.getResource(TEST_RESOURCE.getPath());
			assertThat(loaded).isNull();
		}
	}

	@Test
	public void loadResourceWhenNotFilteredShouldLoadResource() throws Exception {
		try (FilteredClassLoader classLoader = new FilteredClassLoader(
				(resourceName) -> false)) {
			final URL loaded = classLoader.getResource(TEST_RESOURCE.getPath());
			assertThat(loaded).isNotNull();
		}
	}

}
