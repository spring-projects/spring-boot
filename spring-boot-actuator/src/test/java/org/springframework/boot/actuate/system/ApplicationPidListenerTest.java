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

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * Created by jkubrynski@gmail.com / 2014-03-25
 */
public class ApplicationPidListenerTest {

	private static final String[] NO_ARGS = {};

	public static final String PID_FILE_NAME = "test.pid";

	@Test
	public void shouldCreatePidFile() {
		//given
		ApplicationPidListener sut = new ApplicationPidListener();
		sut.setPidFileName(PID_FILE_NAME);

		//when
		sut.onApplicationEvent(new ApplicationStartedEvent(
				new SpringApplication(), NO_ARGS));

		//then
		File pidFile = new File(PID_FILE_NAME);
		assertTrue(pidFile.exists());
		pidFile.delete();
	}

}
