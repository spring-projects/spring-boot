/*
 * Copyright 2012-2025 the original author or authors.
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

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.MockServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.testcomponents.filter.TestFilter;
import org.springframework.boot.web.servlet.testcomponents.listener.TestListener;
import org.springframework.boot.web.servlet.testcomponents.servlet.TestMultipartServlet;
import org.springframework.boot.web.servlet.testcomponents.servlet.TestServlet;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ServletComponentScan @ServletComponentScan}
 *
 * @author Andy Wilkinson
 */
class ServletComponentScanIntegrationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@TempDir
	File temp;

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
		this.context.refresh();
		assertThat(this.context.getServletContext().getFilterRegistrations()).hasSize(1)
			.containsKey(TestFilter.class.getName());
		assertThat(this.context.getServletContext().getServletRegistrations()).hasSize(2)
			.containsKeys(TestServlet.class.getName(), TestMultipartServlet.class.getName());
		assertThat(this.context.getBean(MockServletWebServerFactory.class).getSettings().getWebListenerClassNames())
			.containsExactly(TestListener.class.getName());
	}

	@Test
	void indexedComponentsAreRegistered() throws IOException {
		writeIndex(this.temp);
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { this.temp.toURI().toURL() },
				getClass().getClassLoader())) {
			this.context.setClassLoader(classLoader);
			this.context.register(TestConfiguration.class);
			this.context.refresh();
			assertThat(this.context.getServletContext().getFilterRegistrations()).hasSize(1)
				.containsKey(TestFilter.class.getName());
			assertThat(this.context.getServletContext().getServletRegistrations()).hasSize(1)
				.containsKeys(TestServlet.class.getName());
			assertThat(this.context.getBean(MockServletWebServerFactory.class).getSettings().getWebListenerClassNames())
				.containsExactly(TestListener.class.getName());
		}
	}

	@Test
	void multipartConfigIsHonoured() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(TestConfiguration.class);
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
		index.setProperty(TestFilter.class.getName(), WebFilter.class.getName());
		index.setProperty(TestListener.class.getName(), WebListener.class.getName());
		index.setProperty(TestServlet.class.getName(), WebServlet.class.getName());
		try (FileWriter writer = new FileWriter(new File(metaInf, "spring.components"))) {
			index.store(writer, null);
		}
	}

	@ServletComponentScan(basePackages = "org.springframework.boot.web.servlet.testcomponents")
	static class TestConfiguration {

		@Bean
		protected ServletWebServerFactory webServerFactory(ObjectProvider<WebListenerRegistrar> webListenerRegistrars) {
			ConfigurableServletWebServerFactory factory = new MockServletWebServerFactory();
			webListenerRegistrars.orderedStream().forEach((registrar) -> registrar.register(factory));
			return factory;
		}

	}

}
