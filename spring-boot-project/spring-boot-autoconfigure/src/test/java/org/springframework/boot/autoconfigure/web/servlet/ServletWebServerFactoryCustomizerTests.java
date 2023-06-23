/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.Jsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServletWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Yunkun Huang
 */
class ServletWebServerFactoryCustomizerTests {

	private final ServerProperties properties = new ServerProperties();

	private ServletWebServerFactoryCustomizer customizer;

	@BeforeEach
	void setup() {
		this.customizer = new ServletWebServerFactoryCustomizer(this.properties);
	}

	@Test
	void testDefaultDisplayName() {
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setDisplayName("application");
	}

	@Test
	void testCustomizeDisplayName() {
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.properties.getServlet().setApplicationDisplayName("TestName");
		this.customizer.customize(factory);
		then(factory).should().setDisplayName("TestName");
	}

	@Test
	void testCustomizeDefaultServlet() {
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.properties.getServlet().setRegisterDefaultServlet(false);
		this.customizer.customize(factory);
		then(factory).should().setRegisterDefaultServlet(false);
	}

	@Test
	void testCustomizeSsl() {
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		Ssl ssl = mock(Ssl.class);
		this.properties.setSsl(ssl);
		this.customizer.customize(factory);
		then(factory).should().setSsl(ssl);
	}

	@Test
	void testCustomizeJsp() {
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setJsp(any(Jsp.class));
	}

	@Test
	void customizeSessionProperties() {
		Map<String, String> map = new HashMap<>();
		map.put("server.servlet.session.timeout", "123");
		map.put("server.servlet.session.tracking-modes", "cookie,url");
		map.put("server.servlet.session.cookie.name", "testname");
		map.put("server.servlet.session.cookie.domain", "testdomain");
		map.put("server.servlet.session.cookie.path", "/testpath");
		map.put("server.servlet.session.cookie.http-only", "true");
		map.put("server.servlet.session.cookie.secure", "true");
		map.put("server.servlet.session.cookie.max-age", "60");
		bindProperties(map);
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setSession(assertArg((session) -> {
			assertThat(session.getTimeout()).hasSeconds(123);
			Cookie cookie = session.getCookie();
			assertThat(cookie.getName()).isEqualTo("testname");
			assertThat(cookie.getDomain()).isEqualTo("testdomain");
			assertThat(cookie.getPath()).isEqualTo("/testpath");
			assertThat(cookie.getHttpOnly()).isTrue();
			assertThat(cookie.getMaxAge()).hasSeconds(60);
		}));
	}

	@Test
	void testCustomizeTomcatPort() {
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.properties.setPort(8080);
		this.customizer.customize(factory);
		then(factory).should().setPort(8080);
	}

	@Test
	void customizeServletDisplayName() {
		Map<String, String> map = new HashMap<>();
		map.put("server.servlet.application-display-name", "MyBootApp");
		bindProperties(map);
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setDisplayName("MyBootApp");
	}

	@Test
	void testCustomizeTomcatMinSpareThreads() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.threads.min-spare", "10");
		bindProperties(map);
		assertThat(this.properties.getTomcat().getThreads().getMinSpare()).isEqualTo(10);
	}

	@Test
	void sessionStoreDir() {
		Map<String, String> map = new HashMap<>();
		map.put("server.servlet.session.store-dir", "mydirectory");
		bindProperties(map);
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should()
			.setSession(assertArg((session) -> assertThat(session.getStoreDir()).isEqualTo(new File("mydirectory"))));
	}

	@Test
	void whenShutdownPropertyIsSetThenShutdownIsCustomized() {
		Map<String, String> map = new HashMap<>();
		map.put("server.shutdown", "graceful");
		bindProperties(map);
		ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
		this.customizer.customize(factory);
		then(factory).should().setShutdown(assertArg((shutdown) -> assertThat(shutdown).isEqualTo(Shutdown.GRACEFUL)));
	}

	private void bindProperties(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server", Bindable.ofInstance(this.properties));
	}

}
