/*
 * Copyright 2012-2014 the original author or authors.
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

package sample.bitronix;

import org.hamcrest.Matcher;
import org.hamcrest.core.SubstringMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.OutputCapture;

import sample.bitronix.SampleBitronixApplication;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Phillip Webb
 */
public class SampleBitronixApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testTransactionRollback() throws Exception {
		SampleBitronixApplication.main(new String[] {});
		String expected = "";
		expected += "----> josh\n";
		expected += "Count is 1\n";
		expected += "Simulated error\n";
		expected += "Count is 1\n";
		assertThat(this.outputCapture.toString(), containsString(expected));
		assertThat(this.outputCapture.toString(), containsStringOnce("---->"));
	}

	private Matcher<? super String> containsStringOnce(String s) {
		return new SubstringMatcher(s) {

			@Override
			protected String relationship() {
				return "containing once";
			}

			@Override
			protected boolean evalSubstringOf(String s) {
				int i = 0;
				while (s.contains(this.substring)) {
					s = s.substring(s.indexOf(this.substring) + this.substring.length());
					i++;
				}
				return i == 1;
			}

		};
	}

}
