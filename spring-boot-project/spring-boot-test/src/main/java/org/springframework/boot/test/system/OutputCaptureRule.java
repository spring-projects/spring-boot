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

package org.springframework.boot.test.system;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.hamcrest.Matchers.allOf;

/**
 * JUnit {@code @Rule} to capture output from System.out and System.err.
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

	private List<Matcher<? super String>> matchers = new ArrayList<>();

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
							Assert.assertThat(output, allOf(OutputCaptureRule.this.matchers));
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
	 * Resets the current capture session, clearing its captured output.
	 * @deprecated since 2.2.0 with no replacement
	 */
	@Deprecated
	public void reset() {
		OutputCaptureRule.this.delegate.reset();
	}

	@Override
	public String getAll() {
		return this.delegate.getAll();
	}

	@Override
	public String getOut() {
		return this.delegate.getOut();
	}

	@Override
	public String getErr() {
		return this.delegate.getErr();
	}

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
