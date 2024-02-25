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

package org.springframework.boot.web.servlet.support;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

/**
 * An opinionated {@link WebApplicationInitializer} to run a {@link SpringApplication}
 * from a traditional WAR deployment. Binds {@link Servlet}, {@link Filter} and
 * {@link ServletContextInitializer} beans from the application context to the server.
 * <p>
 * To configure the application either override the
 * {@link #configure(SpringApplicationBuilder)} method (calling
 * {@link SpringApplicationBuilder#sources(Class...)}) or make the initializer itself a
 * {@code @Configuration}. If you are using {@link SpringBootServletInitializer} in
 * combination with other {@link WebApplicationInitializer WebApplicationInitializers} you
 * might also want to add an {@code @Ordered} annotation to configure a specific startup
 * order.
 * <p>
 * Note that a WebApplicationInitializer is only needed if you are building a war file and
 * deploying it. If you prefer to run an embedded web server then you won't need this at
 * all.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see #configure(SpringApplicationBuilder)
 */
public abstract class SpringBootServletInitializer implements WebApplicationInitializer {

	protected Log logger; // Don't initialize early

	private boolean registerErrorPageFilter = true;

	/**
	 * Set if the {@link ErrorPageFilter} should be registered. Set to {@code false} if
	 * error page mappings should be handled through the server and not Spring Boot.
	 * @param registerErrorPageFilter if the {@link ErrorPageFilter} should be registered.
	 */
	protected final void setRegisterErrorPageFilter(boolean registerErrorPageFilter) {
		this.registerErrorPageFilter = registerErrorPageFilter;
	}

	/**
	 * Called when the application starts up.
	 * @param servletContext the ServletContext object
	 * @throws ServletException if an error occurs during startup
	 */
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		servletContext.setAttribute(LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY, false);
		// Logger initialization is deferred in case an ordered
		// LogServletContextInitializer is being used
		this.logger = LogFactory.getLog(getClass());
		WebApplicationContext rootApplicationContext = createRootApplicationContext(servletContext);
		if (rootApplicationContext != null) {
			servletContext.addListener(new SpringBootContextLoaderListener(rootApplicationContext, servletContext));
		}
		else {
			this.logger.debug("No ContextLoaderListener registered, as createRootApplicationContext() did not "
					+ "return an application context");
		}
	}

	/**
	 * Deregisters the JDBC drivers that were registered by the application represented by
	 * the given {@code servletContext}. The default implementation
	 * {@link DriverManager#deregisterDriver(Driver) deregisters} every {@link Driver}
	 * that was loaded by the {@link ServletContext#getClassLoader web application's class
	 * loader}.
	 * @param servletContext the web application's servlet context
	 * @since 2.3.0
	 */
	protected void deregisterJdbcDrivers(ServletContext servletContext) {
		for (Driver driver : Collections.list(DriverManager.getDrivers())) {
			if (driver.getClass().getClassLoader() == servletContext.getClassLoader()) {
				try {
					DriverManager.deregisterDriver(driver);
				}
				catch (SQLException ex) {
					// Continue
				}
			}
		}
	}

	/**
	 * Creates the root application context for the Spring Boot application.
	 * @param servletContext the servlet context
	 * @return the root application context
	 */
	protected WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
		SpringApplicationBuilder builder = createSpringApplicationBuilder();
		builder.main(getClass());
		ApplicationContext parent = getExistingRootWebApplicationContext(servletContext);
		if (parent != null) {
			this.logger.info("Root context already created (using as parent).");
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
			builder.initializers(new ParentContextApplicationContextInitializer(parent));
		}
		builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
		builder.contextFactory((webApplicationType) -> new AnnotationConfigServletWebServerApplicationContext());
		builder = configure(builder);
		builder.listeners(new WebEnvironmentPropertySourceInitializer(servletContext));
		SpringApplication application = builder.build();
		if (application.getAllSources().isEmpty()
				&& MergedAnnotations.from(getClass(), SearchStrategy.TYPE_HIERARCHY).isPresent(Configuration.class)) {
			application.addPrimarySources(Collections.singleton(getClass()));
		}
		Assert.state(!application.getAllSources().isEmpty(),
				"No SpringApplication sources have been defined. Either override the "
						+ "configure method or add an @Configuration annotation");
		// Ensure error pages are registered
		if (this.registerErrorPageFilter) {
			application.addPrimarySources(Collections.singleton(ErrorPageFilterConfiguration.class));
		}
		application.setRegisterShutdownHook(false);
		return run(application);
	}

	/**
	 * Returns the {@code SpringApplicationBuilder} that is used to configure and create
	 * the {@link SpringApplication}. The default implementation returns a new
	 * {@code SpringApplicationBuilder} in its default state.
	 * @return the {@code SpringApplicationBuilder}.
	 * @since 1.3.0
	 */
	protected SpringApplicationBuilder createSpringApplicationBuilder() {
		return new SpringApplicationBuilder();
	}

	/**
	 * Called to run a fully configured {@link SpringApplication}.
	 * @param application the application to run
	 * @return the {@link WebApplicationContext}
	 */
	protected WebApplicationContext run(SpringApplication application) {
		return (WebApplicationContext) application.run();
	}

	/**
	 * Retrieves the existing root web application context from the given servlet context.
	 * @param servletContext the servlet context from which to retrieve the root web
	 * application context
	 * @return the existing root web application context if found, null otherwise
	 */
	private ApplicationContext getExistingRootWebApplicationContext(ServletContext servletContext) {
		Object context = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (context instanceof ApplicationContext applicationContext) {
			return applicationContext;
		}
		return null;
	}

	/**
	 * Configure the application. Normally all you would need to do is to add sources
	 * (e.g. config classes) because other settings have sensible defaults. You might
	 * choose (for instance) to add default command line arguments, or set an active
	 * Spring profile.
	 * @param builder a builder for the application context
	 * @return the application builder
	 * @see SpringApplicationBuilder
	 */
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder;
	}

	/**
	 * {@link ApplicationListener} to trigger
	 * {@link ConfigurableWebEnvironment#initPropertySources(ServletContext, jakarta.servlet.ServletConfig)}.
	 */
	private static final class WebEnvironmentPropertySourceInitializer
			implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

		private final ServletContext servletContext;

		/**
		 * Constructs a new WebEnvironmentPropertySourceInitializer with the specified
		 * ServletContext.
		 * @param servletContext the ServletContext to be used by the initializer
		 */
		private WebEnvironmentPropertySourceInitializer(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		/**
		 * This method is called when the application environment is prepared. It
		 * initializes the property sources for the configurable web environment.
		 * @param event The ApplicationEnvironmentPreparedEvent object representing the
		 * event.
		 */
		@Override
		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
			ConfigurableEnvironment environment = event.getEnvironment();
			if (environment instanceof ConfigurableWebEnvironment configurableWebEnvironment) {
				configurableWebEnvironment.initPropertySources(this.servletContext, null);
			}
		}

		/**
		 * Returns the order of this initializer. The order is set to the highest
		 * precedence.
		 * @return the order of this initializer
		 */
		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

	}

	/**
	 * {@link ContextLoaderListener} for the initialized context.
	 */
	private class SpringBootContextLoaderListener extends ContextLoaderListener {

		private final ServletContext servletContext;

		/**
		 * Constructs a new SpringBootContextLoaderListener with the specified
		 * WebApplicationContext and ServletContext.
		 * @param applicationContext the WebApplicationContext to be used by this listener
		 * @param servletContext the ServletContext to be used by this listener
		 */
		SpringBootContextLoaderListener(WebApplicationContext applicationContext, ServletContext servletContext) {
			super(applicationContext);
			this.servletContext = servletContext;
		}

		/**
		 * Called when the ServletContext is initialized. This method is a no-op because
		 * the application context is already initialized.
		 * @param event the ServletContextEvent containing the ServletContext that is
		 * being initialized
		 */
		@Override
		public void contextInitialized(ServletContextEvent event) {
			// no-op because the application context is already initialized
		}

		/**
		 * Called when the ServletContext is about to be destroyed. This method is
		 * responsible for deregistering JDBC drivers to prevent memory leaks.
		 * @param event the ServletContextEvent representing the event that occurred
		 * @throws IllegalArgumentException if the event is null
		 */
		@Override
		public void contextDestroyed(ServletContextEvent event) {
			try {
				super.contextDestroyed(event);
			}
			finally {
				// Use original context so that the classloader can be accessed
				deregisterJdbcDrivers(this.servletContext);
			}
		}

	}

}
