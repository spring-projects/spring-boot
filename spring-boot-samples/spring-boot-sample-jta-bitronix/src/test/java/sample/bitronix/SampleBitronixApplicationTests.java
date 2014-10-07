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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.OutputCapture;
import org.springframework.context.ApplicationContext;

import bitronix.tm.resource.jms.PoolingConnectionFactory;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
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
		String output = this.outputCapture.toString();
		assertThat(output, containsString(1, "---->"));
		assertThat(output, containsString(1, "----> josh"));
		assertThat(output, containsString(2, "Count is 1"));
		assertThat(output, containsString(1, "Simulated error"));
	}

	@Test
	public void testExposesXaAndNonXa() throws Exception {
		ApplicationContext context = SpringApplication
				.run(SampleBitronixApplication.class);
		Object jmsConnectionFactory = context.getBean("jmsConnectionFactory");
		Object xaJmsConnectionFactory = context.getBean("xaJmsConnectionFactory");
		Object nonXaJmsConnectionFactory = context.getBean("nonXaJmsConnectionFactory");
		assertThat(jmsConnectionFactory, sameInstance(xaJmsConnectionFactory));
		assertThat(jmsConnectionFactory, instanceOf(PoolingConnectionFactory.class));
		assertThat(nonXaJmsConnectionFactory,
				not(instanceOf(PoolingConnectionFactory.class)));
	}

	private Matcher<? super String> containsString(final int times, String s) {
		return new SubstringMatcher(s) {

			@Override
			protected String relationship() {
				return "containing " + times + " times";
			}

			@Override
			protected boolean evalSubstringOf(String s) {
				int i = 0;
				while (s.contains(this.substring)) {
					s = s.substring(s.indexOf(this.substring) + this.substring.length());
					i++;
				}
				return i == times;
			}

		};
	}

}
