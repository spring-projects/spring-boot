/*
 * Copyright 2012-present the original author or authors.
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
