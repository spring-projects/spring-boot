/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.web.server.servlet.jetty;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpCookieUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.session.DefaultSessionCache;
import org.eclipse.jetty.session.FileSessionDataStore;
import org.eclipse.jetty.session.SessionConfig;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.URLResourceFactory;

import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.server.jetty.ForwardHeadersCustomizer;
import org.springframework.boot.web.server.jetty.JettyServerCustomizer;
import org.springframework.boot.web.server.jetty.JettyWebServer;
import org.springframework.boot.web.server.jetty.JettyWebServerFactory;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ContextPath;
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.boot.web.server.servlet.DocumentRoot;
import org.springframework.boot.web.server.servlet.ServletContextInitializer;
import org.springframework.boot.web.server.servlet.ServletContextInitializers;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerSettings;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
 * @since 4.0.0
 * @see #setPort(int)
 * @see #setConfigurations(Collection)
 * @see JettyWebServer
 */
public class JettyServletWebServerFactory extends JettyWebServerFactory
		implements ConfigurableJettyWebServerFactory, ConfigurableServletWebServerFactory, ResourceLoaderAware {

	private static final Log logger = LogFactory.getLog(JettyServletWebServerFactory.class);

	private final ServletWebServerSettings settings = new ServletWebServerSettings();

	private ResourceLoader resourceLoader;

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
		super(port);
		getSettings().setContextPath(ContextPath.of(contextPath));
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
		logger.info("Server initialized with port: " + port);
		if (this.getMaxConnections() > -1) {
			server.addBean(new ConnectionLimit(this.getMaxConnections(), server.getConnectors()));
		}
		if (Ssl.isEnabled(getSsl())) {
			customizeSsl(server, address);
		}
		for (JettyServerCustomizer customizer : getServerCustomizers()) {
			customizer.customize(server);
		}
		if (this.isUseForwardHeaders()) {
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
		for (MimeMappings.Mapping mapping : getSettings().getMimeMappings()) {
			mimeTypes.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
		}
		return server;
	}

	@Override
	protected Handler addHandlerWrappers(Handler handler) {
		handler = super.addHandlerWrappers(handler);
		if (!CollectionUtils.isEmpty(getSettings().getCookieSameSiteSuppliers())) {
			handler = applyWrapper(handler, new SuppliedSameSiteCookieHandlerWrapper(getSessionCookieName(),
					getSettings().getCookieSameSiteSuppliers()));
		}
		return handler;
	}

	private String getSessionCookieName() {
		String name = getSettings().getSession().getCookie().getName();
		return (name != null) ? name : SessionConfig.__DefaultSessionCookie;
	}

	/**
	 * Configure the given Jetty {@link WebAppContext} for use.
	 * @param context the context to configure
	 * @param initializers the set of initializers to apply
	 */
	protected final void configureWebAppContext(WebAppContext context, ServletContextInitializer... initializers) {
		Assert.notNull(context, "'context' must not be null");
		context.clearAliasChecks();
		if (this.resourceLoader != null) {
			context.setClassLoader(this.resourceLoader.getClassLoader());
		}
		String contextPath = getSettings().getContextPath().toString();
		context.setContextPath(StringUtils.hasLength(contextPath) ? contextPath : "/");
		context.setDisplayName(getSettings().getDisplayName());
		configureDocumentRoot(context);
		if (getSettings().isRegisterDefaultServlet()) {
			addDefaultServlet(context);
		}
		if (shouldRegisterJspServlet()) {
			addJspServlet(context);
			context.addBean(new JasperInitializer(context), true);
		}
		addLocaleMappings(context);
		ServletContextInitializers initializersToUse = ServletContextInitializers.from(this.settings, initializers);
		Configuration[] configurations = getWebAppContextConfigurations(context, initializersToUse);
		context.setConfigurations(configurations);
		context.setThrowUnavailableOnStartupException(true);
		configureSession(context);
		context.setTempDirectory(getTempDirectory(context));
		postProcessWebAppContext(context);
	}

	private boolean shouldRegisterJspServlet() {
		return this.settings.getJsp() != null && this.settings.getJsp().getRegistered()
				&& ClassUtils.isPresent(this.settings.getJsp().getClassName(), getClass().getClassLoader());
	}

	private void configureSession(WebAppContext context) {
		SessionHandler handler = context.getSessionHandler();
		SameSite sessionSameSite = getSettings().getSession().getCookie().getSameSite();
		if (sessionSameSite != null) {
			handler.setSameSite(HttpCookie.SameSite.valueOf(sessionSameSite.name()));
		}
		Duration sessionTimeout = getSettings().getSession().getTimeout();
		handler.setMaxInactiveInterval(isNegative(sessionTimeout) ? -1 : (int) sessionTimeout.getSeconds());
		if (getSettings().getSession().isPersistent()) {
			DefaultSessionCache cache = new DefaultSessionCache(handler);
			FileSessionDataStore store = new FileSessionDataStore();
			store.setStoreDir(getSettings().getSession().getSessionStoreDirectory().getValidDirectory(true));
			cache.setSessionDataStore(store);
			handler.setSessionCache(cache);
		}
	}

	private boolean isNegative(Duration sessionTimeout) {
		return sessionTimeout == null || sessionTimeout.isNegative();
	}

	private void addLocaleMappings(WebAppContext context) {
		getSettings().getLocaleCharsetMappings()
			.forEach((locale, charset) -> context.addLocaleEncoding(locale.toString(), charset.toString()));
	}

	private File getTempDirectory(WebAppContext context) {
		String temp = System.getProperty("java.io.tmpdir");
		return (temp != null) ? new File(temp, getTempDirectoryPrefix(context) + UUID.randomUUID()) : null;
	}

	@SuppressWarnings("removal")
	private String getTempDirectoryPrefix(WebAppContext context) {
		try {
			return ((JettyEmbeddedWebAppContext) context).getCanonicalNameForTmpDir();
		}
		catch (Throwable ex) {
			return WebInfConfiguration.getCanonicalNameForWebAppTmpDir(context);
		}
	}

	private void configureDocumentRoot(WebAppContext handler) {
		DocumentRoot documentRoot = new DocumentRoot(logger);
		documentRoot.setDirectory(this.settings.getDocumentRoot());
		File root = documentRoot.getValidDirectory();
		File docBase = (root != null) ? root : createTempDir("jetty-docbase");
		try {
			ResourceFactory resourceFactory = handler.getResourceFactory();
			List<Resource> resources = new ArrayList<>();
			Resource rootResource = (docBase.isDirectory()
					? resourceFactory.newResource(docBase.getCanonicalFile().toURI())
					: resourceFactory.newJarFileResource(docBase.toURI()));
			resources.add((root != null) ? new LoaderHidingResource(rootResource, rootResource) : rootResource);
			URLResourceFactory urlResourceFactory = new URLResourceFactory();
			for (URL resourceJarUrl : getSettings().getStaticResourceUrls()) {
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
		Assert.notNull(context, "'context' must not be null");
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
		Assert.notNull(context, "'context' must not be null");
		ServletHolder holder = new ServletHolder();
		holder.setName("jsp");
		holder.setClassName(this.settings.getJsp().getClassName());
		holder.setInitParameter("fork", "false");
		holder.setInitParameters(this.settings.getJsp().getInitParameters());
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
			ServletContextInitializers initializers) {
		List<Configuration> configurations = new ArrayList<>();
		configurations.add(getServletContextInitializerConfiguration(webAppContext, initializers));
		configurations.add(getErrorPageConfiguration());
		configurations.add(getMimeTypeConfiguration());
		configurations.add(new WebListenersConfiguration(getSettings().getWebListenerClassNames()));
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
				for (MimeMappings.Mapping mapping : getSettings().getMimeMappings()) {
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
			ServletContextInitializers initializers) {
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
		return new JettyServletWebServer(server, getPort() >= 0);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
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

	@Override
	public ServletWebServerSettings getSettings() {
		return this.settings;
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

		private final String sessionCookieName;

		private final List<? extends CookieSameSiteSupplier> suppliers;

		SuppliedSameSiteCookieHandlerWrapper(String sessionCookieName,
				List<? extends CookieSameSiteSupplier> suppliers) {
			this.sessionCookieName = sessionCookieName;
			this.suppliers = suppliers;
		}

		@Override
		public boolean handle(Request request, Response response, Callback callback) throws Exception {
			SuppliedSameSiteCookieResponse wrappedResponse = new SuppliedSameSiteCookieResponse(request, response);
			return super.handle(request, wrappedResponse, callback);
		}

		private class SuppliedSameSiteCookieResponse extends Response.Wrapper {

			private final HttpFields.Mutable wrappedHeaders;

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
				if (cookie == null || isSessionCookie(cookie)) {
					return field;
				}
				SameSite sameSite = getSameSite(cookie);
				if (sameSite == null) {
					return field;
				}
				HttpCookie updatedCookie = buildCookieWithUpdatedSameSite(cookie, sameSite);
				return new HttpCookieUtils.SetCookieHttpField(updatedCookie, this.compliance);
			}

			private boolean isSessionCookie(HttpCookie cookie) {
				return SuppliedSameSiteCookieHandlerWrapper.this.sessionCookieName.equals(cookie.getName());
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
