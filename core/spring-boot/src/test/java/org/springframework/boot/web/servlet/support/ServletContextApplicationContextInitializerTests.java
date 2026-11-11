/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.servlet.support;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ServletContextApplicationContextInitializer}.
 *
 * @author Andy Wilkinson
 */
class ServletContextApplicationContextInitializerTests {

	private final ServletContext servletContext = mock(ServletContext.class);

	private final ConfigurableWebApplicationContext applicationContext = mock(ConfigurableWebApplicationContext.class);

	@Test
	void servletContextIsSetOnTheApplicationContext() {
		new ServletContextApplicationContextInitializer(this.servletContext).initialize(this.applicationContext);
		then(this.applicationContext).should().setServletContext(this.servletContext);
	}

	@Test
	void applicationContextIsNotStoredInServletContextByDefault() {
		new ServletContextApplicationContextInitializer(this.servletContext).initialize(this.applicationContext);
		then(this.servletContext).should(never())
			.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.applicationContext);
	}

	@Test
	void applicationContextCanBeStoredInServletContext() {
		new ServletContextApplicationContextInitializer(this.servletContext, true).initialize(this.applicationContext);
		then(this.servletContext).should()
			.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.applicationContext);
	}

}
