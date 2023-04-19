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

package org.springframework.boot.test.autoconfigure.web.client;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for {@link MockRestServiceServer} support.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see AutoConfigureMockRestServiceServer
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.test.webclient.mockrestserviceserver", name = "enabled")
public class MockRestServiceServerAutoConfiguration {

	@Bean
	public MockServerRestTemplateCustomizer mockServerRestTemplateCustomizer() {
		return new MockServerRestTemplateCustomizer();
	}

	@Bean
	public MockRestServiceServer mockRestServiceServer(MockServerRestTemplateCustomizer customizer) {
		try {
			return createDeferredMockRestServiceServer(customizer);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private MockRestServiceServer createDeferredMockRestServiceServer(MockServerRestTemplateCustomizer customizer)
			throws Exception {
		Constructor<MockRestServiceServer> constructor = MockRestServiceServer.class
			.getDeclaredConstructor(RequestExpectationManager.class);
		constructor.setAccessible(true);
		return constructor.newInstance(new DeferredRequestExpectationManager(customizer));
	}

	/**
	 * {@link RequestExpectationManager} with the injected {@link MockRestServiceServer}
	 * so that the bean can be created before the
	 * {@link MockServerRestTemplateCustomizer#customize(RestTemplate)
	 * MockServerRestTemplateCustomizer} has been called.
	 */
	private static class DeferredRequestExpectationManager implements RequestExpectationManager {

		private final MockServerRestTemplateCustomizer customizer;

		DeferredRequestExpectationManager(MockServerRestTemplateCustomizer customizer) {
			this.customizer = customizer;
		}

		@Override
		public ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher) {
			return getDelegate().expectRequest(count, requestMatcher);
		}

		@Override
		public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
			return getDelegate().validateRequest(request);
		}

		@Override
		public void verify() {
			getDelegate().verify();
		}

		@Override
		public void verify(Duration timeout) {
			getDelegate().verify(timeout);
		}

		@Override
		public void reset() {
			Map<RestTemplate, RequestExpectationManager> expectationManagers = this.customizer.getExpectationManagers();
			if (expectationManagers.size() == 1) {
				getDelegate().reset();
			}
		}

		private RequestExpectationManager getDelegate() {
			Map<RestTemplate, RequestExpectationManager> expectationManagers = this.customizer.getExpectationManagers();
			Assert.state(!expectationManagers.isEmpty(), "Unable to use auto-configured MockRestServiceServer since "
					+ "MockServerRestTemplateCustomizer has not been bound to a RestTemplate");
			Assert.state(expectationManagers.size() == 1, "Unable to use auto-configured MockRestServiceServer since "
					+ "MockServerRestTemplateCustomizer has been bound to more than one RestTemplate");
			return expectationManagers.values().iterator().next();
		}

	}

}
