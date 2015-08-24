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

package org.springframework.boot.devtools.classpath;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClassPathFileSystemWatcher}.
 *
 * @author Phillip Webb
 */
public class ClassPathFileSystemWatcherTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void urlsMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Urls must not be null");
		URL[] urls = null;
		new ClassPathFileSystemWatcher(mock(FileSystemWatcherFactory.class),
				mock(ClassPathRestartStrategy.class), urls);
	}

	@Test
	public void configuredWithRestartStrategy() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		Map<String, Object> properties = new HashMap<String, Object>();
		File folder = this.temp.newFolder();
		List<URL> urls = new ArrayList<URL>();
		urls.add(new URL("http://spring.io"));
		urls.add(folder.toURI().toURL());
		properties.put("urls", urls);
		MapPropertySource propertySource = new MapPropertySource("test", properties);
		context.getEnvironment().getPropertySources().addLast(propertySource);
		context.register(Config.class);
		context.refresh();
		Thread.sleep(100);
		File classFile = new File(folder, "Example.class");
		FileCopyUtils.copy("file".getBytes(), classFile);
		Thread.sleep(1100);
		List<ClassPathChangedEvent> events = context.getBean(Listener.class).getEvents();
		assertThat(events.size(), equalTo(1));
		assertThat(events.get(0).getChangeSet().iterator().next().getFiles().iterator()
				.next().getFile(), equalTo(classFile));
		context.close();
	}

	@Configuration
	public static class Config {

		@Autowired
		public Environment environemnt;

		@Bean
		public ClassPathFileSystemWatcher watcher() {
			FileSystemWatcher watcher = new FileSystemWatcher(false, 100, 10);
			URL[] urls = this.environemnt.getProperty("urls", URL[].class);
			return new ClassPathFileSystemWatcher(new MockFileSystemWatcherFactory(
					watcher), restartStrategy(), urls);
		}

		@Bean
		public ClassPathRestartStrategy restartStrategy() {
			return new ClassPathRestartStrategy() {

				@Override
				public boolean isRestartRequired(ChangedFile file) {
					return false;
				}

			};
		}

		@Bean
		public Listener listener() {
			return new Listener();
		}

	}

	public static class Listener implements ApplicationListener<ClassPathChangedEvent> {

		private List<ClassPathChangedEvent> events = new ArrayList<ClassPathChangedEvent>();

		@Override
		public void onApplicationEvent(ClassPathChangedEvent event) {
			this.events.add(event);
		}

		public List<ClassPathChangedEvent> getEvents() {
			return this.events;
		}

	}

	private static class MockFileSystemWatcherFactory implements FileSystemWatcherFactory {

		private final FileSystemWatcher watcher;

		public MockFileSystemWatcherFactory(FileSystemWatcher watcher) {
			this.watcher = watcher;
		}

		@Override
		public FileSystemWatcher getFileSystemWatcher() {
			return this.watcher;
		}

	}

}
