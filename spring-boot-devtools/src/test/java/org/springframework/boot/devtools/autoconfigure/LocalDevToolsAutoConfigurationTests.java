/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.thymeleaf.templateresolver.TemplateResolver;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.livereload.LiveReloadServer;
import org.springframework.boot.devtools.restart.FailureHandler;
import org.springframework.boot.devtools.restart.MockRestartInitializer;
import org.springframework.boot.devtools.restart.MockRestarter;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LocalDevToolsAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vladimir Tsanev
 */
public class LocalDevToolsAutoConfigurationTests {

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
		assertThat(resolver.isCacheable()).isFalse();
	}

	@Test
	public void defaultPropertyCanBeOverriddenFromCommandLine() throws Exception {
		this.context = initializeAndRun(Config.class, "--spring.thymeleaf.cache=true");
		TemplateResolver resolver = this.context.getBean(TemplateResolver.class);
		resolver.initialize();
		assertThat(resolver.isCacheable()).isTrue();
	}

	@Test
	public void defaultPropertyCanBeOverriddenFromUserHomeProperties() throws Exception {
		String userHome = System.getProperty("user.home");
		System.setProperty("user.home",
				new File("src/test/resources/user-home").getAbsolutePath());
		try {
			this.context = initializeAndRun(Config.class);
			TemplateResolver resolver = this.context.getBean(TemplateResolver.class);
			resolver.initialize();
			assertThat(resolver.isCacheable()).isTrue();
		}
		finally {
			System.setProperty("user.home", userHome);
		}
	}

	@Test
	public void resourceCachePeriodIsZero() throws Exception {
		this.context = initializeAndRun(WebResourcesConfig.class);
		ResourceProperties properties = this.context.getBean(ResourceProperties.class);
		assertThat(properties.getCachePeriod()).isEqualTo(0);
	}

	@Test
	public void liveReloadServer() throws Exception {
		this.context = initializeAndRun(Config.class);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		assertThat(server.isStarted()).isTrue();
	}

	@Test
	public void liveReloadTriggeredOnContextRefresh() throws Exception {
		this.context = initializeAndRun(ConfigWithMockLiveReload.class);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		this.context.publishEvent(new ContextRefreshedEvent(this.context));
		verify(server).triggerReload();
	}

	@Test
	public void liveReloadTriggeredOnClassPathChangeWithoutRestart() throws Exception {
		this.context = initializeAndRun(ConfigWithMockLiveReload.class);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.<ChangedFiles>emptySet(), false);
		this.context.publishEvent(event);
		verify(server).triggerReload();
	}

	@Test
	public void liveReloadNotTriggeredOnClassPathChangeWithRestart() throws Exception {
		this.context = initializeAndRun(ConfigWithMockLiveReload.class);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.<ChangedFiles>emptySet(), true);
		this.context.publishEvent(event);
		verify(server, never()).triggerReload();
	}

	@Test
	public void liveReloadDisabled() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.devtools.livereload.enabled", false);
		this.context = initializeAndRun(Config.class, properties);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(OptionalLiveReloadServer.class);
	}

	@Test
	public void restartTriggeredOnClassPathChangeWithRestart() throws Exception {
		this.context = initializeAndRun(Config.class);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.<ChangedFiles>emptySet(), true);
		this.context.publishEvent(event);
		verify(this.mockRestarter.getMock()).restart(any(FailureHandler.class));
	}

	@Test
	public void restartNotTriggeredOnClassPathChangeWithRestart() throws Exception {
		this.context = initializeAndRun(Config.class);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.<ChangedFiles>emptySet(), false);
		this.context.publishEvent(event);
		verify(this.mockRestarter.getMock(), never()).restart();
	}

	@Test
	public void restartWatchingClassPath() throws Exception {
		this.context = initializeAndRun(Config.class);
		ClassPathFileSystemWatcher watcher = this.context
				.getBean(ClassPathFileSystemWatcher.class);
		assertThat(watcher).isNotNull();
	}

	@Test
	public void restartDisabled() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.devtools.restart.enabled", false);
		this.context = initializeAndRun(Config.class, properties);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(ClassPathFileSystemWatcher.class);
	}

	@Test
	public void restartWithTriggerFile() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.devtools.restart.trigger-file", "somefile.txt");
		this.context = initializeAndRun(Config.class, properties);
		ClassPathFileSystemWatcher classPathWatcher = this.context
				.getBean(ClassPathFileSystemWatcher.class);
		Object watcher = ReflectionTestUtils.getField(classPathWatcher,
				"fileSystemWatcher");
		Object filter = ReflectionTestUtils.getField(watcher, "triggerFilter");
		assertThat(filter).isInstanceOf(TriggerFileFilter.class);
	}

	@Test
	public void watchingAdditionalPaths() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.devtools.restart.additional-paths",
				"src/main/java,src/test/java");
		this.context = initializeAndRun(Config.class, properties);
		ClassPathFileSystemWatcher classPathWatcher = this.context
				.getBean(ClassPathFileSystemWatcher.class);
		Object watcher = ReflectionTestUtils.getField(classPathWatcher,
				"fileSystemWatcher");
		@SuppressWarnings("unchecked")
		Map<File, Object> folders = (Map<File, Object>) ReflectionTestUtils
				.getField(watcher, "folders");
		assertThat(folders).hasSize(2)
				.containsKey(new File("src/main/java").getAbsoluteFile())
				.containsKey(new File("src/test/java").getAbsoluteFile());
	}

	private ConfigurableApplicationContext initializeAndRun(Class<?> config,
			String... args) {
		return initializeAndRun(config, Collections.<String, Object>emptyMap(), args);
	}

	private ConfigurableApplicationContext initializeAndRun(Class<?> config,
			Map<String, Object> properties, String... args) {
		Restarter.initialize(new String[0], false, new MockRestartInitializer(), false);
		SpringApplication application = new SpringApplication(config);
		application.setDefaultProperties(getDefaultProperties(properties));
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run(args);
		return context;
	}

	private Map<String, Object> getDefaultProperties(
			Map<String, Object> specifiedProperties) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.thymeleaf.check-template-location", false);
		properties.put("spring.devtools.livereload.port", this.liveReloadPort);
		properties.putAll(specifiedProperties);
		return properties;
	}

	@Configuration
	@Import({ LocalDevToolsAutoConfiguration.class, ThymeleafAutoConfiguration.class })
	public static class Config {

	}

	@Configuration
	@Import({ LocalDevToolsAutoConfiguration.class, ThymeleafAutoConfiguration.class })
	public static class ConfigWithMockLiveReload {

		@Bean
		public LiveReloadServer liveReloadServer() {
			return mock(LiveReloadServer.class);
		}

	}

	@Configuration
	@Import({ LocalDevToolsAutoConfiguration.class, ResourceProperties.class })
	public static class WebResourcesConfig {

	}

	@Configuration
	public static class SessionRedisTemplateConfig {

		@Bean
		public RedisTemplate<Object, Object> sessionRedisTemplate() {
			RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<Object, Object>();
			redisTemplate.setConnectionFactory(mock(RedisConnectionFactory.class));
			return redisTemplate;
		}

	}

}
