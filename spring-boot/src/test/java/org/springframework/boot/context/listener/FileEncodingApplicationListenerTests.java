/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.listener;

import org.junit.Assume;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationEnvironmentAvailableEvent;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * Tests for {@link FileEncodingApplicationListener}.
 * 
 * @author Dave Syer
 */
public class FileEncodingApplicationListenerTests {

	private final FileEncodingApplicationListener initializer = new FileEncodingApplicationListener();
	private final ConfigurableEnvironment environment = new StandardEnvironment();
	private final SpringApplicationEnvironmentAvailableEvent event = new SpringApplicationEnvironmentAvailableEvent(
			new SpringApplication(), this.environment, new String[0]);

	@Test(expected = IllegalStateException.class)
	public void testIllegalState() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.mandatory_file_encoding:FOO");
		this.initializer.onApplicationEvent(this.event);
	}

	@Test
	public void testSunnyDayNothingMandated() {
		this.initializer.onApplicationEvent(this.event);
	}

	@Test
	public void testSunnyDayMandated() {
		Assume.assumeNotNull(System.getProperty("file.encoding"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.mandatory_file_encoding:" + System.getProperty("file.encoding"));
		this.initializer.onApplicationEvent(this.event);
	}

}
