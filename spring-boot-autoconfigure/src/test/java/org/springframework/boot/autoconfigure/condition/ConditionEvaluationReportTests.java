/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ConditionEvaluationReport}.
 *
 * @author Greg Turnquist
 * @author Phillip Webb
 */
public class ConditionEvaluationReportTests {

	private DefaultListableBeanFactory beanFactory;

	private ConditionEvaluationReport report;

	@Mock
	private Condition condition1;

	@Mock
	private Condition condition2;

	@Mock
	private Condition condition3;

	private ConditionOutcome outcome1;

	private ConditionOutcome outcome2;

	private ConditionOutcome outcome3;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.beanFactory = new DefaultListableBeanFactory();
		this.report = ConditionEvaluationReport.get(this.beanFactory);
	}

	@Test
	public void get() throws Exception {
		assertThat(this.report, not(nullValue()));
		assertThat(this.report,
				sameInstance(ConditionEvaluationReport.get(this.beanFactory)));
	}

	@Test
	public void parent() throws Exception {
		this.beanFactory.setParentBeanFactory(new DefaultListableBeanFactory());
		ConditionEvaluationReport.get((ConfigurableListableBeanFactory) this.beanFactory
				.getParentBeanFactory());
		assertThat(this.report,
				sameInstance(ConditionEvaluationReport.get(this.beanFactory)));
		assertThat(this.report, not(nullValue()));
		assertThat(this.report.getParent(), not(nullValue()));
		ConditionEvaluationReport.get((ConfigurableListableBeanFactory) this.beanFactory
				.getParentBeanFactory());
		assertThat(this.report,
				sameInstance(ConditionEvaluationReport.get(this.beanFactory)));
		assertThat(this.report.getParent(),
				sameInstance(ConditionEvaluationReport
						.get((ConfigurableListableBeanFactory) this.beanFactory
								.getParentBeanFactory())));
	}

	@Test
	public void parentBottomUp() throws Exception {
		this.beanFactory = new DefaultListableBeanFactory(); // NB: overrides setup
		this.beanFactory.setParentBeanFactory(new DefaultListableBeanFactory());
		ConditionEvaluationReport.get((ConfigurableListableBeanFactory) this.beanFactory
				.getParentBeanFactory());
		this.report = ConditionEvaluationReport.get(this.beanFactory);
		assertThat(this.report, not(nullValue()));
		assertThat(this.report, not(sameInstance(this.report.getParent())));
		assertThat(this.report.getParent(), not(nullValue()));
		assertThat(this.report.getParent().getParent(), nullValue());
	}

	@Test
	public void recordConditionEvaluations() throws Exception {
		this.outcome1 = new ConditionOutcome(false, "m1");
		this.outcome2 = new ConditionOutcome(false, "m2");
		this.outcome3 = new ConditionOutcome(false, "m3");
		this.report.recordConditionEvaluation("a", this.condition1, this.outcome1);
		this.report.recordConditionEvaluation("a", this.condition2, this.outcome2);
		this.report.recordConditionEvaluation("b", this.condition3, this.outcome3);
		Map<String, ConditionAndOutcomes> map = this.report
				.getConditionAndOutcomesBySource();
		assertThat(map.size(), equalTo(2));
		Iterator<ConditionAndOutcome> iterator = map.get("a").iterator();

		ConditionAndOutcome conditionAndOutcome = iterator.next();
		assertThat(conditionAndOutcome.getCondition(), equalTo(this.condition1));
		assertThat(conditionAndOutcome.getOutcome(), equalTo(this.outcome1));

		conditionAndOutcome = iterator.next();
		assertThat(conditionAndOutcome.getCondition(), equalTo(this.condition2));
		assertThat(conditionAndOutcome.getOutcome(), equalTo(this.outcome2));
		assertThat(iterator.hasNext(), equalTo(false));

		iterator = map.get("b").iterator();
		conditionAndOutcome = iterator.next();
		assertThat(conditionAndOutcome.getCondition(), equalTo(this.condition3));
		assertThat(conditionAndOutcome.getOutcome(), equalTo(this.outcome3));
		assertThat(iterator.hasNext(), equalTo(false));
	}

	@Test
	public void fullMatch() throws Exception {
		prepareMatches(true, true, true);
		assertThat(this.report.getConditionAndOutcomesBySource().get("a").isFullMatch(),
				equalTo(true));
	}

	@Test
	public void notFullMatch() throws Exception {
		prepareMatches(true, false, true);
		assertThat(this.report.getConditionAndOutcomesBySource().get("a").isFullMatch(),
				equalTo(false));
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
	public void springBootConditionPopulatesReport() throws Exception {
		ConditionEvaluationReport report = ConditionEvaluationReport
				.get(new AnnotationConfigApplicationContext(Config.class)
						.getBeanFactory());
		assertThat(report.getConditionAndOutcomesBySource().size(), not(equalTo(0)));
	}

	@Test
	public void testDuplicateConditionAndOutcomes() {
		ConditionAndOutcome outcome1 = new ConditionAndOutcome(this.condition1,
				new ConditionOutcome(true, "Message 1"));
		ConditionAndOutcome outcome2 = new ConditionAndOutcome(this.condition2,
				new ConditionOutcome(true, "Message 2"));
		ConditionAndOutcome outcome3 = new ConditionAndOutcome(this.condition3,
				new ConditionOutcome(true, "Message 2"));

		assertThat(outcome1, equalTo(outcome1));
		assertThat(outcome1, not(equalTo(outcome2)));
		assertThat(outcome2, equalTo(outcome3));

		ConditionAndOutcomes outcomes = new ConditionAndOutcomes();
		outcomes.add(this.condition1, new ConditionOutcome(true, "Message 1"));
		outcomes.add(this.condition2, new ConditionOutcome(true, "Message 2"));
		outcomes.add(this.condition3, new ConditionOutcome(true, "Message 2"));

		assertThat(getNumberOfOutcomes(outcomes), equalTo(2));
	}

	@Test
	public void duplicateOutcomes() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DuplicateConfig.class);
		ConditionEvaluationReport report = ConditionEvaluationReport.get(context
				.getBeanFactory());
		String autoconfigKey = MultipartAutoConfiguration.class.getName();

		ConditionAndOutcomes outcomes = report.getConditionAndOutcomesBySource().get(
				autoconfigKey);
		assertThat(outcomes, not(nullValue()));
		assertThat(getNumberOfOutcomes(outcomes), equalTo(2));

		List<String> messages = new ArrayList<String>();
		for (ConditionAndOutcome outcome : outcomes) {
			messages.add(outcome.getOutcome().getMessage());
		}

		Matcher<String> onClassMessage = containsString("@ConditionalOnClass "
				+ "classes found: javax.servlet.Servlet,org.springframework.web.multipart.support.StandardServletMultipartResolver");
		assertThat(messages, hasItem(onClassMessage));
		context.close();
	}

	@Test
	public void negativeOuterPositiveInnerBean() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "test.present=true");
		context.register(NegativeOuterConfig.class);
		context.refresh();
		ConditionEvaluationReport report = ConditionEvaluationReport.get(context
				.getBeanFactory());
		Map<String, ConditionAndOutcomes> sourceOutcomes = report
				.getConditionAndOutcomesBySource();
		assertThat(context.containsBean("negativeOuterPositiveInnerBean"), equalTo(false));
		String negativeConfig = NegativeOuterConfig.class.getName();
		assertThat(sourceOutcomes.get(negativeConfig).isFullMatch(), equalTo(false));
		String positiveConfig = NegativeOuterConfig.PositiveInnerConfig.class.getName();
		assertThat(sourceOutcomes.get(positiveConfig).isFullMatch(), equalTo(false));
	}

	private int getNumberOfOutcomes(ConditionAndOutcomes outcomes) {
		Iterator<ConditionAndOutcome> iterator = outcomes.iterator();
		int numberOfOutcomesAdded = 0;
		while (iterator.hasNext()) {
			numberOfOutcomesAdded++;
			iterator.next();
		}
		return numberOfOutcomesAdded;
	}

	@Configuration
	@Import(WebMvcAutoConfiguration.class)
	static class Config {

	}

	@Configuration
	@Import(MultipartAutoConfiguration.class)
	static class DuplicateConfig {

	}

	@Configuration
	@Conditional({ ConditionEvaluationReportTests.MatchParseCondition.class,
			ConditionEvaluationReportTests.NoMatchBeanCondition.class })
	public static class NegativeOuterConfig {

		@Configuration
		@Conditional({ ConditionEvaluationReportTests.MatchParseCondition.class })
		public static class PositiveInnerConfig {

			@Bean
			public String negativeOuterPositiveInnerBean() {
				return "negativeOuterPositiveInnerBean";
			}

		}
	}

	static class TestMatchCondition extends SpringBootCondition implements
			ConfigurationCondition {

		private final ConfigurationPhase phase;
		private final boolean match;

		public TestMatchCondition(ConfigurationPhase phase, boolean match) {
			this.phase = phase;
			this.match = match;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return this.phase;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			return new ConditionOutcome(this.match, ClassUtils.getShortName(getClass()));
		}

	}

	static class MatchParseCondition extends TestMatchCondition {

		public MatchParseCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION, true);
		}

	}

	static class MatchBeanCondition extends TestMatchCondition {

		public MatchBeanCondition() {
			super(ConfigurationPhase.REGISTER_BEAN, true);
		}

	}

	static class NoMatchParseCondition extends TestMatchCondition {

		public NoMatchParseCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION, false);
		}

	}

	static class NoMatchBeanCondition extends TestMatchCondition {

		public NoMatchBeanCondition() {
			super(ConfigurationPhase.REGISTER_BEAN, false);
		}

	}

}
