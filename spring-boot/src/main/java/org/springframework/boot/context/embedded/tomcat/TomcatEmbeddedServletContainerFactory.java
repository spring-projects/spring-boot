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

package org.springframework.boot.context.embedded.tomcat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.coyote.AbstractProtocol;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;

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

	private static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

	private File baseDirectory;

	private List<Valve> contextValves = new ArrayList<Valve>();

	private List<LifecycleListener> contextLifecycleListeners = new ArrayList<LifecycleListener>();

	private List<TomcatContextCustomizer> tomcatContextCustomizers = new ArrayList<TomcatContextCustomizer>();

	private ResourceLoader resourceLoader;

	private String protocol = DEFAULT_PROTOCOL;

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

		Connector connector;
		Tomcat tomcat = new Tomcat();

		if (getPort() == 0) {
			return EmbeddedServletContainer.NONE;
		}
		File baseDir = (this.baseDirectory != null ? this.baseDirectory
				: createTempDir("tomcat"));
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		connector = new Connector(this.protocol);
		customizeConnector(connector);
		tomcat.getService().addConnector(connector);
		tomcat.setConnector(connector);
		tomcat.getHost().setAutoDeploy(false);
		tomcat.getEngine().setBackgroundProcessorDelay(-1);

		prepareContext(tomcat.getHost(), initializers);
		return getTomcatEmbeddedServletContainer(tomcat);
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
		loader.setDelegate(true);
		context.setLoader(loader);
		if (isRegisterDefaultServlet()) {
			addDefaultServlet(context);
		}
		if (isRegisterJspServlet()
				&& ClassUtils.isPresent(getJspServletClassName(), getClass()
						.getClassLoader())) {
			addJspServlet(context);
			context.addLifecycleListener(new StoreMergedWebXmlListener());
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
		if (connector.getProtocolHandler() instanceof AbstractProtocol) {
			if (getAddress() != null) {
				((AbstractProtocol) connector.getProtocolHandler())
						.setAddress(getAddress());
			}
		}
		// If ApplicationContext is slow to start we want Tomcat not to bind to the socket
		// prematurely...
		connector.setProperty("bindOnInit", "false");
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
			new TomcatErrorPage(errorPage).addToContext(context);
		}
		for (MimeMappings.Mapping mapping : getMimeMappings()) {
			context.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
		}
		context.setSessionTimeout(getSessionTimeout());
		for (TomcatContextCustomizer customizer : this.tomcatContextCustomizers) {
			customizer.customize(context);
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
	 * The Tomcat protocol to use when create the {@link Connector}.
	 * @see Connector#Connector(String)
	 */
	public void setProtocol(String protocol) {
		Assert.hasLength(protocol, "Protocol must not be empty");
		this.protocol = protocol;
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
	 * Add {@link LifecycleListener}s that should be added to the Tomcat {@link Context}.
	 * @param contextLifecycleListeners the listeners to add
	 */
	public void addContextLifecycleListeners(
			LifecycleListener... contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners,
				"ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners.addAll(Arrays.asList(contextLifecycleListeners));
	}

	/**
	 * Set {@link TomcatContextCustomizer}s that should be applied to the Tomcat
	 * {@link Context} . Calling this method will replace any existing customizers.
	 * @param tomcatContextCustomizers the customizers to set
	 */
	public void setTomcatContextCustomizers(
			Collection<? extends TomcatContextCustomizer> tomcatContextCustomizers) {
		Assert.notNull(this.contextLifecycleListeners,
				"TomcatContextCustomizers must not be null");
		this.tomcatContextCustomizers = new ArrayList<TomcatContextCustomizer>(
				tomcatContextCustomizers);
	}

	/**
	 * Returns a mutable collection of the {@link TomcatContextCustomizer}s that will be
	 * applied to the Tomcat {@link Context} .
	 * @return the tomcatContextCustomizers the listeners that will be applied
	 */
	public Collection<TomcatContextCustomizer> getTomcatContextCustomizers() {
		return this.tomcatContextCustomizers;
	}

	/**
	 * Add {@link TomcatContextCustomizer}s that should be added to the Tomcat
	 * {@link Context}.
	 * @param tomcatContextCustomizers the customizers to add
	 */
	public void addContextCustomizers(TomcatContextCustomizer... tomcatContextCustomizers) {
		Assert.notNull(this.tomcatContextCustomizers,
				"TomcatContextCustomizer must not be null");
		this.tomcatContextCustomizers.addAll(Arrays.asList(tomcatContextCustomizers));
	}

	private static class TomcatErrorPage {

		private String location;
		private String exceptionType;
		private int errorCode;

		private Object nativePage;

		public TomcatErrorPage(ErrorPage errorPage) {
			this.location = errorPage.getPath();
			this.exceptionType = errorPage.getExceptionName();
			this.errorCode = errorPage.getStatusCode();
			this.nativePage = createNativePage(errorPage);
		}

		private Object createNativePage(ErrorPage errorPage) {
			Object nativePage = null;
			try {
				if (ClassUtils.isPresent("org.apache.catalina.deploy.ErrorPage", null)) {
					nativePage = new org.apache.catalina.deploy.ErrorPage();
				}
				else {
					if (ClassUtils.isPresent(
							"org.apache.tomcat.util.descriptor.web.ErrorPage", null)) {
						nativePage = BeanUtils.instantiate(ClassUtils.forName(
								"org.apache.tomcat.util.descriptor.web.ErrorPage", null));
					}
				}
			}
			catch (ClassNotFoundException e) {
			}
			catch (LinkageError e) {
			}
			return nativePage;
		}

		public void addToContext(Context context) {
			Assert.state(this.nativePage != null,
					"Neither Tomcat 7 nor 8 detected so no native error page exists");
			if (ClassUtils.isPresent("org.apache.catalina.deploy.ErrorPage", null)) {
				org.apache.catalina.deploy.ErrorPage errorPage = (org.apache.catalina.deploy.ErrorPage) this.nativePage;
				errorPage.setLocation(this.location);
				errorPage.setErrorCode(this.errorCode);
				errorPage.setExceptionType(this.exceptionType);
				context.addErrorPage(errorPage);
			}
			else {
				callMethod(this.nativePage, "setLocation", this.location, String.class);
				callMethod(this.nativePage, "setErrorCode", this.errorCode, int.class);
				callMethod(this.nativePage, "setExceptionType", this.exceptionType,
						String.class);
				callMethod(context, "addErrorPage", this.nativePage,
						this.nativePage.getClass());
			}
		}

		private void callMethod(Object target, String name, Object value, Class<?> type) {
			Method method = ReflectionUtils.findMethod(target.getClass(), name, type);
			ReflectionUtils.invokeMethod(method, target, value);
		}

	}

	/**
	 * {@link LifecycleListener} that stores an empty merged web.xml. This is critical for
	 * Jasper to prevent warnings about missing web.xml files and to enable EL.
	 */
	private static class StoreMergedWebXmlListener implements LifecycleListener {

		private String MERGED_WEB_XML = org.apache.tomcat.util.scan.Constants.MERGED_WEB_XML;

		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				onStart((Context) event.getLifecycle());
			}
		}

		private void onStart(Context context) {
			ServletContext servletContext = context.getServletContext();
			if (servletContext.getAttribute(this.MERGED_WEB_XML) == null) {
				servletContext.setAttribute(this.MERGED_WEB_XML, getEmptyWebXml());
			}
		}

		private String getEmptyWebXml() {
			InputStream stream = TomcatEmbeddedServletContainerFactory.class
					.getResourceAsStream("empty-web.xml");
			Assert.state(stream != null, "Unable to read empty web.xml");
			try {
				try {
					return StreamUtils.copyToString(stream, Charset.forName("UTF-8"));
				}
				finally {
					stream.close();
				}
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
