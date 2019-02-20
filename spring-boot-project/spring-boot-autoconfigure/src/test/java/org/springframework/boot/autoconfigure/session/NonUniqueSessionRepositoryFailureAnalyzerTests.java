/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter;
import org.springframework.session.SessionRepository;
import org.springframework.session.hazelcast.HazelcastSessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NonUniqueSessionRepositoryFailureAnalyzer}.
 *
 * @author Stephane Nicoll
 */
public class NonUniqueSessionRepositoryFailureAnalyzerTests {

	private final FailureAnalyzer analyzer = new NonUniqueSessionRepositoryFailureAnalyzer();

	@Test
	public void failureAnalysisWithMultipleCandidates() {
		FailureAnalysis analysis = analyzeFailure(createFailure(
				JdbcOperationsSessionRepository.class, HazelcastSessionRepository.class));
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).contains(
				JdbcOperationsSessionRepository.class.getName(),
				HazelcastSessionRepository.class.getName());
		assertThat(analysis.getAction()).contains("spring.session.store-type");
	}

	@SafeVarargs
	private final Exception createFailure(
			Class<? extends SessionRepository<?>>... candidates) {
		return new NonUniqueSessionRepositoryException(Arrays.asList(candidates));
	}

	private FailureAnalysis analyzeFailure(Exception failure) {
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		if (analysis != null) {
			new LoggingFailureAnalysisReporter().report(analysis);
		}
		return analysis;
	}

}
