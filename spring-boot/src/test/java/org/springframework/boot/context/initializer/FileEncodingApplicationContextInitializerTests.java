/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.context.initializer;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.TestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

/**
 * @author Dave Syer
 */
public class FileEncodingApplicationContextInitializerTests {

	private FileEncodingApplicationContextInitializer initializer = new FileEncodingApplicationContextInitializer();
	private ConfigurableApplicationContext context;

	@Before
	public void init() {
		this.context = new StaticApplicationContext();
	}

	@Test(expected = IllegalStateException.class)
	public void testIllegalState() {
		TestUtils.addEnviroment(this.context, "spring.mandatory_file_encoding:FOO");
		this.initializer.initialize(this.context);
	}

	@Test
	public void testSunnyDayNothingMandated() {
		this.initializer.initialize(this.context);
	}

	@Test
	public void testSunnyDayMandated() {
		Assume.assumeNotNull(System.getProperty("file.encoding"));
		TestUtils.addEnviroment(this.context,
				"spring.mandatory_file_encoding:" + System.getProperty("file.encoding"));
		this.initializer.initialize(this.context);
	}

}
