/*
 * Copyright 2012-2021 the original author or authors.
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
 */
class WebTestClientContextCustomizerFactory implements ContextCustomizerFactory {

	private static final boolean reactorClientPresent;

	private static final boolean jettyClientPresent;

	private static final boolean httpComponentsClientPresent;

	private static final boolean webClientPresent;

	static {
		ClassLoader loader = WebTestClientContextCustomizerFactory.class.getClassLoader();
		reactorClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
		jettyClientPresent = ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", loader);
		httpComponentsClientPresent = ClassUtils
				.isPresent("org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient", loader)
				&& ClassUtils.isPresent("org.apache.hc.core5.reactive.ReactiveDataConsumer", loader);
		webClientPresent = ClassUtils.isPresent("org.springframework.web.reactive.function.client.WebClient", loader);
	}

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		SpringBootTest springBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				SpringBootTest.class);
		return (springBootTest != null && webClientPresent
				&& (reactorClientPresent || jettyClientPresent || httpComponentsClientPresent))
						? new WebTestClientContextCustomizer() : null;
	}

}
