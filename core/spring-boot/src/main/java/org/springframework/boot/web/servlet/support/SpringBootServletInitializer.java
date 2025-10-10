/*
 * Copyright 2012-present the original author or authors.
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
import org.jspecify.annotations.Nullable;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.web.context.servlet.ApplicationServletEnvironment;
import org.springframework.boot.web.context.servlet.WebApplicationContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
 * @author Brian Clozel
 * @since 2.0.0
 * @see #configure(SpringApplicationBuilder)
 */
public abstract class SpringBootServletInitializer implements WebApplicationInitializer {

	private static final boolean REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.scheduler.Schedulers",
			SpringBootServletInitializer.class.getClassLoader());

	protected @Nullable Log logger; // Don't initialize early

	private boolean registerErrorPageFilter = true;

	/**
	 * Set if the {@link ErrorPageFilter} should be registered. Set to {@code false} if
	 * error page mappings should be handled through the server and not Spring Boot.
	 * @param registerErrorPageFilter if the {@link ErrorPageFilter} should be registered.
	 */
	protected final void setRegisterErrorPageFilter(boolean registerErrorPageFilter) {
		this.registerErrorPageFilter = registerErrorPageFilter;
	}

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
	 * Shuts down the reactor {@link Schedulers} that were initialized by
	 * {@code Schedulers.boundedElastic()} (or similar). The default implementation
	 * {@link Schedulers#shutdownNow()} schedulers if they were initialized on this web
	 * application's class loader.
	 * @param servletContext the web application's servlet context
	 * @since 3.4.0
	 */
	protected void shutDownSharedReactorSchedulers(ServletContext servletContext) {
		if (Schedulers.class.getClassLoader() == servletContext.getClassLoader()) {
			Schedulers.shutdownNow();
		}
	}

	protected @Nullable WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
		SpringApplicationBuilder builder = createSpringApplicationBuilder();
		builder.main(getClass());
		ApplicationContext parent = getExistingRootWebApplicationContext(servletContext);
		if (parent != null) {
			getLogger().info("Root context already created (using as parent).");
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
			builder.initializers(new ParentContextApplicationContextInitializer(parent));
		}
		builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
		builder.contextFactory(new WarDeploymentApplicationContextFactory(servletContext));
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

	private Log getLogger() {
		Assert.state(this.logger != null, "Logger not set");
		return this.logger;
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
	protected @Nullable WebApplicationContext run(SpringApplication application) {
		return (WebApplicationContext) application.run();
	}

	private @Nullable ApplicationContext getExistingRootWebApplicationContext(ServletContext servletContext) {
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

		private WebEnvironmentPropertySourceInitializer(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
			ConfigurableEnvironment environment = event.getEnvironment();
			if (environment instanceof ConfigurableWebEnvironment configurableWebEnvironment) {
				configurableWebEnvironment.initPropertySources(this.servletContext, null);
			}
		}

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

		SpringBootContextLoaderListener(WebApplicationContext applicationContext, ServletContext servletContext) {
			super(applicationContext);
			this.servletContext = servletContext;
		}

		@Override
		public void contextInitialized(ServletContextEvent event) {
			// no-op because the application context is already initialized
		}

		@Override
		public void contextDestroyed(ServletContextEvent event) {
			try {
				super.contextDestroyed(event);
			}
			finally {
				// Use original context so that the classloader can be accessed
				deregisterJdbcDrivers(this.servletContext);
				// Shut down shared reactor schedulers tied to this classloader
				if (REACTOR_PRESENT) {
					shutDownSharedReactorSchedulers(this.servletContext);
				}
			}
		}

	}

	private static final class WarDeploymentApplicationContextFactory implements ApplicationContextFactory {

		private final ServletContext servletContext;

		private WarDeploymentApplicationContextFactory(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@Override
		public ConfigurableApplicationContext create(@Nullable WebApplicationType webApplicationType) {
			return new AnnotationConfigServletWebApplicationContext() {

				@Override
				protected void onRefresh() {
					super.onRefresh();
					try {
						new WebApplicationContextInitializer(this)
							.initialize(WarDeploymentApplicationContextFactory.this.servletContext);
					}
					catch (ServletException ex) {
						throw new RuntimeException(ex);
					}
				}

			};
		}

		@Override
		public ConfigurableEnvironment createEnvironment(@Nullable WebApplicationType webApplicationType) {
			return new ApplicationServletEnvironment();
		}

	}

}
