/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.security.servlet;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ApplicationContextRequestMatcher}.
 *
 * @author Phillip Webb
 */
class ApplicationContextRequestMatcherTests {

	@Test
	void createWhenContextClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TestApplicationContextRequestMatcher<>(null))
				.withMessageContaining("Context class must not be null");
	}

	@Test
	void matchesWhenContextClassIsApplicationContextShouldProvideContext() {
		StaticWebApplicationContext context = createWebApplicationContext();
		assertThat(new TestApplicationContextRequestMatcher<>(ApplicationContext.class)
				.callMatchesAndReturnProvidedContext(context).get()).isEqualTo(context);
	}

	@Test
	void matchesWhenContextClassIsExistingBeanShouldProvideBean() {
		StaticWebApplicationContext context = createWebApplicationContext();
		context.registerSingleton("existingBean", ExistingBean.class);
		assertThat(new TestApplicationContextRequestMatcher<>(ExistingBean.class)
				.callMatchesAndReturnProvidedContext(context).get()).isEqualTo(context.getBean(ExistingBean.class));
	}

	@Test
	void matchesWhenContextClassIsBeanThatDoesNotExistShouldSupplyException() {
		StaticWebApplicationContext context = createWebApplicationContext();
		Supplier<ExistingBean> supplier = new TestApplicationContextRequestMatcher<>(ExistingBean.class)
				.callMatchesAndReturnProvidedContext(context);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(supplier::get);
	}

	@Test // gh-18012
	void matchesWhenCalledWithDifferentApplicationContextDoesNotCache() {
		StaticWebApplicationContext context1 = createWebApplicationContext();
		StaticWebApplicationContext context2 = createWebApplicationContext();
		TestApplicationContextRequestMatcher<ApplicationContext> matcher = new TestApplicationContextRequestMatcher<>(
				ApplicationContext.class);
		assertThat(matcher.callMatchesAndReturnProvidedContext(context1).get()).isEqualTo(context1);
		assertThat(matcher.callMatchesAndReturnProvidedContext(context2).get()).isEqualTo(context2);
	}

	@Test
	void initializeAndMatchesAreNotCalledIfContextIsIgnored() {
		StaticWebApplicationContext context = createWebApplicationContext();
		TestApplicationContextRequestMatcher<ApplicationContext> matcher = new TestApplicationContextRequestMatcher<ApplicationContext>(
				ApplicationContext.class) {

			@Override
			protected boolean ignoreApplicationContext(WebApplicationContext webApplicationContext) {
				return true;
			}

			@Override
			protected void initialized(Supplier<ApplicationContext> context) {
				throw new IllegalStateException();
			}

			@Override
			protected boolean matches(HttpServletRequest request, Supplier<ApplicationContext> context) {
				throw new IllegalStateException();
			}

		};
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		assertThat(matcher.matches(request)).isFalse();
	}

	@Test // gh-18211
	void matchesWhenConcurrentlyCalledWaitsForInitialize() {
		ConcurrentApplicationContextRequestMatcher matcher = new ConcurrentApplicationContextRequestMatcher();
		StaticWebApplicationContext context = createWebApplicationContext();
		Runnable target = () -> matcher.matches(new MockHttpServletRequest(context.getServletContext()));
		List<Thread> threads = new ArrayList<>();
		AssertingUncaughtExceptionHandler exceptionHandler = new AssertingUncaughtExceptionHandler();
		for (int i = 0; i < 2; i++) {
			Thread thread = new Thread(target);
			thread.setUncaughtExceptionHandler(exceptionHandler);
			threads.add(thread);
		}
		threads.forEach(Thread::start);
		threads.forEach(this::join);
		exceptionHandler.assertNoExceptions();
	}

	private void join(Thread thread) {
		try {
			thread.join(1000);
		}
		catch (InterruptedException ex) {
		}
	}

	private StaticWebApplicationContext createWebApplicationContext() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		MockServletContext servletContext = new MockServletContext();
		context.setServletContext(servletContext);
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		return context;
	}

	static class ExistingBean {

	}

	static class NewBean {

		private final ExistingBean bean;

		NewBean(ExistingBean bean) {
			this.bean = bean;
		}

		ExistingBean getBean() {
			return this.bean;
		}

	}

	static class TestApplicationContextRequestMatcher<C> extends ApplicationContextRequestMatcher<C> {

		private Supplier<C> providedContext;

		TestApplicationContextRequestMatcher(Class<? extends C> context) {
			super(context);
		}

		Supplier<C> callMatchesAndReturnProvidedContext(WebApplicationContext context) {
			return callMatchesAndReturnProvidedContext(new MockHttpServletRequest(context.getServletContext()));
		}

		Supplier<C> callMatchesAndReturnProvidedContext(HttpServletRequest request) {
			matches(request);
			return getProvidedContext();
		}

		@Override
		protected boolean matches(HttpServletRequest request, Supplier<C> context) {
			this.providedContext = context;
			return false;
		}

		Supplier<C> getProvidedContext() {
			return this.providedContext;
		}

	}

	static class ConcurrentApplicationContextRequestMatcher extends ApplicationContextRequestMatcher<Object> {

		ConcurrentApplicationContextRequestMatcher() {
			super(Object.class);
		}

		private final AtomicBoolean initialized = new AtomicBoolean();

		@Override
		protected void initialized(Supplier<Object> context) {
			try {
				Thread.sleep(200);
			}
			catch (InterruptedException ex) {
			}
			this.initialized.set(true);
		}

		@Override
		protected boolean matches(HttpServletRequest request, Supplier<Object> context) {
			assertThat(this.initialized.get()).isTrue();
			return true;
		}

	}

	private static class AssertingUncaughtExceptionHandler implements UncaughtExceptionHandler {

		private volatile Throwable ex;

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			this.ex = ex;
		}

		void assertNoExceptions() {
			if (this.ex != null) {
				ReflectionUtils.rethrowRuntimeException(this.ex);
			}
		}

	}

}
