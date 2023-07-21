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

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RootUriRequestExpectationManager}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class RootUriRequestExpectationManagerTests {

	private final String uri = "https://example.com";

	@Mock
	private RequestExpectationManager delegate;

	private RootUriRequestExpectationManager manager;

	@BeforeEach
	void setup() {
		this.manager = new RootUriRequestExpectationManager(this.uri, this.delegate);
	}

	@Test
	void createWhenRootUriIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RootUriRequestExpectationManager(null, this.delegate))
			.withMessageContaining("RootUri must not be null");
	}

	@Test
	void createWhenExpectationManagerIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RootUriRequestExpectationManager(this.uri, null))
			.withMessageContaining("ExpectationManager must not be null");
	}

	@Test
	void expectRequestShouldDelegateToExpectationManager() {
		ExpectedCount count = ExpectedCount.once();
		RequestMatcher requestMatcher = mock(RequestMatcher.class);
		this.manager.expectRequest(count, requestMatcher);
		then(this.delegate).should().expectRequest(count, requestMatcher);
	}

	@Test
	void validateRequestWhenUriDoesNotStartWithRootUriShouldDelegateToExpectationManager() throws Exception {
		ClientHttpRequest request = mock(ClientHttpRequest.class);
		given(request.getURI()).willReturn(new URI("https://spring.io/test"));
		this.manager.validateRequest(request);
		then(this.delegate).should().validateRequest(request);
	}

	@Test
	void validateRequestWhenUriStartsWithRootUriShouldReplaceUri() throws Exception {
		ClientHttpRequest request = mock(ClientHttpRequest.class);
		given(request.getURI()).willReturn(new URI(this.uri + "/hello"));
		this.manager.validateRequest(request);
		URI expectedURI = new URI("/hello");
		then(this.delegate).should()
			.validateRequest(assertArg((actual) -> assertThat(actual).isInstanceOfSatisfying(HttpRequestWrapper.class,
					(requestWrapper) -> {
						assertThat(requestWrapper.getRequest()).isSameAs(request);
						assertThat(requestWrapper.getURI()).isEqualTo(expectedURI);
					})));

	}

	@Test
	void validateRequestWhenRequestUriAssertionIsThrownShouldReplaceUriInMessage() throws Exception {
		ClientHttpRequest request = mock(ClientHttpRequest.class);
		given(request.getURI()).willReturn(new URI(this.uri + "/hello"));
		given(this.delegate.validateRequest(any(ClientHttpRequest.class)))
			.willThrow(new AssertionError("Request URI expected:</hello> was:<https://example.com/bad>"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.manager.validateRequest(request))
			.withMessageContaining("Request URI expected:<https://example.com/hello>");
	}

	@Test
	void resetRequestShouldDelegateToExpectationManager() {
		this.manager.reset();
		then(this.delegate).should().reset();
	}

	@Test
	void bindToShouldReturnMockRestServiceServer() {
		RestTemplate restTemplate = new RestTemplateBuilder().build();
		MockRestServiceServer bound = RootUriRequestExpectationManager.bindTo(restTemplate);
		assertThat(bound).isNotNull();
	}

	@Test
	void bindToWithExpectationManagerShouldReturnMockRestServiceServer() {
		RestTemplate restTemplate = new RestTemplateBuilder().build();
		MockRestServiceServer bound = RootUriRequestExpectationManager.bindTo(restTemplate, this.delegate);
		assertThat(bound).isNotNull();
	}

	@Test
	void forRestTemplateWhenUsingRootUriTemplateHandlerShouldReturnRootUriRequestExpectationManager() {
		RestTemplate restTemplate = new RestTemplateBuilder().rootUri(this.uri).build();
		RequestExpectationManager actual = RootUriRequestExpectationManager.forRestTemplate(restTemplate,
				this.delegate);
		assertThat(actual).isInstanceOf(RootUriRequestExpectationManager.class);
		assertThat(actual).extracting("rootUri").isEqualTo(this.uri);
	}

	@Test
	void forRestTemplateWhenNotUsingRootUriTemplateHandlerShouldReturnOriginalRequestExpectationManager() {
		RestTemplate restTemplate = new RestTemplateBuilder().build();
		RequestExpectationManager actual = RootUriRequestExpectationManager.forRestTemplate(restTemplate,
				this.delegate);
		assertThat(actual).isSameAs(this.delegate);
	}

	@Test
	void boundRestTemplateShouldPrefixRootUri() {
		RestTemplate restTemplate = new RestTemplateBuilder().rootUri("https://example.com").build();
		MockRestServiceServer server = RootUriRequestExpectationManager.bindTo(restTemplate);
		server.expect(requestTo("/hello")).andRespond(withSuccess());
		restTemplate.getForEntity("/hello", String.class);
	}

	@Test
	void boundRestTemplateWhenUrlIncludesDomainShouldNotPrefixRootUri() {
		RestTemplate restTemplate = new RestTemplateBuilder().rootUri("https://example.com").build();
		MockRestServiceServer server = RootUriRequestExpectationManager.bindTo(restTemplate);
		server.expect(requestTo("/hello")).andRespond(withSuccess());
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> restTemplate.getForEntity("https://spring.io/hello", String.class))
			.withMessageContaining("expected:<https://example.com/hello> but was:<https://spring.io/hello>");
	}

}
