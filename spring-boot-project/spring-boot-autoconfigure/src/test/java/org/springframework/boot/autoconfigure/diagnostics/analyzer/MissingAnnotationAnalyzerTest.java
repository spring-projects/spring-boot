/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.diagnostics.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import org.elasticsearch.common.util.set.Sets;
import org.junit.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.event.FailedStartupInfoHolder;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ApplicationContextException;

public class MissingAnnotationAnalyzerTest {

	private MissingAnnotationAnalyzer analyzer = new MissingAnnotationAnalyzer();

	@Test
	public void exceptionWithConfiguredClassReturnsNull() {
		configureInfoHolder(ProperlyConfigured.class);

		FailureAnalysis analysis = analyzeFailure(new ApplicationContextException("Test"));
		assertThat(analysis).isNull();
	}

	@Test
	public void exceptionWithUnconfiguredClassReturnsFailureAnalysis() {
		configureInfoHolder(NotConfigured.class);

		FailureAnalysis analysis = analyzeFailure(new ApplicationContextException("Test"));
		assertThat(analysis).isNotNull();
		assertThat(analysis.getAction()).contains(NotConfigured.class.getName());
	}

	@Test
	public void exceptionWithUnconfiguredClassAndDifferentMain() {
		FailedStartupInfoHolder.setAllSources(Sets.newHashSet(ProperlyConfigured.class));
		FailedStartupInfoHolder.setMainApplication(NotConfigured.class);

		FailureAnalysis analysis = analyzeFailure(new ApplicationContextException("Test"));
		assertThat(analysis).isNull();
	}

	private void configureInfoHolder(Class<?> infoHolderClass) {
		FailedStartupInfoHolder.setAllSources(Sets.newHashSet(infoHolderClass));
		FailedStartupInfoHolder.setMainApplication(infoHolderClass);
	}

	private FailureAnalysis analyzeFailure(Exception failure) {
		return this.analyzer.analyze(failure);
	}

	@SpringBootApplication
	private static class ProperlyConfigured {

	}

	private static class NotConfigured {

	}

}
