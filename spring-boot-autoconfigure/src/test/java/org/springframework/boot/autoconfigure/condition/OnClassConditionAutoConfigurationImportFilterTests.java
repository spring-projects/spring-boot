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

package org.springframework.boot.autoconfigure.condition;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.core.io.support.SpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link AutoConfigurationImportFilter} part of {@link OnClassCondition}.
 *
 * @author Phillip Webb
 */
public class OnClassConditionAutoConfigurationImportFilterTests {

	private OnClassCondition filter = new OnClassCondition();

	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Before
	public void setup() {
		this.filter.setBeanClassLoader(getClass().getClassLoader());
		this.filter.setBeanFactory(this.beanFactory);
	}

	@Test
	public void shouldBeRegistered() throws Exception {
		assertThat(SpringFactoriesLoader
				.loadFactories(AutoConfigurationImportFilter.class, null))
						.hasAtLeastOneElementOfType(OnClassCondition.class);
	}

	@Test
	public void matchShouldMatchClasses() throws Exception {
		String[] autoConfigurationClasses = new String[] { "test.match", "test.nomatch" };
		boolean[] result = this.filter.match(autoConfigurationClasses,
				getAutoConfigurationMetadata());
		assertThat(result).containsExactly(true, false);
	}

	@Test
	public void matchShouldRecordOutcome() throws Exception {
		String[] autoConfigurationClasses = new String[] { "test.match", "test.nomatch" };
		this.filter.match(autoConfigurationClasses, getAutoConfigurationMetadata());
		ConditionEvaluationReport report = ConditionEvaluationReport
				.get(this.beanFactory);
		assertThat(report.getConditionAndOutcomesBySource()).hasSize(1)
				.containsKey("test.nomatch");
	}

	private AutoConfigurationMetadata getAutoConfigurationMetadata() {
		AutoConfigurationMetadata metadata = mock(AutoConfigurationMetadata.class);
		given(metadata.wasProcessed("test.match")).willReturn(true);
		given(metadata.getSet("test.match", "ConditionalOnClass"))
				.willReturn(Collections.singleton("java.io.InputStream"));
		given(metadata.wasProcessed("test.nomatch")).willReturn(true);
		given(metadata.getSet("test.nomatch", "ConditionalOnClass"))
				.willReturn(Collections.singleton("java.io.DoesNotExist"));
		return metadata;
	}

}
