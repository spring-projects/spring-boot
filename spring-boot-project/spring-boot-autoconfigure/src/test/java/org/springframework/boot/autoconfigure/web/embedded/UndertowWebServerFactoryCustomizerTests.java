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

package org.springframework.boot.autoconfigure.web.embedded;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link UndertowWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
public class UndertowWebServerFactoryCustomizerTests {

	private MockEnvironment environment;

	private ServerProperties serverProperties;

	private UndertowWebServerFactoryCustomizer customizer;

	@Before
	public void setup() {
		this.environment = new MockEnvironment();
		this.serverProperties = new ServerProperties();
		ConfigurationPropertySources.attach(this.environment);
		this.customizer = new UndertowWebServerFactoryCustomizer(this.environment,
				this.serverProperties);
	}

	@Test
	public void customizeUndertowAccessLog() {
		bind("server.undertow.accesslog.enabled=true",
				"server.undertow.accesslog.pattern=foo",
				"server.undertow.accesslog.prefix=test_log",
				"server.undertow.accesslog.suffix=txt",
				"server.undertow.accesslog.dir=test-logs",
				"server.undertow.accesslog.rotate=false");
		ConfigurableUndertowWebServerFactory factory = mock(
				ConfigurableUndertowWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setAccessLogEnabled(true);
		verify(factory).setAccessLogPattern("foo");
		verify(factory).setAccessLogPrefix("test_log");
		verify(factory).setAccessLogSuffix("txt");
		verify(factory).setAccessLogDirectory(new File("test-logs"));
		verify(factory).setAccessLogRotate(false);
	}

	@Test
	public void deduceUseForwardHeadersUndertow() {
		this.environment.setProperty("DYNO", "-");
		ConfigurableUndertowWebServerFactory factory = mock(
				ConfigurableUndertowWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	public void defaultUseForwardHeadersUndertow() {
		ConfigurableUndertowWebServerFactory factory = mock(
				ConfigurableUndertowWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(false);
	}

	@Test
	public void setUseForwardHeadersUndertow() {
		this.serverProperties.setUseForwardHeaders(true);
		ConfigurableUndertowWebServerFactory factory = mock(
				ConfigurableUndertowWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	private void bind(String... inlinedProperties) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				inlinedProperties);
		new Binder(ConfigurationPropertySources.get(this.environment)).bind("server",
				Bindable.ofInstance(this.serverProperties));
	}

}
