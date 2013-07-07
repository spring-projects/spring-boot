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

package org.springframework.zero.context.embedded.tomcat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.coyote.AbstractProtocol;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.zero.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.zero.context.embedded.EmbeddedServletContainer;
import org.springframework.zero.context.embedded.EmbeddedServletContainerException;
import org.springframework.zero.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.zero.context.embedded.ErrorPage;
import org.springframework.zero.context.embedded.ServletContextInitializer;

/**
 * {@link EmbeddedServletContainerFactory} that can be used to create
 * {@link TomcatEmbeddedServletContainer}s. Can be initialized using Spring's
 * {@link ServletContextInitializer}s or Tomcat {@link LifecycleListener}s.
 * 
 * <p>
 * Unless explicitly configured otherwise this factory will created containers that
 * listens for HTTP requests on port 8080.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see #setPort(int)
 * @see #setContextLifecycleListeners(Collection)
 * @see TomcatEmbeddedServletContainer
 */
public class TomcatEmbeddedServletContainerFactory extends
		AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {

	private File baseDirectory;

	private List<Valve> contextValves = new ArrayList<Valve>();

	private List<LifecycleListener> contextLifecycleListeners = new ArrayList<LifecycleListener>();

	private ResourceLoader resourceLoader;

	private Connector connector;

	private Tomcat tomcat = new Tomcat();

	/**
	 * Create a new {@link TomcatEmbeddedServletContainerFactory} instance.
	 */
	public TomcatEmbeddedServletContainerFactory() {
		super();
	}

	/**
	 * Create a new {@link TomcatEmbeddedServletContainerFactory} that listens for
	 * requests using the specified port.
	 * @param port the port to listen on
	 */
	public TomcatEmbeddedServletContainerFactory(int port) {
		super(port);
	}

	/**
	 * Create a new {@link TomcatEmbeddedServletContainerFactory} with the specified
	 * context path and port.
	 * @param contextPath root the context path
	 * @param port the port to listen on
	 */
	public TomcatEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
	}

	@Override
	public EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers) {
		if (getPort() == 0) {
			return EmbeddedServletContainer.NONE;
		}
		File baseDir = (this.baseDirectory != null ? this.baseDirectory
				: createTempDir("tomcat"));
		this.tomcat.setBaseDir(baseDir.getAbsolutePath());
		if (this.connector != null) {
			this.connector.setPort(getPort());
			this.tomcat.getService().addConnector(this.connector);
			this.tomcat.setConnector(this.connector);
		}
		else {
			Connector connector = new Connector(
					"org.apache.coyote.http11.Http11NioProtocol");
			customizeConnector(connector);
			this.tomcat.getService().addConnector(connector);
			this.tomcat.setConnector(connector);
		}
		this.tomcat.getHost().setAutoDeploy(false);
		this.tomcat.getEngine().setBackgroundProcessorDelay(-1);

		prepareContext(this.tomcat.getHost(), initializers);
		return getTomcatEmbeddedServletContainer(this.tomcat);
	}

	protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
		File docBase = getValidDocumentRoot();
		docBase = (docBase != null ? docBase : createTempDir("tomcat-docbase"));
		Context context = new StandardContext();
		context.setName(getContextPath());
		context.setPath(getContextPath());
		context.setDocBase(docBase.getAbsolutePath());
		context.addLifecycleListener(new FixContextListener());
		context.setParentClassLoader(this.resourceLoader != null ? this.resourceLoader
				.getClassLoader() : ClassUtils.getDefaultClassLoader());
		WebappLoader loader = new WebappLoader(context.getParentClassLoader());
		loader.setLoaderClass(TomcatEmbeddedWebappClassLoader.class.getName());
		context.setLoader(loader);

		if (isRegisterDefaultServlet()) {
			addDefaultServlet(context);
		}
		if (isRegisterJspServlet()
				&& ClassUtils.isPresent(getJspServletClassName(), getClass()
						.getClassLoader())) {
			addJspServlet(context);
		}
		ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
		configureContext(context, initializersToUse);
		host.addChild(context);
		postProcessContext(context);
	}

	private void addDefaultServlet(Context context) {
		Wrapper defaultServlet = context.createWrapper();
		defaultServlet.setName("default");
		defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
		defaultServlet.addInitParameter("debug", "0");
		defaultServlet.addInitParameter("listings", "false");
		defaultServlet.setLoadOnStartup(1);
		// Otherwise the default location of a Spring DispatcherServlet cannot be set
		defaultServlet.setOverridable(true);
		context.addChild(defaultServlet);
		context.addServletMapping("/", "default");
	}

	private void addJspServlet(Context context) {
		Wrapper jspServlet = context.createWrapper();
		jspServlet.setName("jsp");
		jspServlet.setServletClass(getJspServletClassName());
		jspServlet.addInitParameter("fork", "false");
		jspServlet.setLoadOnStartup(3);
		context.addChild(jspServlet);
		context.addServletMapping("*.jsp", "jsp");
		context.addServletMapping("*.jspx", "jsp");
	}

	// Needs to be protected so it can be used by subclasses
	protected void customizeConnector(Connector connector) {
		connector.setPort(getPort());
		if (connector.getProtocolHandler() instanceof AbstractProtocol
				&& getAddress() != null) {
			((AbstractProtocol) connector.getProtocolHandler()).setAddress(getAddress());
		}
	}

	/**
	 * Configure the Tomcat {@link Context}.
	 * @param context the Tomcat context
	 * @param initializers initializers to apply
	 */
	protected void configureContext(Context context,
			ServletContextInitializer[] initializers) {
		context.addLifecycleListener(new ServletContextInitializerLifecycleListener(
				initializers));
		for (LifecycleListener lifecycleListener : this.contextLifecycleListeners) {
			context.addLifecycleListener(lifecycleListener);
		}
		for (Valve valve : this.contextValves) {
			context.getPipeline().addValve(valve);
		}
		for (ErrorPage errorPage : getErrorPages()) {
			org.apache.catalina.deploy.ErrorPage tomcatPage = new org.apache.catalina.deploy.ErrorPage();
			tomcatPage.setLocation(errorPage.getPath());
			tomcatPage.setExceptionType(errorPage.getExceptionName());
			tomcatPage.setErrorCode(errorPage.getStatusCode());
			context.addErrorPage(tomcatPage);
		}
	}

	/**
	 * Post process the Tomcat {@link Context} before it used with the Tomcat Server.
	 * Subclasses can override this method to apply additional processing to the
	 * {@link Context}.
	 * @param context the Tomcat {@link Context}
	 */
	protected void postProcessContext(Context context) {
	}

	/**
	 * Factory method called to create the {@link TomcatEmbeddedServletContainer}.
	 * Subclasses can override this method to return a different
	 * {@link TomcatEmbeddedServletContainer} or apply additional processing to the Tomcat
	 * server.
	 * @param tomcat the Tomcat server.
	 * @return a new {@link TomcatEmbeddedServletContainer} instance
	 */
	protected TomcatEmbeddedServletContainer getTomcatEmbeddedServletContainer(
			Tomcat tomcat) {
		return new TomcatEmbeddedServletContainer(tomcat);
	}

	private File createTempDir(String prefix) {
		try {
			File tempFolder = File.createTempFile(prefix + ".", "." + getPort());
			tempFolder.delete();
			tempFolder.mkdir();
			tempFolder.deleteOnExit();
			return tempFolder;
		}
		catch (IOException ex) {
			throw new EmbeddedServletContainerException(
					"Unable to create Tomcat tempdir", ex);
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Set the Tomcat base directory. If not specified a temporary directory will be used.
	 * @param baseDirectory the tomcat base directory
	 */
	public void setBaseDirectory(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	/**
	 * The tomcat connector to use (e.g. if you want to change the protocol handler).
	 * 
	 * @param connector the connector to set
	 */
	public void setConnector(Connector connector) {
		this.connector = connector;
	}

	/**
	 * Set {@link Valve}s that should be applied to the Tomcat {@link Context}. Calling
	 * this method will replace any existing listeners.
	 * @param contextValves the valves to set
	 */
	public void setContextValves(Collection<? extends Valve> contextValves) {
		Assert.notNull(contextValves, "Valves must not be null");
		this.contextValves = new ArrayList<Valve>(contextValves);
	}

	/**
	 * Returns a mutable collection of the {@link Valve}s that will be applied to the
	 * Tomcat {@link Context}.
	 * @return the contextValves the valves that will be applied
	 */
	public Collection<Valve> getValves() {
		return this.contextValves;
	}

	/**
	 * Add {@link Valve}s that should be applied to the Tomcat {@link Context}.
	 * @param contextValves the valves to add
	 */
	public void addContextValves(Valve... contextValves) {
		Assert.notNull(contextValves, "Valves must not be null");
		this.contextValves.addAll(Arrays.asList(contextValves));
	}

	/**
	 * Set {@link LifecycleListener}s that should be applied to the Tomcat {@link Context}
	 * . Calling this method will replace any existing listeners.
	 * @param contextLifecycleListeners the listeners to set
	 */
	public void setContextLifecycleListeners(
			Collection<? extends LifecycleListener> contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners,
				"ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners = new ArrayList<LifecycleListener>(
				contextLifecycleListeners);
	}

	/**
	 * Returns a mutable collection of the {@link LifecycleListener}s that will be applied
	 * to the Tomcat {@link Context} .
	 * @return the contextLifecycleListeners the listeners that will be applied
	 */
	public Collection<LifecycleListener> getContextLifecycleListeners() {
		return this.contextLifecycleListeners;
	}

	/**
	 * Add {@link LifecycleListener}s that should be applied to the Tomcat {@link Context}
	 * .
	 * @param contextLifecycleListeners the listeners to add
	 */
	public void addContextLifecycleListeners(
			LifecycleListener... contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners,
				"ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners.addAll(Arrays.asList(contextLifecycleListeners));
	}

	// FIXME JavaDoc
	// FIXME Is this still needed?
	public TomcatEmbeddedServletContainerFactory getChildContextFactory(final String name) {

		final Server server = this.tomcat.getServer();

		return new TomcatEmbeddedServletContainerFactory() {

			@Override
			public EmbeddedServletContainer getEmbeddedServletContainer(
					ServletContextInitializer... initializers) {

				if (getPort() == 0) {
					return EmbeddedServletContainer.NONE;
				}
				StandardService service = new StandardService();
				service.setName(name);
				Connector connector = new Connector(
						"org.apache.coyote.http11.Http11NioProtocol");
				customizeConnector(connector);
				service.addConnector(connector);
				StandardEngine engine = new StandardEngine();
				engine.setName(name);
				engine.setDefaultHost("localhost");
				service.setContainer(engine);
				server.addService(service);
				StandardHost host = new StandardHost();
				host.setName("localhost");
				engine.addChild(host);
				prepareContext(host, initializers);

				return new EmbeddedServletContainer() {
					@Override
					public void stop() throws EmbeddedServletContainerException {
						// noop
					}
				};

			}
		};

	}

}
