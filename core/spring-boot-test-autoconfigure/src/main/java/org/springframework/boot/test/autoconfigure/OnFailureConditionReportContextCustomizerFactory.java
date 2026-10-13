/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportMessage;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizerFactory} that customizes the {@link ApplicationContext
 * application context} such that a {@link ConditionEvaluationReport condition evaluation
 * report} is output when the application under test {@link ApplicationFailedEvent fails
 * to start}.
 *
 * @author Andy Wilkinson
 */
class OnFailureConditionReportContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		return new OnFailureConditionReportContextCustomizer();
	}

	static class OnFailureConditionReportContextCustomizer implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			Supplier<ConditionEvaluationReport> reportSupplier;
			if (context instanceof GenericApplicationContext) {
				ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
				reportSupplier = () -> report;
			}
			else {
				reportSupplier = () -> ConditionEvaluationReport.get(context.getBeanFactory());
			}
			context.addApplicationListener(new ApplicationFailureListener(reportSupplier));
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null) && (obj.getClass() == getClass());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

	private static final class ApplicationFailureListener implements ApplicationListener<ApplicationFailedEvent> {

		private final Supplier<ConditionEvaluationReport> reportSupplier;

		private ApplicationFailureListener(Supplier<ConditionEvaluationReport> reportSupplier) {
			this.reportSupplier = reportSupplier;
		}

		@Override
		public void onApplicationEvent(ApplicationFailedEvent event) {
			if (shouldPrintReport(event.getApplicationContext())) {
				System.err.println(new ConditionEvaluationReportMessage(this.reportSupplier.get()));
			}
		}

		private static boolean shouldPrintReport(@Nullable ConfigurableApplicationContext context) {
			return (context == null) || context.getEnvironment()
				.getProperty("spring.test.print-condition-evaluation-report", Boolean.class, true);
		}

	}

}
