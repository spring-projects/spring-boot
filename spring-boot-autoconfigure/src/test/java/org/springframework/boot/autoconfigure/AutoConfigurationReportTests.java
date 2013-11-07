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

import java.util.Iterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

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
	public void recordConditionEvaluations() throws Exception {
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
		AutoConfigurationReport report = AutoConfigurationReport
				.get(new AnnotationConfigApplicationContext(Config.class)
						.getBeanFactory());
		assertThat(report.getConditionAndOutcomesBySource().size(), not(equalTo(0)));
	}

	@Configurable
	@Import(WebMvcAutoConfiguration.class)
	static class Config {

	}

}
