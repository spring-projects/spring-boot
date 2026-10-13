/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationImportEvent;
import org.springframework.boot.autoconfigure.AutoConfigurationImportListener;
import org.springframework.core.io.support.SpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionEvaluationReportAutoConfigurationImportListener}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ConditionEvaluationReportAutoConfigurationImportListenerTests {

	private ConditionEvaluationReportAutoConfigurationImportListener listener;

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@BeforeEach
	void setup() {
		this.listener = new ConditionEvaluationReportAutoConfigurationImportListener();
		this.listener.setBeanFactory(this.beanFactory);
	}

	@Test
	void shouldBeInSpringFactories() {
		List<AutoConfigurationImportListener> factories = SpringFactoriesLoader
			.loadFactories(AutoConfigurationImportListener.class, null);
		assertThat(factories)
			.hasAtLeastOneElementOfType(ConditionEvaluationReportAutoConfigurationImportListener.class);
	}

	@Test
	void onAutoConfigurationImportEventShouldRecordCandidates() {
		List<String> candidateConfigurations = Collections.singletonList("Test");
		Set<String> exclusions = Collections.emptySet();
		AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, candidateConfigurations,
				exclusions);
		this.listener.onAutoConfigurationImportEvent(event);
		ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
		assertThat(report.getUnconditionalClasses()).containsExactlyElementsOf(candidateConfigurations);
	}

	@Test
	void onAutoConfigurationImportEventShouldRecordExclusions() {
		List<String> candidateConfigurations = Collections.emptyList();
		Set<String> exclusions = Collections.singleton("Test");
		AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, candidateConfigurations,
				exclusions);
		this.listener.onAutoConfigurationImportEvent(event);
		ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
		assertThat(report.getExclusions()).containsExactlyElementsOf(exclusions);
	}

	@Test
	void onAutoConfigurationImportEventShouldApplyExclusionsGlobally() {
		AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, Arrays.asList("First", "Second"),
				Collections.emptySet());
		this.listener.onAutoConfigurationImportEvent(event);
		AutoConfigurationImportEvent anotherEvent = new AutoConfigurationImportEvent(this, Collections.emptyList(),
				Collections.singleton("First"));
		this.listener.onAutoConfigurationImportEvent(anotherEvent);
		ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
		assertThat(report.getUnconditionalClasses()).containsExactly("Second");
		assertThat(report.getExclusions()).containsExactly("First");
	}

	@Test
	void onAutoConfigurationImportEventShouldApplyExclusionsGloballyWhenExclusionIsAlreadyApplied() {
		AutoConfigurationImportEvent excludeEvent = new AutoConfigurationImportEvent(this, Collections.emptyList(),
				Collections.singleton("First"));
		this.listener.onAutoConfigurationImportEvent(excludeEvent);
		AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, Arrays.asList("First", "Second"),
				Collections.emptySet());
		this.listener.onAutoConfigurationImportEvent(event);
		ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
		assertThat(report.getUnconditionalClasses()).containsExactly("Second");
		assertThat(report.getExclusions()).containsExactly("First");
	}

}
