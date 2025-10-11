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

package org.springframework.boot.data.rest.autoconfigure;

import java.net.URI;
import java.text.SimpleDateFormat;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.data.rest.domain.city.City;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRestAutoConfiguration}.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class DataRestAutoConfigurationTests {

	private @Nullable AnnotationConfigServletWebApplicationContext context;

	@AfterEach
	void tearDown() {
		if (getContext() != null) {
			getContext().close();
		}
	}

	@Test
	void testDefaultRepositoryConfiguration() {
		load(TestConfiguration.class);
		assertThat(getContext().getBean(RepositoryRestMvcConfiguration.class)).isNotNull();
	}

	@Test
	void testWithCustomBasePath() {
		load(TestConfiguration.class, "spring.data.rest.base-path:foo");
		assertThat(getContext().getBean(RepositoryRestMvcConfiguration.class)).isNotNull();
		RepositoryRestConfiguration bean = getContext().getBean(RepositoryRestConfiguration.class);
		URI expectedUri = URI.create("/foo");
		assertThat(bean.getBasePath()).as("Custom basePath not set").isEqualTo(expectedUri);
		BaseUri baseUri = getContext().getBean(BaseUri.class);
		assertThat(expectedUri).as("Custom basePath has not been applied to BaseUri bean").isEqualTo(baseUri.getUri());
	}

	@Test
	void testWithCustomSettings() {
		load(TestConfiguration.class, "spring.data.rest.default-page-size:42", "spring.data.rest.max-page-size:78",
				"spring.data.rest.page-param-name:_page", "spring.data.rest.limit-param-name:_limit",
				"spring.data.rest.sort-param-name:_sort", "spring.data.rest.detection-strategy=visibility",
				"spring.data.rest.default-media-type:application/my-json",
				"spring.data.rest.return-body-on-create:false", "spring.data.rest.return-body-on-update:false",
				"spring.data.rest.enable-enum-translation:true");
		assertThat(getContext().getBean(RepositoryRestMvcConfiguration.class)).isNotNull();
		RepositoryRestConfiguration bean = getContext().getBean(RepositoryRestConfiguration.class);
		assertThat(bean.getDefaultPageSize()).isEqualTo(42);
		assertThat(bean.getMaxPageSize()).isEqualTo(78);
		assertThat(bean.getPageParamName()).isEqualTo("_page");
		assertThat(bean.getLimitParamName()).isEqualTo("_limit");
		assertThat(bean.getSortParamName()).isEqualTo("_sort");
		assertThat(bean.getRepositoryDetectionStrategy()).isEqualTo(RepositoryDetectionStrategies.VISIBILITY);
		assertThat(bean.getDefaultMediaType()).isEqualTo(MediaType.parseMediaType("application/my-json"));
		assertReturnBody(bean);
		assertThat(bean.isEnableEnumTranslation()).isTrue();
	}

	// https://github.com/spring-projects/spring-data-rest/issues/2515
	// https://github.com/spring-projects/spring-data-rest/issues/2516
	@SuppressWarnings("NullAway")
	private void assertReturnBody(RepositoryRestConfiguration bean) {
		assertThat(bean.returnBodyOnCreate(null)).isFalse();
		assertThat(bean.returnBodyOnUpdate(null)).isFalse();
	}

	@Test
	void testWithCustomConfigurer() {
		load(TestConfigurationWithConfigurer.class, "spring.data.rest.detection-strategy=visibility",
				"spring.data.rest.default-media-type:application/my-json");
		assertThat(getContext().getBean(RepositoryRestMvcConfiguration.class)).isNotNull();
		RepositoryRestConfiguration bean = getContext().getBean(RepositoryRestConfiguration.class);
		assertThat(bean.getRepositoryDetectionStrategy()).isEqualTo(RepositoryDetectionStrategies.ALL);
		assertThat(bean.getDefaultMediaType()).isEqualTo(MediaType.parseMediaType("application/my-custom-json"));
		assertThat(bean.getMaxPageSize()).isEqualTo(78);
	}

	@Test
	void backOffWithCustomConfiguration() {
		load(TestConfigurationWithRestMvcConfig.class, "spring.data.rest.base-path:foo");
		assertThat(getContext().getBean(RepositoryRestMvcConfiguration.class)).isNotNull();
		RepositoryRestConfiguration bean = getContext().getBean(RepositoryRestConfiguration.class);
		assertThat(bean.getBasePath()).isEqualTo(URI.create(""));
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigServletWebApplicationContext applicationContext = new AnnotationConfigServletWebApplicationContext();
		applicationContext.setServletContext(new MockServletContext());
		applicationContext.register(config, BaseConfiguration.class);
		TestPropertyValues.of(environment).applyTo(applicationContext);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	private AnnotationConfigServletWebApplicationContext getContext() {
		AnnotationConfigServletWebApplicationContext context = this.context;
		assertThat(context).isNotNull();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	@Import(EmbeddedDataSourceConfiguration.class)
	@ImportAutoConfiguration({ HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, DataRestAutoConfiguration.class,
			JacksonAutoConfiguration.class })
	static class BaseConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@EnableWebMvc
	static class TestConfiguration {

	}

	@Import({ TestConfiguration.class, TestRepositoryRestConfigurer.class })
	static class TestConfigurationWithConfigurer {

	}

	@Import({ TestConfiguration.class, RepositoryRestMvcConfiguration.class })
	static class TestConfigurationWithRestMvcConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@EnableWebMvc
	static class TestConfigurationWithJsonMapperBuilder {

		@Bean
		JsonMapper.Builder jsonMapperBuilder() {
			return JsonMapper.builder().defaultDateFormat(new SimpleDateFormat("yyyy-MM"));
		}

	}

	static class TestRepositoryRestConfigurer implements RepositoryRestConfigurer {

		@Override
		public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
			config.setRepositoryDetectionStrategy(RepositoryDetectionStrategies.ALL);
			config.setDefaultMediaType(MediaType.parseMediaType("application/my-custom-json"));
			config.setMaxPageSize(78);
		}

	}

}
