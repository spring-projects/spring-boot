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

package org.springframework.boot.test.context.assertj;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.core.style.ToStringCreator;
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

	/**
	 * Constructs a new AssertProviderApplicationContextInvocationHandler with the
	 * specified applicationContextType and contextSupplier.
	 * @param applicationContextType the type of the application context
	 * @param contextSupplier the supplier for the application context
	 */
	AssertProviderApplicationContextInvocationHandler(Class<?> applicationContextType, Supplier<?> contextSupplier) {
		this.applicationContextType = applicationContextType;
		Object contextOrStartupFailure = getContextOrStartupFailure(contextSupplier);
		if (contextOrStartupFailure instanceof RuntimeException runtimeException) {
			this.applicationContext = null;
			this.startupFailure = runtimeException;
		}
		else {
			this.applicationContext = (ApplicationContext) contextOrStartupFailure;
			this.startupFailure = null;
		}
	}

	/**
	 * Retrieves the context or startup failure using the provided context supplier.
	 * @param contextSupplier the supplier used to retrieve the context
	 * @return the context if successfully retrieved, or the startup failure if an
	 * exception occurred
	 */
	private Object getContextOrStartupFailure(Supplier<?> contextSupplier) {
		try {
			return contextSupplier.get();
		}
		catch (RuntimeException ex) {
			return ex;
		}
	}

	/**
	 * Invokes the specified method on the proxy object.
	 * @param proxy the proxy object
	 * @param method the method to be invoked
	 * @param args the arguments to be passed to the method
	 * @return the result of the method invocation
	 * @throws Throwable if an exception occurs during the method invocation
	 */
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

	/**
	 * Checks if the given method is the toString() method.
	 * @param method the method to check
	 * @return true if the method is the toString() method and has no parameters, false
	 * otherwise
	 */
	private boolean isToString(Method method) {
		return ("toString".equals(method.getName()) && method.getParameterCount() == 0);
	}

	/**
	 * Returns a string representation of the object. If the startupFailure is not null,
	 * it returns a string indicating that the application context is unstarted, along
	 * with the type of the startup failure. Otherwise, it creates a ToStringCreator
	 * object and appends the id, applicationName, and beanDefinitionCount of the
	 * applicationContext to it. Finally, it returns a string indicating that the
	 * application is started, along with the string representation of the ToStringCreator
	 * object.
	 * @return a string representation of the object
	 */
	@Override
	public String toString() {
		if (this.startupFailure != null) {
			return "Unstarted application context " + this.applicationContextType.getName() + "[startupFailure="
					+ this.startupFailure.getClass().getName() + "]";
		}
		ToStringCreator builder = new ToStringCreator(this.applicationContext)
			.append("id", this.applicationContext.getId())
			.append("applicationName", this.applicationContext.getApplicationName())
			.append("beanDefinitionCount", this.applicationContext.getBeanDefinitionCount());
		return "Started application " + builder;
	}

	/**
	 * Checks if the given method is the "getSourceApplicationContext" method.
	 * @param method the method to check
	 * @return true if the method is "getSourceApplicationContext" and has either no
	 * parameters or a single parameter of type Class, false otherwise
	 */
	private boolean isGetSourceContext(Method method) {
		return "getSourceApplicationContext".equals(method.getName()) && ((method.getParameterCount() == 0)
				|| Arrays.equals(new Class<?>[] { Class.class }, method.getParameterTypes()));
	}

	/**
	 * Retrieves the source context from the provided arguments.
	 * @param args the arguments passed to the method
	 * @return the source ApplicationContext context
	 * @throws IllegalArgumentException if the first argument is not an instance of Class
	 */
	private Object getSourceContext(Object[] args) {
		ApplicationContext context = getStartedApplicationContext();
		if (!ObjectUtils.isEmpty(args)) {
			Assert.isInstanceOf((Class<?>) args[0], context);
		}
		return context;
	}

	/**
	 * Checks if the given method is the "getStartupFailure" method with no parameters.
	 * @param method the method to check
	 * @return true if the method is "getStartupFailure" with no parameters, false
	 * otherwise
	 */
	private boolean isGetStartupFailure(Method method) {
		return ("getStartupFailure".equals(method.getName()) && method.getParameterCount() == 0);
	}

	/**
	 * Returns the startup failure object.
	 * @return the startup failure object
	 */
	private Object getStartupFailure() {
		return this.startupFailure;
	}

	/**
	 * Checks if the given method is the "assertThat" method.
	 * @param method the method to check
	 * @return true if the method is the "assertThat" method with no parameters, false
	 * otherwise
	 */
	private boolean isAssertThat(Method method) {
		return ("assertThat".equals(method.getName()) && method.getParameterCount() == 0);
	}

	/**
	 * Returns an instance of ApplicationContextAssert that wraps the given proxy object.
	 * @param proxy the proxy object to be wrapped
	 * @return an instance of ApplicationContextAssert
	 */
	private Object getAssertThat(Object proxy) {
		return new ApplicationContextAssert<>((ApplicationContext) proxy, this.startupFailure);
	}

	/**
	 * Checks if the given method is a close method.
	 * @param method the method to check
	 * @return true if the method is a close method, false otherwise
	 */
	private boolean isCloseMethod(Method method) {
		return ("close".equals(method.getName()) && method.getParameterCount() == 0);
	}

	/**
	 * Invokes the close method on the applicationContext if it is an instance of
	 * Closeable.
	 * @return null
	 * @throws IOException if an I/O error occurs while closing the applicationContext
	 */
	private Object invokeClose() throws IOException {
		if (this.applicationContext instanceof Closeable closeable) {
			closeable.close();
		}
		return null;
	}

	/**
	 * Invokes a method on the application context object.
	 * @param method the method to be invoked
	 * @param args the arguments to be passed to the method
	 * @return the result of the method invocation
	 * @throws Throwable if an exception occurs during the method invocation
	 */
	private Object invokeApplicationContextMethod(Method method, Object[] args) throws Throwable {
		try {
			return method.invoke(getStartedApplicationContext(), args);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	/**
	 * Returns the application context that has been started.
	 * @return the started application context
	 * @throws IllegalStateException if the application context failed to start
	 */
	private ApplicationContext getStartedApplicationContext() {
		if (this.startupFailure != null) {
			throw new IllegalStateException(this + " failed to start", this.startupFailure);
		}
		return this.applicationContext;
	}

}
