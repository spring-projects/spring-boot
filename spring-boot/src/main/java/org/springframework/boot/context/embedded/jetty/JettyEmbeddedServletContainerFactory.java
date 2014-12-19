/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.embedded.jetty;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.Ssl.ClientAuth;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
 * @see #setPort(int)
 * @see #setConfigurations(Collection)
 * @see JettyEmbeddedServletContainer
 */
public class JettyEmbeddedServletContainerFactory extends
		AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {

	private List<Configuration> configurations = new ArrayList<Configuration>();

	private List<JettyServerCustomizer> jettyServerCustomizers = new ArrayList<JettyServerCustomizer>();

	private ResourceLoader resourceLoader;

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
		JettyEmbeddedWebAppContext context = new JettyEmbeddedWebAppContext();
		int port = (getPort() >= 0 ? getPort() : 0);
		Server server = new Server(new InetSocketAddress(getAddress(), port));
		configureWebAppContext(context, initializers);
		server.setHandler(context);
		this.logger.info("Server initialized with port: " + port);
		if (getSsl() != null) {
			SslContextFactory sslContextFactory = new SslContextFactory();
			configureSsl(sslContextFactory, getSsl());
			AbstractConnector connector = getSslServerConnectorFactory().getConnector(
					server, sslContextFactory, port);
			server.setConnectors(new Connector[] { connector });
		}
		for (JettyServerCustomizer customizer : getServerCustomizers()) {
			customizer.customize(server);
		}
		return getJettyEmbeddedServletContainer(server);
	}

	private SslServerConnectorFactory getSslServerConnectorFactory() {
		if (ClassUtils.isPresent("org.eclipse.jetty.server.ssl.SslSocketConnector", null)) {
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
		configureSslKeyStore(factory, ssl);
		if (ssl.getCiphers() != null) {
			factory.setIncludeCipherSuites(ssl.getCiphers());
		}
		configureSslTrustStore(factory, ssl);
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
			throw new EmbeddedServletContainerException("Could not find key store '"
					+ ssl.getKeyStore() + "'", ex);
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
		configureDocumentRoot(context);
		if (isRegisterDefaultServlet()) {
			addDefaultServlet(context);
		}
		if (isRegisterJspServlet()
				&& ClassUtils.isPresent(getJspServletClassName(), getClass()
						.getClassLoader())) {
			addJspServlet(context);
		}
		ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
		Configuration[] configurations = getWebAppContextConfigurations(context,
				initializersToUse);
		context.setConfigurations(configurations);
		int sessionTimeout = (getSessionTimeout() > 0 ? getSessionTimeout() : -1);
		SessionManager sessionManager = context.getSessionHandler().getSessionManager();
		sessionManager.setMaxInactiveInterval(sessionTimeout);
		postProcessWebAppContext(context);
	}

	private File getTempDirectory() {
		String temp = System.getProperty("java.io.tmpdir");
		return (temp == null ? null : new File(temp));
	}

	private void configureDocumentRoot(WebAppContext handler) {
		File root = getValidDocumentRoot();
		if (root != null) {
			try {
				if (!root.isDirectory()) {
					Resource resource = Resource.newResource("jar:" + root.toURI() + "!");
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
		configurations.add(getMimeTypeConfiguration());
		return configurations.toArray(new Configuration[configurations.size()]);
	}

	/**
	 * Create a configuration object that adds error handlers
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
	 * Create a configuration object that adds mime type mappings
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
	 * Factory method called to create the {@link JettyEmbeddedServletContainer}.
	 * Subclasses can override this method to return a different
	 * {@link JettyEmbeddedServletContainer} or apply additional processing to the Jetty
	 * server.
	 * @param server the Jetty server.
	 * @return a new {@link JettyEmbeddedServletContainer} instance
	 */
	protected JettyEmbeddedServletContainer getJettyEmbeddedServletContainer(Server server) {
		return new JettyEmbeddedServletContainer(server, getPort() >= 0);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
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
	private static interface SslServerConnectorFactory {

		AbstractConnector getConnector(Server server,
				SslContextFactory sslContextFactory, int port);

	}

	/**
	 * {@link SslServerConnectorFactory} for Jetty 9.
	 */
	private static class Jetty9SslServerConnectorFactory implements
			SslServerConnectorFactory {

		@Override
		public ServerConnector getConnector(Server server,
				SslContextFactory sslContextFactory, int port) {
			ServerConnector serverConnector = new ServerConnector(server,
					new SslConnectionFactory(sslContextFactory,
							HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory());
			serverConnector.setPort(port);
			return serverConnector;
		}
	}

	/**
	 * {@link SslServerConnectorFactory} for Jetty 8.
	 */
	private static class Jetty8SslServerConnectorFactory implements
			SslServerConnectorFactory {

		@Override
		public AbstractConnector getConnector(Server server,
				SslContextFactory sslContextFactory, int port) {
			try {
				Class<?> connectorClass = Class
						.forName("org.eclipse.jetty.server.ssl.SslSocketConnector");
				AbstractConnector connector = (AbstractConnector) connectorClass
						.getConstructor(SslContextFactory.class).newInstance(
								sslContextFactory);
				connector.getClass().getMethod("setPort", int.class)
						.invoke(connector, port);
				return connector;
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
