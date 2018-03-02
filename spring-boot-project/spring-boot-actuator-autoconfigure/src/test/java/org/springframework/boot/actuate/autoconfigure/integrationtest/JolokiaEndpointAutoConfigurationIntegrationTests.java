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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.ServletEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.jolokia.JolokiaEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JolokiaEndpointAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestPropertySource(properties = "management.endpoints.web.exposure.include=jolokia")
public class JolokiaEndpointAutoConfigurationIntegrationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void jolokiaIsExposed() {
		ResponseEntity<String> response = this.restTemplate
				.getForEntity("/actuator/jolokia", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"agent\"");
		assertThat(response.getBody()).contains("\"request\":{\"type\"");
	}

	@Test
	public void search() {
		ResponseEntity<String> response = this.restTemplate
				.getForEntity("/actuator/jolokia/search/java.lang:*", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("GarbageCollector");
	}

	@Test
	public void read() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(
				"/actuator/jolokia/read/java.lang:type=Memory", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("NonHeapMemoryUsage");
	}

	@Test
	public void list() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(
				"/actuator/jolokia/list/java.lang/type=Memory/attr", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("NonHeapMemoryUsage");
	}

	@Configuration
	@MinimalWebConfiguration
	@Import({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			JolokiaEndpointAutoConfiguration.class, EndpointAutoConfiguration.class,
			WebEndpointAutoConfiguration.class,
			ServletManagementContextAutoConfiguration.class,
			ManagementContextAutoConfiguration.class,
			ServletEndpointManagementContextConfiguration.class })
	protected static class Application {

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ ServletWebServerFactoryAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, ValidationAutoConfiguration.class,
			WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {

	}

}
