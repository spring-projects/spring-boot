/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
public class MockitoContextCustomizerFactoryTests {

	private final MockitoContextCustomizerFactory factory = new MockitoContextCustomizerFactory();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void getContextCustomizerWithoutAnnotationReturnsCustomizer()
			throws Exception {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(NoMockBeanAnnotation.class, null);
		assertThat(customizer).isNotNull();
	}

	@Test
	public void getContextCustomizerWithAnnotationReturnsCustomizer() throws Exception {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(WithMockBeanAnnotation.class, null);
		assertThat(customizer).isNotNull();
	}

	@Test
	public void getContextCustomizerUsesMocksAsCacheKey() throws Exception {
		ContextCustomizer customizer = this.factory
				.createContextCustomizer(WithMockBeanAnnotation.class, null);
		assertThat(customizer).isNotNull();
		ContextCustomizer same = this.factory
				.createContextCustomizer(WithSameMockBeanAnnotation.class, null);
		assertThat(customizer).isNotNull();
		ContextCustomizer different = this.factory
				.createContextCustomizer(WithDifferentMockBeanAnnotation.class, null);
		assertThat(different).isNotNull();
		assertThat(customizer.hashCode()).isEqualTo(same.hashCode());
		assertThat(customizer.hashCode()).isNotEqualTo(different.hashCode());
		assertThat(customizer).isEqualTo(customizer);
		assertThat(customizer).isEqualTo(same);
		assertThat(customizer).isNotEqualTo(different);
	}

	static class NoMockBeanAnnotation {

	}

	@MockBean({ Service1.class, Service2.class })
	static class WithMockBeanAnnotation {

	}

	@MockBean({ Service2.class, Service1.class })
	static class WithSameMockBeanAnnotation {

	}

	@MockBean({ Service1.class })
	static class WithDifferentMockBeanAnnotation {

	}

	interface Service1 {

	}

	interface Service2 {

	}

}
