/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardWrapper;
import org.apache.jasper.EmbeddedServletOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
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
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;

/**
 * Tests for {@link LocalDevToolsAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vladimir Tsanev
 */
@ExtendWith(MockRestarter.class)
class LocalDevToolsAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void defaultPropertyCanBeOverriddenFromCommandLine() throws Exception {
		this.context = getContext(() -> initializeAndRun(Config.class, "--spring.freemarker.cache=true"));
		AbstractTemplateViewResolver resolver = this.context.getBean(AbstractTemplateViewResolver.class);
		assertThat(resolver.isCache()).isTrue();
	}

	@Test
	void defaultPropertyCanBeOverriddenFromUserHomeProperties() throws Exception {
		String userHome = System.getProperty("user.home");
		System.setProperty("user.home", new File("src/test/resources/user-home").getAbsolutePath());
		try {
			this.context = getContext(() -> initializeAndRun(Config.class));
			AbstractTemplateViewResolver resolver = this.context.getBean(AbstractTemplateViewResolver.class);
			assertThat(resolver.isCache()).isTrue();
		}
		finally {
			System.setProperty("user.home", userHome);
		}
	}

	@Test
	void resourceCachePeriodIsZero() throws Exception {
		this.context = getContext(() -> initializeAndRun(WebResourcesConfig.class));
		Resources properties = this.context.getBean(WebProperties.class).getResources();
		assertThat(properties.getCache().getPeriod()).isZero();
	}

	@SuppressWarnings("deprecation")
	@Test
	void deprecatedResourceCachePeriodIsZeroWhenDeprecatedResourcePropertiesAreInUse() throws Exception {
		this.context = getContext(() -> initializeAndRun(WebResourcesConfig.class,
				Collections.singletonMap("spring.resources.add-mappings", false)));
		Resources properties = this.context
				.getBean(org.springframework.boot.autoconfigure.web.ResourceProperties.class);
		assertThat(properties.getCache().getPeriod()).isZero();
	}

	@Test
	void liveReloadServer() throws Exception {
		this.context = getContext(() -> initializeAndRun(Config.class));
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		assertThat(server.isStarted()).isTrue();
	}

	@Test
	void liveReloadTriggeredOnContextRefresh() throws Exception {
		this.context = getContext(() -> initializeAndRun(ConfigWithMockLiveReload.class));
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		this.context.publishEvent(new ContextRefreshedEvent(this.context));
		then(server).should().triggerReload();
	}

	@Test
	void liveReloadTriggeredOnClassPathChangeWithoutRestart() throws Exception {
		this.context = getContext(() -> initializeAndRun(ConfigWithMockLiveReload.class));
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context, Collections.emptySet(), false);
		this.context.publishEvent(event);
		then(server).should().triggerReload();
	}

	@Test
	void liveReloadNotTriggeredOnClassPathChangeWithRestart() throws Exception {
		this.context = getContext(() -> initializeAndRun(ConfigWithMockLiveReload.class));
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		reset(server);
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context, Collections.emptySet(), true);
		this.context.publishEvent(event);
		then(server).should(never()).triggerReload();
	}

	@Test
	void liveReloadDisabled() throws Exception {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.devtools.livereload.enabled", false);
		this.context = getContext(() -> initializeAndRun(Config.class, properties));
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(OptionalLiveReloadServer.class));
	}

	@Test
	void restartTriggeredOnClassPathChangeWithRestart(Restarter restarter) throws Exception {
		this.context = getContext(() -> initializeAndRun(Config.class));
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context, Collections.emptySet(), true);
		this.context.publishEvent(event);
		then(restarter).should().restart(any(FailureHandler.class));
	}

	@Test
	void restartNotTriggeredOnClassPathChangeWithRestart(Restarter restarter) throws Exception {
		this.context = getContext(() -> initializeAndRun(Config.class));
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.context, Collections.emptySet(), false);
		this.context.publishEvent(event);
		then(restarter).should(never()).restart();
	}

	@Test
	void restartWatchingClassPath() throws Exception {
		this.context = getContext(() -> initializeAndRun(Config.class));
		ClassPathFileSystemWatcher watcher = this.context.getBean(ClassPathFileSystemWatcher.class);
		assertThat(watcher).isNotNull();
	}

	@Test
	void restartDisabled() throws Exception {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.devtools.restart.enabled", false);
		this.context = getContext(() -> initializeAndRun(Config.class, properties));
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(ClassPathFileSystemWatcher.class));
	}

	@Test
	void restartWithTriggerFile() throws Exception {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.devtools.restart.trigger-file", "somefile.txt");
		this.context = getContext(() -> initializeAndRun(Config.class, properties));
		ClassPathFileSystemWatcher classPathWatcher = this.context.getBean(ClassPathFileSystemWatcher.class);
		Object watcher = ReflectionTestUtils.getField(classPathWatcher, "fileSystemWatcher");
		Object filter = ReflectionTestUtils.getField(watcher, "triggerFilter");
		assertThat(filter).isInstanceOf(TriggerFileFilter.class);
	}

	@Test
	void watchingAdditionalPaths() throws Exception {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.devtools.restart.additional-paths", "src/main/java,src/test/java");
		this.context = getContext(() -> initializeAndRun(Config.class, properties));
		ClassPathFileSystemWatcher classPathWatcher = this.context.getBean(ClassPathFileSystemWatcher.class);
		Object watcher = ReflectionTestUtils.getField(classPathWatcher, "fileSystemWatcher");
		@SuppressWarnings("unchecked")
		Map<File, Object> directories = (Map<File, Object>) ReflectionTestUtils.getField(watcher, "directories");
		assertThat(directories).hasSize(2).containsKey(new File("src/main/java").getAbsoluteFile())
				.containsKey(new File("src/test/java").getAbsoluteFile());
	}

	@Test
	void devToolsSwitchesJspServletToDevelopmentMode() throws Exception {
		this.context = getContext(() -> initializeAndRun(Config.class));
		TomcatWebServer tomcatContainer = (TomcatWebServer) ((ServletWebServerApplicationContext) this.context)
				.getWebServer();
		Container context = tomcatContainer.getTomcat().getHost().findChildren()[0];
		StandardWrapper jspServletWrapper = (StandardWrapper) context.findChild("jsp");
		EmbeddedServletOptions options = (EmbeddedServletOptions) ReflectionTestUtils
				.getField(jspServletWrapper.getServlet(), "options");
		assertThat(options.getDevelopment()).isTrue();
	}

	private ConfigurableApplicationContext getContext(Supplier<ConfigurableApplicationContext> supplier)
			throws Exception {
		AtomicReference<ConfigurableApplicationContext> atomicReference = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			ConfigurableApplicationContext context = supplier.get();
			atomicReference.getAndSet(context);
		});
		thread.start();
		thread.join();
		return atomicReference.get();
	}

	private ConfigurableApplicationContext initializeAndRun(Class<?> config, String... args) {
		return initializeAndRun(config, Collections.emptyMap(), args);
	}

	private ConfigurableApplicationContext initializeAndRun(Class<?> config, Map<String, Object> properties,
			String... args) {
		Restarter.initialize(new String[0], false, new MockRestartInitializer(), false);
		SpringApplication application = new SpringApplication(config);
		application.setDefaultProperties(getDefaultProperties(properties));
		return application.run(args);
	}

	private Map<String, Object> getDefaultProperties(Map<String, Object> specifiedProperties) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.devtools.livereload.port", 0);
		properties.put("server.port", 0);
		properties.putAll(specifiedProperties);
		return properties;
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ ServletWebServerFactoryAutoConfiguration.class, LocalDevToolsAutoConfiguration.class,
			FreeMarkerAutoConfiguration.class })
	static class Config {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ ServletWebServerFactoryAutoConfiguration.class, LocalDevToolsAutoConfiguration.class,
			FreeMarkerAutoConfiguration.class })
	static class ConfigWithMockLiveReload {

		@Bean
		LiveReloadServer liveReloadServer() {
			return mock(LiveReloadServer.class);
		}

	}

	@SuppressWarnings("deprecation")
	@Configuration(proxyBeanMethods = false)
	@Import({ ServletWebServerFactoryAutoConfiguration.class, LocalDevToolsAutoConfiguration.class, WebProperties.class,
			org.springframework.boot.autoconfigure.web.ResourceProperties.class })
	static class WebResourcesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	static class SessionRedisTemplateConfig {

		@Bean
		RedisTemplate<Object, Object> sessionRedisTemplate() {
			RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
			redisTemplate.setConnectionFactory(mock(RedisConnectionFactory.class));
			return redisTemplate;
		}

	}

}
