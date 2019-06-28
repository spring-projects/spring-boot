/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ApplicationPidFileWriter}.
 *
 * @author Jakub Kubrynski
 * @author Dave Syer
 * @author Phillip Webb
 * @author Tomasz Przybyla
 */
class ApplicationPidFileWriterTests {

	private static final ApplicationPreparedEvent EVENT = new ApplicationPreparedEvent(new SpringApplication(),
			new String[] {}, mock(ConfigurableApplicationContext.class));

	@TempDir
	File tempDir;

	@BeforeEach
	@AfterEach
	void resetListener() {
		System.clearProperty("PIDFILE");
		System.clearProperty("PID_FAIL_ON_WRITE_ERROR");
		ApplicationPidFileWriter.reset();
	}

	@Test
	void createPidFile() throws Exception {
		File file = new File(this.tempDir, "pid");
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.onApplicationEvent(EVENT);
		assertThat(contentOf(file)).isNotEmpty();
	}

	@Test
	void overridePidFile() throws Exception {
		File file = new File(this.tempDir, "pid");
		System.setProperty("PIDFILE", new File(this.tempDir, "override").getAbsolutePath());
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.onApplicationEvent(EVENT);
		assertThat(contentOf(new File(System.getProperty("PIDFILE")))).isNotEmpty();
	}

	@Test
	void overridePidFileWithSpring() throws Exception {
		File file = new File(this.tempDir, "pid");
		SpringApplicationEvent event = createPreparedEvent("spring.pid.file", file.getAbsolutePath());
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter();
		listener.onApplicationEvent(event);
		assertThat(contentOf(file)).isNotEmpty();
	}

	@Test
	void tryEnvironmentPreparedEvent() throws Exception {
		File file = new File(this.tempDir, "pid");
		file.createNewFile();
		SpringApplicationEvent event = createEnvironmentPreparedEvent("spring.pid.file", file.getAbsolutePath());
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter();
		listener.onApplicationEvent(event);
		assertThat(contentOf(file)).isEmpty();
		listener.setTriggerEventType(ApplicationEnvironmentPreparedEvent.class);
		listener.onApplicationEvent(event);
		assertThat(contentOf(file)).isNotEmpty();
	}

	@Test
	void tryReadyEvent() throws Exception {
		File file = new File(this.tempDir, "pid");
		file.createNewFile();
		SpringApplicationEvent event = createReadyEvent("spring.pid.file", file.getAbsolutePath());
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter();
		listener.onApplicationEvent(event);
		assertThat(contentOf(file)).isEmpty();
		listener.setTriggerEventType(ApplicationReadyEvent.class);
		listener.onApplicationEvent(event);
		assertThat(contentOf(file)).isNotEmpty();
	}

	@Test
	void withNoEnvironment() throws Exception {
		File file = new File(this.tempDir, "pid");
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.setTriggerEventType(ApplicationStartingEvent.class);
		listener.onApplicationEvent(new ApplicationStartingEvent(new SpringApplication(), new String[] {}));
		assertThat(contentOf(file)).isNotEmpty();
	}

	@Test
	void continueWhenPidFileIsReadOnly() throws Exception {
		withReadOnlyPidFile((file) -> {
			ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
			listener.onApplicationEvent(EVENT);
			assertThat(contentOf(file)).isEmpty();
		});
	}

	@Test
	void throwWhenPidFileIsReadOnly() throws Exception {
		withReadOnlyPidFile((file) -> {
			System.setProperty("PID_FAIL_ON_WRITE_ERROR", "true");
			ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
			assertThatIllegalStateException().isThrownBy(() -> listener.onApplicationEvent(EVENT))
					.withMessageContaining("Cannot create pid file");
		});
	}

	@Test
	void throwWhenPidFileIsReadOnlyWithSpring() throws Exception {
		withReadOnlyPidFile((file) -> {
			SpringApplicationEvent event = createPreparedEvent("spring.pid.fail-on-write-error", "true");
			ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
			assertThatIllegalStateException().isThrownBy(() -> listener.onApplicationEvent(event))
					.withMessageContaining("Cannot create pid file");
		});
	}

	private void withReadOnlyPidFile(Consumer<File> consumer) throws IOException {
		File file = new File(this.tempDir, "pid");
		file.createNewFile();
		file.setReadOnly();
		try {
			consumer.accept(file);
		}
		finally {
			file.setWritable(true);
		}
	}

	private SpringApplicationEvent createEnvironmentPreparedEvent(String propName, String propValue) {
		ConfigurableEnvironment environment = createEnvironment(propName, propValue);
		return new ApplicationEnvironmentPreparedEvent(new SpringApplication(), new String[] {}, environment);
	}

	private SpringApplicationEvent createPreparedEvent(String propName, String propValue) {
		ConfigurableEnvironment environment = createEnvironment(propName, propValue);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		given(context.getEnvironment()).willReturn(environment);
		return new ApplicationPreparedEvent(new SpringApplication(), new String[] {}, context);
	}

	private SpringApplicationEvent createReadyEvent(String propName, String propValue) {
		ConfigurableEnvironment environment = createEnvironment(propName, propValue);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		given(context.getEnvironment()).willReturn(environment);
		return new ApplicationReadyEvent(new SpringApplication(), new String[] {}, context);
	}

	private ConfigurableEnvironment createEnvironment(String propName, String propValue) {
		MockPropertySource propertySource = mockPropertySource(propName, propValue);
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(propertySource);
		return environment;
	}

	private MockPropertySource mockPropertySource(String name, String value) {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty(name, value);
		return propertySource;
	}

}
