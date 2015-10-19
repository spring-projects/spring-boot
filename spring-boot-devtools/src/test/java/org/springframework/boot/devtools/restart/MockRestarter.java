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

package org.springframework.boot.devtools.restart;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.ObjectFactory;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

/**
 * Mocked version of {@link Restarter}.
 *
 * @author Phillip Webb
 */
public class MockRestarter implements TestRule {

	private Map<String, Object> attributes = new HashMap<String, Object>();

	private Restarter mock = mock(Restarter.class);

	@Override
	public Statement apply(final Statement base, Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				setup();
				base.evaluate();
				cleanup();
			}

		};
	}

	@SuppressWarnings("rawtypes")
	private void setup() {
		Restarter.setInstance(this.mock);
		given(this.mock.getInitialUrls()).willReturn(new URL[] {});
		given(this.mock.getOrAddAttribute(anyString(), (ObjectFactory) any()))
				.willAnswer(new Answer<Object>() {

					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
						String name = (String) invocation.getArguments()[0];
						ObjectFactory factory = (ObjectFactory) invocation
								.getArguments()[1];
						Object attribute = MockRestarter.this.attributes.get(name);
						if (attribute == null) {
							attribute = factory.getObject();
							MockRestarter.this.attributes.put(name, attribute);
						}
						return attribute;
					}

				});
		given(this.mock.getThreadFactory()).willReturn(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r);
			}

		});
	}

	private void cleanup() {
		this.attributes.clear();
		Restarter.clearInstance();
	}

	public Restarter getMock() {
		return this.mock;
	}

}
