/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.rest;

import java.net.URI;
import java.util.Date;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.city.City;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link RepositoryRestMvcAutoConfiguration}.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
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
		assertNotNull(this.context.getBean(RepositoryRestMvcConfiguration.class));
	}

	@Test
	public void testWithCustomBaseUri() throws Exception {
		load(TestConfiguration.class, "spring.data.rest.baseUri:foo");
		assertNotNull(this.context.getBean(RepositoryRestMvcConfiguration.class));
		RepositoryRestConfiguration bean = this.context
				.getBean(RepositoryRestConfiguration.class);
		URI expectedUri = URI.create("foo");
		assertEquals("Custom baseURI not set", expectedUri, bean.getBaseUri());
		BaseUri baseUri = this.context.getBean(BaseUri.class);
		assertEquals("Custom baseUri has not been applied to BaseUri bean", expectedUri,
				baseUri.getUri());
	}

	@Test
	public void backOffWithCustomConfiguration() {
		load(TestConfigurationWithRestMvcConfig.class, "spring.data.rest.baseUri:foo");
		assertNotNull(this.context.getBean(RepositoryRestMvcConfiguration.class));
		RepositoryRestConfiguration bean = this.context
				.getBean(RepositoryRestConfiguration.class);
		assertEquals("Custom base URI should not have been set", URI.create(""),
				bean.getBaseUri());
	}

	@Test
	public void propertiesStillAppliedWithCustomBootConfig() {
		load(TestConfigurationWithRestMvcBootConfig.class, "spring.data.rest.baseUri:foo");
		assertNotNull(this.context.getBean(RepositoryRestMvcConfiguration.class));
		RepositoryRestConfiguration bean = this.context
				.getBean(RepositoryRestConfiguration.class);
		assertEquals("Custom base URI should have been set", URI.create("foo"),
				bean.getBaseUri());
	}

	@Test
	public void objectMappersAreConfiguredUsingObjectMapperBuilder()
			throws JsonProcessingException {
		load(TestConfigurationWithObjectMapperBuilder.class);

		assertThatDateIsFormattedCorrectly("halObjectMapper");
		assertThatDateIsFormattedCorrectly("objectMapper");
	}

	public void assertThatDateIsFormattedCorrectly(String beanName)
			throws JsonProcessingException {
		ObjectMapper objectMapper = this.context.getBean(beanName, ObjectMapper.class);

		assertEquals("\"2014-10\"",
				objectMapper.writeValueAsString(new Date(1413387983267L)));
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		applicationContext.setServletContext(new MockServletContext());
		applicationContext.register(config, EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class,
				JpaRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				RepositoryRestMvcAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	@EnableWebMvc
	protected static class TestConfiguration {

	}

	@Import({ TestConfiguration.class, RepositoryRestMvcConfiguration.class })
	protected static class TestConfigurationWithRestMvcConfig {

	}

	@Import({ TestConfiguration.class, RepositoryRestMvcBootConfiguration.class })
	protected static class TestConfigurationWithRestMvcBootConfig {

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

}
