/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.zero.context.embedded.jetty;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.zero.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.zero.context.embedded.EmbeddedServletContainer;
import org.springframework.zero.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.zero.context.embedded.ErrorPage;
import org.springframework.zero.context.embedded.ServletContextInitializer;

/**
 * {@link EmbeddedServletContainerFactory} that can be used to create
 * {@link JettyEmbeddedServletContainer}s. Can be initialized using Spring's
 * {@link ServletContextInitializer}s or Jetty {@link Configuration}s.
 * 
 * <p>
 * Unless explicitly configured otherwise this factory will created containers that
 * listens for HTTP requests on port 8080.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see #setPort(int)
 * @see #setConfigurations(Collection)
 * @see JettyEmbeddedServletContainer
 */
public class JettyEmbeddedServletContainerFactory extends
		AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {

	private List<Configuration> configurations = new ArrayList<Configuration>();

	private ResourceLoader resourceLoader;

	private WebAppContext context = new WebAppContext();

	/**
	 * Create a new {@link JettyEmbeddedServletContainerFactory} instance.
	 */
	public JettyEmbeddedServletContainerFactory() {
		super();
	}

	/**
	 * Create a new {@link JettyEmbeddedServletContainerFactory} that listens for requests
	 * using the specified port.
	 * @param port the port to listen on
	 */
	public JettyEmbeddedServletContainerFactory(int port) {
		super(port);
	}

	/**
	 * Create a new {@link JettyEmbeddedServletContainerFactory} with the specified
	 * context path and port.
	 * @param contextPath root the context path
	 * @param port the port to listen on
	 */
	public JettyEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
	}

	@Override
	public EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers) {
		if (getPort() == 0) {
			return EmbeddedServletContainer.NONE;
		}
		Server server = new Server(new InetSocketAddress(getAddress(), getPort()));

		if (this.resourceLoader != null) {
			this.context.setClassLoader(this.resourceLoader.getClassLoader());
		}
		String contextPath = getContextPath();
		this.context.setContextPath(StringUtils.hasLength(contextPath) ? contextPath
				: "/");
		configureDocumentRoot(this.context);
		if (isRegisterDefaultServlet()) {
			addDefaultServlet(this.context);
		}
		if (isRegisterJspServlet()
				&& ClassUtils.isPresent(getJspServletClassName(), getClass()
						.getClassLoader())) {
			addJspServlet(this.context);
		}

		ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
		Configuration[] configurations = getWebAppContextConfigurations(this.context,
				initializersToUse);
		this.context.setConfigurations(configurations);
		postProcessWebAppContext(this.context);

		server.setHandler(this.context);
		return getJettyEmbeddedServletContainer(server);
	}

	private void configureDocumentRoot(WebAppContext handler) {
		File root = getValidDocumentRoot();
		if (root != null) {
			try {
				handler.setBaseResource(Resource.newResource(root));
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	private void addDefaultServlet(WebAppContext context) {
		ServletHolder holder = new ServletHolder();
		holder.setName("default");
		holder.setClassName("org.eclipse.jetty.servlet.DefaultServlet");
		holder.setInitParameter("dirAllowed", "false");
		holder.setInitOrder(1);
		context.getServletHandler().addServletWithMapping(holder, "/");
		context.getServletHandler().getServletMapping("/").setDefault(true);
	}

	private void addJspServlet(WebAppContext context) {
		ServletHolder holder = new ServletHolder();
		holder.setName("jsp");
		holder.setClassName(getJspServletClassName());
		holder.setInitParameter("fork", "false");
		holder.setInitOrder(3);
		context.getServletHandler().addServlet(holder);
		ServletMapping mapping = new ServletMapping();
		mapping.setServletName("jsp");
		mapping.setPathSpecs(new String[] { "*.jsp", "*.jspx" });
		context.getServletHandler().addServletMapping(mapping);
	}

	/**
	 * Return the Jetty {@link Configuration}s that should be applied to the server.
	 * @param webAppContext the Jetty {@link WebAppContext}
	 * @param initializers the {@link ServletContextInitializer}s to apply
	 * @return configurations to apply
	 */
	protected Configuration[] getWebAppContextConfigurations(WebAppContext webAppContext,
			ServletContextInitializer... initializers) {
		List<Configuration> configurations = new ArrayList<Configuration>();
		configurations.add(getServletContextInitializerConfiguration(webAppContext,
				initializers));
		configurations.addAll(getConfigurations());
		configurations.add(getErrorPageConfiguration());
		return configurations.toArray(new Configuration[configurations.size()]);
	}

	/**
	 * Create a configuration object that adds error handlers
	 * 
	 * @return a configuration object for adding error pages
	 */
	private Configuration getErrorPageConfiguration() {
		return new AbstractConfiguration() {
			@Override
			public void configure(WebAppContext context) throws Exception {
				ErrorHandler errorHandler = context.getErrorHandler();
				addJettyErrorPages(errorHandler, getErrorPages());
			}
		};
	}

	/**
	 * Return a Jetty {@link Configuration} that will invoke the specified
	 * {@link ServletContextInitializer}s. By default this method will return a
	 * {@link ServletContextInitializerConfiguration}.
	 * @param webAppContext the Jetty {@link WebAppContext}
	 * @param initializers the {@link ServletContextInitializer}s to apply
	 * @return the {@link Configuration} instance
	 */
	protected Configuration getServletContextInitializerConfiguration(
			WebAppContext webAppContext, ServletContextInitializer... initializers) {
		return new ServletContextInitializerConfiguration(webAppContext, initializers);
	}

	/**
	 * Post process the Jetty {@link WebAppContext} before it used with the Jetty Server.
	 * Subclasses can override this method to apply additional processing to the
	 * {@link WebAppContext}.
	 * @param webAppContext the Jetty {@link WebAppContext}
	 */
	protected void postProcessWebAppContext(WebAppContext webAppContext) {
	}

	/**
	 * Factory method called to create the {@link JettyEmbeddedServletContainer}.
	 * Subclasses can override this method to return a different
	 * {@link JettyEmbeddedServletContainer} or apply additional processing to the Jetty
	 * server.
	 * @param server the Jetty server.
	 * @return a new {@link JettyEmbeddedServletContainer} instance
	 */
	protected JettyEmbeddedServletContainer getJettyEmbeddedServletContainer(Server server) {
		return new JettyEmbeddedServletContainer(server);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Sets Jetty {@link Configuration}s that will be applied to the {@link WebAppContext}
	 * before the server is created. Calling this method will replace any existing
	 * configurations.
	 * @param configurations the Jetty configurations to apply
	 */
	public void setConfigurations(Collection<? extends Configuration> configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		this.configurations = new ArrayList<Configuration>(configurations);
	}

	/**
	 * Returns a mutable collection of Jetty {@link Configuration}s that will be applied
	 * to the {@link WebAppContext} before the server is created.
	 * @return the Jetty {@link Configuration}s
	 */
	public Collection<Configuration> getConfigurations() {
		return this.configurations;
	}

	/**
	 * Add {@link Configuration}s that will be applied to the {@link WebAppContext} before
	 * the server is started.
	 * 
	 * @param configurations the configurations to add
	 */
	public void addConfigurations(Configuration... configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		this.configurations.addAll(Arrays.asList(configurations));
	}

	private void addJettyErrorPages(ErrorHandler errorHandler,
			Collection<ErrorPage> errorPages) {
		if (errorHandler instanceof ErrorPageErrorHandler) {
			ErrorPageErrorHandler handler = (ErrorPageErrorHandler) errorHandler;
			for (ErrorPage errorPage : errorPages) {
				if (errorPage.isGlobal()) {
					handler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE,
							errorPage.getPath());
				} else {
					if (errorPage.getExceptionName() != null) {
						handler.addErrorPage(errorPage.getExceptionName(),
								errorPage.getPath());
					} else {
						handler.addErrorPage(errorPage.getStatusCode(),
								errorPage.getPath());
					}
				}
			}
		}
	}
}
