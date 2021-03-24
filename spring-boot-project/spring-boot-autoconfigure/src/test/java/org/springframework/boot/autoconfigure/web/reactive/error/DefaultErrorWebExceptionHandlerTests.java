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

package org.springframework.boot.autoconfigure.web.reactive.error;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultErrorWebExceptionHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class DefaultErrorWebExceptionHandlerTests {

	@Test
	void disconnectedClientExceptionsMatchesFramework() {
		Object errorHandlers = ReflectionTestUtils.getField(AbstractErrorWebExceptionHandler.class,
				"DISCONNECTED_CLIENT_EXCEPTIONS");
		Object webHandlers = ReflectionTestUtils.getField(HttpWebHandlerAdapter.class,
				"DISCONNECTED_CLIENT_EXCEPTIONS");
		assertThat(errorHandlers).isNotNull().isEqualTo(webHandlers);
	}

	@Test
	void nonStandardErrorStatusCodeShouldNotFail() {
		ErrorAttributes errorAttributes = mock(ErrorAttributes.class);
		given(errorAttributes.getErrorAttributes(any(), any())).willReturn(getErrorAttributes());
		ResourceProperties resourceProperties = new ResourceProperties();
		ErrorProperties errorProperties = new ErrorProperties();
		ApplicationContext context = new AnnotationConfigReactiveWebApplicationContext();
		DefaultErrorWebExceptionHandler exceptionHandler = new DefaultErrorWebExceptionHandler(errorAttributes,
				resourceProperties, errorProperties, context);
		setupViewResolver(exceptionHandler);
		ServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/some-other-path").accept(MediaType.TEXT_HTML));
		exceptionHandler.handle(exchange, new RuntimeException()).block();
	}

	private Map<String, Object> getErrorAttributes() {
		return Collections.singletonMap("status", 498);
	}

	private void setupViewResolver(DefaultErrorWebExceptionHandler exceptionHandler) {
		View view = mock(View.class);
		given(view.render(any(), any(), any())).willReturn(Mono.empty());
		ViewResolver viewResolver = mock(ViewResolver.class);
		given(viewResolver.resolveViewName(any(), any())).willReturn(Mono.just(view));
		exceptionHandler.setViewResolvers(Collections.singletonList(viewResolver));
	}

	@Test
	void acceptsTextHtmlShouldNotConsiderMediaAllEvenWithQuality() {
		ErrorAttributes errorAttributes = mock(ErrorAttributes.class);
		ResourceProperties resourceProperties = new ResourceProperties();
		ErrorProperties errorProperties = new ErrorProperties();
		ApplicationContext context = new AnnotationConfigReactiveWebApplicationContext();
		DefaultErrorWebExceptionHandler exceptionHandler = new DefaultErrorWebExceptionHandler(errorAttributes,
				resourceProperties, errorProperties, context);
		MediaType allWithQuality = new MediaType(MediaType.ALL.getType(), MediaType.ALL.getSubtype(), 0.9);
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/test").accept(allWithQuality));
		List<HttpMessageReader<?>> readers = ServerCodecConfigurer.create().getReaders();
		ServerRequest request = ServerRequest.create(exchange, readers);
		assertThat(exceptionHandler.acceptsTextHtml().test(request)).isFalse();
	}

}
