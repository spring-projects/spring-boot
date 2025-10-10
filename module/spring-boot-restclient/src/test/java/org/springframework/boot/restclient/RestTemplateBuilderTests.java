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

package org.springframework.boot.restclient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestTemplateBuilder}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 * @author Kevin Strijbos
 * @author Ilya Lukyanovich
 * @author Brian Clozel
 * @author Yanming Zhou
 */
@ExtendWith(MockitoExtension.class)
class RestTemplateBuilderTests {

	private final RestTemplateBuilder builder = new RestTemplateBuilder();

	@Mock
	private HttpMessageConverter<Object> messageConverter;

	@Mock
	private ClientHttpRequestInterceptor interceptor;

	@Test
	void createWhenCustomizersAreNullShouldThrowException() {
		RestTemplateCustomizer[] customizers = null;
		assertThatIllegalArgumentException().isThrownBy(() -> new RestTemplateBuilder(customizers))
			.withMessageContaining("'customizers' must not be null");
	}

	@Test
	void createWithCustomizersShouldApplyCustomizers() {
		RestTemplateCustomizer customizer = mock(RestTemplateCustomizer.class);
		RestTemplate template = new RestTemplateBuilder(customizer).build();
		then(customizer).should().customize(template);
	}

	@Test
	void buildShouldDetectRequestFactory() {
		RestTemplate restTemplate = this.builder.build();
		assertThat(restTemplate.getRequestFactory()).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	void detectRequestFactoryWhenFalseShouldDisableDetection() {
		RestTemplate restTemplate = this.builder.detectRequestFactory(false).build();
		assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
	}

	@Test
	void rootUriShouldApply() {
		RestTemplate restTemplate = this.builder.rootUri("https://example.com").build();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo("https://example.com/hello")).andRespond(withSuccess());
		restTemplate.getForEntity("/hello", String.class);
		server.verify();
	}

	@Test
	void rootUriShouldApplyAfterUriTemplateHandler() {
		UriTemplateHandler uriTemplateHandler = mock(UriTemplateHandler.class);
		RestTemplate template = this.builder.uriTemplateHandler(uriTemplateHandler)
			.rootUri("https://example.com")
			.build();
		UriTemplateHandler handler = template.getUriTemplateHandler();
		handler.expand("/hello");
		assertThat(handler).isInstanceOf(RootUriBuilderFactory.class);
		then(uriTemplateHandler).should().expand("https://example.com/hello");
	}

	@Test
	void messageConvertersWhenConvertersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.messageConverters((HttpMessageConverter<?>[]) null))
			.withMessageContaining("'messageConverters' must not be null");
	}

	@Test
	void messageConvertersCollectionWhenConvertersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.messageConverters((Set<HttpMessageConverter<?>>) null))
			.withMessageContaining("'messageConverters' must not be null");
	}

	@Test
	void messageConvertersShouldApply() {
		RestTemplate template = this.builder.messageConverters(this.messageConverter).build();
		assertThat(template.getMessageConverters()).containsOnly(this.messageConverter);
	}

	@Test
	void messageConvertersShouldReplaceExisting() {
		RestTemplate template = this.builder.messageConverters(new ResourceHttpMessageConverter())
			.messageConverters(Collections.singleton(this.messageConverter))
			.build();
		assertThat(template.getMessageConverters()).containsOnly(this.messageConverter);
	}

	@Test
	void additionalMessageConvertersWhenConvertersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalMessageConverters((HttpMessageConverter<?>[]) null))
			.withMessageContaining("'messageConverters' must not be null");
	}

	@Test
	void additionalMessageConvertersCollectionWhenConvertersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalMessageConverters((Set<HttpMessageConverter<?>>) null))
			.withMessageContaining("'messageConverters' must not be null");
	}

	@Test
	void additionalMessageConvertersShouldAddToExisting() {
		HttpMessageConverter<?> resourceConverter = new ResourceHttpMessageConverter();
		RestTemplate template = this.builder.messageConverters(resourceConverter)
			.additionalMessageConverters(this.messageConverter)
			.build();
		assertThat(template.getMessageConverters()).containsOnly(resourceConverter, this.messageConverter);
	}

	@Test
	void defaultMessageConvertersShouldSetDefaultList() {
		RestTemplate template = new RestTemplate(Collections.singletonList(new StringHttpMessageConverter()));
		this.builder.defaultMessageConverters().configure(template);
		assertThat(template.getMessageConverters()).hasSameSizeAs(new RestTemplate().getMessageConverters());
	}

	@Test
	void defaultMessageConvertersShouldClearExisting() {
		RestTemplate template = new RestTemplate(Collections.singletonList(new StringHttpMessageConverter()));
		this.builder.additionalMessageConverters(this.messageConverter).defaultMessageConverters().configure(template);
		assertThat(template.getMessageConverters()).hasSameSizeAs(new RestTemplate().getMessageConverters());
	}

	@Test
	void interceptorsWhenInterceptorsAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.interceptors((ClientHttpRequestInterceptor[]) null))
			.withMessageContaining("'interceptors' must not be null");
	}

	@Test
	void interceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.interceptors((Set<ClientHttpRequestInterceptor>) null))
			.withMessageContaining("'interceptors' must not be null");
	}

	@Test
	void interceptorsShouldApply() {
		RestTemplate template = this.builder.interceptors(this.interceptor).build();
		assertThat(template.getInterceptors()).containsOnly(this.interceptor);
	}

	@Test
	void interceptorsShouldReplaceExisting() {
		RestTemplate template = this.builder.interceptors(mock(ClientHttpRequestInterceptor.class))
			.interceptors(Collections.singleton(this.interceptor))
			.build();
		assertThat(template.getInterceptors()).containsOnly(this.interceptor);
	}

	@Test
	void additionalInterceptorsWhenInterceptorsAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalInterceptors((ClientHttpRequestInterceptor[]) null))
			.withMessageContaining("'interceptors' must not be null");
	}

	@Test
	void additionalInterceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalInterceptors((Set<ClientHttpRequestInterceptor>) null))
			.withMessageContaining("'interceptors' must not be null");
	}

	@Test
	void additionalInterceptorsShouldAddToExisting() {
		ClientHttpRequestInterceptor interceptor = mock(ClientHttpRequestInterceptor.class);
		RestTemplate template = this.builder.interceptors(interceptor).additionalInterceptors(this.interceptor).build();
		assertThat(template.getInterceptors()).containsOnly(interceptor, this.interceptor);
	}

	@Test
	void requestFactoryClassWhenFactoryIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.requestFactory((Class<ClientHttpRequestFactory>) null))
			.withMessageContaining("'requestFactoryType' must not be null");
	}

	@Test
	void requestFactoryClassShouldApply() {
		RestTemplate template = this.builder.requestFactory(SimpleClientHttpRequestFactory.class).build();
		assertThat(template.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
	}

	@Test
	void requestFactoryPackagePrivateClassShouldApply() {
		RestTemplate template = this.builder.requestFactory(TestClientHttpRequestFactory.class).build();
		assertThat(template.getRequestFactory()).isInstanceOf(TestClientHttpRequestFactory.class);
	}

	@Test
	void requestFactoryWhenSupplierIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.requestFactory((Supplier<ClientHttpRequestFactory>) null))
			.withMessageContaining("requestFactorySupplier' must not be null");
	}

	@Test
	void requestFactoryShouldApply() {
		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		RestTemplate template = this.builder.requestFactory(() -> requestFactory).build();
		assertThat(template.getRequestFactory()).isSameAs(requestFactory);
	}

	@Test
	void uriTemplateHandlerWhenHandlerIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.builder.uriTemplateHandler(null))
			.withMessageContaining("'uriTemplateHandler' must not be null");
	}

	@Test
	void uriTemplateHandlerShouldApply() {
		UriTemplateHandler uriTemplateHandler = mock(UriTemplateHandler.class);
		RestTemplate template = this.builder.uriTemplateHandler(uriTemplateHandler).build();
		assertThat(template.getUriTemplateHandler()).isSameAs(uriTemplateHandler);
	}

	@Test
	void errorHandlerWhenHandlerIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.builder.errorHandler(null))
			.withMessageContaining("'errorHandler' must not be null");
	}

	@Test
	void errorHandlerShouldApply() {
		ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);
		RestTemplate template = this.builder.errorHandler(errorHandler).build();
		assertThat(template.getErrorHandler()).isSameAs(errorHandler);
	}

	@Test
	void basicAuthenticationShouldApply() {
		RestTemplate template = this.builder.basicAuthentication("spring", "boot", StandardCharsets.UTF_8).build();
		ClientHttpRequest request = createRequest(template);
		assertThat(request.getHeaders().headerNames()).containsOnly(HttpHeaders.AUTHORIZATION);
		assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
	}

	@Test
	void defaultHeaderAddsHeader() {
		RestTemplate template = this.builder.defaultHeader("spring", "boot").build();
		ClientHttpRequest request = createRequest(template);
		assertThat(request.getHeaders().headerSet()).contains(entry("spring", Collections.singletonList("boot")));
	}

	@Test
	void defaultHeaderAddsHeaderValues() {
		String name = HttpHeaders.ACCEPT;
		String[] values = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE };
		RestTemplate template = this.builder.defaultHeader(name, values).build();
		ClientHttpRequest request = createRequest(template);
		assertThat(request.getHeaders().headerSet()).contains(entry(name, Arrays.asList(values)));
	}

	@Test // gh-17885
	void defaultHeaderWhenUsingMockRestServiceServerAddsHeader() {
		RestTemplate template = this.builder.defaultHeader("spring", "boot").build();
		MockRestServiceServer.bindTo(template).build();
		ClientHttpRequest request = createRequest(template);
		assertThat(request.getHeaders().headerSet()).contains(entry("spring", Collections.singletonList("boot")));
	}

	@Test
	void clientSettingsAppliesSettings() {
		HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(1));
		RestTemplate template = this.builder.clientSettings(settings).build();
		assertThat(template.getRequestFactory()).extracting("connectTimeout").isEqualTo(1000L);
	}

	@Test
	void requestCustomizersAddsCustomizers() {
		RestTemplate template = this.builder
			.requestCustomizers((request) -> request.getHeaders().add("spring", "framework"))
			.build();
		ClientHttpRequest request = createRequest(template);
		assertThat(request.getHeaders().headerSet()).contains(entry("spring", Collections.singletonList("framework")));
	}

	@Test
	void additionalRequestCustomizersAddsCustomizers() {
		RestTemplate template = this.builder
			.requestCustomizers((request) -> request.getHeaders().add("spring", "framework"))
			.additionalRequestCustomizers((request) -> request.getHeaders().add("for", "java"))
			.build();
		ClientHttpRequest request = createRequest(template);
		assertThat(request.getHeaders().headerSet()).contains(entry("spring", Collections.singletonList("framework")))
			.contains(entry("for", Collections.singletonList("java")));
	}

	@Test
	void customizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.builder.customizers((RestTemplateCustomizer[]) null))
			.withMessageContaining("'customizers' must not be null");
	}

	@Test
	void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.customizers((Set<RestTemplateCustomizer>) null))
			.withMessageContaining("'customizers' must not be null");
	}

	@Test
	void customizersShouldApply() {
		RestTemplateCustomizer customizer = mock(RestTemplateCustomizer.class);
		RestTemplate template = this.builder.customizers(customizer).build();
		then(customizer).should().customize(template);
	}

	@Test
	void customizersShouldBeAppliedLast() {
		RestTemplate template = spy(new RestTemplate());
		this.builder.additionalCustomizers(
				(restTemplate) -> then(restTemplate).should().setRequestFactory(any(ClientHttpRequestFactory.class)));
		this.builder.configure(template);
	}

	@Test
	void customizersShouldReplaceExisting() {
		RestTemplateCustomizer customizer1 = mock(RestTemplateCustomizer.class);
		RestTemplateCustomizer customizer2 = mock(RestTemplateCustomizer.class);
		RestTemplate template = this.builder.customizers(customizer1)
			.customizers(Collections.singleton(customizer2))
			.build();
		then(customizer1).shouldHaveNoInteractions();
		then(customizer2).should().customize(template);
	}

	@Test
	void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalCustomizers((RestTemplateCustomizer[]) null))
			.withMessageContaining("'customizers' must not be null");
	}

	@Test
	void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalCustomizers((Set<RestTemplateCustomizer>) null))
			.withMessageContaining("customizers' must not be null");
	}

	@Test
	void additionalCustomizersShouldAddToExisting() {
		RestTemplateCustomizer customizer1 = mock(RestTemplateCustomizer.class);
		RestTemplateCustomizer customizer2 = mock(RestTemplateCustomizer.class);
		RestTemplate template = this.builder.customizers(customizer1).additionalCustomizers(customizer2).build();
		InOrder ordered = inOrder(customizer1, customizer2);
		ordered.verify(customizer1).customize(template);
		ordered.verify(customizer2).customize(template);
	}

	@Test
	void customizerShouldBeAppliedAtTheEnd() {
		ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);
		ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		this.builder.interceptors(this.interceptor)
			.messageConverters(this.messageConverter)
			.rootUri("http://localhost:8080")
			.errorHandler(errorHandler)
			.basicAuthentication("spring", "boot")
			.requestFactory(() -> requestFactory)
			.customizers((restTemplate) -> {
				assertThat(restTemplate.getInterceptors()).hasSize(1);
				assertThat(restTemplate.getMessageConverters()).contains(this.messageConverter);
				assertThat(restTemplate.getUriTemplateHandler()).isInstanceOf(RootUriBuilderFactory.class);
				assertThat(restTemplate.getErrorHandler()).isEqualTo(errorHandler);
				ClientHttpRequestFactory actualRequestFactory = restTemplate.getRequestFactory();
				assertThat(actualRequestFactory).isInstanceOf(InterceptingClientHttpRequestFactory.class);
				ClientHttpRequestInitializer initializer = restTemplate.getClientHttpRequestInitializers().get(0);
				assertThat(initializer).isInstanceOf(RestTemplateBuilderClientHttpRequestInitializer.class);
			})
			.build();
	}

	@Test
	void buildShouldReturnRestTemplate() {
		RestTemplate template = this.builder.build();
		assertThat(template.getClass()).isEqualTo(RestTemplate.class);
	}

	@Test
	void buildClassShouldReturnClassInstance() {
		RestTemplateSubclass template = this.builder.build(RestTemplateSubclass.class);
		assertThat(template.getClass()).isEqualTo(RestTemplateSubclass.class);
	}

	@Test
	void configureShouldApply() {
		RestTemplate template = new RestTemplate();
		this.builder.configure(template);
		assertThat(template.getRequestFactory()).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	void unwrappingDoesNotAffectRequestFactoryThatIsSetOnTheBuiltTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		RestTemplate template = this.builder.requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory))
			.build();
		assertThat(template.getRequestFactory()).isInstanceOf(BufferingClientHttpRequestFactory.class);
	}

	@Test
	void configureRedirects() {
		assertThat(this.builder.redirects(HttpRedirects.DONT_FOLLOW)).extracting("clientSettings")
			.extracting("redirects")
			.isSameAs(HttpRedirects.DONT_FOLLOW);
	}

	private ClientHttpRequest createRequest(RestTemplate template) {
		return ReflectionTestUtils.invokeMethod(template, "createRequest", URI.create("http://localhost"),
				HttpMethod.GET);
	}

	static class RestTemplateSubclass extends RestTemplate {

	}

	static class TestClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

	}

	static class TestHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

	}

}
