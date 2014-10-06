/*
 * Copyright 2010-2014 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.FileCopyUtils;

/**
 * Tests for {@link ApplicationPidListener}.
 *
 * @author Jakub Kubrynski
 * @author Dave Syer
 * @author Holger Stolzenberg
 */
public class ApplicationPidListenerTests {

	private static final SpringApplication APPLICATION = new SpringApplication();

	private static final ApplicationStartedEvent STARTED_EVENT = new ApplicationStartedEvent(
			APPLICATION, new String[] {});

	private ApplicationEnvironmentPreparedEvent environmentPreparedEvent;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private static final File DEFAULT_PID_FILE = new File("application.pid");
	private static final File OVERRIDE_PID_FILE = new File("application_override.pid");

	@Before
	public void initPrepareEvent() {
		final MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.application.pid-file",
				OVERRIDE_PID_FILE.getAbsolutePath());

		environmentPreparedEvent = new ApplicationEnvironmentPreparedEvent(APPLICATION,
				new String[] {}, environment);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Before
	@After
	public void resetListener() {
		System.clearProperty("PIDFILE");
		ApplicationPidListener.reset();

		DEFAULT_PID_FILE.delete();
		OVERRIDE_PID_FILE.delete();
	}

	@Test
	public void createPidFileWithDefaults() throws Exception {
		ApplicationPidListener listener = new ApplicationPidListener();
		listener.onApplicationEvent(STARTED_EVENT);
		assertThat(FileCopyUtils.copyToString(new FileReader(DEFAULT_PID_FILE)),
				not(isEmptyString()));
	}

	@Test
	public void createPidFileFromFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		ApplicationPidListener listener = new ApplicationPidListener(file);
		listener.onApplicationEvent(STARTED_EVENT);
		assertThat(FileCopyUtils.copyToString(new FileReader(file)), not(isEmptyString()));
	}

	@Test
	public void createPidFileFromFilePath() throws Exception {
		File file = this.temporaryFolder.newFile();
		ApplicationPidListener listener = new ApplicationPidListener(
				file.getAbsolutePath());
		listener.onApplicationEvent(STARTED_EVENT);
		assertThat(FileCopyUtils.copyToString(new FileReader(file)), not(isEmptyString()));
	}

	@Test
	public void setGetOrder() {
		ApplicationPidListener listener = new ApplicationPidListener();
		listener.setOrder(10);
		assertThat(listener.getOrder(), equalTo(10));
	}

	@Test
	public void overridePidFileWithSystemProperty() throws Exception {
		System.setProperty("PIDFILE", this.temporaryFolder.newFile().getAbsolutePath());
		ApplicationPidListener listener = new ApplicationPidListener();
		listener.onApplicationEvent(STARTED_EVENT);
		assertThat(
				FileCopyUtils.copyToString(new FileReader(System.getProperty("PIDFILE"))),
				not(isEmptyString()));
	}

	@Test
	public void overridePidFileFromEnvironment() throws Exception {
		ApplicationPidListener listener = new ApplicationPidListener();
		listener.onApplicationEvent(environmentPreparedEvent);
		assertThat(FileCopyUtils.copyToString(new FileReader(OVERRIDE_PID_FILE)),
				not(isEmptyString()));
	}
}
