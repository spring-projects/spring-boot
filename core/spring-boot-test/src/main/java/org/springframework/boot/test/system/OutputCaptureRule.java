/*
 * Copyright 2012-present the original author or authors.
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
 * @deprecated since 4.0.0 in favor of JUnit 5 and {@link OutputCaptureExtension}
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class OutputCaptureRule implements TestRule, CapturedOutput {

	private final OutputCapture delegate = new OutputCapture();

	private final List<Matcher<? super String>> matchers = new ArrayList<>();

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
