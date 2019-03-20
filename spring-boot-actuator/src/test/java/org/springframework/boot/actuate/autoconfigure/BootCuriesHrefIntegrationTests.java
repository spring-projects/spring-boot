/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.net.URL;

import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the href of the actuator's curies link
 *
 * @author Andy Wilkinson
 */
public class BootCuriesHrefIntegrationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void closeContext() {
		this.context.close();
	}

	@Test
	public void basicCuriesHref() {
		int port = load("endpoints.docs.curies.enabled:true", "server.port:0");
		assertThat(getCurieHref("http://localhost:" + port + "/actuator")).isEqualTo(
				"http://localhost:" + port + "/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void curiesHrefWithCustomContextPath() {
		int port = load("endpoints.docs.curies.enabled:true", "server.port:0",
				"server.context-path:/context");
		assertThat(getCurieHref("http://localhost:" + port + "/context/actuator"))
				.isEqualTo("http://localhost:" + port
						+ "/context/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void curiesHrefWithCustomServletPath() {
		int port = load("endpoints.docs.curies.enabled:true", "server.port:0",
				"server.servlet-path:/servlet");
		assertThat(getCurieHref("http://localhost:" + port + "/servlet/actuator"))
				.isEqualTo("http://localhost:" + port
						+ "/servlet/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void curiesHrefWithCustomServletAndContextPaths() {
		int port = load("endpoints.docs.curies.enabled:true", "server.port:0",
				"server.context-path:/context", "server.servlet-path:/servlet");
		assertThat(getCurieHref("http://localhost:" + port + "/context/servlet/actuator"))
				.isEqualTo("http://localhost:" + port
						+ "/context/servlet/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void curiesHrefWithCustomServletContextAndManagementContextPaths() {
		int port = load("endpoints.docs.curies.enabled:true", "server.port:0",
				"server.context-path:/context", "server.servlet-path:/servlet",
				"management.context-path:/management");
		assertThat(getCurieHref("http://localhost:" + port
				+ "/context/servlet/management/")).isEqualTo("http://localhost:" + port
						+ "/context/servlet/management/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void serverPathsAreIgnoredWithSeparateManagementPort() {
		int port = load("endpoints.docs.curies.enabled:true", "server.port:0",
				"server.context-path:/context", "server.servlet-path:/servlet",
				"management.port:0");
		assertThat(getCurieHref("http://localhost:" + port + "/actuator/")).isEqualTo(
				"http://localhost:" + port + "/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void managementContextPathWithSeparateManagementPort() {
		int port = load("endpoints.docs.curies.enabled:true",
				"management.context-path:/management", "server.port:0",
				"management.port:0");
		assertThat(getCurieHref("http://localhost:" + port + "/management/"))
				.isEqualTo("http://localhost:" + port
						+ "/management/docs/#spring_boot_actuator__{rel}");
	}

	private int load(String... properties) {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.setClassLoader(new ClassLoader(getClass().getClassLoader()) {

			@Override
			public URL getResource(String name) {
				if ("META-INF/resources/spring-boot-actuator/docs/index.html"
						.equals(name)) {
					return super.getResource("actuator-docs-index.html");
				}
				return super.getResource(name);
			}

		});
		EnvironmentTestUtils.addEnvironment(this.context, properties);
		this.context.register(TestConfiguration.class);
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		return Integer.parseInt(
				this.context.getEnvironment().getProperty("local.management.port"));
	}

	private String getCurieHref(String uri) {
		ResponseEntity<String> response = new TestRestTemplate().getForEntity(uri,
				String.class);
		JSONArray bootCuriesHrefs = JsonPath.parse(response.getBody())
				.read("_links.curies[?(@.name == 'boot')].href");
		assertThat(bootCuriesHrefs).hasSize(1);
		return (String) bootCuriesHrefs.get(0);
	}

	@Configuration
	@MinimalActuatorHypermediaApplication
	static class TestConfiguration {

	}

}
