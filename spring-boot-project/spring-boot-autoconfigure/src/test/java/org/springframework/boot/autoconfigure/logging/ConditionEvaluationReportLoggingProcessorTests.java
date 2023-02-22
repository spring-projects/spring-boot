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

package org.springframework.boot.autoconfigure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionEvaluationReportLoggingProcessor}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class ConditionEvaluationReportLoggingProcessorTests {

	@Test
	void logsDebugOnProcessAheadOfTime(CapturedOutput output) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		ConditionEvaluationReport.get(beanFactory)
			.recordConditionEvaluation("test", mock(Condition.class), ConditionOutcome.match());
		ConditionEvaluationReportLoggingProcessor processor = new ConditionEvaluationReportLoggingProcessor();
		processor.processAheadOfTime(beanFactory);
		assertThat(output).doesNotContain("CONDITIONS EVALUATION REPORT");
		withDebugLogging(() -> processor.processAheadOfTime(beanFactory));
		assertThat(output).contains("CONDITIONS EVALUATION REPORT");
	}

	private void withDebugLogging(Runnable runnable) {
		Logger logger = ((LoggerContext) LoggerFactory.getILoggerFactory())
			.getLogger(ConditionEvaluationReportLogger.class);
		Level currentLevel = logger.getLevel();
		logger.setLevel(Level.DEBUG);
		try {
			runnable.run();
		}
		finally {
			logger.setLevel(currentLevel);
		}
	}

}
