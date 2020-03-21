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

package org.springframework.boot.web.servlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;

import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.testcomponents.TestMultipartServlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ServletComponentScan @ServletComponentScan}
 *
 * @author Andy Wilkinson
 */
class ServletComponentScanIntegrationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void componentsAreRegistered() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(TestConfiguration.class);
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		String port = this.context.getEnvironment().getProperty("local.server.port");
		String response = new RestTemplate().getForObject("http://localhost:" + port + "/test", String.class);
		assertThat(response).isEqualTo("alpha bravo");
	}

	@Test
	void indexedComponentsAreRegistered(@TempDir File temp) throws IOException {
		writeIndex(temp);
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { temp.toURI().toURL() },
				getClass().getClassLoader())) {
			this.context.setClassLoader(classLoader);
			this.context.register(TestConfiguration.class);
			new ServerPortInfoApplicationContextInitializer().initialize(this.context);
			this.context.refresh();
			String port = this.context.getEnvironment().getProperty("local.server.port");
			String response = new RestTemplate().getForObject("http://localhost:" + port + "/test", String.class);
			assertThat(response).isEqualTo("alpha bravo");
		}
	}

	@Test
	void multipartConfigIsHonoured() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(TestConfiguration.class);
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		@SuppressWarnings("rawtypes")
		Map<String, ServletRegistrationBean> beans = this.context.getBeansOfType(ServletRegistrationBean.class);
		ServletRegistrationBean<?> servletRegistrationBean = beans.get(TestMultipartServlet.class.getName());
		assertThat(servletRegistrationBean).isNotNull();
		MultipartConfigElement multipartConfig = servletRegistrationBean.getMultipartConfig();
		assertThat(multipartConfig).isNotNull();
		assertThat(multipartConfig.getLocation()).isEqualTo("test");
		assertThat(multipartConfig.getMaxRequestSize()).isEqualTo(2048);
		assertThat(multipartConfig.getMaxFileSize()).isEqualTo(1024);
		assertThat(multipartConfig.getFileSizeThreshold()).isEqualTo(512);
	}

	private void writeIndex(File temp) throws IOException {
		File metaInf = new File(temp, "META-INF");
		metaInf.mkdirs();
		Properties index = new Properties();
		index.setProperty("org.springframework.boot.web.servlet.testcomponents.TestFilter", WebFilter.class.getName());
		index.setProperty("org.springframework.boot.web.servlet.testcomponents.TestListener",
				WebListener.class.getName());
		index.setProperty("org.springframework.boot.web.servlet.testcomponents.TestServlet",
				WebServlet.class.getName());
		try (FileWriter writer = new FileWriter(new File(metaInf, "spring.components"))) {
			index.store(writer, null);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ServletComponentScan(basePackages = "org.springframework.boot.web.servlet.testcomponents")
	static class TestConfiguration {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

}
