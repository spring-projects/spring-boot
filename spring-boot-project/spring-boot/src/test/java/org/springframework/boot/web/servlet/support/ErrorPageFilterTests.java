/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.servlet.support;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.util.NestedServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ErrorPageFilter}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class ErrorPageFilterTests {

	private ErrorPageFilter filter = new ErrorPageFilter();

	private DispatchRecordingMockHttpServletRequest request = new DispatchRecordingMockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private MockFilterChain chain = new TestFilterChain((request, response, chain) -> {
	});

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void notAnError() throws Exception {
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(this.response.isCommitted()).isTrue();
		assertThat(this.response.getForwardedUrl()).isNull();
	}

	@Test
	public void notAnErrorButNotOK() throws Exception {
		this.chain = new TestFilterChain((request, response, chain) -> {
			response.setStatus(201);
			chain.call();
			response.flushBuffer();
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponse) this.chain.getResponse()).getStatus()).isEqualTo(201);
		assertThat(((HttpServletResponse) ((HttpServletResponseWrapper) this.chain.getResponse()).getResponse())
				.getStatus()).isEqualTo(201);
		assertThat(this.response.isCommitted()).isTrue();
	}

	@Test
	public void unauthorizedWithErrorPath() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.chain = new TestFilterChain((request, response, chain) -> response.sendError(401, "UNAUTHORIZED"));
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		HttpServletResponseWrapper wrapper = (HttpServletResponseWrapper) this.chain.getResponse();
		assertThat(wrapper.getResponse()).isEqualTo(this.response);
		assertThat(this.response.isCommitted()).isTrue();
		assertThat(wrapper.getStatus()).isEqualTo(401);
		// The real response has to be 401 as well...
		assertThat(this.response.getStatus()).isEqualTo(401);
		assertThat(this.response.getForwardedUrl()).isEqualTo("/error");
	}

	@Test
	public void responseCommitted() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.response.setCommitted(true);
		this.chain = new TestFilterChain((request, response, chain) -> response.sendError(400, "BAD"));
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
		assertThat(this.response.getForwardedUrl()).isNull();
		assertThat(this.response.isCommitted()).isTrue();
	}

	@Test
	public void responseCommittedWhenFromClientAbortException() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.response.setCommitted(true);
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			throw new ClientAbortException();
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.isCommitted()).isTrue();
		assertThat(this.output.toString()).doesNotContain("Cannot forward");
	}

	@Test
	public void responseUncommittedWithoutErrorPage() throws Exception {
		this.chain = new TestFilterChain((request, response, chain) -> response.sendError(400, "BAD"));
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
		assertThat(this.response.getForwardedUrl()).isNull();
		assertThat(this.response.isCommitted()).isTrue();
	}

	@Test
	public void oncePerRequest() throws Exception {
		this.chain = new TestFilterChain((request, response, chain) -> {
			response.sendError(400, "BAD");
			assertThat(request.getAttribute("FILTER.FILTERED")).isNotNull();
		});
		this.filter.init(new MockFilterConfig("FILTER"));
		this.filter.doFilter(this.request, this.response, this.chain);
	}

	@Test
	public void globalError() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.chain = new TestFilterChain((request, response, chain) -> response.sendError(400, "BAD"));
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(400);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
		assertThat(this.response.isCommitted()).isTrue();
		assertThat(this.response.getForwardedUrl()).isEqualTo("/error");
	}

	@Test
	public void statusError() throws Exception {
		this.filter.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/400"));
		this.chain = new TestFilterChain((request, response, chain) -> response.sendError(400, "BAD"));
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(400);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
		assertThat(this.response.isCommitted()).isTrue();
		assertThat(this.response.getForwardedUrl()).isEqualTo("/400");
	}

	@Test
	public void statusErrorWithCommittedResponse() throws Exception {
		this.filter.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/400"));
		this.chain = new TestFilterChain((request, response, chain) -> {
			response.sendError(400, "BAD");
			response.flushBuffer();
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
		assertThat(this.response.isCommitted()).isTrue();
		assertThat(this.response.getForwardedUrl()).isNull();
	}

	@Test
	public void exceptionError() throws Exception {
		this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			throw new RuntimeException("BAD");
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(500);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(500);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
		Map<String, Object> requestAttributes = getAttributesForDispatch("/500");
		assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isEqualTo(RuntimeException.class);
		assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION)).isInstanceOf(RuntimeException.class);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isNull();
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
		assertThat(this.response.isCommitted()).isTrue();
		assertThat(this.response.getForwardedUrl()).isEqualTo("/500");
	}

	@Test
	public void exceptionErrorWithCommittedResponse() throws Exception {
		this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			response.flushBuffer();
			throw new RuntimeException("BAD");
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getForwardedUrl()).isNull();
	}

	@Test
	public void statusCode() throws Exception {
		this.chain = new TestFilterChain((request, response, chain) -> assertThat(response.getStatus()).isEqualTo(200));
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(200);
	}

	@Test
	public void subClassExceptionError() throws Exception {
		this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			throw new IllegalStateException("BAD");
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(500);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(500);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
		Map<String, Object> requestAttributes = getAttributesForDispatch("/500");
		assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION_TYPE))
				.isEqualTo(IllegalStateException.class);
		assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION)).isInstanceOf(IllegalStateException.class);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isNull();
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
		assertThat(this.response.isCommitted()).isTrue();
	}

	@Test
	public void responseIsNotCommittedWhenRequestIsAsync() throws Exception {
		this.request.setAsyncStarted(true);
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(this.response.isCommitted()).isFalse();
	}

	@Test
	public void responseIsCommittedWhenRequestIsAsyncAndExceptionIsThrown() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.request.setAsyncStarted(true);
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			throw new RuntimeException("BAD");
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(this.response.isCommitted()).isTrue();
	}

	@Test
	public void responseIsCommittedWhenRequestIsAsyncAndStatusIs400Plus() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.request.setAsyncStarted(true);
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			response.sendError(400, "BAD");
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(this.response.isCommitted()).isTrue();
	}

	@Test
	public void responseIsNotCommittedDuringAsyncDispatch() throws Exception {
		setUpAsyncDispatch();
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(this.response.isCommitted()).isFalse();
	}

	@Test
	public void responseIsCommittedWhenExceptionIsThrownDuringAsyncDispatch() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		setUpAsyncDispatch();
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			throw new RuntimeException("BAD");
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(this.response.isCommitted()).isTrue();
	}

	@Test
	public void responseIsCommittedWhenStatusIs400PlusDuringAsyncDispatch() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		setUpAsyncDispatch();
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			response.sendError(400, "BAD");
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest()).isEqualTo(this.request);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
		assertThat(this.response.isCommitted()).isTrue();
	}

	@Test
	public void responseIsNotFlushedIfStatusIsLessThan400AndItHasAlreadyBeenCommitted() throws Exception {
		HttpServletResponse committedResponse = mock(HttpServletResponse.class);
		given(committedResponse.isCommitted()).willReturn(true);
		given(committedResponse.getStatus()).willReturn(200);
		this.filter.doFilter(this.request, committedResponse, this.chain);
		verify(committedResponse, never()).flushBuffer();
	}

	@Test
	public void errorMessageForRequestWithoutPathInfo() throws IOException, ServletException {
		this.request.setServletPath("/test");
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			throw new RuntimeException();
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.output.toString()).contains("request [/test]");
	}

	@Test
	public void errorMessageForRequestWithPathInfo() throws IOException, ServletException {
		this.request.setServletPath("/test");
		this.request.setPathInfo("/alpha");
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			throw new RuntimeException();
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.output.toString()).contains("request [/test/alpha]");
	}

	@Test
	public void nestedServletExceptionIsUnwrapped() throws Exception {
		this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
		this.chain = new TestFilterChain((request, response, chain) -> {
			chain.call();
			throw new NestedServletException("Wrapper", new RuntimeException("BAD"));
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(500);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(500);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
		Map<String, Object> requestAttributes = getAttributesForDispatch("/500");
		assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isEqualTo(RuntimeException.class);
		assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION)).isInstanceOf(RuntimeException.class);
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isNull();
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
		assertThat(this.response.isCommitted()).isTrue();
		assertThat(this.response.getForwardedUrl()).isEqualTo("/500");
	}

	@Test
	public void whenErrorIsSentAndWriterIsFlushedErrorIsSentToTheClient() throws Exception {
		this.chain = new TestFilterChain((request, response, chain) -> {
			response.sendError(400);
			response.getWriter().flush();
		});
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(400);
	}

	private void setUpAsyncDispatch() throws Exception {
		this.request.setAsyncSupported(true);
		this.request.setAsyncStarted(true);
		DeferredResult<String> result = new DeferredResult<>();
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setAsyncWebRequest(new StandardServletAsyncWebRequest(this.request, this.response));
		asyncManager.startDeferredResultProcessing(result);
	}

	private Map<String, Object> getAttributesForDispatch(String path) {
		return this.request.getDispatcher(path).getRequestAttributes();
	}

	private static class TestFilterChain extends MockFilterChain {

		private final FilterHandler handler;

		TestFilterChain(FilterHandler handler) {
			this.handler = handler;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
			AtomicBoolean called = new AtomicBoolean();
			Chain chain = () -> {
				if (called.compareAndSet(false, true)) {
					super.doFilter(request, response);
				}
			};
			this.handler.handle((HttpServletRequest) request, (HttpServletResponse) response, chain);
			chain.call();
		}

	}

	@FunctionalInterface
	private interface FilterHandler {

		void handle(HttpServletRequest request, HttpServletResponse response, Chain chain)
				throws IOException, ServletException;

	}

	@FunctionalInterface
	private interface Chain {

		void call() throws IOException, ServletException;

	}

	private static final class DispatchRecordingMockHttpServletRequest extends MockHttpServletRequest {

		private final Map<String, AttributeCapturingRequestDispatcher> dispatchers = new HashMap<>();

		private DispatchRecordingMockHttpServletRequest() {
			super("GET", "/test/path");
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String path) {
			AttributeCapturingRequestDispatcher dispatcher = new AttributeCapturingRequestDispatcher(path);
			this.dispatchers.put(path, dispatcher);
			return dispatcher;
		}

		private AttributeCapturingRequestDispatcher getDispatcher(String path) {
			return this.dispatchers.get(path);
		}

		private static final class AttributeCapturingRequestDispatcher extends MockRequestDispatcher {

			private final Map<String, Object> requestAttributes = new HashMap<>();

			private AttributeCapturingRequestDispatcher(String resource) {
				super(resource);
			}

			@Override
			public void forward(ServletRequest request, ServletResponse response) {
				captureAttributes(request);
				super.forward(request, response);
			}

			private void captureAttributes(ServletRequest request) {
				Enumeration<String> names = request.getAttributeNames();
				while (names.hasMoreElements()) {
					String name = names.nextElement();
					this.requestAttributes.put(name, request.getAttribute(name));
				}
			}

			private Map<String, Object> getRequestAttributes() {
				return this.requestAttributes;
			}

		}

	}

}
