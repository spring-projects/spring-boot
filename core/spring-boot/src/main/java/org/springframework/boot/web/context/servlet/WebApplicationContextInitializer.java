/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextScope;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Common initialization logic for Servlet web applications.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class WebApplicationContextInitializer {

	private static final Log logger = LogFactory.getLog(WebApplicationContextInitializer.class);

	private final ConfigurableWebApplicationContext context;

	public WebApplicationContextInitializer(ConfigurableWebApplicationContext context) {
		this.context = context;
	}

	public void initialize(ServletContext servletContext) throws ServletException {
		prepareWebApplicationContext(servletContext);
		registerApplicationScope(servletContext, this.context.getBeanFactory());
		WebApplicationContextUtils.registerEnvironmentBeans(this.context.getBeanFactory(), servletContext);
		for (ServletContextInitializer initializerBean : new ServletContextInitializerBeans(
				this.context.getBeanFactory())) {
			initializerBean.onStartup(servletContext);
		}
	}

	private void prepareWebApplicationContext(ServletContext servletContext) {
		Object rootContext = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (rootContext != null) {
			if (rootContext == this) {
				throw new IllegalStateException(
						"Cannot initialize context because there is already a root application context present - "
								+ "check whether you have multiple ServletContextInitializers!");
			}
			return;
		}
		try {
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name ["
						+ WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
			}
			this.context.setServletContext(servletContext);
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - this.context.getStartupDate();
				logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
			}
		}
		catch (RuntimeException | Error ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
	}

	private void registerApplicationScope(ServletContext servletContext, ConfigurableListableBeanFactory beanFactory) {
		ServletContextScope appScope = new ServletContextScope(servletContext);
		beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
		// Register as ServletContext attribute, for ContextCleanupListener to detect it.
		servletContext.setAttribute(ServletContextScope.class.getName(), appScope);
	}

}
