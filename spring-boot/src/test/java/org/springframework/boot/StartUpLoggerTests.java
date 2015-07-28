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

package org.springframework.boot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.apache.commons.logging.impl.SimpleLog;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link StartupInfoLogger}.
 *
 * @author Dave Syer
 */
public class StartUpLoggerTests {

	private final StringBuffer output = new StringBuffer();

	@Before
	public void cleanLog() {
		output.setLength(0);
	}
	
	@SuppressWarnings("serial")
	private final SimpleLog log = new SimpleLog("test") {
		@Override
		protected void write(StringBuffer buffer) {
			StartUpLoggerTests.this.output.append(buffer).append("\n");
		};
	};

	@Test
	public void simpleLogging() {
		new StartupInfoLogger(getClass()).logStarting(this.log);
		assertThat(this.output.toString(), containsString("Starting " + getClass().getSimpleName()));
		// debug is not enabled
		assertThat(this.output.toString(), not(containsString("Running with")));
	}

	@Test
	public void versionLogging() {
		new StartupInfoLogger(getClass()).logStartingWithVersions(this.log);
		assertThat(this.output.toString(), containsString("Starting " + getClass().getSimpleName()));
		assertThat(this.output.toString(), containsString("Running with Spring Boot and these libraries"));
	}

}
