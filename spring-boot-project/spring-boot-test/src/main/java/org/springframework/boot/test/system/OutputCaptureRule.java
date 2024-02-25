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

package org.springframework.boot.test.system;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.hamcrest.Matchers.allOf;

/**
 * JUnit {@code @Rule} to capture output from {@code System.out} and {@code System.err}.
 * <p>
 * To use add as a {@link Rule @Rule}:
 *
 * <pre class="code">
 * public class MyTest {
 *
 *     &#064;Rule
 *     public OutputCaptureRule output = new OutputCaptureRule();
 *
 *     &#064;Test
 *     public void test() {
 *         assertThat(output).contains("ok");
 *     }
 *
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.2.0
 */
public class OutputCaptureRule implements TestRule, CapturedOutput {

	private final OutputCapture delegate = new OutputCapture();

	private final List<Matcher<? super String>> matchers = new ArrayList<>();

	/**
	 * Applies the given statement to the description and evaluates it. This method
	 * captures the output of the statement and applies any matchers specified in the rule
	 * before evaluating the statement.
	 * @param base the statement to be applied
	 * @param description the description of the test
	 * @return a new statement that captures the output and applies matchers
	 * @throws Throwable if an error occurs during evaluation
	 */
	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				OutputCaptureRule.this.delegate.push();
				try {
					base.evaluate();
				}
				finally {
					try {
						if (!OutputCaptureRule.this.matchers.isEmpty()) {
							String output = OutputCaptureRule.this.delegate.toString();
							MatcherAssert.assertThat(output, allOf(OutputCaptureRule.this.matchers));
						}
					}
					finally {
						OutputCaptureRule.this.delegate.pop();
					}
				}
			}
		};
	}

	/**
	 * Returns all the captured output.
	 * @return a string containing all the captured output
	 */
	@Override
	public String getAll() {
		return this.delegate.getAll();
	}

	/**
	 * Returns the output captured by the delegate object.
	 * @return the captured output as a string
	 */
	@Override
	public String getOut() {
		return this.delegate.getOut();
	}

	/**
	 * Returns the error output captured by the OutputCaptureRule.
	 * @return the error output captured by the OutputCaptureRule
	 */
	@Override
	public String getErr() {
		return this.delegate.getErr();
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object
	 */
	@Override
	public String toString() {
		return this.delegate.toString();
	}

	/**
	 * Verify that the output is matched by the supplied {@code matcher}. Verification is
	 * performed after the test method has executed.
	 * @param matcher the matcher
	 */
	public void expect(Matcher<? super String> matcher) {
		this.matchers.add(matcher);
	}

}
