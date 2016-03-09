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

package org.springframework.boot.actuate.system;

import java.io.File;
import java.io.FileReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
public class ApplicationPidFileWriterTests {

	private static final ApplicationPreparedEvent EVENT = new ApplicationPreparedEvent(
			new SpringApplication(), new String[] {},
			mock(ConfigurableApplicationContext.class));

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	@After
	public void resetListener() {
		System.clearProperty("PIDFILE");
		System.clearProperty("PID_FAIL_ON_WRITE_ERROR");
		ApplicationPidFileWriter.reset();
	}

	@Test
	public void createPidFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.onApplicationEvent(EVENT);
		FileReader reader = new FileReader(file);
		assertThat(FileCopyUtils.copyToString(reader)).isNotEmpty();
	}

	@Test
	public void overridePidFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		System.setProperty("PIDFILE", this.temporaryFolder.newFile().getAbsolutePath());
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.onApplicationEvent(EVENT);
		FileReader reader = new FileReader(System.getProperty("PIDFILE"));
		assertThat(FileCopyUtils.copyToString(reader)).isNotEmpty();
	}

	@Test
	public void overridePidFileWithSpring() throws Exception {
		File file = this.temporaryFolder.newFile();
		SpringApplicationEvent event = createPreparedEvent("spring.pid.file",
				file.getAbsolutePath());
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter();
		listener.onApplicationEvent(event);
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).isNotEmpty();
	}

	@Test
	public void differentEventTypes() throws Exception {
		File file = this.temporaryFolder.newFile();
		SpringApplicationEvent event = createEnvironmentPreparedEvent("spring.pid.file",
				file.getAbsolutePath());
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter();
		listener.onApplicationEvent(event);
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).isEmpty();
		listener.setTriggerEventType(ApplicationEnvironmentPreparedEvent.class);
		listener.onApplicationEvent(event);
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).isNotEmpty();
	}

	@Test
	public void withNoEnvironment() throws Exception {
		File file = this.temporaryFolder.newFile();
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.setTriggerEventType(ApplicationStartedEvent.class);
		listener.onApplicationEvent(
				new ApplicationStartedEvent(new SpringApplication(), new String[] {}));
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).isNotEmpty();
	}

	@Test
	public void continueWhenPidFileIsReadOnly() throws Exception {
		File file = this.temporaryFolder.newFile();
		file.setReadOnly();
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.onApplicationEvent(EVENT);
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).isEmpty();
	}

	@Test
	public void throwWhenPidFileIsReadOnly() throws Exception {
		File file = this.temporaryFolder.newFile();
		file.setReadOnly();
		System.setProperty("PID_FAIL_ON_WRITE_ERROR", "true");
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		this.exception.expect(IllegalStateException.class);
		this.exception.expectMessage("Cannot create pid file");
		listener.onApplicationEvent(EVENT);
	}

	@Test
	public void throwWhenPidFileIsReadOnlyWithSpring() throws Exception {
		File file = this.temporaryFolder.newFile();
		file.setReadOnly();
		SpringApplicationEvent event = createPreparedEvent(
				"spring.pid.fail-on-write-error", "true");
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		this.exception.expect(IllegalStateException.class);
		this.exception.expectMessage("Cannot create pid file");
		listener.onApplicationEvent(event);
	}

	private SpringApplicationEvent createEnvironmentPreparedEvent(String propName,
			String propValue) {
		ConfigurableEnvironment environment = createEnvironment(propName, propValue);
		return new ApplicationEnvironmentPreparedEvent(new SpringApplication(),
				new String[] {}, environment);
	}

	private SpringApplicationEvent createPreparedEvent(String propName,
			String propValue) {
		ConfigurableEnvironment environment = createEnvironment(propName, propValue);
		ConfigurableApplicationContext context = mock(
				ConfigurableApplicationContext.class);
		given(context.getEnvironment()).willReturn(environment);
		return new ApplicationPreparedEvent(new SpringApplication(), new String[] {},
				context);
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
