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

package org.springframework.boot.context.properties;

import java.io.IOException;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.scan.valid.a.AScanConfiguration;
import org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration.BFirstProperties;
import org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration.BProperties;
import org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration.BSecondProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link ConfigurationPropertiesScan @ConfigurationPropertiesScan}.
 *
 * @author Madhura Bhave
 * @author Johnny Lim
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesScanTests {

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void setup() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@AfterEach
	void teardown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void scanImportBeanRegistrarShouldBeEnvironmentAwareWithRequiredProfile() {
		this.context.getEnvironment().addActiveProfile("test");
		load(TestConfiguration.class);
		assertThat(this.context.containsBean(
				"profile-org.springframework.boot.context.properties.scan.valid.a.AScanConfiguration$MyProfileProperties"))
						.isTrue();
	}

	@Test
	void scanImportBeanRegistrarShouldBeEnvironmentAwareWithoutRequiredProfile() {
		load(TestConfiguration.class);
		assertThat(this.context.containsBean(
				"profile-org.springframework.boot.context.properties.scan.valid.a.AScanConfiguration$MyProfileProperties"))
						.isFalse();
	}

	@Test
	void scanImportBeanRegistrarShouldBeResourceLoaderAwareWithRequiredResource() {
		DefaultResourceLoader resourceLoader = mock(DefaultResourceLoader.class);
		this.context.setResourceLoader(resourceLoader);
		willCallRealMethod().given(resourceLoader).getClassLoader();
		given(resourceLoader.getResource("test")).willReturn(new ByteArrayResource("test".getBytes()));
		load(TestConfiguration.class);
		assertThat(this.context.containsBean(
				"resource-org.springframework.boot.context.properties.scan.valid.a.AScanConfiguration$MyResourceProperties"))
						.isTrue();
	}

	@Test
	void scanImportBeanRegistrarShouldBeResourceLoaderAwareWithoutRequiredResource() {
		load(TestConfiguration.class);
		assertThat(this.context.containsBean(
				"resource-org.springframework.boot.context.properties.scan.valid.a.AScanConfiguration$MyResourceProperties"))
						.isFalse();
	}

	@Test
	void scanImportBeanRegistrarShouldUsePackageName() {
		load(TestAnotherPackageConfiguration.class);
		assertThat(this.context.getBeanNamesForType(BProperties.class)).containsOnly(
				"b.first-org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration$BFirstProperties",
				"b.second-org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration$BSecondProperties");
	}

	@Test
	void scanImportBeanRegistrarShouldApplyTypeExcludeFilter() {
		this.context.getBeanFactory().registerSingleton("filter", new ConfigurationPropertiesTestTypeExcludeFilter());
		this.context.register(TestAnotherPackageConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(BProperties.class)).containsOnly(
				"b.first-org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration$BFirstProperties");
	}

	@Test
	void scanShouldBindConfigurationProperties() {
		load(TestAnotherPackageConfiguration.class, "b.first.name=constructor", "b.second.number=42");
		assertThat(this.context.getBean(BFirstProperties.class).getName()).isEqualTo("constructor");
		assertThat(this.context.getBean(BSecondProperties.class).getNumber()).isEqualTo(42);
	}

	private void load(Class<?> configuration, String... inlinedProperties) {
		this.context.register(configuration);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, inlinedProperties);
		this.context.refresh();
	}

	@ConfigurationPropertiesScan(basePackageClasses = AScanConfiguration.class)
	static class TestConfiguration {

	}

	@ConfigurationPropertiesScan(basePackages = "org.springframework.boot.context.properties.scan.valid.b")
	static class TestAnotherPackageConfiguration {

	}

	static class ConfigurationPropertiesTestTypeExcludeFilter extends TypeExcludeFilter {

		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
				throws IOException {
			AssignableTypeFilter typeFilter = new AssignableTypeFilter(BFirstProperties.class);
			return !typeFilter.match(metadataReader, metadataReaderFactory);
		}

		@Override
		public boolean equals(Object o) {
			return (this == o);
		}

		@Override
		public int hashCode() {
			return Objects.hash(42);
		}

	}

}
