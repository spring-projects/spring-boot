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

package org.springframework.boot.test.web.client;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.SimpleRequestExpectationManager;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClientCustomizer} that can be applied to {@link RestClient.Builder}
 * instances to add {@link MockRestServiceServer} support.
 * <p>
 * Typically applied to an existing builder before it is used, for example:
 * <pre class="code">
 * MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer();
 * RestClient.Builder builder = RestClient.builder();
 * customizer.customize(builder);
 * MyBean bean = new MyBean(client.build());
 * customizer.getServer().expect(requestTo("/hello")).andRespond(withSuccess());
 * bean.makeRestCall();
 * </pre>
 * <p>
 * If the customizer is only used once, the {@link #getServer()} method can be used to
 * obtain the mock server. If the customizer has been used more than once the
 * {@link #getServer(RestClient.Builder)} or {@link #getServers()} method must be used to
 * access the related server.
 *
 * @author Scott Frederick
 * @since 3.2.0
 * @see #getServer()
 * @see #getServer(RestClient.Builder)
 */
public class MockServerRestClientCustomizer implements RestClientCustomizer {

	private final Map<RestClient.Builder, RequestExpectationManager> expectationManagers = new ConcurrentHashMap<>();

	private final Map<RestClient.Builder, MockRestServiceServer> servers = new ConcurrentHashMap<>();

	private final Supplier<? extends RequestExpectationManager> expectationManagerSupplier;

	private boolean bufferContent = false;

	public MockServerRestClientCustomizer() {
		this(SimpleRequestExpectationManager::new);
	}

	/**
	 * Crate a new {@link MockServerRestClientCustomizer} instance.
	 * @param expectationManager the expectation manager class to use
	 */
	public MockServerRestClientCustomizer(Class<? extends RequestExpectationManager> expectationManager) {
		this(() -> BeanUtils.instantiateClass(expectationManager));
		Assert.notNull(expectationManager, "ExpectationManager must not be null");
	}

	/**
	 * Crate a new {@link MockServerRestClientCustomizer} instance.
	 * @param expectationManagerSupplier a supplier that provides the
	 * {@link RequestExpectationManager} to use
	 * @since 3.0.0
	 */
	public MockServerRestClientCustomizer(Supplier<? extends RequestExpectationManager> expectationManagerSupplier) {
		Assert.notNull(expectationManagerSupplier, "ExpectationManagerSupplier must not be null");
		this.expectationManagerSupplier = expectationManagerSupplier;
	}

	/**
	 * Set if the {@link BufferingClientHttpRequestFactory} wrapper should be used to
	 * buffer the input and output streams, and for example, allow multiple reads of the
	 * response body.
	 * @param bufferContent if request and response content should be buffered
	 * @since 3.1.0
	 */
	public void setBufferContent(boolean bufferContent) {
		this.bufferContent = bufferContent;
	}

	@Override
	public void customize(RestClient.Builder restClientBuilder) {
		RequestExpectationManager expectationManager = createExpectationManager();
		MockRestServiceServerBuilder serverBuilder = MockRestServiceServer.bindTo(restClientBuilder);
		if (this.bufferContent) {
			serverBuilder.bufferContent();
		}
		MockRestServiceServer server = serverBuilder.build(expectationManager);
		this.expectationManagers.put(restClientBuilder, expectationManager);
		this.servers.put(restClientBuilder, server);
	}

	protected RequestExpectationManager createExpectationManager() {
		return this.expectationManagerSupplier.get();
	}

	public MockRestServiceServer getServer() {
		Assert.state(!this.servers.isEmpty(), "Unable to return a single MockRestServiceServer since "
				+ "MockServerRestClientCustomizer has not been bound to a RestClient");
		Assert.state(this.servers.size() == 1, "Unable to return a single MockRestServiceServer since "
				+ "MockServerRestClientCustomizer has been bound to more than one RestClient");
		return this.servers.values().iterator().next();
	}

	public Map<RestClient.Builder, RequestExpectationManager> getExpectationManagers() {
		return this.expectationManagers;
	}

	public MockRestServiceServer getServer(RestClient.Builder restClientBuilder) {
		return this.servers.get(restClientBuilder);
	}

	public Map<RestClient.Builder, MockRestServiceServer> getServers() {
		return Collections.unmodifiableMap(this.servers);
	}

}
