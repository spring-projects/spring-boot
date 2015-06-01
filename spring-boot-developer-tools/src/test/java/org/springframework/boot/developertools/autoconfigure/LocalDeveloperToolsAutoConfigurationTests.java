/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.autoconfigure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.developertools.classpath.ClassPathChangedEvent;
import org.springframework.boot.developertools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.developertools.filewatch.ChangedFiles;
import org.springframework.boot.developertools.restart.MockRestartInitializer;
import org.springframework.boot.developertools.restart.MockRestarter;
import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.SocketUtils;
import org.thymeleaf.templateresolver.TemplateResolver;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LocalDeveloperToolsAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class LocalDeveloperToolsAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public MockRestarter mockRestarter = new MockRestarter();

	private int liveReloadPort = SocketUtils.findAvailableTcpPort();

	private ConfigurableApplicationContext context;

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void thymeleafCacheIsFalse() throws Exception {
		this.context = initializeAndRun(Config.class);
		TemplateResolver resolver = this.context.getBean(TemplateResolver.class);
		resolver.initialize();
		assertThat(resolver.isCacheable(), equalTo(false));
	}

	@Test
	public void restartTriggerdOnClassPathChangeWithRestart() throws Exception {
		this.context = initializeAndRun(Config.class);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.<ChangedFiles> emptySet(), true);
		this.context.publishEvent(event);
		verify(this.mockRestarter.getMock()).restart();
	}

	@Test
	public void restartNotTriggerdOnClassPathChangeWithRestart() throws Exception {
		this.context = initializeAndRun(Config.class);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.<ChangedFiles> emptySet(), false);
		this.context.publishEvent(event);
		verify(this.mockRestarter.getMock(), never()).restart();
	}

	@Test
	public void restartWatchingClassPath() throws Exception {
		this.context = initializeAndRun(Config.class);
		ClassPathFileSystemWatcher watcher = this.context
				.getBean(ClassPathFileSystemWatcher.class);
		assertThat(watcher, notNullValue());
	}

	@Test
	public void restartDisabled() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.developertools.restart.enabled", false);
		this.context = initializeAndRun(Config.class, properties);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(ClassPathFileSystemWatcher.class);
	}

	private ConfigurableApplicationContext initializeAndRun(Class<?> config) {
		return initializeAndRun(config, Collections.<String, Object> emptyMap());
	}

	private ConfigurableApplicationContext initializeAndRun(Class<?> config,
			Map<String, Object> properties) {
		Restarter.initialize(new String[0], false, new MockRestartInitializer(), false);
		SpringApplication application = new SpringApplication(config);
		application.setDefaultProperties(getDefaultProperties(properties));
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		return context;
	}

	private Map<String, Object> getDefaultProperties(
			Map<String, Object> specifiedProperties) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.thymeleaf.check-template-location", false);
		properties.put("spring.developertools.livereload.port", this.liveReloadPort);
		properties.putAll(specifiedProperties);
		return properties;
	}

	@Configuration
	@Import({ LocalDeveloperToolsAutoConfiguration.class,
			ThymeleafAutoConfiguration.class })
	public static class Config {

	}

}
