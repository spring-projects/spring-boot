/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.web;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

/**
 * An opinionated {@link WebApplicationInitializer} to run a {@link SpringApplication}
 * from a traditional WAR deployment. Binds {@link Servlet}, {@link Filter} and
 * {@link ServletContextInitializer} beans from the application context to the servlet
 * container.
 * <p>
 * To configure the application either override the
 * {@link #configure(SpringApplicationBuilder)} method (calling
 * {@link SpringApplicationBuilder#sources(Object...)}) or make the initializer itself a
 * {@code @Configuration}.
 * <p>
 * Note that a WebApplicationInitializer is only needed if you are building a war file and
 * deploying it. If you prefer to run an embedded container then you won't need this at
 * all.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @see #configure(SpringApplicationBuilder)
 */
public abstract class SpringBootServletInitializer implements WebApplicationInitializer {

	protected final Log logger = LogFactory.getLog(getClass());

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		WebApplicationContext rootAppContext = createRootApplicationContext(servletContext);
		if (rootAppContext != null) {
			servletContext.addListener(new ContextLoaderListener(rootAppContext) {
				@Override
				public void contextInitialized(ServletContextEvent event) {
					// no-op because the application context is already initialized
				}
			});
		}
		else {
			this.logger.debug("No ContextLoaderListener registered, as "
					+ "createRootApplicationContext() did not "
					+ "return an application context");
		}
	}

	protected WebApplicationContext createRootApplicationContext(
			ServletContext servletContext) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder();
		ApplicationContext parent = getExistingRootWebApplicationContext(servletContext);
		if (parent != null) {
			this.logger.info("Root context already created (using as parent).");
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
			builder.initializers(new ParentContextApplicationContextInitializer(parent));
		}
		builder.initializers(new ServletContextApplicationContextInitializer(
				servletContext));
		builder.contextClass(AnnotationConfigEmbeddedWebApplicationContext.class);
		builder = configure(builder);
		SpringApplication application = builder.build();
		if (application.getSources().isEmpty()
				&& AnnotationUtils.findAnnotation(getClass(), Configuration.class) != null) {
			application.getSources().add(getClass());
		}
		Assert.state(application.getSources().size() > 0,
				"No SpringApplication sources have been defined. Either override the "
						+ "configure method or add an @Configuration annotation");
		// Ensure error pages are registered
		application.getSources().add(ErrorPageFilter.class);
		return run(application);
	}

	/**
	 * Called to run a fully configured {@link SpringApplication}.
	 * @param application the application to run
	 * @return the {@link WebApplicationContext}
	 */
	protected WebApplicationContext run(SpringApplication application) {
		return (WebApplicationContext) application.run();
	}

	private ApplicationContext getExistingRootWebApplicationContext(
			ServletContext servletContext) {
		Object context = servletContext
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (context instanceof ApplicationContext) {
			return (ApplicationContext) context;
		}
		return null;
	}

	/**
	 * Configure the application. Normally all you would need to do it add sources (e.g.
	 * config classes) because other settings have sensible defaults. You might choose
	 * (for instance) to add default command line arguments, or set an active Spring
	 * profile.
	 * @param application a builder for the application context
	 * @see SpringApplicationBuilder
	 */
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application;
	}

}
