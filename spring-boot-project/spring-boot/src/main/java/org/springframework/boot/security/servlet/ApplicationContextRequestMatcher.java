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

package org.springframework.boot.security.servlet;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * {@link ApplicationContext} backed {@link RequestMatcher}. Can work directly with the
 * {@link ApplicationContext}, obtain an existing bean or
 * {@link AutowireCapableBeanFactory#createBean(Class, int, boolean) create a new bean}
 * that is autowired in the usual way.
 *
 * @param <C> the type of the context that the match method actually needs to use. Can be
 * an {@link ApplicationContext} or a class of an {@link ApplicationContext#getBean(Class)
 * existing bean}.
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class ApplicationContextRequestMatcher<C> implements RequestMatcher {

	private final Class<? extends C> contextClass;

	private volatile boolean initialized;

	private final Object initializeLock = new Object();

	public ApplicationContextRequestMatcher(Class<? extends C> contextClass) {
		Assert.notNull(contextClass, "Context class must not be null");
		this.contextClass = contextClass;
	}

	@Override
	public final boolean matches(HttpServletRequest request) {
		WebApplicationContext webApplicationContext = WebApplicationContextUtils
				.getRequiredWebApplicationContext(request.getServletContext());
		if (ignoreApplicationContext(webApplicationContext)) {
			return false;
		}
		Supplier<C> context = () -> getContext(webApplicationContext);
		if (!this.initialized) {
			synchronized (this.initializeLock) {
				if (!this.initialized) {
					initialized(context);
					this.initialized = true;
				}
			}
		}
		return matches(request, context);
	}

	@SuppressWarnings("unchecked")
	private C getContext(WebApplicationContext webApplicationContext) {
		if (this.contextClass.isInstance(webApplicationContext)) {
			return (C) webApplicationContext;
		}
		return webApplicationContext.getBean(this.contextClass);
	}

	/**
	 * Returns if the {@link WebApplicationContext} should be ignored and not used for
	 * matching. If this method returns {@code true} then the context will not be used and
	 * the {@link #matches(HttpServletRequest) matches} method will return {@code false}.
	 * @param webApplicationContext the candidate web application context
	 * @return if the application context should be ignored
	 * @since 2.1.8
	 */
	protected boolean ignoreApplicationContext(WebApplicationContext webApplicationContext) {
		return false;
	}

	/**
	 * Method that can be implemented by subclasses that wish to initialize items the
	 * first time that the matcher is called. This method will be called only once and
	 * only if {@link #ignoreApplicationContext(WebApplicationContext)} returns
	 * {@code false}. Note that the supplied context will be based on the
	 * <strong>first</strong> request sent to the matcher.
	 * @param context a supplier for the initialized context (may throw an exception)
	 * @see #ignoreApplicationContext(WebApplicationContext)
	 */
	protected void initialized(Supplier<C> context) {
	}

	/**
	 * Decides whether the rule implemented by the strategy matches the supplied request.
	 * @param request the source request
	 * @param context a supplier for the initialized context (may throw an exception)
	 * @return if the request matches
	 */
	protected abstract boolean matches(HttpServletRequest request, Supplier<C> context);

}
