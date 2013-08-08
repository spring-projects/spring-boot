/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

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
 * <li>{@link HttpSessionListener}</li>
 * <li>{@link ServletContextListener}</li>
 * </ul>
 * @author Dave Syer
 * @author Phillip Webb
 * @param <T> the type of listener
 */
public class ServletListenerRegistrationBean<T extends EventListener> extends
		RegistrationBean {

	private static final Set<Class<?>> SUPPORTED_TYPES;
	static {
		Set<Class<?>> types = new HashSet<Class<?>>();
		types.add(ServletContextAttributeListener.class);
		types.add(ServletRequestListener.class);
		types.add(ServletRequestAttributeListener.class);
		types.add(HttpSessionAttributeListener.class);
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
		Assert.isTrue(isSupportedType(listener), "EventLisener is not a supported type");
		this.listener = listener;
	}

	/**
	 * Set the listener that will be registered.
	 * @param listener the listener to register
	 */
	public void setListener(T listener) {
		Assert.notNull(listener, "Listener must not be null");
		Assert.isTrue(isSupportedType(listener), "EventLisener is not a supported type");
		this.listener = listener;
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		servletContext.addListener(this.listener);
	}

	public T getListener() {
		return this.listener;
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

}
