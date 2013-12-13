/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfigurationReport}.
 * 
 * @author Greg Turnquist
 * @author Phillip Webb
 */
public class AutoConfigurationReportTests {

	private DefaultListableBeanFactory beanFactory;

	private AutoConfigurationReport report;

	@Mock
	private Condition condition1;

	@Mock
	private Condition condition2;

	@Mock
	private Condition condition3;

	@Mock
	private ConditionOutcome outcome1;

	@Mock
	private ConditionOutcome outcome2;

	@Mock
	private ConditionOutcome outcome3;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.beanFactory = new DefaultListableBeanFactory();
		this.report = AutoConfigurationReport.get(this.beanFactory);
	}

	@Test
	public void get() throws Exception {
		assertThat(this.report, not(nullValue()));
		assertThat(this.report,
				sameInstance(AutoConfigurationReport.get(this.beanFactory)));
	}

	@Test
	public void parent() throws Exception {
		this.beanFactory.setParentBeanFactory(new DefaultListableBeanFactory());
		AutoConfigurationReport.get((ConfigurableListableBeanFactory) this.beanFactory.getParentBeanFactory());
		assertThat(this.report,
				sameInstance(AutoConfigurationReport.get(this.beanFactory)));
		assertThat(this.report, not(nullValue()));
		assertThat(this.report.getParent(), not(nullValue()));
	}

	@Test
	public void recordConditionEvaluations() throws Exception {
		given(this.outcome1.getMessage()).willReturn("Message 1");
		given(this.outcome2.getMessage()).willReturn("Message 2");
		given(this.outcome3.getMessage()).willReturn("Message 3");

		this.report.recordConditionEvaluation("a", this.condition1, this.outcome1);
		this.report.recordConditionEvaluation("a", this.condition2, this.outcome2);
		this.report.recordConditionEvaluation("b", this.condition3, this.outcome3);

		Map<String, ConditionAndOutcomes> map = this.report.getConditionAndOutcomesBySource();
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
		given(this.outcome1.isMatch()).willReturn(m1);
		given(this.outcome2.isMatch()).willReturn(m2);
		given(this.outcome3.isMatch()).willReturn(m3);
		this.report.recordConditionEvaluation("a", this.condition1, this.outcome1);
		this.report.recordConditionEvaluation("a", this.condition2, this.outcome2);
		this.report.recordConditionEvaluation("a", this.condition3, this.outcome3);
	}

	@Test
	@SuppressWarnings("resource")
	public void springBootConditionPopulatesReport() throws Exception {
		AutoConfigurationReport report = AutoConfigurationReport.get(new AnnotationConfigApplicationContext(
				Config.class).getBeanFactory());
		assertThat(report.getConditionAndOutcomesBySource().size(), not(equalTo(0)));
	}

	@Test
	public void testDuplicateConditionAndOutcomes() {
		Condition condition1 = mock(Condition.class);
		ConditionOutcome conditionOutcome1 = mock(ConditionOutcome.class);
		given(conditionOutcome1.getMessage()).willReturn("This is message 1");

		Condition condition2 = mock(Condition.class);
		ConditionOutcome conditionOutcome2 = mock(ConditionOutcome.class);
		given(conditionOutcome2.getMessage()).willReturn("This is message 2");

		Condition condition3 = mock(Condition.class);
		ConditionOutcome conditionOutcome3 = mock(ConditionOutcome.class);
		given(conditionOutcome3.getMessage()).willReturn("This is message 2"); // identical in value to #2

		ConditionAndOutcome outcome1 = new ConditionAndOutcome(condition1,
				conditionOutcome1);
		assertThat(outcome1, equalTo(outcome1));

		ConditionAndOutcome outcome2 = new ConditionAndOutcome(condition2,
				conditionOutcome2);
		assertThat(outcome1, not(equalTo(outcome2)));

		ConditionAndOutcome outcome3 = new ConditionAndOutcome(condition3,
				conditionOutcome3);
		assertThat(outcome2, equalTo(outcome3));

		Set<ConditionAndOutcome> set = new HashSet<ConditionAndOutcome>();
		set.add(outcome1);
		set.add(outcome2);
		set.add(outcome3);
		assertThat(set.size(), equalTo(2));

		ConditionAndOutcomes outcomes = new ConditionAndOutcomes();
		outcomes.add(condition1, conditionOutcome1);
		outcomes.add(condition2, conditionOutcome2);
		outcomes.add(condition3, conditionOutcome3);

		int i = 0;
		Iterator<ConditionAndOutcome> iterator = outcomes.iterator();
		while (iterator.hasNext()) {
			i++;
			iterator.next();
		}
		assertThat(i, equalTo(2));
	}

	@Test
	public void duplicateOutcomes() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DuplicateConfig.class);
		AutoConfigurationReport report = AutoConfigurationReport.get(context.getBeanFactory());
		String autoconfigKey = "org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration";

		assertThat(report.getConditionAndOutcomesBySource().keySet(),
				hasItem(autoconfigKey));

		ConditionAndOutcomes conditionAndOutcomes = report.getConditionAndOutcomesBySource().get(
				autoconfigKey);
		Iterator<ConditionAndOutcome> iterator = conditionAndOutcomes.iterator();
		int i = 0;
		boolean foundConditionalOnClass = false;
		boolean foundConditionalOnBean = false;
		while (iterator.hasNext()) {
			ConditionAndOutcome conditionAndOutcome = iterator.next();
			if (conditionAndOutcome.getOutcome().getMessage().contains(
					"@ConditionalOnClass classes found: javax.servlet.Servlet,org.springframework.web.multipart.support.StandardServletMultipartResolver")) {
				foundConditionalOnClass = true;
			}
			if (conditionAndOutcome.getOutcome().getMessage().contains(
					"@ConditionalOnBean (types: javax.servlet.MultipartConfigElement; SearchStrategy: all) found no beans")) {
				foundConditionalOnBean = true;
			}
			i++;
		}

		assertThat(i, equalTo(2));
		assertTrue(foundConditionalOnClass);
		assertTrue(foundConditionalOnBean);

	}

	@Configurable
	@Import(WebMvcAutoConfiguration.class)
	static class Config {

	}

	@Configurable
	@Import(MultipartAutoConfiguration.class)
	static class DuplicateConfig {

	}

}
