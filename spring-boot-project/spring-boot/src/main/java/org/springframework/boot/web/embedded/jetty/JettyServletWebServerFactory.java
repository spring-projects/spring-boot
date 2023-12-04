/*
 * Copyright 2012-2023 the original author or authors.
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
			server.addBean(new ConnectionLimit(this.maxConnections, server));
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

	private AbstractConnector createConnector(InetSocketAddress address, Server server) {
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		httpConfiguration.setIdleTimeout(30000);
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

	private Handler applyWrapper(Handler handler, Handler.Wrapper wrapper) {
		wrapper.setHandler(handler);
		return wrapper;
	}

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

	private boolean isNegative(Duration sessionTimeout) {
		return sessionTimeout == null || sessionTimeout.isNegative();
	}

	private void addLocaleMappings(WebAppContext context) {
		getLocaleCharsetMappings()
			.forEach((locale, charset) -> context.addLocaleEncoding(locale.toString(), charset.toString()));
	}

	private File getTempDirectory(WebAppContext context) {
		String temp = System.getProperty("java.io.tmpdir");
		return (temp != null)
				? new File(temp, WebInfConfiguration.getCanonicalNameForWebAppTmpDir(context) + UUID.randomUUID())
				: null;
	}

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

	@Override
	public void setThreadPool(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}

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

	private static final class LoaderHidingResource extends Resource {

		private static final String LOADER_RESOURCE_PATH_PREFIX = "/org/springframework/boot/";

		private final Resource base;

		private final Resource delegate;

		private LoaderHidingResource(Resource base, Resource delegate) {
			this.base = base;
			this.delegate = delegate;
		}

		@Override
		public void forEach(Consumer<? super Resource> action) {
			this.delegate.forEach(action);
		}

		@Override
		public Path getPath() {
			return this.delegate.getPath();
		}

		@Override
		public boolean isContainedIn(Resource r) {
			return this.delegate.isContainedIn(r);
		}

		@Override
		public Iterator<Resource> iterator() {
			if (this.delegate instanceof CombinedResource) {
				return list().iterator();
			}
			return List.<Resource>of(this).iterator();
		}

		@Override
		public boolean equals(Object obj) {
			return this.delegate.equals(obj);
		}

		@Override
		public int hashCode() {
			return this.delegate.hashCode();
		}

		@Override
		public boolean exists() {
			return this.delegate.exists();
		}

		@Override
		public Spliterator<Resource> spliterator() {
			return this.delegate.spliterator();
		}

		@Override
		public boolean isDirectory() {
			return this.delegate.isDirectory();
		}

		@Override
		public boolean isReadable() {
			return this.delegate.isReadable();
		}

		@Override
		public Instant lastModified() {
			return this.delegate.lastModified();
		}

		@Override
		public long length() {
			return this.delegate.length();
		}

		@Override
		public URI getURI() {
			return this.delegate.getURI();
		}

		@Override
		public String getName() {
			return this.delegate.getName();
		}

		@Override
		public String getFileName() {
			return this.delegate.getFileName();
		}

		@Override
		public InputStream newInputStream() throws IOException {
			return this.delegate.newInputStream();
		}

		@Override
		public ReadableByteChannel newReadableByteChannel() throws IOException {
			return this.delegate.newReadableByteChannel();
		}

		@Override
		public List<Resource> list() {
			return this.delegate.list().stream().filter(this::nonLoaderResource).toList();
		}

		private boolean nonLoaderResource(Resource resource) {
			Path prefix = this.base.getPath().resolve(Path.of("org", "springframework", "boot"));
			return !resource.getPath().startsWith(prefix);
		}

		@Override
		public Resource resolve(String subUriPath) {
			if (subUriPath.startsWith(LOADER_RESOURCE_PATH_PREFIX)) {
				return null;
			}
			Resource resolved = this.delegate.resolve(subUriPath);
			return (resolved != null) ? new LoaderHidingResource(this.base, resolved) : null;
		}

		@Override
		public boolean isAlias() {
			return this.delegate.isAlias();
		}

		@Override
		public URI getRealURI() {
			return this.delegate.getRealURI();
		}

		@Override
		public void copyTo(Path destination) throws IOException {
			this.delegate.copyTo(destination);
		}

		@Override
		public Collection<Resource> getAllResources() {
			return this.delegate.getAllResources().stream().filter(this::nonLoaderResource).toList();
		}

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

		WebListenersConfiguration(Set<String> webListenerClassNames) {
			super(new AbstractConfiguration.Builder());
			this.classNames = webListenerClassNames;
		}

		@Override
		public void configure(WebAppContext context) throws Exception {
			ServletHandler servletHandler = context.getServletHandler();
			for (String className : this.classNames) {
				configure(context, servletHandler, className);
			}
		}

		private void configure(WebAppContext context, ServletHandler servletHandler, String className)
				throws ClassNotFoundException {
			ListenerHolder holder = servletHandler.newListenerHolder(new Source(Source.Origin.ANNOTATION, className));
			holder.setHeldClass(loadClass(context, className));
			servletHandler.addListener(holder);
		}

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

		SuppliedSameSiteCookieHandlerWrapper(List<CookieSameSiteSupplier> suppliers) {
			this.suppliers = suppliers;
		}

		@Override
		public boolean handle(Request request, Response response, Callback callback) throws Exception {
			SuppliedSameSiteCookieResponse wrappedResponse = new SuppliedSameSiteCookieResponse(request, response);
			return super.handle(request, wrappedResponse, callback);
		}

		private class SuppliedSameSiteCookieResponse extends Response.Wrapper {

			private HttpFields.Mutable wrappedHeaders;

			SuppliedSameSiteCookieResponse(Request request, Response wrapped) {
				super(request, wrapped);
				this.wrappedHeaders = new SuppliedSameSiteCookieHeaders(
						request.getConnectionMetaData().getHttpConfiguration().getResponseCookieCompliance(),
						wrapped.getHeaders());
			}

			@Override
			public Mutable getHeaders() {
				return this.wrappedHeaders;
			}

		}

		private class SuppliedSameSiteCookieHeaders extends HttpFields.Mutable.Wrapper {

			private final CookieCompliance compliance;

			SuppliedSameSiteCookieHeaders(CookieCompliance compliance, HttpFields.Mutable fields) {
				super(fields);
				this.compliance = compliance;
			}

			@Override
			public HttpField onAddField(HttpField field) {
				return (field.getHeader() != HttpHeader.SET_COOKIE) ? field : onAddSetCookieField(field);
			}

			private HttpField onAddSetCookieField(HttpField field) {
				HttpCookie cookie = setCookieParser.parse(field.getValue());
				SameSite sameSite = (cookie != null) ? getSameSite(cookie) : null;
				if (sameSite == null) {
					return field;
				}
				HttpCookie updatedCookie = buildCookieWithUpdatedSameSite(cookie, sameSite);
				return new HttpCookieUtils.SetCookieHttpField(updatedCookie, this.compliance);
			}

			private HttpCookie buildCookieWithUpdatedSameSite(HttpCookie cookie, SameSite sameSite) {
				return HttpCookie.build(cookie)
					.sameSite(org.eclipse.jetty.http.HttpCookie.SameSite.from(sameSite.name()))
					.build();
			}

			private SameSite getSameSite(HttpCookie cookie) {
				return getSameSite(asServletCookie(cookie));
			}

			private SameSite getSameSite(Cookie cookie) {
				return SuppliedSameSiteCookieHandlerWrapper.this.suppliers.stream()
					.map((supplier) -> supplier.getSameSite(cookie))
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(null);
			}

			private Cookie asServletCookie(HttpCookie cookie) {
				Cookie servletCookie = new Cookie(cookie.getName(), cookie.getValue());
				cookie.getAttributes().forEach(servletCookie::setAttribute);
				return servletCookie;
			}

		}

	}

}
