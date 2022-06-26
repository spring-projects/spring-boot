/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.context.bootstrap;

import java.util.List;

import org.springframework.boot.test.context.DefaultTestExecutionListenersPostProcessor;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test {@link DefaultTestExecutionListenersPostProcessor}.
 *
 * @author Phillip Webb
 */
public class TestDefaultTestExecutionListenersPostProcessor implements DefaultTestExecutionListenersPostProcessor {

	@Override
	public List<TestExecutionListener> postProcessDefaultTestExecutionListeners(List<TestExecutionListener> listeners) {
		listeners.add(new ExampleTestExecutionListener());
		return listeners;
	}

	static class ExampleTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void prepareTestInstance(TestContext testContext) throws Exception {
			Object testInstance = testContext.getTestInstance();
			if (testInstance instanceof SpringBootTestContextBootstrapperIntegrationTests test) {
				test.defaultTestExecutionListenersPostProcessorCalled = true;
			}
		}

	}

}
