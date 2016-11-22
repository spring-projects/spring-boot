/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.web.client;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.junit.Test;

import org.springframework.boot.test.web.client.TestRestTemplate.CustomHttpComponentsClientHttpRequestFactory;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestRestTemplate}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class TestRestTemplateTests {

	@Test
	public void fromRestTemplateBuilder() {
		RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
		RestTemplate delegate = new RestTemplate();
		given(builder.build()).willReturn(delegate);
		assertThat(new TestRestTemplate(builder).getRestTemplate()).isEqualTo(delegate);
	}

	@Test
	public void simple() {
		// The Apache client is on the classpath so we get the fully-fledged factory
		assertThat(new TestRestTemplate().getRestTemplate().getRequestFactory())
				.isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	public void authenticated() {
		assertThat(new TestRestTemplate("user", "password").getRestTemplate()
				.getRequestFactory())
						.isInstanceOf(InterceptingClientHttpRequestFactory.class);
	}

	@Test
	public void options() throws Exception {
		TestRestTemplate template = new TestRestTemplate(
				HttpClientOption.ENABLE_REDIRECTS);
		CustomHttpComponentsClientHttpRequestFactory factory = (CustomHttpComponentsClientHttpRequestFactory) template
				.getRestTemplate().getRequestFactory();
		RequestConfig config = factory.getRequestConfig();
		assertThat(config.isRedirectsEnabled()).isTrue();
	}

	@Test
	public void restOperationsAreAvailable() throws Exception {
		RestTemplate delegate = mock(RestTemplate.class);
		final TestRestTemplate restTemplate = new TestRestTemplate(delegate);
		ReflectionUtils.doWithMethods(RestOperations.class, new MethodCallback() {

			@Override
			public void doWith(Method method)
					throws IllegalArgumentException, IllegalAccessException {
				Method equivalent = ReflectionUtils.findMethod(TestRestTemplate.class,
						method.getName(), method.getParameterTypes());
				try {
					equivalent.invoke(restTemplate,
							mockArguments(method.getParameterTypes()));
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
				return mock(type);
			}

		});

	}

	@Test
	public void withBasicAuthAddsBasicAuthInterceptorWhenNotAlreadyPresent() {
		TestRestTemplate originalTemplate = new TestRestTemplate();
		TestRestTemplate basicAuthTemplate = originalTemplate.withBasicAuth("user",
				"password");
		assertThat(basicAuthTemplate.getRestTemplate().getMessageConverters())
				.containsExactlyElementsOf(
						originalTemplate.getRestTemplate().getMessageConverters());
		assertThat(basicAuthTemplate.getRestTemplate().getRequestFactory())
				.isInstanceOf(InterceptingClientHttpRequestFactory.class);
		assertThat(ReflectionTestUtils.getField(
				basicAuthTemplate.getRestTemplate().getRequestFactory(),
				"requestFactory"))
						.isInstanceOf(CustomHttpComponentsClientHttpRequestFactory.class);
		assertThat(basicAuthTemplate.getRestTemplate().getUriTemplateHandler())
				.isSameAs(originalTemplate.getRestTemplate().getUriTemplateHandler());
		assertThat(basicAuthTemplate.getRestTemplate().getInterceptors())
				.containsExactlyElementsOf(
						originalTemplate.getRestTemplate().getInterceptors());
		assertBasicAuthorizationInterceptorCredentials(basicAuthTemplate, "user",
				"password");
	}

	@Test
	public void withBasicAuthReplacesBasicAuthInterceptorWhenAlreadyPresent() {
		TestRestTemplate original = new TestRestTemplate("foo", "bar");
		TestRestTemplate basicAuth = original.withBasicAuth("user", "password");
		assertThat(basicAuth.getRestTemplate().getMessageConverters())
				.containsExactlyElementsOf(
						original.getRestTemplate().getMessageConverters());
		assertThat(basicAuth.getRestTemplate().getRequestFactory())
				.isInstanceOf(InterceptingClientHttpRequestFactory.class);
		assertThat(ReflectionTestUtils.getField(
				basicAuth.getRestTemplate().getRequestFactory(), "requestFactory"))
						.isInstanceOf(CustomHttpComponentsClientHttpRequestFactory.class);
		assertThat(basicAuth.getRestTemplate().getUriTemplateHandler())
				.isSameAs(original.getRestTemplate().getUriTemplateHandler());
		assertThat(basicAuth.getRestTemplate().getInterceptors())
				.containsExactlyElementsOf(original.getRestTemplate().getInterceptors());
		assertBasicAuthorizationInterceptorCredentials(basicAuth, "user", "password");
	}

	@Test
	public void withBasicAuthDoesNotResetErrorHandler() throws Exception {
		TestRestTemplate originalTemplate = new TestRestTemplate("foo", "bar");
		ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);
		originalTemplate.getRestTemplate().setErrorHandler(errorHandler);
		TestRestTemplate basicAuthTemplate = originalTemplate.withBasicAuth("user",
				"password");
		assertThat(basicAuthTemplate.getRestTemplate().getErrorHandler())
				.isSameAs(errorHandler);
	}

	private void assertBasicAuthorizationInterceptorCredentials(
			TestRestTemplate testRestTemplate, String username, String password) {
		@SuppressWarnings("unchecked")
		List<ClientHttpRequestInterceptor> requestFactoryInterceptors = (List<ClientHttpRequestInterceptor>) ReflectionTestUtils
				.getField(testRestTemplate.getRestTemplate().getRequestFactory(),
						"interceptors");
		assertThat(requestFactoryInterceptors).hasSize(1);
		ClientHttpRequestInterceptor interceptor = requestFactoryInterceptors.get(0);
		assertThat(interceptor).isInstanceOf(BasicAuthorizationInterceptor.class);
		assertThat(ReflectionTestUtils.getField(interceptor, "username"))
				.isEqualTo(username);
		assertThat(ReflectionTestUtils.getField(interceptor, "password"))
				.isEqualTo(password);

	}
}
