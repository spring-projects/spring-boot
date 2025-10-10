/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.config.UniqueShortNameAutoConfiguration;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportMessage;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionEvaluationReport}.
 *
 * @author Greg Turnquist
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class ConditionEvaluationReportTests {

	private DefaultListableBeanFactory beanFactory;

	private ConditionEvaluationReport report;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private Condition condition1;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private Condition condition2;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private Condition condition3;

	private @Nullable ConditionOutcome outcome1;

	private @Nullable ConditionOutcome outcome2;

	private @Nullable ConditionOutcome outcome3;

	@BeforeEach
	void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		this.report = ConditionEvaluationReport.get(this.beanFactory);
	}

	@Test
	void get() {
		assertThat(this.report).isNotNull();
		assertThat(this.report).isSameAs(ConditionEvaluationReport.get(this.beanFactory));
	}

	@Test
	void parent() {
		this.beanFactory.setParentBeanFactory(new DefaultListableBeanFactory());
		BeanFactory parentBeanFactory = this.beanFactory.getParentBeanFactory();
		assertThat(parentBeanFactory).isNotNull();
		ConditionEvaluationReport.get((ConfigurableListableBeanFactory) parentBeanFactory);
		assertThat(this.report).isSameAs(ConditionEvaluationReport.get(this.beanFactory));
		assertThat(this.report).isNotNull();
		assertThat(this.report.getParent()).isNotNull();
		ConditionEvaluationReport.get((ConfigurableListableBeanFactory) parentBeanFactory);
		assertThat(this.report).isSameAs(ConditionEvaluationReport.get(this.beanFactory));
		assertThat(this.report.getParent())
			.isSameAs(ConditionEvaluationReport.get((ConfigurableListableBeanFactory) parentBeanFactory));
	}

	@Test
	void parentBottomUp() {
		this.beanFactory = new DefaultListableBeanFactory(); // NB: overrides setup
		this.beanFactory.setParentBeanFactory(new DefaultListableBeanFactory());
		BeanFactory parentBeanFactory = this.beanFactory.getParentBeanFactory();
		assertThat(parentBeanFactory).isNotNull();
		ConditionEvaluationReport.get((ConfigurableListableBeanFactory) parentBeanFactory);
		this.report = ConditionEvaluationReport.get(this.beanFactory);
		assertThat(this.report).isNotNull();
		assertThat(this.report).isNotSameAs(this.report.getParent());
		assertThat(this.report.getParent()).isNotNull();
		assertThat(this.report.getParent().getParent()).isNull();
	}

	@Test
	void recordConditionEvaluations() {
		this.outcome1 = new ConditionOutcome(false, "m1");
		this.outcome2 = new ConditionOutcome(false, "m2");
		this.outcome3 = new ConditionOutcome(false, "m3");
		this.report.recordConditionEvaluation("a", this.condition1, this.outcome1);
		this.report.recordConditionEvaluation("a", this.condition2, this.outcome2);
		this.report.recordConditionEvaluation("b", this.condition3, this.outcome3);
		Map<String, ConditionAndOutcomes> map = this.report.getConditionAndOutcomesBySource();
		assertThat(map).hasSize(2);
		ConditionAndOutcomes a = map.get("a");
		assertThat(a).isNotNull();
		Iterator<ConditionAndOutcome> iterator = a.iterator();
		ConditionAndOutcome conditionAndOutcome = iterator.next();
		assertThat(conditionAndOutcome.getCondition()).isEqualTo(this.condition1);
		assertThat(conditionAndOutcome.getOutcome()).isEqualTo(this.outcome1);
		conditionAndOutcome = iterator.next();
		assertThat(conditionAndOutcome.getCondition()).isEqualTo(this.condition2);
		assertThat(conditionAndOutcome.getOutcome()).isEqualTo(this.outcome2);
		assertThat(iterator.hasNext()).isFalse();
		ConditionAndOutcomes b = map.get("b");
		assertThat(b).isNotNull();
		iterator = b.iterator();
		conditionAndOutcome = iterator.next();
		assertThat(conditionAndOutcome.getCondition()).isEqualTo(this.condition3);
		assertThat(conditionAndOutcome.getOutcome()).isEqualTo(this.outcome3);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void fullMatch() {
		prepareMatches(true, true, true);
		ConditionAndOutcomes a = this.report.getConditionAndOutcomesBySource().get("a");
		assertThat(a).isNotNull();
		assertThat(a.isFullMatch()).isTrue();
	}

	@Test
	void notFullMatch() {
		prepareMatches(true, false, true);
		ConditionAndOutcomes a = this.report.getConditionAndOutcomesBySource().get("a");
		assertThat(a).isNotNull();
		assertThat(a.isFullMatch()).isFalse();
	}

	private void prepareMatches(boolean m1, boolean m2, boolean m3) {
		this.outcome1 = new ConditionOutcome(m1, "m1");
		this.outcome2 = new ConditionOutcome(m2, "m2");
		this.outcome3 = new ConditionOutcome(m3, "m3");
		this.report.recordConditionEvaluation("a", this.condition1, this.outcome1);
		this.report.recordConditionEvaluation("a", this.condition2, this.outcome2);
		this.report.recordConditionEvaluation("a", this.condition3, this.outcome3);
	}

	@Test
	@SuppressWarnings("resource")
	void springBootConditionPopulatesReport() {
		ConditionEvaluationReport report = ConditionEvaluationReport
			.get(new AnnotationConfigApplicationContext(Config.class).getBeanFactory());
		assertThat(report.getUnconditionalClasses()).containsExactly(UnconditionalAutoConfiguration.class.getName());
		assertThat(report.getConditionAndOutcomesBySource()).containsOnlyKeys(MatchingAutoConfiguration.class.getName(),
				NonMatchingAutoConfiguration.class.getName());
		assertThat(report.getConditionAndOutcomesBySource().get(MatchingAutoConfiguration.class.getName()))
			.satisfies((outcomes) -> assertThat(outcomes).extracting(ConditionAndOutcome::getOutcome)
				.extracting(ConditionOutcome::isMatch)
				.containsOnly(true));
		assertThat(report.getConditionAndOutcomesBySource().get(NonMatchingAutoConfiguration.class.getName()))
			.satisfies((outcomes) -> assertThat(outcomes).extracting(ConditionAndOutcome::getOutcome)
				.extracting(ConditionOutcome::isMatch)
				.containsOnly(false));
	}

	@Test
	void testDuplicateConditionAndOutcomes() {
		ConditionAndOutcome outcome1 = new ConditionAndOutcome(this.condition1,
				new ConditionOutcome(true, "Message 1"));
		ConditionAndOutcome outcome2 = new ConditionAndOutcome(this.condition2,
				new ConditionOutcome(true, "Message 2"));
		ConditionAndOutcome outcome3 = new ConditionAndOutcome(this.condition3,
				new ConditionOutcome(true, "Message 2"));
		assertThat(outcome1).isNotEqualTo(outcome2);
		assertThat(outcome2).isEqualTo(outcome3);
		ConditionAndOutcomes outcomes = new ConditionAndOutcomes();
		outcomes.add(this.condition1, new ConditionOutcome(true, "Message 1"));
		outcomes.add(this.condition2, new ConditionOutcome(true, "Message 2"));
		outcomes.add(this.condition3, new ConditionOutcome(true, "Message 2"));
		assertThat(outcomes).hasSize(2);
	}

	@Test
	void negativeOuterPositiveInnerBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("test.present=true").applyTo(context);
		context.register(NegativeOuterConfig.class);
		context.refresh();
		ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
		Map<String, ConditionAndOutcomes> sourceOutcomes = report.getConditionAndOutcomesBySource();
		assertThat(context.containsBean("negativeOuterPositiveInnerBean")).isFalse();
		String negativeConfig = NegativeOuterConfig.class.getName();
		ConditionAndOutcomes negativeOutcome = sourceOutcomes.get(negativeConfig);
		assertThat(negativeOutcome).isNotNull();
		assertThat(negativeOutcome.isFullMatch()).isFalse();
		String positiveConfig = NegativeOuterConfig.PositiveInnerConfig.class.getName();
		ConditionAndOutcomes positiveOutcome = sourceOutcomes.get(positiveConfig);
		assertThat(positiveOutcome).isNotNull();
		assertThat(positiveOutcome.isFullMatch()).isFalse();
	}

	@Test
	void reportWhenSameShortNamePresentMoreThanOnceShouldUseFullyQualifiedName() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(UniqueShortNameAutoConfiguration.class,
				org.springframework.boot.autoconfigure.condition.config.first.SampleAutoConfiguration.class,
				org.springframework.boot.autoconfigure.condition.config.second.SampleAutoConfiguration.class);
		context.refresh();
		ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
		assertThat(report.getConditionAndOutcomesBySource()).containsKeys(
				"org.springframework.boot.autoconfigure.condition.config.UniqueShortNameAutoConfiguration",
				"org.springframework.boot.autoconfigure.condition.config.first.SampleAutoConfiguration",
				"org.springframework.boot.autoconfigure.condition.config.second.SampleAutoConfiguration");
		context.close();
	}

	@Test
	void reportMessageWhenSameShortNamePresentMoreThanOnceShouldUseFullyQualifiedName() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(UniqueShortNameAutoConfiguration.class,
				org.springframework.boot.autoconfigure.condition.config.first.SampleAutoConfiguration.class,
				org.springframework.boot.autoconfigure.condition.config.second.SampleAutoConfiguration.class);
		context.refresh();
		ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
		String reportMessage = new ConditionEvaluationReportMessage(report).toString();
		assertThat(reportMessage).contains("UniqueShortNameAutoConfiguration",
				"org.springframework.boot.autoconfigure.condition.config.first.SampleAutoConfiguration",
				"org.springframework.boot.autoconfigure.condition.config.second.SampleAutoConfiguration");
		assertThat(reportMessage)
			.doesNotContain("org.springframework.boot.autoconfigure.condition.config.UniqueShortNameAutoConfiguration");
		context.close();
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional({ ConditionEvaluationReportTests.MatchParseCondition.class,
			ConditionEvaluationReportTests.NoMatchBeanCondition.class })
	static class NegativeOuterConfig {

		@Configuration(proxyBeanMethods = false)
		@Conditional({ ConditionEvaluationReportTests.MatchParseCondition.class })
		static class PositiveInnerConfig {

			@Bean
			String negativeOuterPositiveInnerBean() {
				return "negativeOuterPositiveInnerBean";
			}

		}

	}

	static class TestMatchCondition extends SpringBootCondition implements ConfigurationCondition {

		private final ConfigurationPhase phase;

		private final boolean match;

		TestMatchCondition(ConfigurationPhase phase, boolean match) {
			this.phase = phase;
			this.match = match;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return this.phase;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return new ConditionOutcome(this.match, ClassUtils.getShortName(getClass()));
		}

	}

	static class MatchParseCondition extends TestMatchCondition {

		MatchParseCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION, true);
		}

	}

	static class MatchBeanCondition extends TestMatchCondition {

		MatchBeanCondition() {
			super(ConfigurationPhase.REGISTER_BEAN, true);
		}

	}

	static class NoMatchParseCondition extends TestMatchCondition {

		NoMatchParseCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION, false);
		}

	}

	static class NoMatchBeanCondition extends TestMatchCondition {

		NoMatchBeanCondition() {
			super(ConfigurationPhase.REGISTER_BEAN, false);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ MatchingAutoConfiguration.class, NonMatchingAutoConfiguration.class,
			UnconditionalAutoConfiguration.class })
	static class Config {

	}

	@AutoConfiguration
	@ConditionalOnProperty(name = "com.example.property", matchIfMissing = true)
	public static final class MatchingAutoConfiguration {

	}

	@AutoConfiguration
	@ConditionalOnBean(Duration.class)
	public static final class NonMatchingAutoConfiguration {

	}

	@AutoConfiguration
	public static final class UnconditionalAutoConfiguration {

		@Bean
		String example() {
			return "example";
		}

	}

}
