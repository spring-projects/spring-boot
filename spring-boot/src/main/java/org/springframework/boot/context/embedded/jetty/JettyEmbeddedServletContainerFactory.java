/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded.jetty;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.Compression;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.Ssl.ClientAuth;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EmbeddedServletContainerFactory} that can be used to create
 * {@link JettyEmbeddedServletContainer}s. Can be initialized using Spring's
 * {@link ServletContextInitializer}s or Jetty {@link Configuration}s.
 * <p>
 * Unless explicitly configured otherwise this factory will created containers that
 * listens for HTTP requests on port 8080.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andrey Hihlovskiy
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Venil Noronha
 * @author Henri Kerola
 * @see #setPort(int)
 * @see #setConfigurations(Collection)
 * @see JettyEmbeddedServletContainer
 */
public class JettyEmbeddedServletContainerFactory
		extends AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {

	private static final String GZIP_HANDLER_JETTY_9_2 = "org.eclipse.jetty.servlets.gzip.GzipHandler";

	private static final String GZIP_HANDLER_JETTY_8 = "org.eclipse.jetty.server.handler.GzipHandler";

	private static final String GZIP_HANDLER_JETTY_9_3 = "org.eclipse.jetty.server.handler.gzip.GzipHandler";

	private static final String CONNECTOR_JETTY_8 = "org.eclipse.jetty.server.nio.SelectChannelConnector";

	private List<Configuration> configurations = new ArrayList<Configuration>();

	private boolean useForwardHeaders;

	/**
	 * The number of acceptor threads to use.
	 */
	private int acceptors = -1;

	/**
	 * The number of selector threads to use.
	 */
	private int selectors = -1;

	private List<JettyServerCustomizer> jettyServerCustomizers = new ArrayList<JettyServerCustomizer>();

	private ResourceLoader resourceLoader;

	private ThreadPool threadPool;

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
	 * @param contextPath the root context path
	 * @param port the port to listen on
	 */
	public JettyEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
	}

	@Override
	public EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers) {
		JettyEmbeddedWebAppContext context = new JettyEmbeddedWebAppContext();
		int port = (getPort() >= 0 ? getPort() : 0);
		InetSocketAddress address = new InetSocketAddress(getAddress(), port);
		Server server = createServer(address);
		configureWebAppContext(context, initializers);
		server.setHandler(addHandlerWrappers(context));
		this.logger.info("Server initialized with port: " + port);
		if (getSsl() != null && getSsl().isEnabled()) {
			SslContextFactory sslContextFactory = new SslContextFactory();
			configureSsl(sslContextFactory, getSsl());
			AbstractConnector connector = getSslServerConnectorFactory()
					.getConnector(server, sslContextFactory, port);
			server.setConnectors(new Connector[] { connector });
		}
		for (JettyServerCustomizer customizer : getServerCustomizers()) {
			customizer.customize(server);
		}
		if (this.useForwardHeaders) {
			new ForwardHeadersCustomizer().customize(server);
		}
		return getJettyEmbeddedServletContainer(server);
	}

	private Server createServer(InetSocketAddress address) {
		Server server;
		if (ClassUtils.hasConstructor(Server.class, ThreadPool.class)) {
			server = new Jetty9ServerFactory().createServer(getThreadPool());
		}
		else {
			server = new Jetty8ServerFactory().createServer(getThreadPool());
		}
		server.setConnectors(new Connector[] { createConnector(address, server) });
		return server;
	}

	private AbstractConnector createConnector(InetSocketAddress address, Server server) {
		if (ClassUtils.isPresent(CONNECTOR_JETTY_8, getClass().getClassLoader())) {
			return new Jetty8ConnectorFactory().createConnector(server, address,
					this.acceptors, this.selectors);
		}
		return new Jetty9ConnectorFactory().createConnector(server, address,
				this.acceptors, this.selectors);
	}

	private Handler addHandlerWrappers(Handler handler) {
		if (getCompression() != null && getCompression().getEnabled()) {
			handler = applyWrapper(handler, createGzipHandler());
		}
		if (StringUtils.hasText(getServerHeader())) {
			handler = applyWrapper(handler, new ServerHeaderHandler(getServerHeader()));
		}
		return handler;
	}

	private Handler applyWrapper(Handler handler, HandlerWrapper wrapper) {
		wrapper.setHandler(handler);
		return wrapper;
	}

	private HandlerWrapper createGzipHandler() {
		ClassLoader classLoader = getClass().getClassLoader();
		if (ClassUtils.isPresent(GZIP_HANDLER_JETTY_9_2, classLoader)) {
			return new Jetty92GzipHandlerFactory().createGzipHandler(getCompression());
		}
		if (ClassUtils.isPresent(GZIP_HANDLER_JETTY_8, getClass().getClassLoader())) {
			return new Jetty8GzipHandlerFactory().createGzipHandler(getCompression());
		}
		if (ClassUtils.isPresent(GZIP_HANDLER_JETTY_9_3, getClass().getClassLoader())) {
			return new Jetty93GzipHandlerFactory().createGzipHandler(getCompression());
		}
		throw new IllegalStateException(
				"Compression is enabled, but GzipHandler is not on the classpath");
	}

	private SslServerConnectorFactory getSslServerConnectorFactory() {
		if (ClassUtils.isPresent("org.eclipse.jetty.server.ssl.SslSocketConnector",
				null)) {
			return new Jetty8SslServerConnectorFactory();
		}
		return new Jetty9SslServerConnectorFactory();
	}

	/**
	 * Configure the SSL connection.
	 * @param factory the Jetty {@link SslContextFactory}.
	 * @param ssl the ssl details.
	 */
	protected void configureSsl(SslContextFactory factory, Ssl ssl) {
		factory.setProtocol(ssl.getProtocol());
		configureSslClientAuth(factory, ssl);
		configureSslPasswords(factory, ssl);
		factory.setCertAlias(ssl.getKeyAlias());
		if (!ObjectUtils.isEmpty(ssl.getCiphers())) {
			factory.setIncludeCipherSuites(ssl.getCiphers());
			factory.setExcludeCipherSuites();
		}
		if (ssl.getEnabledProtocols() != null) {
			factory.setIncludeProtocols(ssl.getEnabledProtocols());
		}
		if (getSslStoreProvider() != null) {
			try {
				factory.setKeyStore(getSslStoreProvider().getKeyStore());
				factory.setTrustStore(getSslStoreProvider().getTrustStore());
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to set SSL store", ex);
			}
		}
		else {
			configureSslKeyStore(factory, ssl);
			configureSslTrustStore(factory, ssl);
		}
	}

	private void configureSslClientAuth(SslContextFactory factory, Ssl ssl) {
		if (ssl.getClientAuth() == ClientAuth.NEED) {
			factory.setNeedClientAuth(true);
			factory.setWantClientAuth(true);
		}
		else if (ssl.getClientAuth() == ClientAuth.WANT) {
			factory.setWantClientAuth(true);
		}
	}

	private void configureSslPasswords(SslContextFactory factory, Ssl ssl) {
		if (ssl.getKeyStorePassword() != null) {
			factory.setKeyStorePassword(ssl.getKeyStorePassword());
		}
		if (ssl.getKeyPassword() != null) {
			factory.setKeyManagerPassword(ssl.getKeyPassword());
		}
	}

	private void configureSslKeyStore(SslContextFactory factory, Ssl ssl) {
		try {
			URL url = ResourceUtils.getURL(ssl.getKeyStore());
			factory.setKeyStoreResource(Resource.newResource(url));
		}
		catch (IOException ex) {
			throw new EmbeddedServletContainerException(
					"Could not find key store '" + ssl.getKeyStore() + "'", ex);
		}
		if (ssl.getKeyStoreType() != null) {
			factory.setKeyStoreType(ssl.getKeyStoreType());
		}
		if (ssl.getKeyStoreProvider() != null) {
			factory.setKeyStoreProvider(ssl.getKeyStoreProvider());
		}
	}

	private void configureSslTrustStore(SslContextFactory factory, Ssl ssl) {
		if (ssl.getTrustStorePassword() != null) {
			factory.setTrustStorePassword(ssl.getTrustStorePassword());
		}
		if (ssl.getTrustStore() != null) {
			try {
				URL url = ResourceUtils.getURL(ssl.getTrustStore());
				factory.setTrustStoreResource(Resource.newResource(url));
			}
			catch (IOException ex) {
				throw new EmbeddedServletContainerException(
						"Could not find trust store '" + ssl.getTrustStore() + "'", ex);
			}
		}
		if (ssl.getTrustStoreType() != null) {
			factory.setTrustStoreType(ssl.getTrustStoreType());
		}
		if (ssl.getTrustStoreProvider() != null) {
			factory.setTrustStoreProvider(ssl.getTrustStoreProvider());
		}
	}

	/**
	 * Configure the given Jetty {@link WebAppContext} for use.
	 * @param context the context to configure
	 * @param initializers the set of initializers to apply
	 */
	protected final void configureWebAppContext(WebAppContext context,
			ServletContextInitializer... initializers) {
		Assert.notNull(context, "Context must not be null");
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
		ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
		Configuration[] configurations = getWebAppContextConfigurations(context,
				initializersToUse);
		context.setConfigurations(configurations);
		configureSession(context);
		postProcessWebAppContext(context);
	}

	private void configureSession(WebAppContext context) {
		SessionManager sessionManager = context.getSessionHandler().getSessionManager();
		int sessionTimeout = (getSessionTimeout() > 0 ? getSessionTimeout() : -1);
		sessionManager.setMaxInactiveInterval(sessionTimeout);
		if (isPersistSession()) {
			Assert.isInstanceOf(HashSessionManager.class, sessionManager,
					"Unable to use persistent sessions");
			configurePersistSession(sessionManager);
		}
	}

	private void configurePersistSession(SessionManager sessionManager) {
		try {
			((HashSessionManager) sessionManager)
					.setStoreDirectory(getValidSessionStoreDir());
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private File getTempDirectory() {
		String temp = System.getProperty("java.io.tmpdir");
		return (temp == null ? null : new File(temp));
	}

	private void configureDocumentRoot(WebAppContext handler) {
		File root = getValidDocumentRoot();
		root = (root != null ? root : createTempDir("jetty-docbase"));
		try {
			if (!root.isDirectory()) {
				Resource resource = JarResource
						.newJarResource(Resource.newResource(root));
				handler.setBaseResource(resource);
			}
			else {
				handler.setBaseResource(Resource.newResource(root.getCanonicalFile()));
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
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
		holder.setClassName(getJspServlet().getClassName());
		holder.setInitParameter("fork", "false");
		holder.setInitParameters(getJspServlet().getInitParameters());
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
		configurations.add(
				getServletContextInitializerConfiguration(webAppContext, initializers));
		configurations.addAll(getConfigurations());
		configurations.add(getErrorPageConfiguration());
		configurations.add(getMimeTypeConfiguration());
		return configurations.toArray(new Configuration[configurations.size()]);
	}

	/**
	 * Create a configuration object that adds error handlers.
	 * @return a configuration object for adding error pages
	 */
	private Configuration getErrorPageConfiguration() {
		return new AbstractConfiguration() {

			@Override
			public void configure(WebAppContext context) throws Exception {
				ErrorHandler errorHandler = context.getErrorHandler();
				context.setErrorHandler(new JettyEmbeddedErrorHandler(errorHandler));
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
					mimeTypes.addMimeMapping(mapping.getExtension(),
							mapping.getMimeType());
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
	protected Configuration getServletContextInitializerConfiguration(
			WebAppContext webAppContext, ServletContextInitializer... initializers) {
		return new ServletContextInitializerConfiguration(initializers);
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
	 * Factory method called to create the {@link JettyEmbeddedServletContainer} .
	 * Subclasses can override this method to return a different
	 * {@link JettyEmbeddedServletContainer} or apply additional processing to the Jetty
	 * server.
	 * @param server the Jetty server.
	 * @return a new {@link JettyEmbeddedServletContainer} instance
	 */
	protected JettyEmbeddedServletContainer getJettyEmbeddedServletContainer(
			Server server) {
		return new JettyEmbeddedServletContainer(server, getPort() >= 0);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Set if x-forward-* headers should be processed.
	 * @param useForwardHeaders if x-forward headers should be used
	 * @since 1.3.0
	 */
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	/**
	 * Set the number of acceptor threads to use.
	 * @param acceptors the number of acceptor threads to use
	 * @since 1.4.0
	 */
	public void setAcceptors(int acceptors) {
		this.acceptors = acceptors;
	}

	/**
	 * Set the number of selector threads to use.
	 * @param selectors the number of selector threads to use
	 * @since 1.4.0
	 */
	public void setSelectors(int selectors) {
		this.selectors = selectors;
	}

	/**
	 * Sets {@link JettyServerCustomizer}s that will be applied to the {@link Server}
	 * before it is started. Calling this method will replace any existing configurations.
	 * @param customizers the Jetty customizers to apply
	 */
	public void setServerCustomizers(
			Collection<? extends JettyServerCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.jettyServerCustomizers = new ArrayList<JettyServerCustomizer>(customizers);
	}

	/**
	 * Returns a mutable collection of Jetty {@link Configuration}s that will be applied
	 * to the {@link WebAppContext} before the server is created.
	 * @return the Jetty {@link Configuration}s
	 */
	public Collection<JettyServerCustomizer> getServerCustomizers() {
		return this.jettyServerCustomizers;
	}

	/**
	 * Add {@link JettyServerCustomizer}s that will be applied to the {@link Server}
	 * before it is started.
	 * @param customizers the customizers to add
	 */
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

	private void addJettyErrorPages(ErrorHandler errorHandler,
			Collection<ErrorPage> errorPages) {
		if (errorHandler instanceof ErrorPageErrorHandler) {
			ErrorPageErrorHandler handler = (ErrorPageErrorHandler) errorHandler;
			for (ErrorPage errorPage : errorPages) {
				if (errorPage.isGlobal()) {
					handler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE,
							errorPage.getPath());
				}
				else {
					if (errorPage.getExceptionName() != null) {
						handler.addErrorPage(errorPage.getExceptionName(),
								errorPage.getPath());
					}
					else {
						handler.addErrorPage(errorPage.getStatusCode(),
								errorPage.getPath());
					}
				}
			}
		}
	}

	/**
	 * Factory to create the SSL {@link ServerConnector}.
	 */
	private interface SslServerConnectorFactory {

		AbstractConnector getConnector(Server server, SslContextFactory sslContextFactory,
				int port);

	}

	/**
	 * {@link SslServerConnectorFactory} for Jetty 9.
	 */
	private static class Jetty9SslServerConnectorFactory
			implements SslServerConnectorFactory {

		@Override
		public ServerConnector getConnector(Server server,
				SslContextFactory sslContextFactory, int port) {
			HttpConfiguration config = new HttpConfiguration();
			config.addCustomizer(new SecureRequestCustomizer());
			HttpConnectionFactory connectionFactory = new HttpConnectionFactory(config);
			SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(
					sslContextFactory, HttpVersion.HTTP_1_1.asString());
			ServerConnector serverConnector = new ServerConnector(server,
					sslConnectionFactory, connectionFactory);
			serverConnector.setPort(port);
			return serverConnector;
		}
	}

	/**
	 * {@link SslServerConnectorFactory} for Jetty 8.
	 */
	private static class Jetty8SslServerConnectorFactory
			implements SslServerConnectorFactory {

		@Override
		public AbstractConnector getConnector(Server server,
				SslContextFactory sslContextFactory, int port) {
			try {
				Class<?> connectorClass = Class
						.forName("org.eclipse.jetty.server.ssl.SslSocketConnector");
				AbstractConnector connector = (AbstractConnector) connectorClass
						.getConstructor(SslContextFactory.class)
						.newInstance(sslContextFactory);
				connector.getClass().getMethod("setPort", int.class).invoke(connector,
						port);
				return connector;
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

	private interface GzipHandlerFactory {

		HandlerWrapper createGzipHandler(Compression compression);

	}

	private static class Jetty8GzipHandlerFactory implements GzipHandlerFactory {

		@Override
		public HandlerWrapper createGzipHandler(Compression compression) {
			try {
				Class<?> handlerClass = ClassUtils.forName(GZIP_HANDLER_JETTY_8,
						getClass().getClassLoader());
				HandlerWrapper handler = (HandlerWrapper) handlerClass.newInstance();
				ReflectionUtils.findMethod(handlerClass, "setMinGzipSize", int.class)
						.invoke(handler, compression.getMinResponseSize());
				ReflectionUtils.findMethod(handlerClass, "setMimeTypes", Set.class)
						.invoke(handler, new HashSet<String>(
								Arrays.asList(compression.getMimeTypes())));
				if (compression.getExcludedUserAgents() != null) {
					ReflectionUtils.findMethod(handlerClass, "setExcluded", Set.class)
							.invoke(handler, new HashSet<String>(
									Arrays.asList(compression.getExcludedUserAgents())));
				}
				return handler;
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to configure Jetty 8 gzip handler",
						ex);
			}
		}

	}

	private static class Jetty92GzipHandlerFactory implements GzipHandlerFactory {

		@Override
		public HandlerWrapper createGzipHandler(Compression compression) {
			try {
				Class<?> handlerClass = ClassUtils.forName(GZIP_HANDLER_JETTY_9_2,
						getClass().getClassLoader());
				HandlerWrapper gzipHandler = (HandlerWrapper) handlerClass.newInstance();
				ReflectionUtils.findMethod(handlerClass, "setMinGzipSize", int.class)
						.invoke(gzipHandler, compression.getMinResponseSize());
				ReflectionUtils
						.findMethod(handlerClass, "addIncludedMimeTypes", String[].class)
						.invoke(gzipHandler, new Object[] { compression.getMimeTypes() });
				if (compression.getExcludedUserAgents() != null) {
					ReflectionUtils.findMethod(handlerClass, "setExcluded", Set.class)
							.invoke(gzipHandler, new HashSet<String>(
									Arrays.asList(compression.getExcludedUserAgents())));
				}
				return gzipHandler;
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to configure Jetty 9.2 gzip handler",
						ex);
			}
		}

	}

	private static class Jetty93GzipHandlerFactory implements GzipHandlerFactory {

		@Override
		public HandlerWrapper createGzipHandler(Compression compression) {
			GzipHandler handler = new GzipHandler();
			handler.setMinGzipSize(compression.getMinResponseSize());
			handler.setIncludedMimeTypes(compression.getMimeTypes());
			if (compression.getExcludedUserAgents() != null) {
				handler.setExcludedAgentPatterns(compression.getExcludedUserAgents());
			}
			return handler;
		}

	}

	/**
	 * {@link JettyServerCustomizer} to add {@link ForwardedRequestCustomizer}. Only
	 * supported with Jetty 9 (hence the inner class)
	 */
	private static class ForwardHeadersCustomizer implements JettyServerCustomizer {

		@Override
		public void customize(Server server) {
			ForwardedRequestCustomizer customizer = new ForwardedRequestCustomizer();
			for (Connector connector : server.getConnectors()) {
				for (ConnectionFactory connectionFactory : connector
						.getConnectionFactories()) {
					if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
						((HttpConfiguration.ConnectionFactory) connectionFactory)
								.getHttpConfiguration().addCustomizer(customizer);
					}
				}
			}
		}

	}

	/**
	 * {@link HandlerWrapper} to add a custom {@code server} header.
	 */
	private static class ServerHeaderHandler extends HandlerWrapper {

		private static final String SERVER_HEADER = "server";

		private final String value;

		ServerHeaderHandler(String value) {
			this.value = value;
		}

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request,
				HttpServletResponse response) throws IOException, ServletException {
			if (!response.getHeaderNames().contains(SERVER_HEADER)) {
				response.setHeader(SERVER_HEADER, this.value);
			}
			super.handle(target, baseRequest, request, response);
		}

	}

	private interface ConnectorFactory {

		AbstractConnector createConnector(Server server, InetSocketAddress address,
				int acceptors, int selectors);

	}

	private static class Jetty8ConnectorFactory implements ConnectorFactory {

		@Override
		public AbstractConnector createConnector(Server server, InetSocketAddress address,
				int acceptors, int selectors) {
			try {
				Class<?> connectorClass = ClassUtils.forName(CONNECTOR_JETTY_8,
						getClass().getClassLoader());
				AbstractConnector connector = (AbstractConnector) connectorClass
						.newInstance();
				ReflectionUtils.findMethod(connectorClass, "setPort", int.class)
						.invoke(connector, address.getPort());
				ReflectionUtils.findMethod(connectorClass, "setHost", String.class)
						.invoke(connector, address.getHostName());
				if (acceptors > 0) {
					ReflectionUtils.findMethod(connectorClass, "setAcceptors", int.class)
							.invoke(connector, acceptors);
				}
				if (selectors > 0) {
					Object selectorManager = ReflectionUtils
							.findMethod(connectorClass, "getSelectorManager")
							.invoke(connector);
					ReflectionUtils.findMethod(selectorManager.getClass(),
							"setSelectSets", int.class)
							.invoke(selectorManager, selectors);
				}

				return connector;
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to configure Jetty 8 connector", ex);
			}
		}

	}

	private static class Jetty9ConnectorFactory implements ConnectorFactory {

		@Override
		public AbstractConnector createConnector(Server server, InetSocketAddress address,
				int acceptors, int selectors) {
			ServerConnector connector = new ServerConnector(server, acceptors, selectors);
			connector.setHost(address.getHostName());
			connector.setPort(address.getPort());
			for (ConnectionFactory connectionFactory : connector
					.getConnectionFactories()) {
				if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
					((HttpConfiguration.ConnectionFactory) connectionFactory)
							.getHttpConfiguration().setSendServerVersion(false);
				}
			}
			return connector;
		}

	}

	private interface ServerFactory {

		Server createServer(ThreadPool threadPool);

	}

	private static class Jetty8ServerFactory implements ServerFactory {

		@Override
		public Server createServer(ThreadPool threadPool) {
			Server server = new Server();
			try {
				ReflectionUtils
						.findMethod(Server.class, "setThreadPool", ThreadPool.class)
						.invoke(server, threadPool);
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to configure Jetty 8 ThreadPool", ex);
			}
			try {
				ReflectionUtils
						.findMethod(Server.class, "setSendServerVersion", boolean.class)
						.invoke(server, false);
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to disable Server header", ex);
			}
			return server;
		}

	}

	private static class Jetty9ServerFactory implements ServerFactory {

		@Override
		public Server createServer(ThreadPool threadPool) {
			return new Server(threadPool);
		}

	}

}
