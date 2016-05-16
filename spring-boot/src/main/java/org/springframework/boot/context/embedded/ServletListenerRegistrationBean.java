/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.EventListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.springframework.boot.web.servlet.ServletContextInitializer;

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
 *
 * @param <T> the type of listener
 * @author Dave Syer
 * @author Phillip Webb
 * @deprecated as of 1.4 in favor of
 * org.springframework.boot.web.ServletListenerRegistrationBean
 */
@Deprecated
public class ServletListenerRegistrationBean<T extends EventListener>
		extends org.springframework.boot.web.servlet.ServletListenerRegistrationBean<T>
		implements org.springframework.boot.context.embedded.ServletContextInitializer {

	public ServletListenerRegistrationBean() {
		super();
	}

	public ServletListenerRegistrationBean(T listener) {
		super(listener);
	}

}
