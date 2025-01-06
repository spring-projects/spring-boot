/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.web.servlet.testcomponents.filter.TestFilter;
import org.springframework.boot.web.servlet.testcomponents.listener.TestListener;
import org.springframework.boot.web.servlet.testcomponents.servlet.TestMultipartServlet;
import org.springframework.boot.web.servlet.testcomponents.servlet.TestServlet;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link ServletComponentScan @ServletComponentScan} with a mock
 * web environment.
 *
 * @author Andy Wilkinson
 */
class MockWebEnvironmentServletComponentScanIntegrationTests {

	private AnnotationConfigServletWebApplicationContext context;

	@TempDir
	File temp;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	@ForkedClassPath
	void componentsAreRegistered() {
		prepareContext();
		this.context.refresh();
		Map<String, RegistrationBean> registrationBeans = this.context.getBeansOfType(RegistrationBean.class);
		assertThat(registrationBeans).hasSize(3);
		assertThat(registrationBeans.keySet()).containsExactlyInAnyOrder(TestServlet.class.getName(),
				TestFilter.class.getName(), TestMultipartServlet.class.getName());
		WebListenerRegistry registry = mock(WebListenerRegistry.class);
		this.context.getBean(WebListenerRegistrar.class).register(registry);
		then(registry).should().addWebListeners(TestListener.class.getName());
	}

	@Test
	@ForkedClassPath
	void indexedComponentsAreRegistered() throws IOException {
		writeIndex(this.temp);
		prepareContext();
		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { this.temp.toURI().toURL() },
				getClass().getClassLoader())) {
			this.context.setClassLoader(classLoader);
			this.context.refresh();
			Map<String, RegistrationBean> registrationBeans = this.context.getBeansOfType(RegistrationBean.class);
			assertThat(registrationBeans).hasSize(2);
			assertThat(registrationBeans.keySet()).containsExactlyInAnyOrder(TestServlet.class.getName(),
					TestFilter.class.getName());
			WebListenerRegistry registry = mock(WebListenerRegistry.class);
			this.context.getBean(WebListenerRegistrar.class).register(registry);
			then(registry).should().addWebListeners(TestListener.class.getName());
		}
	}

	@Test
	@ForkedClassPath
	void multipartConfigIsHonoured() {
		prepareContext();
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

	private void prepareContext() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(ScanningConfiguration.class);
		this.context.setServletContext(new MockServletContext());
	}

	@ServletComponentScan(basePackages = "org.springframework.boot.web.servlet.testcomponents")
	static class ScanningConfiguration {

	}

}
