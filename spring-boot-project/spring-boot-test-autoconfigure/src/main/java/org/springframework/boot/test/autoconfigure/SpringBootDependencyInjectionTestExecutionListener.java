/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportMessage;
import org.springframework.boot.test.context.DefaultTestExecutionListenersPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * Alternative {@link DependencyInjectionTestExecutionListener} prints the
 * {@link ConditionEvaluationReport} when the context cannot be prepared.
 *
 * @author Phillip Webb
 * @since 1.4.1
 */
public class SpringBootDependencyInjectionTestExecutionListener
		extends DependencyInjectionTestExecutionListener {

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		try {
			super.prepareTestInstance(testContext);
		}
		catch (Exception ex) {
			outputConditionEvaluationReport(testContext);
			throw ex;
		}
	}

	private void outputConditionEvaluationReport(TestContext testContext) {
		try {
			ApplicationContext context = testContext.getApplicationContext();
			if (context instanceof ConfigurableApplicationContext) {
				ConditionEvaluationReport report = ConditionEvaluationReport
						.get(((ConfigurableApplicationContext) context).getBeanFactory());
				System.err.println(new ConditionEvaluationReportMessage(report));
			}
		}
		catch (Exception ex) {
			// Allow original failure to be reported
		}
	}

	static class PostProcessor implements DefaultTestExecutionListenersPostProcessor {

		@Override
		public Set<Class<? extends TestExecutionListener>> postProcessDefaultTestExecutionListeners(
				Set<Class<? extends TestExecutionListener>> listeners) {
			Set<Class<? extends TestExecutionListener>> updated = new LinkedHashSet<>(
					listeners.size());
			for (Class<? extends TestExecutionListener> listener : listeners) {
				updated.add(
						listener.equals(DependencyInjectionTestExecutionListener.class)
								? SpringBootDependencyInjectionTestExecutionListener.class
								: listener);
			}
			return updated;
		}

	}

}
