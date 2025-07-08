/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context.config;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.ResourcePath;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StandardConfigDataResource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class StandardConfigDataResourceTests {

	StandardConfigDataReference reference = mock(StandardConfigDataReference.class);

	private final Resource resource = mock(Resource.class);

	@Test
	void createWhenReferenceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new StandardConfigDataResource(null, this.resource))
			.withMessage("'reference' must not be null");
	}

	@Test
	void createWhenResourceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new StandardConfigDataResource(this.reference, null))
			.withMessage("'resource' must not be null");
	}

	@Test
	void equalsWhenResourceIsTheSameReturnsTrue() {
		Resource resource = new ClassPathResource("config/");
		StandardConfigDataResource location = new StandardConfigDataResource(this.reference, resource);
		StandardConfigDataResource other = new StandardConfigDataResource(this.reference, resource);
		assertThat(location).isEqualTo(other);
	}

	@Test
	void equalsWhenResourceIsDifferentReturnsFalse() {
		Resource resource1 = new ClassPathResource("config/");
		Resource resource2 = new ClassPathResource("configdata/");
		StandardConfigDataResource location = new StandardConfigDataResource(this.reference, resource1);
		StandardConfigDataResource other = new StandardConfigDataResource(this.reference, resource2);
		assertThat(location).isNotEqualTo(other);
	}

	@Test // gh-34212
	@WithResource(name = "test.resource", content = "test")
	void equalsAndHashCodeWhenSameUnderlyingResource(@ResourcePath("test.resource") Path path) throws IOException {
		Path directory = path.getParent();
		URLClassLoader classLoader = new URLClassLoader(new URL[] { directory.toUri().toURL() },
				getClass().getClassLoader());
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			ClassPathResource classResource = new ClassPathResource("test.resource", classLoader);
			FileUrlResource fileResource = new FileUrlResource(classResource.getURL());
			ConfigDataResource classDataResource = new StandardConfigDataResource(this.reference, classResource);
			ConfigDataResource fileDataResource = new StandardConfigDataResource(this.reference, fileResource);
			assertThat(classDataResource).isEqualTo(fileDataResource);
			assertThat(classDataResource).hasSameHashCodeAs(fileDataResource);
		}
		finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

}
