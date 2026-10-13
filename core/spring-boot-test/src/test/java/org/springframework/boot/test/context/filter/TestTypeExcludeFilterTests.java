/*
 * Copyright 2012-present the original author or authors.
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

	private final TestTypeExcludeFilter filter = new TestTypeExcludeFilter();

	private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

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
				this.metadataReaderFactory))
			.isTrue();
	}

	@Test
	void matchesNestedConfigurationClassWithoutTestMethodsIfItHasExtendWith() throws Exception {
		assertThat(this.filter.match(getMetadataReader(AbstractJupiterTestWithConfigAndExtendWith.Config.class),
				this.metadataReaderFactory))
			.isTrue();
	}

	@Test
	void matchesNestedConfigurationClassWithoutTestMethodsIfItHasTestable() throws Exception {
		assertThat(this.filter.match(getMetadataReader(AbstractJupiterTestWithConfigAndTestable.Config.class),
				this.metadataReaderFactory))
			.isTrue();
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
				this.metadataReaderFactory))
			.isTrue();
	}

	private MetadataReader getMetadataReader(Class<?> source) throws IOException {
		return this.metadataReaderFactory.getMetadataReader(source.getName());
	}

	@Configuration(proxyBeanMethods = false)
	static class NestedConfig {

	}

}
