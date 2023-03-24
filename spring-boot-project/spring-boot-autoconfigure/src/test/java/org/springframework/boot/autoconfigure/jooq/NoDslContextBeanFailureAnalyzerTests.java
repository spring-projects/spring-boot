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

package org.springframework.boot.autoconfigure.jooq;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoDslContextBeanFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class NoDslContextBeanFailureAnalyzerTests {

	@Test
	void noAnalysisWithoutR2dbcAutoConfiguration() {
		new ApplicationContextRunner().run((context) -> {
			NoDslContextBeanFailureAnalyzer failureAnalyzer = new NoDslContextBeanFailureAnalyzer(
					context.getBeanFactory());
			assertThat(failureAnalyzer.analyze(new NoSuchBeanDefinitionException(DSLContext.class))).isNull();
		});
	}

	@Test
	void analysisWithR2dbcAutoConfiguration() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				NoDslContextBeanFailureAnalyzer failureAnalyzer = new NoDslContextBeanFailureAnalyzer(
						context.getBeanFactory());
				assertThat(failureAnalyzer.analyze(new NoSuchBeanDefinitionException(DSLContext.class))).isNotNull();
			});
	}

}
