/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.devtools.classpath;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.boot.devtools.filewatch.FileSystemWatcherFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClassPathFileSystemWatcher}.
 *
 * @author Phillip Webb
 */
class ClassPathFileSystemWatcherTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void urlsMustNotBeNull() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ClassPathFileSystemWatcher(mock(FileSystemWatcherFactory.class),
					mock(ClassPathRestartStrategy.class), (URL[]) null))
			.withMessageContaining("'urls' must not be null");
	}

	@Test
	void configuredWithRestartStrategy(@TempDir File directory) throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		Map<String, Object> properties = new HashMap<>();
		List<URL> urls = new ArrayList<>();
		urls.add(new URL("https://spring.io"));
		urls.add(directory.toURI().toURL());
		properties.put("urls", urls);
		MapPropertySource propertySource = new MapPropertySource("test", properties);
		context.getEnvironment().getPropertySources().addLast(propertySource);
		context.register(Config.class);
		context.refresh();
		Thread.sleep(200);
		File classFile = new File(directory, "Example.class");
		FileCopyUtils.copy("file".getBytes(), classFile);
		Thread.sleep(1000);
		List<ClassPathChangedEvent> events = context.getBean(Listener.class).getEvents();
		for (int i = 0; i < 20; i++) {
			if (!events.isEmpty()) {
				break;
			}
			Thread.sleep(500);
		}
		assertThat(events).hasSize(1);
		assertThat(events.get(0).getChangeSet().iterator().next()).extracting(ChangedFile::getFile)
			.containsExactly(classFile);
		context.close();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		public final Environment environment;

		Config(Environment environment) {
			this.environment = environment;
		}

		@Bean
		ClassPathFileSystemWatcher watcher(ClassPathRestartStrategy restartStrategy) {
			FileSystemWatcher watcher = new FileSystemWatcher(false, Duration.ofMillis(100), Duration.ofMillis(10));
			URL[] urls = this.environment.getProperty("urls", URL[].class);
			assertThat(urls).isNotNull();
			return new ClassPathFileSystemWatcher(new MockFileSystemWatcherFactory(watcher), restartStrategy, urls);
		}

		@Bean
		ClassPathRestartStrategy restartStrategy() {
			return (file) -> false;
		}

		@Bean
		Listener listener() {
			return new Listener();
		}

	}

	static class Listener implements ApplicationListener<ClassPathChangedEvent> {

		private final List<ClassPathChangedEvent> events = new CopyOnWriteArrayList<>();

		@Override
		public void onApplicationEvent(ClassPathChangedEvent event) {
			this.events.add(event);
		}

		List<ClassPathChangedEvent> getEvents() {
			return this.events;
		}

	}

	static class MockFileSystemWatcherFactory implements FileSystemWatcherFactory {

		private final FileSystemWatcher watcher;

		MockFileSystemWatcherFactory(FileSystemWatcher watcher) {
			this.watcher = watcher;
		}

		@Override
		public FileSystemWatcher getFileSystemWatcher() {
			return this.watcher;
		}

	}

}
