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

package org.springframework.boot.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.initializer.ParentContextApplicationContextInitializer;
import org.springframework.boot.context.initializer.ServletContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;
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
public abstract class SpringBootServletInitializer implements WebApplicationInitializer {

	protected final Log logger = LogFactory.getLog(getClass());

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
		SpringApplicationBuilder application = new SpringApplicationBuilder()
				.sources(getConfigClasses());
		if (parent != null) {
			application.initializers(new ParentContextApplicationContextInitializer(
					parent));
		}
		application.initializers(new ServletContextApplicationContextInitializer(
				servletContext));
		application.contextClass(AnnotationConfigEmbeddedWebApplicationContext.class);
		return (WebApplicationContext) application.run();
	}

	private Object[] getConfigClasses() {
		Class<?>[] additionalConfigClasses = getAdditionalConfigClasses();
		if (ObjectUtils.isEmpty(additionalConfigClasses)) {
			return new Object[] { getConfigClass() };
		}
		Object[] configClasses = new Object[additionalConfigClasses.length + 1];
		configClasses[0] = getConfigClass();
		System.arraycopy(additionalConfigClasses, 0, configClasses, 1,
				additionalConfigClasses.length);
		return configClasses;
	}

	/**
	 * Returns the main configuration class to load. If you need additional configuration
	 * classes you can also override {@link #getAdditionalConfigClasses()}.
	 */
	protected abstract Class<?> getConfigClass();

	/**
	 * Returns configuration classes that should be loaded in addition to the
	 * {@link #getConfigClass() main configuration class}.
	 */
	protected Class<?>[] getAdditionalConfigClasses() {
		return null;
	}

}
