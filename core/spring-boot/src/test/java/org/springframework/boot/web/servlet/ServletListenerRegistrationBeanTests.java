/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.servlet;

import java.util.EventListener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ServletListenerRegistrationBean}.
 *
 * @author Dave Syer
 */
@ExtendWith(MockitoExtension.class)
class ServletListenerRegistrationBeanTests {

	@Mock
	private ServletContextListener listener;

	@Mock
	private ServletContext servletContext;

	@Test
	void startupWithDefaults() throws Exception {
		ServletListenerRegistrationBean<ServletContextListener> bean = new ServletListenerRegistrationBean<>(
				this.listener);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addListener(this.listener);
	}

	@Test
	void disable() throws Exception {
		ServletListenerRegistrationBean<ServletContextListener> bean = new ServletListenerRegistrationBean<>(
				this.listener);
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should(never()).addListener(any(ServletContextListener.class));
	}

	@Test
	void cannotRegisterUnsupportedType() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ServletListenerRegistrationBean<>(new EventListener() {

			}))
			.withMessageContaining("'listener' is not of a supported type");
	}

}
