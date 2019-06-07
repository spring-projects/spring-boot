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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ClassPathOverrides;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4J2MetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathOverrides({ "org.apache.logging.log4j:log4j-to-slf4j:2.11.1", "org.apache.logging.log4j:log4j-core:2.11.1" })
@ClassPathExclusions("log4j-slf4j-impl-*.jar")
public class Log4J2MetricsWithSlf4jLoggerContextAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(Log4J2MetricsAutoConfiguration.class));

	@Test
	public void backsOffWhenLoggerContextIsBackedBySlf4j() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Log4j2Metrics.class));
	}

}
