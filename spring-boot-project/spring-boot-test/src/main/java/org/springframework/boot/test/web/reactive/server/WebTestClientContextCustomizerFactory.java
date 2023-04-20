/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.web.reactive.server;

import java.util.List;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} for {@code WebTestClient}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Anugrah Singhal
 */
class WebTestClientContextCustomizerFactory implements ContextCustomizerFactory {

	private static final boolean webClientPresent;

	static {
		ClassLoader loader = WebTestClientContextCustomizerFactory.class.getClassLoader();
		webClientPresent = ClassUtils.isPresent("org.springframework.web.reactive.function.client.WebClient", loader);
	}

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		SpringBootTest springBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				SpringBootTest.class);
		return (springBootTest != null && webClientPresent) ? new WebTestClientContextCustomizer() : null;
	}

}
