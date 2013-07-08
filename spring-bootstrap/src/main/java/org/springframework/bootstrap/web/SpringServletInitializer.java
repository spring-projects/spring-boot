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

package org.springframework.bootstrap.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

/**
 * A handy opinionated {@link WebApplicationInitializer} for applications that only have
 * one Spring servlet, and no more than a single filter (which itself is only enabled when
 * Spring Security is detected). If your application is more complicated consider using
 * one of the other WebApplicationInitializers.
 * 
 * <p/>
 * Note that a WebApplicationInitializer is only needed if you are building a war file and
 * deploying it. If you prefer to run an embedded container (we do) then you won't need
 * this at all.
 * 
 * @author Dave Syer
 */
public abstract class SpringServletInitializer implements WebApplicationInitializer {

	private final Log logger = LogFactory.getLog(getClass());

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		WebApplicationContext rootAppContext = this
				.createRootApplicationContext(servletContext);
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
		ApplicationContext parent = null;
		Object object = servletContext
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (object instanceof ApplicationContext) {
			this.logger.info("Root context already created (using as parent).");
			parent = (ApplicationContext) object;
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
		}
		SpringApplication application = new SpringApplication(
				(Object[]) getConfigClasses());
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.setParent(parent);
		context.setServletContext(servletContext);
		application.setApplicationContext(context);
		return (WebApplicationContext) application.run();
	}

	protected abstract Class<?>[] getConfigClasses();

}
