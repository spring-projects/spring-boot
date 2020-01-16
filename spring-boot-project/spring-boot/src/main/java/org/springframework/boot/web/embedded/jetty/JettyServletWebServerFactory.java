/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.embedded.jetty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ServletWebServerFactory} that can be used to create a {@link JettyWebServer}.
 * Can be initialized using Spring's {@link ServletContextInitializer}s or Jetty
 * {@link Configuration}s.
 * <p>
 * Unless explicitly configured otherwise this factory will create servers that listen for
 * HTTP requests on port 8080.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andrey Hihlovskiy
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Venil Noronha
 * @author Henri Kerola
 * @since 2.0.0
 * @see #setPort(int)
 * @see #setConfigurations(Collection)
 * @see JettyWebServer
 */
public class JettyServletWebServerFactory extends AbstractServletWebServerFactory
		implements ConfigurableJettyWebServerFactory, ResourceLoaderAware {

	private List<Configuration> configurations = new ArrayList<>();

	private boolean useForwardHeaders;

	/**
	 * The number of acceptor threads to use.
	 */
	private int acceptors = -1;

	/**
	 * The number of selector threads to use.
	 */
	private int selectors = -1;

	private Set<JettyServerCustomizer> jettyServerCustomizers = new LinkedHashSet<>();

	private ResourceLoader resourceLoader;

	private ThreadPool threadPool;

	/**
	 * Create a new {@link JettyServletWebServerFactory} instance.
	 */
	public JettyServletWebServerFactory() {
	}

	/**
	 * Create a new {@link JettyServletWebServerFactory} that listens for requests using
	 * the specified port.
	 * @param port the port to listen on
	 */
	public JettyServletWebServerFactory(int port) {
		super(port);
	}

	/**
	 * Create a new {@link JettyServletWebServerFactory} with the specified context path
	 * and port.
	 * @param contextPath the root context path
	 * @param port the port to listen on
	 */
	public JettyServletWebServerFactory(String contextPath, int port) {
		super(contextPath, port);
	}

	@Override
	public WebServer getWebServer(ServletContextInitializer... initializers) {
		JettyEmbeddedWebAppContext context = new JettyEmbeddedWebAppContext();
		int port = Math.max(getPort(), 0);
		InetSocketAddress address = new InetSocketAddress(getAddress(), port);
		Server server = createServer(address);
		configureWebAppContext(context, initializers);
		server.setHandler(addHandlerWrappers(context));
		this.logger.info("Server initialized with port: " + port);
		if (getSsl() != null && getSsl().isEnabled()) {
			customizeSsl(server, address);
		}
		for (JettyServerCustomizer customizer : getServerCustomizers()) {
			customizer.customize(server);
		}
		if (this.useForwardHeaders) {
			new ForwardHeadersCustomizer().customize(server);
		}
		return getJettyWebServer(server);
	}

	private Server createServer(InetSocketAddress address) {
		Server server = new Server(getThreadPool());
		server.setConnectors(new Connector[] { createConnector(address, server) });
		return server;
	}

	private AbstractConnector createConnector(InetSocketAddress address, Server server) {
		ServerConnector connector = new ServerConnector(server, this.acceptors, this.selectors);
		connector.setHost(address.getHostString());
		connector.setPort(address.getPort());
		for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
			if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
				((HttpConfiguration.ConnectionFactory) connectionFactory).getHttpConfiguration()
						.setSendServerVersion(false);
			}
		}
		return connector;
	}

	private Handler addHandlerWrappers(Handler handler) {
		if (getCompression() != null && getCompression().getEnabled()) {
			handler = applyWrapper(handler, JettyHandlerWrappers.createGzipHandlerWrapper(getCompression()));
		}
		if (StringUtils.hasText(getServerHeader())) {
			handler = applyWrapper(handler, JettyHandlerWrappers.createServerHeaderHandlerWrapper(getServerHeader()));
		}
		return handler;
	}

	private Handler applyWrapper(Handler handler, HandlerWrapper wrapper) {
		wrapper.setHandler(handler);
		return wrapper;
	}

	private void customizeSsl(Server server, InetSocketAddress address) {
		new SslServerCustomizer(address, getSsl(), getSslStoreProvider(), getHttp2()).customize(server);
	}

	/**
	 * Configure the given Jetty {@link WebAppContext} for use.
	 * @param context the context to configure
	 * @param initializers the set of initializers to apply
	 */
	protected final void configureWebAppContext(WebAppContext context, ServletContextInitializer... initializers) {
		Assert.notNull(context, "Context must not be null");
		context.getAliasChecks().clear();
		context.setTempDirectory(getTempDirectory());
		if (this.resourceLoader != null) {
			context.setClassLoader(this.resourceLoader.getClassLoader());
		}
		String contextPath = getContextPath();
		context.setContextPath(StringUtils.hasLength(contextPath) ? contextPath : "/");
		context.setDisplayName(getDisplayName());
		configureDocumentRoot(context);
		if (isRegisterDefaultServlet()) {
			addDefaultServlet(context);
		}
		if (shouldRegisterJspServlet()) {
			addJspServlet(context);
			context.addBean(new JasperInitializer(context), true);
		}
		addLocaleMappings(context);
		ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
		Configuration[] configurations = getWebAppContextConfigurations(context, initializersToUse);
		context.setConfigurations(configurations);
		context.setThrowUnavailableOnStartupException(true);
		configureSession(context);
		postProcessWebAppContext(context);
	}

	private void configureSession(WebAppContext context) {
		SessionHandler handler = context.getSessionHandler();
		Duration sessionTimeout = getSession().getTimeout();
		handler.setMaxInactiveInterval(isNegative(sessionTimeout) ? -1 : (int) sessionTimeout.getSeconds());
		if (getSession().isPersistent()) {
			DefaultSessionCache cache = new DefaultSessionCache(handler);
			FileSessionDataStore store = new FileSessionDataStore();
			store.setStoreDir(getValidSessionStoreDir());
			cache.setSessionDataStore(store);
			handler.setSessionCache(cache);
		}
	}

	private boolean isNegative(Duration sessionTimeout) {
		return sessionTimeout == null || sessionTimeout.isNegative();
	}

	private void addLocaleMappings(WebAppContext context) {
		getLocaleCharsetMappings()
				.forEach((locale, charset) -> context.addLocaleEncoding(locale.toString(), charset.toString()));
	}

	private File getTempDirectory() {
		String temp = System.getProperty("java.io.tmpdir");
		return (temp != null) ? new File(temp) : null;
	}

	private void configureDocumentRoot(WebAppContext handler) {
		File root = getValidDocumentRoot();
		File docBase = (root != null) ? root : createTempDir("jetty-docbase");
		try {
			List<Resource> resources = new ArrayList<>();
			Resource rootResource = (docBase.isDirectory() ? Resource.newResource(docBase.getCanonicalFile())
					: JarResource.newJarResource(Resource.newResource(docBase)));
			resources.add((root != null) ? new LoaderHidingResource(rootResource) : rootResource);
			for (URL resourceJarUrl : this.getUrlsOfJarsWithMetaInfResources()) {
				Resource resource = createResource(resourceJarUrl);
				if (resource.exists() && resource.isDirectory()) {
					resources.add(resource);
				}
			}
			handler.setBaseResource(new ResourceCollection(resources.toArray(new Resource[0])));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Resource createResource(URL url) throws Exception {
		if ("file".equals(url.getProtocol())) {
			File file = new File(url.toURI());
			if (file.isFile()) {
				return Resource.newResource("jar:" + url + "!/META-INF/resources");
			}
		}
		return Resource.newResource(url + "META-INF/resources");
	}

	/**
	 * Add Jetty's {@code DefaultServlet} to the given {@link WebAppContext}.
	 * @param context the jetty {@link WebAppContext}
	 */
	protected final void addDefaultServlet(WebAppContext context) {
		Assert.notNull(context, "Context must not be null");
		ServletHolder holder = new ServletHolder();
		holder.setName("default");
		holder.setClassName("org.eclipse.jetty.servlet.DefaultServlet");
		holder.setInitParameter("dirAllowed", "false");
		holder.setInitOrder(1);
		context.getServletHandler().addServletWithMapping(holder, "/");
		context.getServletHandler().getServletMapping("/").setDefault(true);
	}

	/**
	 * Add Jetty's {@code JspServlet} to the given {@link WebAppContext}.
	 * @param context the jetty {@link WebAppContext}
	 */
	protected final void addJspServlet(WebAppContext context) {
		Assert.notNull(context, "Context must not be null");
		ServletHolder holder = new ServletHolder();
		holder.setName("jsp");
		holder.setClassName(getJsp().getClassName());
		holder.setInitParameter("fork", "false");
		holder.setInitParameters(getJsp().getInitParameters());
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
		List<Configuration> configurations = new ArrayList<>();
		configurations.add(getServletContextInitializerConfiguration(webAppContext, initializers));
		configurations.add(getErrorPageConfiguration());
		configurations.add(getMimeTypeConfiguration());
		configurations.addAll(getConfigurations());
		return configurations.toArray(new Configuration[0]);
	}

	/**
	 * Create a configuration object that adds error handlers.
	 * @return a configuration object for adding error pages
	 */
	private Configuration getErrorPageConfiguration() {
		return new AbstractConfiguration() {

			@Override
			public void configure(WebAppContext context) throws Exception {
				JettyEmbeddedErrorHandler errorHandler = new JettyEmbeddedErrorHandler();
				context.setErrorHandler(errorHandler);
				addJettyErrorPages(errorHandler, getErrorPages());
			}

		};
	}

	/**
	 * Create a configuration object that adds mime type mappings.
	 * @return a configuration object for adding mime type mappings
	 */
	private Configuration getMimeTypeConfiguration() {
		return new AbstractConfiguration() {

			@Override
			public void configure(WebAppContext context) throws Exception {
				MimeTypes mimeTypes = context.getMimeTypes();
				for (MimeMappings.Mapping mapping : getMimeMappings()) {
					mimeTypes.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
				}
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
	protected Configuration getServletContextInitializerConfiguration(WebAppContext webAppContext,
			ServletContextInitializer... initializers) {
		return new ServletContextInitializerConfiguration(initializers);
	}

	/**
	 * Post process the Jetty {@link WebAppContext} before it's used with the Jetty
	 * Server. Subclasses can override this method to apply additional processing to the
	 * {@link WebAppContext}.
	 * @param webAppContext the Jetty {@link WebAppContext}
	 */
	protected void postProcessWebAppContext(WebAppContext webAppContext) {
	}

	/**
	 * Factory method called to create the {@link JettyWebServer}. Subclasses can override
	 * this method to return a different {@link JettyWebServer} or apply additional
	 * processing to the Jetty server.
	 * @param server the Jetty server.
	 * @return a new {@link JettyWebServer} instance
	 */
	protected JettyWebServer getJettyWebServer(Server server) {
		return new JettyWebServer(server, getPort() >= 0);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	@Override
	public void setAcceptors(int acceptors) {
		this.acceptors = acceptors;
	}

	@Override
	public void setSelectors(int selectors) {
		this.selectors = selectors;
	}

	/**
	 * Sets {@link JettyServerCustomizer}s that will be applied to the {@link Server}
	 * before it is started. Calling this method will replace any existing customizers.
	 * @param customizers the Jetty customizers to apply
	 */
	public void setServerCustomizers(Collection<? extends JettyServerCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.jettyServerCustomizers = new LinkedHashSet<>(customizers);
	}

	/**
	 * Returns a mutable collection of Jetty {@link JettyServerCustomizer}s that will be
	 * applied to the {@link Server} before the it is created.
	 * @return the {@link JettyServerCustomizer}s
	 */
	public Collection<JettyServerCustomizer> getServerCustomizers() {
		return this.jettyServerCustomizers;
	}

	@Override
	public void addServerCustomizers(JettyServerCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.jettyServerCustomizers.addAll(Arrays.asList(customizers));
	}

	/**
	 * Sets Jetty {@link Configuration}s that will be applied to the {@link WebAppContext}
	 * before the server is created. Calling this method will replace any existing
	 * configurations.
	 * @param configurations the Jetty configurations to apply
	 */
	public void setConfigurations(Collection<? extends Configuration> configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		this.configurations = new ArrayList<>(configurations);
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
	 * @param configurations the configurations to add
	 */
	public void addConfigurations(Configuration... configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		this.configurations.addAll(Arrays.asList(configurations));
	}

	/**
	 * Returns a Jetty {@link ThreadPool} that should be used by the {@link Server}.
	 * @return a Jetty {@link ThreadPool} or {@code null}
	 */
	public ThreadPool getThreadPool() {
		return this.threadPool;
	}

	/**
	 * Set a Jetty {@link ThreadPool} that should be used by the {@link Server}. If set to
	 * {@code null} (default), the {@link Server} creates a {@link ThreadPool} implicitly.
	 * @param threadPool a Jetty ThreadPool to be used
	 */
	public void setThreadPool(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}

	private void addJettyErrorPages(ErrorHandler errorHandler, Collection<ErrorPage> errorPages) {
		if (errorHandler instanceof ErrorPageErrorHandler) {
			ErrorPageErrorHandler handler = (ErrorPageErrorHandler) errorHandler;
			for (ErrorPage errorPage : errorPages) {
				if (errorPage.isGlobal()) {
					handler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE, errorPage.getPath());
				}
				else {
					if (errorPage.getExceptionName() != null) {
						handler.addErrorPage(errorPage.getExceptionName(), errorPage.getPath());
					}
					else {
						handler.addErrorPage(errorPage.getStatusCode(), errorPage.getPath());
					}
				}
			}
		}
	}

	private static final class LoaderHidingResource extends Resource {

		private final Resource delegate;

		private LoaderHidingResource(Resource delegate) {
			this.delegate = delegate;
		}

		@Override
		public Resource addPath(String path) throws IOException, MalformedURLException {
			if (path.startsWith("/org/springframework/boot")) {
				return null;
			}
			return this.delegate.addPath(path);
		}

		@Override
		public boolean isContainedIn(Resource resource) throws MalformedURLException {
			return this.delegate.isContainedIn(resource);
		}

		@Override
		public void close() {
			this.delegate.close();
		}

		@Override
		public boolean exists() {
			return this.delegate.exists();
		}

		@Override
		public boolean isDirectory() {
			return this.delegate.isDirectory();
		}

		@Override
		public long lastModified() {
			return this.delegate.lastModified();
		}

		@Override
		public long length() {
			return this.delegate.length();
		}

		@Override
		@Deprecated
		public URL getURL() {
			return this.delegate.getURL();
		}

		@Override
		public File getFile() throws IOException {
			return this.delegate.getFile();
		}

		@Override
		public String getName() {
			return this.delegate.getName();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.delegate.getInputStream();
		}

		@Override
		public ReadableByteChannel getReadableByteChannel() throws IOException {
			return this.delegate.getReadableByteChannel();
		}

		@Override
		public boolean delete() throws SecurityException {
			return this.delegate.delete();
		}

		@Override
		public boolean renameTo(Resource dest) throws SecurityException {
			return this.delegate.renameTo(dest);
		}

		@Override
		public String[] list() {
			return this.delegate.list();
		}

	}

}
