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

package org.springframework.boot.web.servlet;

import java.util.EventListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServletListenerRegistrationBean}.
 *
 * @author Dave Syer
 */
public class ServletListenerRegistrationBeanTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private ServletContextListener listener;

	@Mock
	private ServletContext servletContext;

	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void startupWithDefaults() throws Exception {
		ServletListenerRegistrationBean<ServletContextListener> bean = new ServletListenerRegistrationBean<>(
				this.listener);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addListener(this.listener);
	}

	@Test
	public void disable() throws Exception {
		ServletListenerRegistrationBean<ServletContextListener> bean = new ServletListenerRegistrationBean<>(
				this.listener);
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		verify(this.servletContext, times(0))
				.addListener(any(ServletContextListener.class));
	}

	@Test
	public void cannotRegisterUnsupportedType() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Listener is not of a supported type");
		new ServletListenerRegistrationBean<EventListener>(new EventListener() {
		});
	}

}
