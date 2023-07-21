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

package org.springframework.boot.web.servlet;

import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link ServletContextInitializer} to register {@link EventListener}s in a Servlet
 * 3.0+ container. Similar to the {@link ServletContext#addListener(EventListener)
 * registration} features provided by {@link ServletContext} but with a Spring Bean
 * friendly design.
 *
 * This bean can be used to register the following types of listener:
 * <ul>
 * <li>{@link ServletContextAttributeListener}</li>
 * <li>{@link ServletRequestListener}</li>
 * <li>{@link ServletRequestAttributeListener}</li>
 * <li>{@link HttpSessionAttributeListener}</li>
 * <li>{@link HttpSessionIdListener}</li>
 * <li>{@link HttpSessionListener}</li>
 * <li>{@link ServletContextListener}</li>
 * </ul>
 *
 * @param <T> the type of listener
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.4.0
 */
public class ServletListenerRegistrationBean<T extends EventListener> extends RegistrationBean {

	private static final Set<Class<?>> SUPPORTED_TYPES;

	static {
		Set<Class<?>> types = new HashSet<>();
		types.add(ServletContextAttributeListener.class);
		types.add(ServletRequestListener.class);
		types.add(ServletRequestAttributeListener.class);
		types.add(HttpSessionAttributeListener.class);
		types.add(HttpSessionIdListener.class);
		types.add(HttpSessionListener.class);
		types.add(ServletContextListener.class);
		SUPPORTED_TYPES = Collections.unmodifiableSet(types);
	}

	private T listener;

	/**
	 * Create a new {@link ServletListenerRegistrationBean} instance.
	 */
	public ServletListenerRegistrationBean() {
	}

	/**
	 * Create a new {@link ServletListenerRegistrationBean} instance.
	 * @param listener the listener to register
	 */
	public ServletListenerRegistrationBean(T listener) {
		Assert.notNull(listener, "Listener must not be null");
		Assert.isTrue(isSupportedType(listener), "Listener is not of a supported type");
		this.listener = listener;
	}

	/**
	 * Set the listener that will be registered.
	 * @param listener the listener to register
	 */
	public void setListener(T listener) {
		Assert.notNull(listener, "Listener must not be null");
		Assert.isTrue(isSupportedType(listener), "Listener is not of a supported type");
		this.listener = listener;
	}

	/**
	 * Return the listener to be registered.
	 * @return the listener to be registered
	 */
	public T getListener() {
		return this.listener;
	}

	@Override
	protected String getDescription() {
		Assert.notNull(this.listener, "Listener must not be null");
		return "listener " + this.listener;
	}

	@Override
	protected void register(String description, ServletContext servletContext) {
		try {
			servletContext.addListener(this.listener);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Failed to add listener '" + this.listener + "' to servlet context", ex);
		}
	}

	/**
	 * Returns {@code true} if the specified listener is one of the supported types.
	 * @param listener the listener to test
	 * @return if the listener is of a supported type
	 */
	public static boolean isSupportedType(EventListener listener) {
		for (Class<?> type : SUPPORTED_TYPES) {
			if (ClassUtils.isAssignableValue(type, listener)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the supported types for this registration.
	 * @return the supported types
	 */
	public static Set<Class<?>> getSupportedTypes() {
		return SUPPORTED_TYPES;
	}

}
