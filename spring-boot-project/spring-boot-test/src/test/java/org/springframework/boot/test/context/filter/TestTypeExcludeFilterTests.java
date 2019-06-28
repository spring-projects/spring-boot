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

package org.springframework.boot.test.context.filter;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestTypeExcludeFilter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TestTypeExcludeFilterTests {

	private TestTypeExcludeFilter filter = new TestTypeExcludeFilter();

	private MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Test
	void matchesJUnit4TestClass() throws Exception {
		assertThat(this.filter.match(getMetadataReader(TestTypeExcludeFilterTests.class), this.metadataReaderFactory))
				.isTrue();
	}

	@Test
	void matchesJUnitJupiterTestClass() throws Exception {
		assertThat(this.filter.match(getMetadataReader(JupiterTestExample.class), this.metadataReaderFactory)).isTrue();
	}

	@Test
	void matchesJUnitJupiterRepeatedTestClass() throws Exception {
		assertThat(this.filter.match(getMetadataReader(JupiterRepeatedTestExample.class), this.metadataReaderFactory))
				.isTrue();
	}

	@Test
	void matchesJUnitJupiterTestFactoryClass() throws Exception {
		assertThat(this.filter.match(getMetadataReader(JupiterTestFactoryExample.class), this.metadataReaderFactory))
				.isTrue();
	}

	@Test
	void matchesNestedConfiguration() throws Exception {
		assertThat(this.filter.match(getMetadataReader(NestedConfig.class), this.metadataReaderFactory)).isTrue();
	}

	@Test
	void matchesNestedConfigurationClassWithoutTestMethodsIfItHasRunWith() throws Exception {
		assertThat(this.filter.match(getMetadataReader(AbstractTestWithConfigAndRunWith.Config.class),
				this.metadataReaderFactory)).isTrue();
	}

	@Test
	void matchesNestedConfigurationClassWithoutTestMethodsIfItHasExtendWith() throws Exception {
		assertThat(this.filter.match(getMetadataReader(AbstractJupiterTestWithConfigAndExtendWith.Config.class),
				this.metadataReaderFactory)).isTrue();
	}

	@Test
	void matchesTestConfiguration() throws Exception {
		assertThat(this.filter.match(getMetadataReader(SampleTestConfig.class), this.metadataReaderFactory)).isTrue();
	}

	@Test
	void doesNotMatchRegularConfiguration() throws Exception {
		assertThat(this.filter.match(getMetadataReader(SampleConfig.class), this.metadataReaderFactory)).isFalse();
	}

	@Test
	void matchesNestedConfigurationClassWithoutTestNgAnnotation() throws Exception {
		assertThat(this.filter.match(getMetadataReader(AbstractTestNgTestWithConfig.Config.class),
				this.metadataReaderFactory)).isTrue();
	}

	private MetadataReader getMetadataReader(Class<?> source) throws IOException {
		return this.metadataReaderFactory.getMetadataReader(source.getName());
	}

	@Configuration(proxyBeanMethods = false)
	static class NestedConfig {

	}

}
