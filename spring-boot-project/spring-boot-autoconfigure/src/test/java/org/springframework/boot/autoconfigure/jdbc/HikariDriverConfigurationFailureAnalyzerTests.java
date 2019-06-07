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

package org.springframework.boot.autoconfigure.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HikariDriverConfigurationFailureAnalyzer}.
 *
 * @author Stephane Nicoll
 */
public class HikariDriverConfigurationFailureAnalyzerTests {

	@Test
	public void failureAnalysisIsPerformed() {
		FailureAnalysis failureAnalysis = performAnalysis(TestConfiguration.class);
		assertThat(failureAnalysis).isNotNull();
		assertThat(failureAnalysis.getDescription()).isEqualTo(
				"Configuration of the Hikari connection pool failed: " + "'dataSourceClassName' is not supported.");
		assertThat(failureAnalysis.getAction()).contains("Spring Boot auto-configures only a driver");
	}

	@Test
	public void unrelatedIllegalStateExceptionIsSkipped() {
		FailureAnalysis failureAnalysis = new HikariDriverConfigurationFailureAnalyzer()
				.analyze(new RuntimeException("foo", new IllegalStateException("bar")));
		assertThat(failureAnalysis).isNull();
	}

	private FailureAnalysis performAnalysis(Class<?> configuration) {
		BeanCreationException failure = createFailure(configuration);
		assertThat(failure).isNotNull();
		return new HikariDriverConfigurationFailureAnalyzer().analyze(failure);
	}

	private BeanCreationException createFailure(Class<?> configuration) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.datasource.type=" + HikariDataSource.class.getName(),
				"spring.datasource.hikari.data-source-class-name=com.example.Foo",
				"spring.datasource.initialization-mode=always").applyTo(context);
		context.register(configuration);
		try {
			context.refresh();
			context.close();
			return null;
		}
		catch (BeanCreationException ex) {
			return ex;
		}
	}

	@Configuration
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class TestConfiguration {

	}

}
