/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.context.assertj;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.commons.lang3.builder.ToStringBuilder;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link InvocationHandler} used by {@link ApplicationContextAssertProvider} generated
 * proxies.
 *
 * @author Phillip Webb
 */
class AssertProviderApplicationContextInvocationHandler implements InvocationHandler {

	private final Class<?> applicationContextType;

	private final ApplicationContext applicationContext;

	private final RuntimeException startupFailure;

	AssertProviderApplicationContextInvocationHandler(Class<?> applicationContextType,
			Supplier<?> contextSupplier) {
		this.applicationContextType = applicationContextType;
		Object contextOrStartupFailure = getContextOrStartupFailure(contextSupplier);
		if (contextOrStartupFailure instanceof RuntimeException) {
			this.applicationContext = null;
			this.startupFailure = (RuntimeException) contextOrStartupFailure;
		}
		else {
			this.applicationContext = (ApplicationContext) contextOrStartupFailure;
			this.startupFailure = null;
		}
	}

	private Object getContextOrStartupFailure(Supplier<?> contextSupplier) {
		try {
			return contextSupplier.get();
		}
		catch (RuntimeException ex) {
			return ex;
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (isToString(method)) {
			return toString();
		}
		if (isGetSourceContext(method)) {
			return getSourceContext(args);
		}
		if (isGetStartupFailure(method)) {
			return getStartupFailure();
		}
		if (isAssertThat(method)) {
			return getAssertThat(proxy);
		}
		if (isCloseMethod(method)) {
			return invokeClose();
		}
		return invokeApplicationContextMethod(method, args);
	}

	private boolean isToString(Method method) {
		return ("toString".equals(method.getName()) && method.getParameterCount() == 0);
	}

	@Override
	public String toString() {
		if (this.startupFailure != null) {
			return "Unstarted application context "
					+ this.applicationContextType.getName() + "[startupFailure="
					+ this.startupFailure.getClass().getName() + "]";
		}
		ToStringBuilder builder = new ToStringBuilder(this.applicationContext)
				.append("id", this.applicationContext.getId())
				.append("applicationName", this.applicationContext.getApplicationName())
				.append("beanDefinitionCount",
						this.applicationContext.getBeanDefinitionCount());
		return "Started application " + builder;
	}

	private boolean isGetSourceContext(Method method) {
		return "getSourceApplicationContext".equals(method.getName())
				&& ((method.getParameterCount() == 0) || Arrays.equals(
						new Class<?>[] { Class.class }, method.getParameterTypes()));
	}

	private Object getSourceContext(Object[] args) {
		ApplicationContext context = getStartedApplicationContext();
		if (!ObjectUtils.isEmpty(args)) {
			Assert.isInstanceOf((Class<?>) args[0], context);
		}
		return context;
	}

	private boolean isGetStartupFailure(Method method) {
		return ("getStartupFailure".equals(method.getName())
				&& method.getParameterCount() == 0);
	}

	private Object getStartupFailure() {
		return this.startupFailure;
	}

	private boolean isAssertThat(Method method) {
		return ("assertThat".equals(method.getName()) && method.getParameterCount() == 0);
	}

	private Object getAssertThat(Object proxy) {
		return new ApplicationContextAssert<>((ApplicationContext) proxy,
				this.startupFailure);
	}

	private boolean isCloseMethod(Method method) {
		return ("close".equals(method.getName()) && method.getParameterCount() == 0);
	}

	private Object invokeClose() throws IOException {
		if (this.applicationContext instanceof Closeable) {
			((Closeable) this.applicationContext).close();
		}
		return null;
	}

	private Object invokeApplicationContextMethod(Method method, Object[] args)
			throws Throwable {
		try {
			return method.invoke(getStartedApplicationContext(), args);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	private ApplicationContext getStartedApplicationContext() {
		if (this.startupFailure != null) {
			throw new IllegalStateException(toString() + " failed to start",
					this.startupFailure);
		}
		return this.applicationContext;
	}

}
