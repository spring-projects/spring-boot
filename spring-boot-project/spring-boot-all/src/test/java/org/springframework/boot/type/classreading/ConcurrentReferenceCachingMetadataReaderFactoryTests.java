/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.type.classreading;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;
import org.springframework.core.type.classreading.MetadataReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link ConcurrentReferenceCachingMetadataReaderFactory}.
 *
 * @author Phillip Webb
 */
class ConcurrentReferenceCachingMetadataReaderFactoryTests {

	@Test
	void getMetadataReaderUsesCache() throws Exception {
		TestConcurrentReferenceCachingMetadataReaderFactory factory = spy(
				new TestConcurrentReferenceCachingMetadataReaderFactory());
		MetadataReader metadataReader1 = factory.getMetadataReader(getClass().getName());
		MetadataReader metadataReader2 = factory.getMetadataReader(getClass().getName());
		assertThat(metadataReader1).isSameAs(metadataReader2);
		then(factory).should().createMetadataReader(any(Resource.class));
	}

	@Test
	void clearResetsCache() throws Exception {
		TestConcurrentReferenceCachingMetadataReaderFactory factory = spy(
				new TestConcurrentReferenceCachingMetadataReaderFactory());
		MetadataReader metadataReader1 = factory.getMetadataReader(getClass().getName());
		factory.clearCache();
		MetadataReader metadataReader2 = factory.getMetadataReader(getClass().getName());
		assertThat(metadataReader1).isNotSameAs(metadataReader2);
		then(factory).should(times(2)).createMetadataReader(any(Resource.class));
	}

	static class TestConcurrentReferenceCachingMetadataReaderFactory
			extends ConcurrentReferenceCachingMetadataReaderFactory {

		@Override
		public MetadataReader createMetadataReader(Resource resource) {
			return mock(MetadataReader.class);
		}

	}

}
