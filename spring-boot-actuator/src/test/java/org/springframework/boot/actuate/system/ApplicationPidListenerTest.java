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

import java.io.File;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;

import static org.junit.Assert.assertTrue;

/**
 * @author Jakub Kubrynski
 * @author Dave Syer
 */
public class ApplicationPidListenerTest {

	private static final String[] NO_ARGS = {};

	@After
	public void init() {
		ApplicationPidListener.reset();
	}

	@Test
	public void shouldCreatePidFile() {
		// given
		String pidFileName = "test.pid";
		ApplicationPidListener sut = new ApplicationPidListener(pidFileName);

		// when
		sut.onApplicationEvent(new ApplicationStartedEvent(new SpringApplication(),
				NO_ARGS));

		// then
		File pidFile = new File(pidFileName);
		assertTrue(pidFile.exists());
		pidFile.delete();
	}

	@Test
	public void shouldCreatePidFileParentDirectory() {
		// given
		String pidFileName = "target/pid/test.pid";
		ApplicationPidListener sut = new ApplicationPidListener(pidFileName);

		// when
		sut.onApplicationEvent(new ApplicationStartedEvent(new SpringApplication(),
				NO_ARGS));

		// then
		File pidFile = new File(pidFileName);
		assertTrue(pidFile.exists());
		pidFile.delete();
	}

}
