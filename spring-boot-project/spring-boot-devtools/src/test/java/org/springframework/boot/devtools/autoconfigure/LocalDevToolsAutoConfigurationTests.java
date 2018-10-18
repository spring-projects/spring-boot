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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardWrapper;
import org.apache.jasper.EmbeddedServletOptions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.devtools.livereload.LiveReloadServer;
import org.springframework.boot.devtools.restart.FailureHandler;
import org.springframework.boot.devtools.restart.MockRestartInitializer;
import org.springframework.boot.devtools.restart.MockRestarter;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
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
	public MockRestarter mockRestarter = new MockRestarter();

	private ConfigurableApplicationContext context;

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void thymeleafCacheIsFalse() {
		this.context = initializeAndRun(Config.class);
		SpringResourceTemplateResolver resolver = this.context
				.getBean(SpringResourceTemplateResolver.class);
		assertThat(resolver.isCacheable()).isFalse();
	}

	@Test
	public void defaultPropertyCanBeOverriddenFromCommandLine() {
		this.context = initializeAndRun(Config.class, "--spring.thymeleaf.cache=true");
		SpringResourceTemplateResolver resolver = this.context
				.getBean(SpringResourceTemplateResolver.class);
		assertThat(resolver.isCacheable()).isTrue();
	}

	@Test
	public void defaultPropertyCanBeOverriddenFromUserHomeProperties() {
		String userHome = System.getProperty("user.home");
		System.setProperty("user.home",
				new File("src/test/resources/user-home").getAbsolutePath());
		try {
			this.context = initializeAndRun(Config.class);
			SpringResourceTemplateResolver resolver = this.context
					.getBean(SpringResourceTemplateResolver.class);
			assertThat(resolver.isCacheable()).isTrue();
		}
		finally {
			System.setProperty("user.home", userHome);
		}
	}

	@Test
	public void resourceCachePeriodIsZero() {
		this.context = initializeAndRun(WebResourcesConfig.class);
		ResourceProperties properties = this.context.getBean(ResourceProperties.class);
		assertThat(properties.getCache().getPeriod()).isEqualTo(Duration.ZERO);
	}

	@Test
	public void liveReloadServer() {
		this.context = initializeAndRun(Config.class);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		assertThat(server.isStarted()).isTrue();
	}

	@Test
	public void liveReloadTriggeredOnContextRefresh() {
		this.context = initializeAndRun(ConfigWithMockLiveReload.class);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		this.context.publishEvent(new ContextRefreshedEvent(this.context));
		verify(server).triggerReload();
	}

	@Test
	public void liveReloadTriggeredOnClassPathChangeWithoutRestart() {
		this.context = initializeAndRun(ConfigWithMockLiveReload.class);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.emptySet(), false);
		this.context.publishEvent(event);
		verify(server).triggerReload();
	}

	@Test
	public void liveReloadNotTriggeredOnClassPathChangeWithRestart() {
		this.context = initializeAndRun(ConfigWithMockLiveReload.class);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.emptySet(), true);
		this.context.publishEvent(event);
		verify(server, never()).triggerReload();
	}

	@Test
	public void liveReloadDisabled() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.devtools.livereload.enabled", false);
		this.context = initializeAndRun(Config.class, properties);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(OptionalLiveReloadServer.class));
	}

	@Test
	public void restartTriggeredOnClassPathChangeWithRestart() {
		this.context = initializeAndRun(Config.class);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.emptySet(), true);
		this.context.publishEvent(event);
		verify(this.mockRestarter.getMock()).restart(any(FailureHandler.class));
	}

	@Test
	public void restartNotTriggeredOnClassPathChangeWithRestart() {
		this.context = initializeAndRun(Config.class);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context,
				Collections.emptySet(), false);
		this.context.publishEvent(event);
		verify(this.mockRestarter.getMock(), never()).restart();
	}

	@Test
	public void restartWatchingClassPath() {
		this.context = initializeAndRun(Config.class);
		ClassPathFileSystemWatcher watcher = this.context
				.getBean(ClassPathFileSystemWatcher.class);
		assertThat(watcher).isNotNull();
	}

	@Test
	public void restartDisabled() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.devtools.restart.enabled", false);
		this.context = initializeAndRun(Config.class, properties);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(ClassPathFileSystemWatcher.class));
	}

	@Test
	public void restartWithTriggerFile() {
		Map<String, Object> properties = new HashMap<>();
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
	public void watchingAdditionalPaths() {
		Map<String, Object> properties = new HashMap<>();
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

	@Test
	public void devToolsSwitchesJspServletToDevelopmentMode() {
		this.context = initializeAndRun(Config.class);
		TomcatWebServer tomcatContainer = (TomcatWebServer) ((ServletWebServerApplicationContext) this.context)
				.getWebServer();
		Container context = tomcatContainer.getTomcat().getHost().findChildren()[0];
		StandardWrapper jspServletWrapper = (StandardWrapper) context.findChild("jsp");
		EmbeddedServletOptions options = (EmbeddedServletOptions) ReflectionTestUtils
				.getField(jspServletWrapper.getServlet(), "options");
		assertThat(options.getDevelopment()).isTrue();
	}

	private ConfigurableApplicationContext initializeAndRun(Class<?> config,
			String... args) {
		return initializeAndRun(config, Collections.emptyMap(), args);
	}

	private ConfigurableApplicationContext initializeAndRun(Class<?> config,
			Map<String, Object> properties, String... args) {
		Restarter.initialize(new String[0], false, new MockRestartInitializer(), false);
		SpringApplication application = new SpringApplication(config);
		application.setDefaultProperties(getDefaultProperties(properties));
		ConfigurableApplicationContext context = application.run(args);
		return context;
	}

	private Map<String, Object> getDefaultProperties(
			Map<String, Object> specifiedProperties) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.thymeleaf.check-template-location", false);
		properties.put("spring.devtools.livereload.port", 0);
		properties.put("server.port", 0);
		properties.putAll(specifiedProperties);
		return properties;
	}

	@Configuration
	@Import({ ServletWebServerFactoryAutoConfiguration.class,
			LocalDevToolsAutoConfiguration.class, ThymeleafAutoConfiguration.class })
	public static class Config {

	}

	@Configuration
	@ImportAutoConfiguration({ ServletWebServerFactoryAutoConfiguration.class,
			LocalDevToolsAutoConfiguration.class, ThymeleafAutoConfiguration.class })
	public static class ConfigWithMockLiveReload {

		@Bean
		public LiveReloadServer liveReloadServer() {
			return mock(LiveReloadServer.class);
		}

	}

	@Configuration
	@Import({ ServletWebServerFactoryAutoConfiguration.class,
			LocalDevToolsAutoConfiguration.class, ResourceProperties.class })
	public static class WebResourcesConfig {

	}

	@Configuration
	public static class SessionRedisTemplateConfig {

		@Bean
		public RedisTemplate<Object, Object> sessionRedisTemplate() {
			RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
			redisTemplate.setConnectionFactory(mock(RedisConnectionFactory.class));
			return redisTemplate;
		}

	}

}
