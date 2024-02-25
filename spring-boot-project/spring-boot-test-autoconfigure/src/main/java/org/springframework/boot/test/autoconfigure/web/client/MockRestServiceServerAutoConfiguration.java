/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Collection;
import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for {@link MockRestServiceServer} support.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 1.4.0
 * @see AutoConfigureMockRestServiceServer
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.test.webclient.mockrestserviceserver", name = "enabled")
public class MockRestServiceServerAutoConfiguration {

	/**
	 * Creates a new instance of {@link MockServerRestTemplateCustomizer}.
	 * @return the created {@link MockServerRestTemplateCustomizer} instance
	 */
	@Bean
	public MockServerRestTemplateCustomizer mockServerRestTemplateCustomizer() {
		return new MockServerRestTemplateCustomizer();
	}

	/**
	 * Creates a new instance of MockServerRestClientCustomizer.
	 * @return the created MockServerRestClientCustomizer instance
	 */
	@Bean
	public MockServerRestClientCustomizer mockServerRestClientCustomizer() {
		return new MockServerRestClientCustomizer();
	}

	/**
	 * Creates a mock server for testing RESTful services.
	 * @param restTemplateCustomizer The customizer for the RestTemplate used by the
	 * server.
	 * @param restClientCustomizer The customizer for the RestClient used by the server.
	 * @return The created MockRestServiceServer.
	 * @throws IllegalStateException if an exception occurs while creating the server.
	 */
	@Bean
	public MockRestServiceServer mockRestServiceServer(MockServerRestTemplateCustomizer restTemplateCustomizer,
			MockServerRestClientCustomizer restClientCustomizer) {
		try {
			return createDeferredMockRestServiceServer(restTemplateCustomizer, restClientCustomizer);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Creates a deferred {@link MockRestServiceServer} instance with the provided
	 * {@link MockServerRestTemplateCustomizer} and
	 * {@link MockServerRestClientCustomizer}.
	 * @param restTemplateCustomizer the {@link MockServerRestTemplateCustomizer} to
	 * customize the {@link RestTemplate}
	 * @param restClientCustomizer the {@link MockServerRestClientCustomizer} to customize
	 * the {@link RestClient}
	 * @return a deferred {@link MockRestServiceServer} instance
	 * @throws Exception if an error occurs during the creation of the
	 * {@link MockRestServiceServer}
	 */
	private MockRestServiceServer createDeferredMockRestServiceServer(
			MockServerRestTemplateCustomizer restTemplateCustomizer,
			MockServerRestClientCustomizer restClientCustomizer) throws Exception {
		Constructor<MockRestServiceServer> constructor = MockRestServiceServer.class
			.getDeclaredConstructor(RequestExpectationManager.class);
		constructor.setAccessible(true);
		return constructor
			.newInstance(new DeferredRequestExpectationManager(restTemplateCustomizer, restClientCustomizer));
	}

	/**
	 * {@link RequestExpectationManager} with the injected {@link MockRestServiceServer}
	 * so that the bean can be created before the
	 * {@link MockServerRestTemplateCustomizer#customize(RestTemplate)
	 * MockServerRestTemplateCustomizer} has been called.
	 */
	private static class DeferredRequestExpectationManager implements RequestExpectationManager {

		private final MockServerRestTemplateCustomizer restTemplateCustomizer;

		private final MockServerRestClientCustomizer restClientCustomizer;

		/**
		 * Constructs a new DeferredRequestExpectationManager with the specified
		 * MockServerRestTemplateCustomizer and MockServerRestClientCustomizer.
		 * @param restTemplateCustomizer the MockServerRestTemplateCustomizer to customize
		 * the MockServerRestTemplate
		 * @param restClientCustomizer the MockServerRestClientCustomizer to customize the
		 * MockServerRestClient
		 */
		DeferredRequestExpectationManager(MockServerRestTemplateCustomizer restTemplateCustomizer,
				MockServerRestClientCustomizer restClientCustomizer) {
			this.restTemplateCustomizer = restTemplateCustomizer;
			this.restClientCustomizer = restClientCustomizer;
		}

		/**
		 * Sets the expectation for a request with the specified count and request
		 * matcher.
		 * @param count The expected count of the request.
		 * @param requestMatcher The matcher to match the request against.
		 * @return The response actions for the expectation.
		 */
		@Override
		public ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher) {
			return getDelegate().expectRequest(count, requestMatcher);
		}

		/**
		 * Validates the given client HTTP request.
		 * @param request the client HTTP request to be validated
		 * @return the client HTTP response
		 * @throws IOException if an I/O error occurs
		 */
		@Override
		public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
			return getDelegate().validateRequest(request);
		}

		/**
		 * Verifies the deferred request expectation manager. This method calls the verify
		 * method of the delegate object.
		 */
		@Override
		public void verify() {
			getDelegate().verify();
		}

		/**
		 * Verifies the deferred request expectation within the specified timeout
		 * duration.
		 * @param timeout the duration within which the deferred request expectation
		 * should be verified
		 */
		@Override
		public void verify(Duration timeout) {
			getDelegate().verify(timeout);
		}

		/**
		 * Resets the expectations for the {@link RestTemplateCustomizer} and
		 * {@link RestClientCustomizer}. This method clears all the expectation managers
		 * for both the {@link RestTemplateCustomizer} and {@link RestClientCustomizer}.
		 */
		@Override
		public void reset() {
			resetExpectations(this.restTemplateCustomizer.getExpectationManagers().values());
			resetExpectations(this.restClientCustomizer.getExpectationManagers().values());
		}

		/**
		 * Resets the expectations for the given collection of RequestExpectationManagers.
		 * If the collection contains only one RequestExpectationManager, it will be
		 * reset.
		 * @param expectationManagers the collection of RequestExpectationManagers to
		 * reset expectations for
		 */
		private void resetExpectations(Collection<RequestExpectationManager> expectationManagers) {
			if (expectationManagers.size() == 1) {
				expectationManagers.iterator().next().reset();
			}
		}

		/**
		 * Returns the delegate RequestExpectationManager based on the bound customizers.
		 * @return The delegate RequestExpectationManager
		 * @throws IllegalStateException if neither a RestTemplate nor a RestClient
		 * customizer is bound
		 * @throws IllegalStateException if both a RestTemplate and a RestClient
		 * customizer are bound
		 * @throws IllegalStateException if more than one RestTemplate or RestClient
		 * customizer is bound
		 */
		private RequestExpectationManager getDelegate() {
			Map<RestTemplate, RequestExpectationManager> restTemplateExpectationManagers = this.restTemplateCustomizer
				.getExpectationManagers();
			Map<RestClient.Builder, RequestExpectationManager> restClientExpectationManagers = this.restClientCustomizer
				.getExpectationManagers();
			boolean neitherBound = restTemplateExpectationManagers.isEmpty() && restClientExpectationManagers.isEmpty();
			boolean bothBound = !restTemplateExpectationManagers.isEmpty() && !restClientExpectationManagers.isEmpty();
			Assert.state(!neitherBound, "Unable to use auto-configured MockRestServiceServer since "
					+ "a mock server customizer has not been bound to a RestTemplate or RestClient");
			Assert.state(!bothBound, "Unable to use auto-configured MockRestServiceServer since "
					+ "mock server customizers have been bound to both a RestTemplate and a RestClient");
			if (!restTemplateExpectationManagers.isEmpty()) {
				Assert.state(restTemplateExpectationManagers.size() == 1,
						"Unable to use auto-configured MockRestServiceServer since "
								+ "MockServerRestTemplateCustomizer has been bound to more than one RestTemplate");
				return restTemplateExpectationManagers.values().iterator().next();
			}
			Assert.state(restClientExpectationManagers.size() == 1,
					"Unable to use auto-configured MockRestServiceServer since "
							+ "MockServerRestClientCustomizer has been bound to more than one RestClient");
			return restClientExpectationManagers.values().iterator().next();
		}

	}

}
