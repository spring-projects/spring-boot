/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.load.it.war.embedded;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;

/**
 * Jetty {@link Configuration} that allows Spring {@link WebApplicationInitializer} to be
 * started. This is required because Jetty annotation scanning does not work with packaged
 * WARs.
 *
 * @author Phillip Webb
 */
public class WebApplicationInitializersConfiguration extends AbstractConfiguration {

	private Class<?>[] webApplicationInitializers;

	public WebApplicationInitializersConfiguration(Class<?> webApplicationInitializer,
			Class<?>... webApplicationInitializers) {
		this.webApplicationInitializers = new Class<?>[webApplicationInitializers.length + 1];
		this.webApplicationInitializers[0] = webApplicationInitializer;
		System.arraycopy(webApplicationInitializers, 0, this.webApplicationInitializers,
				1, webApplicationInitializers.length);
		for (Class<?> i : webApplicationInitializers) {
			Assert.notNull(i, "WebApplicationInitializer must not be null");
			Assert.isAssignable(WebApplicationInitializer.class, i);
		}
	}

	@Override
	public void configure(WebAppContext context) throws Exception {
		context.getServletContext().addListener(new ServletContextListener() {

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				try {
					for (Class<?> webApplicationInitializer : webApplicationInitializers) {
						WebApplicationInitializer initializer = (WebApplicationInitializer) webApplicationInitializer.newInstance();
						initializer.onStartup(sce.getServletContext());
					}
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

			@Override
			public void contextDestroyed(ServletContextEvent sce) {
			}
		});
	}

}
