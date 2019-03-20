/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.rest;

import java.net.URI;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.city.City;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RepositoryRestMvcAutoConfiguration}.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class RepositoryRestMvcAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		load(TestConfiguration.class);
		assertThat(this.context.getBean(RepositoryRestMvcConfiguration.class))
				.isNotNull();
	}

	@Test
	public void testWithCustomBasePath() throws Exception {
		load(TestConfiguration.class, "spring.data.rest.base-path:foo");
		assertThat(this.context.getBean(RepositoryRestMvcConfiguration.class))
				.isNotNull();
		RepositoryRestConfiguration bean = this.context
				.getBean(RepositoryRestConfiguration.class);
		URI expectedUri = URI.create("/foo");
		assertThat(bean.getBaseUri()).as("Custom basePath not set")
				.isEqualTo(expectedUri);
		BaseUri baseUri = this.context.getBean(BaseUri.class);
		assertThat(expectedUri).as("Custom basePath has not been applied to BaseUri bean")
				.isEqualTo(baseUri.getUri());
	}

	@Test
	public void testWithCustomSettings() throws Exception {
		load(TestConfiguration.class, "spring.data.rest.default-page-size:42",
				"spring.data.rest.max-page-size:78",
				"spring.data.rest.page-param-name:_page",
				"spring.data.rest.limit-param-name:_limit",
				"spring.data.rest.sort-param-name:_sort",
				"spring.data.rest.detection-strategy=visibility",
				"spring.data.rest.default-media-type:application/my-json",
				"spring.data.rest.return-body-on-create:false",
				"spring.data.rest.return-body-on-update:false",
				"spring.data.rest.enable-enum-translation:true");
		assertThat(this.context.getBean(RepositoryRestMvcConfiguration.class))
				.isNotNull();
		RepositoryRestConfiguration bean = this.context
				.getBean(RepositoryRestConfiguration.class);
		assertThat(bean.getDefaultPageSize()).isEqualTo(42);
		assertThat(bean.getMaxPageSize()).isEqualTo(78);
		assertThat(bean.getPageParamName()).isEqualTo("_page");
		assertThat(bean.getLimitParamName()).isEqualTo("_limit");
		assertThat(bean.getSortParamName()).isEqualTo("_sort");
		assertThat(bean.getRepositoryDetectionStrategy())
				.isEqualTo(RepositoryDetectionStrategies.VISIBILITY);
		assertThat(bean.getDefaultMediaType())
				.isEqualTo(MediaType.parseMediaType("application/my-json"));
		assertThat(bean.returnBodyOnCreate(null)).isFalse();
		assertThat(bean.returnBodyOnUpdate(null)).isFalse();
		assertThat(bean.isEnableEnumTranslation()).isTrue();
	}

	@Test
	public void testWithCustomConfigurer() {
		load(TestConfigurationWithConfigurer.class,
				"spring.data.rest.detection-strategy=visibility",
				"spring.data.rest.default-media-type:application/my-json");
		assertThat(this.context.getBean(RepositoryRestMvcConfiguration.class))
				.isNotNull();
		RepositoryRestConfiguration bean = this.context
				.getBean(RepositoryRestConfiguration.class);
		assertThat(bean.getRepositoryDetectionStrategy())
				.isEqualTo(RepositoryDetectionStrategies.ALL);
		assertThat(bean.getDefaultMediaType())
				.isEqualTo(MediaType.parseMediaType("application/my-custom-json"));
		assertThat(bean.getMaxPageSize()).isEqualTo(78);
	}

	@Test
	public void backOffWithCustomConfiguration() {
		load(TestConfigurationWithRestMvcConfig.class, "spring.data.rest.base-path:foo");
		assertThat(this.context.getBean(RepositoryRestMvcConfiguration.class))
				.isNotNull();
		RepositoryRestConfiguration bean = this.context
				.getBean(RepositoryRestConfiguration.class);
		assertThat(bean.getBaseUri()).isEqualTo(URI.create(""));
	}

	@Test
	public void objectMappersAreConfiguredUsingObjectMapperBuilder()
			throws JsonProcessingException {
		load(TestConfigurationWithObjectMapperBuilder.class);

		assertThatDateIsFormattedCorrectly("halObjectMapper");
		assertThatDateIsFormattedCorrectly("objectMapper");
	}

	@Test
	public void primaryObjectMapperIsAvailable() {
		load(TestConfiguration.class);
		Map<String, ObjectMapper> objectMappers = this.context
				.getBeansOfType(ObjectMapper.class);
		assertThat(objectMappers.size()).isGreaterThan(1);
		this.context.getBean(ObjectMapper.class);
	}

	public void assertThatDateIsFormattedCorrectly(String beanName)
			throws JsonProcessingException {
		ObjectMapper objectMapper = this.context.getBean(beanName, ObjectMapper.class);
		assertThat(objectMapper.writeValueAsString(new Date(1413387983267L)))
				.isEqualTo("\"2014-10\"");
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		applicationContext.setServletContext(new MockServletContext());
		applicationContext.register(config, BaseConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	@Import(EmbeddedDataSourceConfiguration.class)
	@ImportAutoConfiguration({ HibernateJpaAutoConfiguration.class,
			JpaRepositoriesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class,
			RepositoryRestMvcAutoConfiguration.class, JacksonAutoConfiguration.class })
	protected static class BaseConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	@EnableWebMvc
	protected static class TestConfiguration {

	}

	@Import({ TestConfiguration.class, TestRepositoryRestConfigurer.class })
	protected static class TestConfigurationWithConfigurer {

	}

	@Import({ TestConfiguration.class, RepositoryRestMvcConfiguration.class })
	protected static class TestConfigurationWithRestMvcConfig {

	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	@EnableWebMvc
	static class TestConfigurationWithObjectMapperBuilder {

		@Bean
		public Jackson2ObjectMapperBuilder objectMapperBuilder() {
			Jackson2ObjectMapperBuilder objectMapperBuilder = new Jackson2ObjectMapperBuilder();
			objectMapperBuilder.simpleDateFormat("yyyy-MM");
			return objectMapperBuilder;
		}

	}

	static class TestRepositoryRestConfigurer extends RepositoryRestConfigurerAdapter {

		@Override
		public void configureRepositoryRestConfiguration(
				RepositoryRestConfiguration config) {
			config.setRepositoryDetectionStrategy(RepositoryDetectionStrategies.ALL);
			config.setDefaultMediaType(
					MediaType.parseMediaType("application/my-custom-json"));
			config.setMaxPageSize(78);
		}

	}

}
