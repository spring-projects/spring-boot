/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import org.assertj.core.matcher.AssertionMatcher;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test runner used for {@link WebMvcTestPrintDefaultIntegrationTests}.
 *
 * @author Phillip Webb
 */
public class WebMvcTestPrintDefaultRunner extends SpringJUnit4ClassRunner {

	public WebMvcTestPrintDefaultRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
	}

	@Override
	protected Statement methodBlock(FrameworkMethod frameworkMethod) {
		Statement statement = super.methodBlock(frameworkMethod);
		statement = new AlwaysPassStatement(statement);
		OutputCapture outputCapture = new OutputCapture();
		if (frameworkMethod.getName().equals("shouldPrint")) {
			outputCapture.expect(new AssertionMatcher<String>() {

				@Override
				public void assertion(String actual) throws AssertionError {
					assertThat(actual).containsOnlyOnce("HTTP Method");
				}

			});
		}
		else if (frameworkMethod.getName().equals("shouldNotPrint")) {
			outputCapture.expect(new AssertionMatcher<String>() {

				@Override
				public void assertion(String actual) throws AssertionError {
					assertThat(actual).doesNotContain("HTTP Method");
				}

			});
		}
		else {
			throw new IllegalStateException("Unexpected test method");
		}
		System.err.println(frameworkMethod.getName());
		return outputCapture.apply(statement, null);
	}

	private static class AlwaysPassStatement extends Statement {

		private final Statement delegate;

		AlwaysPassStatement(Statement delegate) {
			this.delegate = delegate;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				this.delegate.evaluate();
			}
			catch (AssertionError ex) {
			}
		}

	}

}
