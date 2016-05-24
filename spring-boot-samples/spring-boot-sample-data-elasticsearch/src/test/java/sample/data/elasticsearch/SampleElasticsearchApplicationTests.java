/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.data.elasticsearch;

import java.io.File;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.OutputCapture;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SampleElasticsearchApplication}.
 *
 * @author Artur Konczak
 */
public class SampleElasticsearchApplicationTests {

	private static final String[] PROPERTIES = {
			"spring.data.elasticsearch.properties.path.data:target/data",
			"spring.data.elasticsearch.properties.path.logs:target/logs" };

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@ClassRule
	public static SkipOnWindows skipOnWindows = new SkipOnWindows();

	@Test
	public void testDefaultSettings() throws Exception {
		new SpringApplicationBuilder(SampleElasticsearchApplication.class)
				.properties(PROPERTIES).run();
		String output = this.outputCapture.toString();
		assertTrue("Wrong output: " + output,
				output.contains("firstName='Alice', lastName='Smith'"));
	}

	static class SkipOnWindows implements TestRule {

		@Override
		public Statement apply(final Statement base, Description description) {
			return new Statement() {

				@Override
				public void evaluate() throws Throwable {
					if (!runningOnWindows()) {
						base.evaluate();
					}
				}

				private boolean runningOnWindows() {
					return File.separatorChar == '\\';
				}

			};
		}

	}

}
