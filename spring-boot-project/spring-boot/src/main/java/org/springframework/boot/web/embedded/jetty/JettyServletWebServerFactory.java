/*
 * Copyright 2012-2024 the original author or authors.
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
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.servlet.http.Cookie;
import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.ListenerHolder;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.ServletMapping;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.servlet.Source;
import org.eclipse.jetty.ee10.webapp.AbstractConfiguration;
import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.webapp.WebInfConfiguration;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.MimeTypes.Wrapper;
import org.eclipse.jetty.http.SetCookieParser;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.HttpCookieUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.session.DefaultSessionCache;
import org.eclipse.jetty.session.FileSessionDataStore;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.CombinedResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.URLResourceFactory;
import org.eclipse.jetty.util.thread.ThreadPool;

import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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
 * @author Moritz Halbritter
 * @author Onur Kagan Ozcan
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

	private int maxConnections = -1;

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

	/**
	 * Returns a WebServer instance configured with the provided
	 * ServletContextInitializers.
	 * @param initializers the ServletContextInitializers to be applied to the WebServer
	 * @return a configured WebServer instance
	 */
	@Override
	public WebServer getWebServer(ServletContextInitializer... initializers) {
		JettyEmbeddedWebAppContext context = new JettyEmbeddedWebAppContext();
		context.getContext().getServletContext().setExtendedListenerTypes(true);
		int port = Math.max(getPort(), 0);
		InetSocketAddress address = new InetSocketAddress(getAddress(), port);
		Server server = createServer(address);
		context.setServer(server);
		configureWebAppContext(context, initializers);
		server.setHandler(addHandlerWrappers(context));
		this.logger.info("Server initialized with port: " + port);
		if (this.maxConnections > -1) {
			server.addBean(new ConnectionLimit(this.maxConnections, server.getConnectors()));
		}
		if (Ssl.isEnabled(getSsl())) {
			customizeSsl(server, address);
		}
		for (JettyServerCustomizer customizer : getServerCustomizers()) {
			customizer.customize(server);
		}
		if (this.useForwardHeaders) {
			new ForwardHeadersCustomizer().customize(server);
		}
		if (getShutdown() == Shutdown.GRACEFUL) {
			StatisticsHandler statisticsHandler = new StatisticsHandler();
			statisticsHandler.setHandler(server.getHandler());
			server.setHandler(statisticsHandler);
		}
		return getJettyWebServer(server);
	}

	/**
	 * Creates a Jetty server instance with the specified address.
	 * @param address the InetSocketAddress to bind the server to
	 * @return the created Server instance
	 */
	private Server createServer(InetSocketAddress address) {
		Server server = new Server(getThreadPool());
		server.setConnectors(new Connector[] { createConnector(address, server) });
		server.setStopTimeout(0);
		MimeTypes.Mutable mimeTypes = server.getMimeTypes();
		for (MimeMappings.Mapping mapping : getMimeMappings()) {
			mimeTypes.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
		}
		return server;
	}

	/**
	 * Creates a connector for the given address and server.
	 * @param address the address to bind the connector to
	 * @param server the server instance to create the connector for
	 * @return the created connector
	 */
	private AbstractConnector createConnector(InetSocketAddress address, Server server) {
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		List<ConnectionFactory> connectionFactories = new ArrayList<>();
		connectionFactories.add(new HttpConnectionFactory(httpConfiguration));
		if (getHttp2() != null && getHttp2().isEnabled()) {
			connectionFactories.add(new HTTP2CServerConnectionFactory(httpConfiguration));
		}
		ServerConnector connector = new ServerConnector(server, this.acceptors, this.selectors,
				connectionFactories.toArray(new ConnectionFactory[0]));
		connector.setHost(address.getHostString());
		connector.setPort(address.getPort());
		return connector;
	}

	/**
	 * Adds wrapper handlers to the given handler based on the configuration settings.
	 * @param handler the original handler to which the wrappers will be added
	 * @return the modified handler with the added wrappers
	 */
	private Handler addHandlerWrappers(Handler handler) {
		if (getCompression() != null && getCompression().getEnabled()) {
			handler = applyWrapper(handler, JettyHandlerWrappers.createGzipHandlerWrapper(getCompression()));
		}
		if (StringUtils.hasText(getServerHeader())) {
			handler = applyWrapper(handler, JettyHandlerWrappers.createServerHeaderHandlerWrapper(getServerHeader()));
		}
		if (!CollectionUtils.isEmpty(getCookieSameSiteSuppliers())) {
			handler = applyWrapper(handler, new SuppliedSameSiteCookieHandlerWrapper(getCookieSameSiteSuppliers()));
		}
		return handler;
	}

	/**
	 * Applies a wrapper to the given handler.
	 * @param handler the original handler to be wrapped
	 * @param wrapper the wrapper to be applied
	 * @return the wrapped handler
	 */
	private Handler applyWrapper(Handler handler, Handler.Wrapper wrapper) {
		wrapper.setHandler(handler);
		return wrapper;
	}

	/**
	 * Customizes the SSL configuration for the given server and address.
	 * @param server the server to customize
	 * @param address the address to bind the server to
	 */
	private void customizeSsl(Server server, InetSocketAddress address) {
		new SslServerCustomizer(getHttp2(), address, getSsl().getClientAuth(), getSslBundle()).customize(server);
	}

	/**
	 * Configure the given Jetty {@link WebAppContext} for use.
	 * @param context the context to configure
	 * @param initializers the set of initializers to apply
	 */
	protected final void configureWebAppContext(WebAppContext context, ServletContextInitializer... initializers) {
		Assert.notNull(context, "Context must not be null");
		context.clearAliasChecks();
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
		context.setTempDirectory(getTempDirectory(context));
		postProcessWebAppContext(context);
	}

	/**
	 * Configures the session for the given WebAppContext.
	 * @param context the WebAppContext to configure the session for
	 */
	private void configureSession(WebAppContext context) {
		SessionHandler handler = context.getSessionHandler();
		SameSite sessionSameSite = getSession().getCookie().getSameSite();
		if (sessionSameSite != null) {
			handler.setSameSite(HttpCookie.SameSite.valueOf(sessionSameSite.name()));
		}
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

	/**
	 * Checks if the given session timeout is negative.
	 * @param sessionTimeout the session timeout duration to be checked
	 * @return {@code true} if the session timeout is negative or {@code null},
	 * {@code false} otherwise
	 */
	private boolean isNegative(Duration sessionTimeout) {
		return sessionTimeout == null || sessionTimeout.isNegative();
	}

	/**
	 * Adds locale mappings to the given WebAppContext.
	 * @param context the WebAppContext to add the locale mappings to
	 */
	private void addLocaleMappings(WebAppContext context) {
		getLocaleCharsetMappings()
			.forEach((locale, charset) -> context.addLocaleEncoding(locale.toString(), charset.toString()));
	}

	/**
	 * Returns the temporary directory for the given web application context.
	 * @param context the web application context
	 * @return the temporary directory for the web application context, or null if the
	 * system property "java.io.tmpdir" is not set
	 */
	private File getTempDirectory(WebAppContext context) {
		String temp = System.getProperty("java.io.tmpdir");
		return (temp != null)
				? new File(temp, WebInfConfiguration.getCanonicalNameForWebAppTmpDir(context) + UUID.randomUUID())
				: null;
	}

	/**
	 * Configures the document root for the given WebAppContext handler.
	 * @param handler the WebAppContext handler to configure
	 */
	private void configureDocumentRoot(WebAppContext handler) {
		File root = getValidDocumentRoot();
		File docBase = (root != null) ? root : createTempDir("jetty-docbase");
		try {
			ResourceFactory resourceFactory = handler.getResourceFactory();
			List<Resource> resources = new ArrayList<>();
			Resource rootResource = (docBase.isDirectory()
					? resourceFactory.newResource(docBase.getCanonicalFile().toURI())
					: resourceFactory.newJarFileResource(docBase.toURI()));
			resources.add((root != null) ? new LoaderHidingResource(rootResource, rootResource) : rootResource);
			URLResourceFactory urlResourceFactory = new URLResourceFactory();
			for (URL resourceJarUrl : getUrlsOfJarsWithMetaInfResources()) {
				Resource resource = createResource(resourceJarUrl, resourceFactory, urlResourceFactory);
				if (resource != null) {
					resources.add(resource);
				}
			}
			handler.setBaseResource(ResourceFactory.combine(resources));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Creates a resource based on the given URL.
	 * @param url the URL of the resource
	 * @param resourceFactory the factory for creating resources
	 * @param urlResourceFactory the factory for creating URL resources
	 * @return the created resource
	 * @throws Exception if an error occurs while creating the resource
	 */
	private Resource createResource(URL url, ResourceFactory resourceFactory, URLResourceFactory urlResourceFactory)
			throws Exception {
		if ("file".equals(url.getProtocol())) {
			File file = new File(url.toURI());
			if (file.isFile()) {
				return resourceFactory.newResource("jar:" + url + "!/META-INF/resources/");
			}
			if (file.isDirectory()) {
				return resourceFactory.newResource(url).resolve("META-INF/resources/");
			}
		}
		return urlResourceFactory.newResource(url + "META-INF/resources/");
	}

	/**
	 * Add Jetty's {@code DefaultServlet} to the given {@link WebAppContext}.
	 * @param context the jetty {@link WebAppContext}
	 */
	protected final void addDefaultServlet(WebAppContext context) {
		Assert.notNull(context, "Context must not be null");
		ServletHolder holder = new ServletHolder();
		holder.setName("default");
		holder.setClassName("org.eclipse.jetty.ee10.servlet.DefaultServlet");
		holder.setInitParameter("dirAllowed", "false");
		holder.setInitOrder(1);
		context.getServletHandler().addServletWithMapping(holder, "/");
		ServletMapping servletMapping = context.getServletHandler().getServletMapping("/");
		servletMapping.setFromDefaultDescriptor(true);
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
		configurations.add(new WebListenersConfiguration(getWebListenerClassNames()));
		configurations.addAll(getConfigurations());
		return configurations.toArray(new Configuration[0]);
	}

	/**
	 * Create a configuration object that adds error handlers.
	 * @return a configuration object for adding error pages
	 */
	private Configuration getErrorPageConfiguration() {
		return new AbstractConfiguration(new AbstractConfiguration.Builder()) {

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
		return new AbstractConfiguration(new AbstractConfiguration.Builder()) {

			@Override
			public void configure(WebAppContext context) throws Exception {
				MimeTypes.Wrapper mimeTypes = (Wrapper) context.getMimeTypes();
				mimeTypes.setWrapped(new MimeTypes(null));
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

	/**
	 * Set the resource loader to be used by this JettyServletWebServerFactory.
	 * @param resourceLoader the resource loader to be set
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Sets whether to use forward headers.
	 * @param useForwardHeaders true to use forward headers, false otherwise
	 */
	@Override
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	/**
	 * Sets the number of acceptors for the Jetty server.
	 * @param acceptors the number of acceptors to set
	 */
	@Override
	public void setAcceptors(int acceptors) {
		this.acceptors = acceptors;
	}

	/**
	 * Sets the number of selectors to be used by the Jetty server.
	 * @param selectors the number of selectors to be used
	 */
	@Override
	public void setSelectors(int selectors) {
		this.selectors = selectors;
	}

	/**
	 * Sets the maximum number of connections allowed for this Jetty servlet web server
	 * factory.
	 * @param maxConnections the maximum number of connections to set
	 */
	@Override
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
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
	 * applied to the {@link Server} before it is created.
	 * @return the {@link JettyServerCustomizer}s
	 */
	public Collection<JettyServerCustomizer> getServerCustomizers() {
		return this.jettyServerCustomizers;
	}

	/**
	 * Adds customizers to the Jetty server.
	 * @param customizers the customizers to be added (must not be null)
	 */
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
	 * Sets the thread pool for the Jetty servlet web server factory.
	 * @param threadPool the thread pool to be set
	 */
	@Override
	public void setThreadPool(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}

	/**
	 * Adds Jetty error pages to the provided ErrorHandler.
	 * @param errorHandler The ErrorHandler to which the error pages will be added.
	 * @param errorPages The collection of ErrorPage objects representing the error pages
	 * to be added.
	 */
	private void addJettyErrorPages(ErrorHandler errorHandler, Collection<ErrorPage> errorPages) {
		if (errorHandler instanceof ErrorPageErrorHandler handler) {
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

	/**
	 * LoaderHidingResource class.
	 */
	private static final class LoaderHidingResource extends Resource {

		private static final String LOADER_RESOURCE_PATH_PREFIX = "/org/springframework/boot/";

		private final Resource base;

		private final Resource delegate;

		/**
		 * Constructs a new LoaderHidingResource with the specified base and delegate
		 * resources.
		 * @param base the base resource to be used
		 * @param delegate the delegate resource to be used
		 */
		private LoaderHidingResource(Resource base, Resource delegate) {
			this.base = base;
			this.delegate = delegate;
		}

		/**
		 * Applies the specified action to each element in this collection.
		 * @param action the action to be applied to each element
		 * @throws NullPointerException if the specified action is null
		 */
		@Override
		public void forEach(Consumer<? super Resource> action) {
			this.delegate.forEach(action);
		}

		/**
		 * Returns the path of the resource.
		 * @return the path of the resource
		 */
		@Override
		public Path getPath() {
			return this.delegate.getPath();
		}

		/**
		 * Checks if the current resource is contained within the specified resource.
		 * @param r the resource to check containment against
		 * @return true if the current resource is contained within the specified
		 * resource, false otherwise
		 */
		@Override
		public boolean isContainedIn(Resource r) {
			return this.delegate.isContainedIn(r);
		}

		/**
		 * Returns an iterator over the resources in this LoaderHidingResource object.
		 * @return an iterator over the resources in this LoaderHidingResource object
		 */
		@Override
		public Iterator<Resource> iterator() {
			if (this.delegate instanceof CombinedResource) {
				return list().iterator();
			}
			return List.<Resource>of(this).iterator();
		}

		/**
		 * Compares this LoaderHidingResource object to the specified object. The result
		 * is true if and only if the argument is not null and is a LoaderHidingResource
		 * object that represents the same resource as this object.
		 * @param obj the object to compare this LoaderHidingResource against
		 * @return true if the given object represents a LoaderHidingResource equivalent
		 * to this object, false otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			return this.delegate.equals(obj);
		}

		/**
		 * Returns a hash code value for the object. This method overrides the hashCode()
		 * method in the Object class.
		 * @return the hash code value for the object
		 */
		@Override
		public int hashCode() {
			return this.delegate.hashCode();
		}

		/**
		 * Returns a boolean value indicating whether the resource exists.
		 * @return {@code true} if the resource exists, {@code false} otherwise.
		 */
		@Override
		public boolean exists() {
			return this.delegate.exists();
		}

		/**
		 * Returns a Spliterator over the elements in this LoaderHidingResource object.
		 * @return a Spliterator over the elements in this LoaderHidingResource object
		 */
		@Override
		public Spliterator<Resource> spliterator() {
			return this.delegate.spliterator();
		}

		/**
		 * Returns a boolean value indicating whether the resource is a directory.
		 * @return true if the resource is a directory, false otherwise
		 */
		@Override
		public boolean isDirectory() {
			return this.delegate.isDirectory();
		}

		/**
		 * Returns a boolean value indicating whether the resource is readable.
		 * @return {@code true} if the resource is readable, {@code false} otherwise.
		 */
		@Override
		public boolean isReadable() {
			return this.delegate.isReadable();
		}

		/**
		 * Returns the last modified timestamp of the resource.
		 * @return the last modified timestamp of the resource
		 */
		@Override
		public Instant lastModified() {
			return this.delegate.lastModified();
		}

		/**
		 * Returns the length of the resource.
		 * @return the length of the resource
		 */
		@Override
		public long length() {
			return this.delegate.length();
		}

		/**
		 * Returns the URI of the resource.
		 * @return the URI of the resource
		 */
		@Override
		public URI getURI() {
			return this.delegate.getURI();
		}

		/**
		 * Returns the name of the resource.
		 * @return the name of the resource
		 */
		@Override
		public String getName() {
			return this.delegate.getName();
		}

		/**
		 * Returns the file name of the resource.
		 * @return the file name of the resource
		 */
		@Override
		public String getFileName() {
			return this.delegate.getFileName();
		}

		/**
		 * Returns a new input stream for reading the content of this resource.
		 * @return a new input stream
		 * @throws IOException if an I/O error occurs
		 */
		@Override
		public InputStream newInputStream() throws IOException {
			return this.delegate.newInputStream();
		}

		/**
		 * Returns a new ReadableByteChannel that reads bytes from this resource.
		 * @return a new ReadableByteChannel
		 * @throws IOException if an I/O error occurs
		 */
		@Override
		public ReadableByteChannel newReadableByteChannel() throws IOException {
			return this.delegate.newReadableByteChannel();
		}

		/**
		 * Returns a list of resources, filtering out any loader resources.
		 * @return a list of non-loader resources
		 */
		@Override
		public List<Resource> list() {
			return this.delegate.list().stream().filter(this::nonLoaderResource).toList();
		}

		/**
		 * Checks if the given resource is a non-loader resource.
		 * @param resource the resource to be checked
		 * @return {@code true} if the resource is not a loader resource, {@code false}
		 * otherwise
		 */
		private boolean nonLoaderResource(Resource resource) {
			Path prefix = this.base.getPath().resolve(Path.of("org", "springframework", "boot"));
			return !resource.getPath().startsWith(prefix);
		}

		/**
		 * Resolves the given subUriPath to a Resource object.
		 * @param subUriPath the sub-path of the URI to resolve
		 * @return the resolved Resource object, or null if the subUriPath starts with
		 * LOADER_RESOURCE_PATH_PREFIX or if the delegate's resolve method returns null
		 */
		@Override
		public Resource resolve(String subUriPath) {
			if (subUriPath.startsWith(LOADER_RESOURCE_PATH_PREFIX)) {
				return null;
			}
			Resource resolved = this.delegate.resolve(subUriPath);
			return (resolved != null) ? new LoaderHidingResource(this.base, resolved) : null;
		}

		/**
		 * Returns a boolean value indicating whether the resource is an alias.
		 * @return true if the resource is an alias, false otherwise
		 */
		@Override
		public boolean isAlias() {
			return this.delegate.isAlias();
		}

		/**
		 * Returns the real URI of the resource.
		 * @return the real URI of the resource
		 */
		@Override
		public URI getRealURI() {
			return this.delegate.getRealURI();
		}

		/**
		 * Copies the content of this resource to the specified destination path.
		 * @param destination the path where the content will be copied to
		 * @throws IOException if an I/O error occurs during the copying process
		 */
		@Override
		public void copyTo(Path destination) throws IOException {
			this.delegate.copyTo(destination);
		}

		/**
		 * Returns a collection of all resources, excluding loader resources.
		 * @return a collection of resources
		 */
		@Override
		public Collection<Resource> getAllResources() {
			return this.delegate.getAllResources().stream().filter(this::nonLoaderResource).toList();
		}

		/**
		 * Returns a string representation of the object.
		 * @return a string representation of the object
		 */
		@Override
		public String toString() {
			return this.delegate.toString();
		}

	}

	/**
	 * {@link AbstractConfiguration} to apply {@code @WebListener} classes.
	 */
	private static class WebListenersConfiguration extends AbstractConfiguration {

		private final Set<String> classNames;

		/**
		 * Constructs a new WebListenersConfiguration object with the specified set of web
		 * listener class names.
		 * @param webListenerClassNames the set of web listener class names to be used for
		 * configuration
		 */
		WebListenersConfiguration(Set<String> webListenerClassNames) {
			super(new AbstractConfiguration.Builder());
			this.classNames = webListenerClassNames;
		}

		/**
		 * Configures the WebAppContext by adding servlets for each class name specified.
		 * @param context the WebAppContext to be configured
		 * @throws Exception if an error occurs during configuration
		 */
		@Override
		public void configure(WebAppContext context) throws Exception {
			ServletHandler servletHandler = context.getServletHandler();
			for (String className : this.classNames) {
				configure(context, servletHandler, className);
			}
		}

		/**
		 * Configures the given WebAppContext with the specified ServletHandler and class
		 * name.
		 * @param context The WebAppContext to be configured.
		 * @param servletHandler The ServletHandler to be used.
		 * @param className The name of the class to be loaded.
		 * @throws ClassNotFoundException If the specified class cannot be found.
		 */
		private void configure(WebAppContext context, ServletHandler servletHandler, String className)
				throws ClassNotFoundException {
			ListenerHolder holder = servletHandler.newListenerHolder(new Source(Source.Origin.ANNOTATION, className));
			holder.setHeldClass(loadClass(context, className));
			servletHandler.addListener(holder);
		}

		/**
		 * Loads the class with the given className using the classLoader of the provided
		 * WebAppContext. If the classLoader is null, the classLoader of the current class
		 * is used.
		 * @param context the WebAppContext to use for loading the class
		 * @param className the name of the class to load
		 * @return the loaded class as a subclass of EventListener
		 * @throws ClassNotFoundException if the class with the given className cannot be
		 * found
		 */
		@SuppressWarnings("unchecked")
		private Class<? extends EventListener> loadClass(WebAppContext context, String className)
				throws ClassNotFoundException {
			ClassLoader classLoader = context.getClassLoader();
			classLoader = (classLoader != null) ? classLoader : getClass().getClassLoader();
			return (Class<? extends EventListener>) classLoader.loadClass(className);
		}

	}

	/**
	 * {@link Handler.Wrapper} to apply {@link CookieSameSiteSupplier supplied}
	 * {@link SameSite} cookie values.
	 */
	private static class SuppliedSameSiteCookieHandlerWrapper extends Handler.Wrapper {

		private static final SetCookieParser setCookieParser = SetCookieParser.newInstance();

		private final List<CookieSameSiteSupplier> suppliers;

		/**
		 * Constructs a new SuppliedSameSiteCookieHandlerWrapper with the specified list
		 * of CookieSameSiteSupplier objects.
		 * @param suppliers the list of CookieSameSiteSupplier objects to be used by the
		 * wrapper
		 */
		SuppliedSameSiteCookieHandlerWrapper(List<CookieSameSiteSupplier> suppliers) {
			this.suppliers = suppliers;
		}

		/**
		 * Overrides the handle method to wrap the response with a
		 * SuppliedSameSiteCookieResponse object.
		 * @param request the request object
		 * @param response the response object
		 * @param callback the callback object
		 * @return true if the request is handled successfully, false otherwise
		 * @throws Exception if an error occurs during handling the request
		 */
		@Override
		public boolean handle(Request request, Response response, Callback callback) throws Exception {
			SuppliedSameSiteCookieResponse wrappedResponse = new SuppliedSameSiteCookieResponse(request, response);
			return super.handle(request, wrappedResponse, callback);
		}

		/**
		 * SuppliedSameSiteCookieResponse class.
		 */
		private class SuppliedSameSiteCookieResponse extends Response.Wrapper {

			private HttpFields.Mutable wrappedHeaders;

			/**
			 * Constructs a new SuppliedSameSiteCookieResponse object.
			 * @param request the request object associated with the response
			 * @param wrapped the wrapped response object
			 */
			SuppliedSameSiteCookieResponse(Request request, Response wrapped) {
				super(request, wrapped);
				this.wrappedHeaders = new SuppliedSameSiteCookieHeaders(
						request.getConnectionMetaData().getHttpConfiguration().getResponseCookieCompliance(),
						wrapped.getHeaders());
			}

			/**
			 * Returns the headers of the SuppliedSameSiteCookieResponse.
			 * @return the headers of the SuppliedSameSiteCookieResponse
			 */
			@Override
			public Mutable getHeaders() {
				return this.wrappedHeaders;
			}

		}

		/**
		 * SuppliedSameSiteCookieHeaders class.
		 */
		private class SuppliedSameSiteCookieHeaders extends HttpFields.Mutable.Wrapper {

			private final CookieCompliance compliance;

			/**
			 * Constructs a new instance of the SuppliedSameSiteCookieHeaders class with
			 * the specified compliance and fields.
			 * @param compliance the cookie compliance level
			 * @param fields the HTTP fields containing the cookie headers
			 */
			SuppliedSameSiteCookieHeaders(CookieCompliance compliance, HttpFields.Mutable fields) {
				super(fields);
				this.compliance = compliance;
			}

			/**
			 * This method is called when a field is added to the HTTP message. It checks
			 * if the field is a Set-Cookie header and calls the onAddSetCookieField
			 * method if it is.
			 * @param field the HttpField object being added
			 * @return the HttpField object being added, or the result of the
			 * onAddSetCookieField method if the field is a Set-Cookie header
			 */
			@Override
			public HttpField onAddField(HttpField field) {
				return (field.getHeader() != HttpHeader.SET_COOKIE) ? field : onAddSetCookieField(field);
			}

			/**
			 * Parses the value of the supplied {@link HttpField} as a Set-Cookie header
			 * and checks if it contains a SameSite attribute. If the SameSite attribute
			 * is present, it updates the SameSite attribute value and returns a new
			 * {@link HttpField} with the updated cookie. If the SameSite attribute is not
			 * present, it returns the original {@link HttpField}.
			 * @param field the {@link HttpField} to parse and update
			 * @return a new {@link HttpField} with the updated SameSite attribute value,
			 * or the original {@link HttpField} if no update is required
			 */
			private HttpField onAddSetCookieField(HttpField field) {
				HttpCookie cookie = setCookieParser.parse(field.getValue());
				SameSite sameSite = (cookie != null) ? getSameSite(cookie) : null;
				if (sameSite == null) {
					return field;
				}
				HttpCookie updatedCookie = buildCookieWithUpdatedSameSite(cookie, sameSite);
				return new HttpCookieUtils.SetCookieHttpField(updatedCookie, this.compliance);
			}

			/**
			 * Builds a new HttpCookie object with an updated SameSite attribute.
			 * @param cookie the original HttpCookie object
			 * @param sameSite the new SameSite value to be set
			 * @return a new HttpCookie object with the updated SameSite attribute
			 */
			private HttpCookie buildCookieWithUpdatedSameSite(HttpCookie cookie, SameSite sameSite) {
				return HttpCookie.build(cookie)
					.sameSite(org.eclipse.jetty.http.HttpCookie.SameSite.from(sameSite.name()))
					.build();
			}

			/**
			 * Returns the SameSite attribute of the given HttpCookie.
			 * @param cookie the HttpCookie to get the SameSite attribute from
			 * @return the SameSite attribute of the HttpCookie
			 */
			private SameSite getSameSite(HttpCookie cookie) {
				return getSameSite(asServletCookie(cookie));
			}

			/**
			 * Returns the SameSite attribute value for the given cookie.
			 * @param cookie the cookie for which to retrieve the SameSite attribute value
			 * @return the SameSite attribute value for the given cookie, or null if not
			 * found
			 */
			private SameSite getSameSite(Cookie cookie) {
				return SuppliedSameSiteCookieHandlerWrapper.this.suppliers.stream()
					.map((supplier) -> supplier.getSameSite(cookie))
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(null);
			}

			/**
			 * Converts an HttpCookie object to a Servlet Cookie object.
			 * @param cookie the HttpCookie to be converted
			 * @return the converted Servlet Cookie object
			 */
			private Cookie asServletCookie(HttpCookie cookie) {
				Cookie servletCookie = new Cookie(cookie.getName(), cookie.getValue());
				cookie.getAttributes().forEach(servletCookie::setAttribute);
				return servletCookie;
			}

		}

	}

}
