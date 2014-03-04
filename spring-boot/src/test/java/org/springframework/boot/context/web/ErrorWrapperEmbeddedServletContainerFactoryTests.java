/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.junit.Test;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ErrorWrapperEmbeddedServletContainerFactory}.
 * 
 * @author Dave Syer
 */
public class ErrorWrapperEmbeddedServletContainerFactoryTests {

	private ErrorWrapperEmbeddedServletContainerFactory filter = new ErrorWrapperEmbeddedServletContainerFactory();

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private MockFilterChain chain = new MockFilterChain();

	@Test
	public void notAnError() throws Exception {
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.chain.getRequest(), equalTo((ServletRequest) this.request));
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse(),
				equalTo((ServletResponse) this.response));
	}

	@Test
	public void globalError() throws Exception {
		this.filter.addErrorPages(new ErrorPage("/error"));
		this.chain = new MockFilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response)
					throws IOException, ServletException {
				((HttpServletResponse) response).sendError(400, "BAD");
				super.doFilter(request, response);
			}
		};
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus(),
				equalTo(400));
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE),
				equalTo((Object) 400));
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE),
				equalTo((Object) "BAD"));
	}

	@Test
	public void statusError() throws Exception {
		this.filter.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/400"));
		this.chain = new MockFilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response)
					throws IOException, ServletException {
				((HttpServletResponse) response).sendError(400, "BAD");
				super.doFilter(request, response);
			}
		};
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus(),
				equalTo(400));
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE),
				equalTo((Object) 400));
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE),
				equalTo((Object) "BAD"));
	}

	@Test
	public void exceptionError() throws Exception {
		this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
		this.chain = new MockFilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response)
					throws IOException, ServletException {
				super.doFilter(request, response);
				throw new RuntimeException("BAD");
			}
		};
		this.filter.doFilter(this.request, this.response, this.chain);
		assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus(),
				equalTo(500));
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE),
				equalTo((Object) 500));
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE),
				equalTo((Object) "BAD"));
		assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE),
				equalTo((Object) RuntimeException.class.getName()));
	}
}
