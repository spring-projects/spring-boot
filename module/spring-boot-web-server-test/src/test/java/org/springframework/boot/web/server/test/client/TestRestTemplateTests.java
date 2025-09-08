/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.test.client;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.Base64;
import java.util.stream.Stream;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.RedirectExec;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.web.server.test.client.TestRestTemplate.HttpClientOption;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestRestTemplate}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Kristine Jetzke
 * @author Yanming Zhou
 */
class TestRestTemplateTests {

	@Test
	void fromRestTemplateBuilder() {
		RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
		RestTemplate delegate = new RestTemplate();
		given(builder.build()).willReturn(delegate);
		assertThat(new TestRestTemplate(builder).getRestTemplate()).isEqualTo(delegate);
	}

	@Test
	void simple() {
		// The Apache client is on the classpath so we get the fully-fledged factory
		assertThat(new TestRestTemplate().getRestTemplate().getRequestFactory())
			.isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	void doNotReplaceCustomRequestFactory() {
		RestTemplateBuilder builder = new RestTemplateBuilder().requestFactory(TestClientHttpRequestFactory.class);
		TestRestTemplate testRestTemplate = new TestRestTemplate(builder);
		assertThat(testRestTemplate.getRestTemplate().getRequestFactory())
			.isInstanceOf(TestClientHttpRequestFactory.class);
	}

	@Test
	void useTheSameRequestFactoryClassWithBasicAuth() {
		TestClientHttpRequestFactory customFactory = new TestClientHttpRequestFactory();
		RestTemplateBuilder builder = new RestTemplateBuilder().requestFactory(() -> customFactory);
		TestRestTemplate testRestTemplate = new TestRestTemplate(builder).withBasicAuth("test", "test");
		RestTemplate restTemplate = testRestTemplate.getRestTemplate();
		assertThat(restTemplate.getRequestFactory()).isEqualTo(customFactory).hasSameClassAs(customFactory);
	}

	@Test
	void getRootUriRootUriSetViaRestTemplateBuilder() {
		String rootUri = "https://example.com";
		RestTemplateBuilder delegate = new RestTemplateBuilder().rootUri(rootUri);
		assertThat(new TestRestTemplate(delegate).getRootUri()).isEqualTo(rootUri);
	}

	@Test
	void getRootUriRootUriSetViaLocalHostUriTemplateHandler() {
		String rootUri = "https://example.com";
		TestRestTemplate template = new TestRestTemplate();
		LocalHostUriTemplateHandler templateHandler = mock(LocalHostUriTemplateHandler.class);
		given(templateHandler.getRootUri()).willReturn(rootUri);
		template.setUriTemplateHandler(templateHandler);
		assertThat(template.getRootUri()).isEqualTo(rootUri);
	}

	@Test
	void getRootUriRootUriNotSet() {
		assertThat(new TestRestTemplate().getRootUri()).isEmpty();
	}

	@Test
	void authenticated() {
		TestRestTemplate restTemplate = new TestRestTemplate("user", "password");
		assertBasicAuthorizationCredentials(restTemplate, "user", "password");
	}

	@Test
	void options() {
		RequestConfig config = getRequestConfig(new TestRestTemplate(HttpClientOption.ENABLE_COOKIES));
		assertThat(config.getCookieSpec()).isEqualTo("strict");
	}

	@Test
	void jdkBuilderCanBeSpecifiedWithSpecificRedirects() {
		RestTemplateBuilder builder = new RestTemplateBuilder()
			.requestFactoryBuilder(ClientHttpRequestFactoryBuilder.jdk());
		TestRestTemplate templateWithRedirects = new TestRestTemplate(builder.redirects(HttpRedirects.FOLLOW));
		assertThat(getJdkHttpClient(templateWithRedirects).followRedirects()).isEqualTo(Redirect.NORMAL);
		TestRestTemplate templateWithoutRedirects = new TestRestTemplate(builder.redirects(HttpRedirects.DONT_FOLLOW));
		assertThat(getJdkHttpClient(templateWithoutRedirects).followRedirects()).isEqualTo(Redirect.NEVER);
	}

	@Test
	void httpComponentsAreBuiltConsideringSettingsInRestTemplateBuilder() {
		RestTemplateBuilder builder = new RestTemplateBuilder()
			.requestFactoryBuilder(ClientHttpRequestFactoryBuilder.httpComponents());
		assertThat(getRedirectStrategy((RestTemplateBuilder) null)).matches(this::isFollowStrategy);
		assertThat(getRedirectStrategy(builder)).matches(this::isFollowStrategy);
		assertThat(getRedirectStrategy(builder.redirects(HttpRedirects.DONT_FOLLOW)))
			.matches(this::isDontFollowStrategy);
	}

	@Test
	void withRequestFactorySettingsRedirectsForHttpComponents() {
		TestRestTemplate template = new TestRestTemplate();
		assertThat(getRedirectStrategy(template)).matches(this::isFollowStrategy);
		assertThat(getRedirectStrategy(template.withRequestFactorySettings(
				ClientHttpRequestFactorySettings.defaults().withRedirects(HttpRedirects.FOLLOW))))
			.matches(this::isFollowStrategy);
		assertThat(getRedirectStrategy(template.withRequestFactorySettings(
				ClientHttpRequestFactorySettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW))))
			.matches(this::isDontFollowStrategy);
	}

	@Test
	void withRedirects() {
		TestRestTemplate template = new TestRestTemplate();
		assertThat(getRedirectStrategy(template)).matches(this::isFollowStrategy);
		assertThat(getRedirectStrategy(template.withRedirects(HttpRedirects.FOLLOW))).matches(this::isFollowStrategy);
		assertThat(getRedirectStrategy(template.withRedirects(HttpRedirects.DONT_FOLLOW)))
			.matches(this::isDontFollowStrategy);
	}

	@Test
	void withRequestFactorySettingsRedirectsForJdk() {
		TestRestTemplate template = new TestRestTemplate(
				new RestTemplateBuilder().requestFactoryBuilder(ClientHttpRequestFactoryBuilder.jdk()));
		assertThat(getJdkHttpClient(template).followRedirects()).isEqualTo(Redirect.NORMAL);
		assertThat(getJdkHttpClient(template.withRequestFactorySettings(
				ClientHttpRequestFactorySettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW)))
			.followRedirects()).isEqualTo(Redirect.NEVER);
	}

	@Test
	void withRequestFactorySettingsUpdateRedirectsForJdk() {
		TestRestTemplate template = new TestRestTemplate(
				new RestTemplateBuilder().requestFactoryBuilder(ClientHttpRequestFactoryBuilder.jdk()));
		assertThat(getJdkHttpClient(template).followRedirects()).isEqualTo(Redirect.NORMAL);
		assertThat(getJdkHttpClient(
				template.withRequestFactorySettings((settings) -> settings.withRedirects(HttpRedirects.DONT_FOLLOW)))
			.followRedirects()).isEqualTo(Redirect.NEVER);
	}

	private RequestConfig getRequestConfig(TestRestTemplate template) {
		ClientHttpRequestFactory requestFactory = template.getRestTemplate().getRequestFactory();
		return (RequestConfig) Extractors.byName("httpClient.defaultConfig").apply(requestFactory);
	}

	private RedirectStrategy getRedirectStrategy(RestTemplateBuilder builder, HttpClientOption... httpClientOptions) {
		builder = (builder != null) ? builder : new RestTemplateBuilder();
		TestRestTemplate template = new TestRestTemplate(builder, null, null, httpClientOptions);
		return getRedirectStrategy(template);
	}

	private RedirectStrategy getRedirectStrategy(TestRestTemplate template) {
		ClientHttpRequestFactory requestFactory = template.getRestTemplate().getRequestFactory();
		Object chain = Extractors.byName("httpClient.execChain").apply(requestFactory);
		while (chain != null) {
			Object handler = Extractors.byName("handler").apply(chain);
			if (handler instanceof RedirectExec) {
				return (RedirectStrategy) Extractors.byName("redirectStrategy").apply(handler);
			}
			chain = Extractors.byName("next").apply(chain);
		}
		return null;
	}

	private boolean isFollowStrategy(RedirectStrategy redirectStrategy) {
		return redirectStrategy instanceof DefaultRedirectStrategy;
	}

	private boolean isDontFollowStrategy(RedirectStrategy redirectStrategy) {
		return redirectStrategy.getClass().getName().contains("NoFollow");
	}

	private HttpClient getJdkHttpClient(TestRestTemplate template) {
		JdkClientHttpRequestFactory requestFactory = (JdkClientHttpRequestFactory) template.getRestTemplate()
			.getRequestFactory();
		return (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
	}

	@Test
	void restOperationsAreAvailable() {
		RestTemplate delegate = mock(RestTemplate.class);
		given(delegate.getRequestFactory()).willReturn(new SimpleClientHttpRequestFactory());
		given(delegate.getUriTemplateHandler()).willReturn(new DefaultUriBuilderFactory());
		RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
		given(builder.build()).willReturn(delegate);
		TestRestTemplate restTemplate = new TestRestTemplate(builder);
		ReflectionUtils.doWithMethods(RestOperations.class, new MethodCallback() {

			@Override
			public void doWith(Method method) {
				Method equivalent = ReflectionUtils.findMethod(TestRestTemplate.class, method.getName(),
						method.getParameterTypes());
				assertThat(equivalent).as("Method %s not found", method).isNotNull();
				assertThat(Modifier.isPublic(equivalent.getModifiers()))
					.as("Method %s should have been public", equivalent)
					.isTrue();
				try {
					equivalent.invoke(restTemplate, mockArguments(method.getParameterTypes()));
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}

			private Object[] mockArguments(Class<?>[] parameterTypes) throws Exception {
				Object[] arguments = new Object[parameterTypes.length];
				for (int i = 0; i < parameterTypes.length; i++) {
					arguments[i] = mockArgument(parameterTypes[i]);
				}
				return arguments;
			}

			@SuppressWarnings("rawtypes")
			private Object mockArgument(Class<?> type) throws Exception {
				if (String.class.equals(type)) {
					return "String";
				}
				if (Object[].class.equals(type)) {
					return new Object[0];
				}
				if (URI.class.equals(type)) {
					return new URI("http://localhost");
				}
				if (HttpMethod.class.equals(type)) {
					return HttpMethod.GET;
				}
				if (Class.class.equals(type)) {
					return Object.class;
				}
				if (RequestEntity.class.equals(type)) {
					return new RequestEntity(HttpMethod.GET, new URI("http://localhost"));
				}
				return mock(type);
			}

		}, (method) -> Modifier.isPublic(method.getModifiers()));

	}

	@Test
	void withBasicAuthAddsBasicAuthWhenNotAlreadyPresent() {
		TestRestTemplate original = new TestRestTemplate();
		TestRestTemplate basicAuth = original.withBasicAuth("user", "password");
		assertThat(getConverterClasses(original)).containsExactlyElementsOf(getConverterClasses(basicAuth).toList());
		assertThat(basicAuth.getRestTemplate().getInterceptors()).isEmpty();
		assertBasicAuthorizationCredentials(original, null, null);
		assertBasicAuthorizationCredentials(basicAuth, "user", "password");
	}

	@Test
	void withBasicAuthReplacesBasicAuthWhenAlreadyPresent() {
		TestRestTemplate original = new TestRestTemplate("foo", "bar").withBasicAuth("replace", "replace");
		TestRestTemplate basicAuth = original.withBasicAuth("user", "password");
		assertThat(getConverterClasses(basicAuth)).containsExactlyElementsOf(getConverterClasses(original).toList());
		assertBasicAuthorizationCredentials(original, "replace", "replace");
		assertBasicAuthorizationCredentials(basicAuth, "user", "password");
	}

	private Stream<Class<?>> getConverterClasses(TestRestTemplate testRestTemplate) {
		return testRestTemplate.getRestTemplate().getMessageConverters().stream().map(Object::getClass);
	}

	@Test
	void withBasicAuthShouldUseNoOpErrorHandler() {
		TestRestTemplate originalTemplate = new TestRestTemplate("foo", "bar");
		ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);
		originalTemplate.getRestTemplate().setErrorHandler(errorHandler);
		TestRestTemplate basicAuthTemplate = originalTemplate.withBasicAuth("user", "password");
		assertThat(basicAuthTemplate.getRestTemplate().getErrorHandler()).isInstanceOf(NoOpResponseErrorHandler.class);
	}

	@Test
	void exchangeWithRelativeTemplatedUrlRequestEntity() throws Exception {
		RequestEntity<Void> entity = RequestEntity.get("/a/b/c.{ext}", "txt").build();
		TestRestTemplate template = new TestRestTemplate();
		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		MockClientHttpRequest request = new MockClientHttpRequest();
		request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
		URI absoluteUri = URI.create("http://localhost:8080/a/b/c.txt");
		given(requestFactory.createRequest(eq(absoluteUri), eq(HttpMethod.GET))).willReturn(request);
		template.getRestTemplate().setRequestFactory(requestFactory);
		LocalHostUriTemplateHandler uriTemplateHandler = new LocalHostUriTemplateHandler(new MockEnvironment());
		template.setUriTemplateHandler(uriTemplateHandler);
		template.exchange(entity, String.class);
		then(requestFactory).should().createRequest(eq(absoluteUri), eq(HttpMethod.GET));
	}

	@Test
	void exchangeWithAbsoluteTemplatedUrlRequestEntity() throws Exception {
		RequestEntity<Void> entity = RequestEntity.get("https://api.example.com/a/b/c.{ext}", "txt").build();
		TestRestTemplate template = new TestRestTemplate();
		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		MockClientHttpRequest request = new MockClientHttpRequest();
		request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
		URI absoluteUri = URI.create("https://api.example.com/a/b/c.txt");
		given(requestFactory.createRequest(eq(absoluteUri), eq(HttpMethod.GET))).willReturn(request);
		template.getRestTemplate().setRequestFactory(requestFactory);
		template.exchange(entity, String.class);
		then(requestFactory).should().createRequest(eq(absoluteUri), eq(HttpMethod.GET));
	}

	@Test
	void deleteHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(TestRestTemplate::delete);
	}

	@Test
	void exchangeWithRequestEntityAndClassHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling((testRestTemplate, relativeUri) -> testRestTemplate
			.exchange(new RequestEntity<>(HttpMethod.GET, relativeUri), String.class));
	}

	@Test
	void exchangeWithRequestEntityAndParameterizedTypeReferenceHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling((testRestTemplate, relativeUri) -> testRestTemplate
			.exchange(new RequestEntity<>(HttpMethod.GET, relativeUri), new ParameterizedTypeReference<String>() {
			}));
	}

	@Test
	void exchangeHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling((testRestTemplate, relativeUri) -> testRestTemplate.exchange(relativeUri,
				HttpMethod.GET, new HttpEntity<>(new byte[0]), String.class));
	}

	@Test
	void exchangeWithParameterizedTypeReferenceHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling((testRestTemplate, relativeUri) -> testRestTemplate.exchange(relativeUri,
				HttpMethod.GET, new HttpEntity<>(new byte[0]), new ParameterizedTypeReference<String>() {
				}));
	}

	@Test
	void executeHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(
				(testRestTemplate, relativeUri) -> testRestTemplate.execute(relativeUri, HttpMethod.GET, null, null));
	}

	@Test
	void getForEntityHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(
				(testRestTemplate, relativeUri) -> testRestTemplate.getForEntity(relativeUri, String.class));
	}

	@Test
	void getForObjectHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(
				(testRestTemplate, relativeUri) -> testRestTemplate.getForObject(relativeUri, String.class));
	}

	@Test
	void headForHeadersHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(TestRestTemplate::headForHeaders);
	}

	@Test
	void optionsForAllowHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(TestRestTemplate::optionsForAllow);
	}

	@Test
	void patchForObjectHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(
				(testRestTemplate, relativeUri) -> testRestTemplate.patchForObject(relativeUri, "hello", String.class));
	}

	@Test
	void postForEntityHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(
				(testRestTemplate, relativeUri) -> testRestTemplate.postForEntity(relativeUri, "hello", String.class));
	}

	@Test
	void postForLocationHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(
				(testRestTemplate, relativeUri) -> testRestTemplate.postForLocation(relativeUri, "hello"));
	}

	@Test
	void postForObjectHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling(
				(testRestTemplate, relativeUri) -> testRestTemplate.postForObject(relativeUri, "hello", String.class));
	}

	@Test
	void putHandlesRelativeUris() throws IOException {
		verifyRelativeUriHandling((testRestTemplate, relativeUri) -> testRestTemplate.put(relativeUri, "hello"));
	}

	private void verifyRelativeUriHandling(TestRestTemplateCallback callback) throws IOException {
		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		MockClientHttpRequest request = new MockClientHttpRequest();
		request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
		URI absoluteUri = URI.create("http://localhost:8080/a/b/c.txt?param=%7Bsomething%7D");
		given(requestFactory.createRequest(eq(absoluteUri), any(HttpMethod.class))).willReturn(request);
		TestRestTemplate template = new TestRestTemplate();
		template.getRestTemplate().setRequestFactory(requestFactory);
		LocalHostUriTemplateHandler uriTemplateHandler = new LocalHostUriTemplateHandler(new MockEnvironment());
		template.setUriTemplateHandler(uriTemplateHandler);
		callback.doWithTestRestTemplate(template, URI.create("/a/b/c.txt?param=%7Bsomething%7D"));
		then(requestFactory).should().createRequest(eq(absoluteUri), any(HttpMethod.class));
	}

	private void assertBasicAuthorizationCredentials(TestRestTemplate testRestTemplate, String username,
			String password) {
		ClientHttpRequest request = ReflectionTestUtils.invokeMethod(testRestTemplate.getRestTemplate(),
				"createRequest", URI.create("http://localhost"), HttpMethod.POST);
		if (username == null) {
			assertThat(request.getHeaders().headerNames()).doesNotContain(HttpHeaders.AUTHORIZATION);
		}
		else {
			assertThat(request.getHeaders().headerNames()).contains(HttpHeaders.AUTHORIZATION);
			assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic "
					+ Base64.getEncoder().encodeToString(String.format("%s:%s", username, password).getBytes()));
		}

	}

	interface TestRestTemplateCallback {

		void doWithTestRestTemplate(TestRestTemplate testRestTemplate, URI relativeUri);

	}

	static class TestClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

	}

}
