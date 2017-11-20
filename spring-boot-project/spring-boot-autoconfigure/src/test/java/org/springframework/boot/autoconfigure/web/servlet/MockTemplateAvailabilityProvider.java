/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * Assists tests in mocking templates
 *
 * @author Bruce Brouwer
 */
public class MockTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

	private static final TemplateAvailabilityProvider MOCK = Mockito.mock(TemplateAvailabilityProvider.class);

	public static void mock(final String view) {
		given(MOCK.isTemplateAvailable(eq(view), any(), any(), any())).willReturn(true);
	}

	public static TestRule testRule() {
		return new TestRule() {

			public Statement apply(Statement base, Description description) {
				return new Statement() {

					@Override
					public void evaluate() throws Throwable {
						Mockito.reset(MOCK);
						try {
							base.evaluate();
						}
						finally {
							Mockito.reset(MOCK);
						}
					}

				};
			}

		};
	}

	@Override
	public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader,
			ResourceLoader resourceLoader) {
		return MOCK.isTemplateAvailable(view, environment, classLoader, resourceLoader);
	}

}
