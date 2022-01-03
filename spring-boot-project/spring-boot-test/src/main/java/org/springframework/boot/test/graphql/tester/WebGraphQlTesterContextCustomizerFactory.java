/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.boot.test.graphql.tester;

import java.util.List;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} for {@link GraphQlTester}.
 *
 * @author Brian Clozel
 * @see WebGraphQlTesterContextCustomizer
 */
class WebGraphQlTesterContextCustomizerFactory implements ContextCustomizerFactory {

	private static final String WEBGRAPHQLTESTER_CLASS = "org.springframework.graphql.test.tester.WebGraphQlTester";

	private static final String WEBTESTCLIENT_CLASS = "org.springframework.test.web.reactive.server.WebTestClient";

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		SpringBootTest springBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				SpringBootTest.class);
		return (springBootTest != null && isGraphQlTesterPresent()) ? new WebGraphQlTesterContextCustomizer() : null;
	}

	private boolean isGraphQlTesterPresent() {
		return ClassUtils.isPresent(WEBTESTCLIENT_CLASS, getClass().getClassLoader())
				&& ClassUtils.isPresent(WEBGRAPHQLTESTER_CLASS, getClass().getClassLoader());
	}

}
