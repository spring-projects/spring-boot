/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SpringEnvironmentLookup}.
 *
 * @author Phillip Webb
 */
class SpringEnvironmentLookupTests {

	private MockEnvironment environment;

	private LoggerContext loggerContext;

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.loggerContext = (LoggerContext) LogManager.getContext(false);
		this.loggerContext.putObject(Log4J2LoggingSystem.ENVIRONMENT_KEY, this.environment);
	}

	@AfterEach
	void cleanup() {
		this.loggerContext.removeObject(Log4J2LoggingSystem.ENVIRONMENT_KEY);
	}

	@Test
	void lookupWhenFoundInEnvironmentReturnsValue() {
		this.environment.setProperty("test", "test");
		Interpolator lookup = createLookup(this.loggerContext);
		assertThat(lookup.lookup("spring:test")).isEqualTo("test");
	}

	@Test
	void lookupWhenNotFoundInEnvironmentReturnsNull() {
		Interpolator lookup = createLookup(this.loggerContext);
		assertThat(lookup.lookup("spring:test")).isNull();
	}

	@Test
	void lookupWhenNoSpringEnvironmentThrowsException() {
		this.loggerContext.removeObject(Log4J2LoggingSystem.ENVIRONMENT_KEY);
		Interpolator lookup = createLookup(this.loggerContext);
		assertThatIllegalStateException().isThrownBy(() -> lookup.lookup("spring:test"))
			.withMessage("Unable to obtain Spring Environment from LoggerContext");
	}

	private Interpolator createLookup(LoggerContext context) {
		Interpolator lookup = new Interpolator();
		lookup.setConfiguration(context.getConfiguration());
		lookup.setLoggerContext(context);
		return lookup;
	}

}
