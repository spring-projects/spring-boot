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

package org.springframework.boot.test.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ApplicationContextFailureProcessor;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * Since 3.0.0 this class has been replaced by
 * {@link ConditionReportApplicationContextFailureProcessor} and is not used internally.
 *
 * @author Phillip Webb
 * @since 1.4.1
 * @deprecated since 3.0.0 for removal in 3.2.0 in favor of
 * {@link ApplicationContextFailureProcessor}
 */
@Deprecated(since = "3.0.0", forRemoval = true)
public class SpringBootDependencyInjectionTestExecutionListener extends DependencyInjectionTestExecutionListener {

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
			if (context instanceof ConfigurableApplicationContext configurableContext) {
				ConditionEvaluationReport report = ConditionEvaluationReport.get(configurableContext.getBeanFactory());
				System.err.println(new ConditionEvaluationReportMessage(report));
			}
		}
		catch (Exception ex) {
			// Allow original failure to be reported
		}
	}

}
