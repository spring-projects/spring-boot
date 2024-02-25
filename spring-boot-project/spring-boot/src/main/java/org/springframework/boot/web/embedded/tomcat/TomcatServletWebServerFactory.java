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

package org.springframework.boot.web.embedded.tomcat;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.Valve;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.catalina.util.LifecycleBase;
import org.apache.catalina.util.SessionConfig;
import org.apache.catalina.webresources.AbstractResourceSet;
import org.apache.catalina.webresources.EmptyResource;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.scan.StandardJarScanFilter;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.NativeDetector;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractServletWebServerFactory} that can be used to create
 * {@link TomcatWebServer}s. Can be initialized using Spring's
 * {@link ServletContextInitializer}s or Tomcat {@link LifecycleListener}s.
 * <p>
 * Unless explicitly configured otherwise this factory will create containers that listen
 * for HTTP requests on port 8080.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Brock Mills
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoffer Sawicki
 * @author Dawid Antecki
 * @author Moritz Halbritter
 * @since 2.0.0
 * @see #setPort(int)
 * @see #setContextLifecycleListeners(Collection)
 * @see TomcatWebServer
 */
public class TomcatServletWebServerFactory extends AbstractServletWebServerFactory
		implements ConfigurableTomcatWebServerFactory, ResourceLoaderAware {

	private static final Log logger = LogFactory.getLog(TomcatServletWebServerFactory.class);

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Set<Class<?>> NO_CLASSES = Collections.emptySet();

	/**
	 * The class name of default protocol used.
	 */
	public static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

	private File baseDirectory;

	private List<Valve> engineValves = new ArrayList<>();

	private List<Valve> contextValves = new ArrayList<>();

	private List<LifecycleListener> contextLifecycleListeners = new ArrayList<>();

	private final List<LifecycleListener> serverLifecycleListeners = getDefaultServerLifecycleListeners();

	private Set<TomcatContextCustomizer> tomcatContextCustomizers = new LinkedHashSet<>();

	private Set<TomcatConnectorCustomizer> tomcatConnectorCustomizers = new LinkedHashSet<>();

	private Set<TomcatProtocolHandlerCustomizer<?>> tomcatProtocolHandlerCustomizers = new LinkedHashSet<>();

	private final List<Connector> additionalTomcatConnectors = new ArrayList<>();

	private ResourceLoader resourceLoader;

	private String protocol = DEFAULT_PROTOCOL;

	private Set<String> tldSkipPatterns = new LinkedHashSet<>(TldPatterns.DEFAULT_SKIP);

	private final Set<String> tldScanPatterns = new LinkedHashSet<>(TldPatterns.DEFAULT_SCAN);

	private Charset uriEncoding = DEFAULT_CHARSET;

	private int backgroundProcessorDelay;

	private boolean disableMBeanRegistry = true;

	/**
	 * Create a new {@link TomcatServletWebServerFactory} instance.
	 */
	public TomcatServletWebServerFactory() {
	}

	/**
	 * Create a new {@link TomcatServletWebServerFactory} that listens for requests using
	 * the specified port.
	 * @param port the port to listen on
	 */
	public TomcatServletWebServerFactory(int port) {
		super(port);
	}

	/**
	 * Create a new {@link TomcatServletWebServerFactory} with the specified context path
	 * and port.
	 * @param contextPath the root context path
	 * @param port the port to listen on
	 */
	public TomcatServletWebServerFactory(String contextPath, int port) {
		super(contextPath, port);
	}

	/**
	 * Returns the default server lifecycle listeners for the
	 * TomcatServletWebServerFactory.
	 * @return the list of default server lifecycle listeners
	 */
	private static List<LifecycleListener> getDefaultServerLifecycleListeners() {
		ArrayList<LifecycleListener> lifecycleListeners = new ArrayList<>();
		if (!NativeDetector.inNativeImage()) {
			AprLifecycleListener aprLifecycleListener = new AprLifecycleListener();
			if (AprLifecycleListener.isAprAvailable()) {
				lifecycleListeners.add(aprLifecycleListener);
			}
		}
		return lifecycleListeners;
	}

	/**
	 * Returns a WebServer instance configured with the given ServletContextInitializers.
	 * If the MBeanRegistry is disabled, it will be disabled in the Registry. Creates a
	 * Tomcat instance and sets the base directory to the provided baseDirectory or a
	 * temporary directory if baseDirectory is null. Adds the serverLifecycleListeners to
	 * the Tomcat server's LifecycleListeners. Creates a Connector with the provided
	 * protocol and sets throwOnFailure to true. Adds the Connector to the Tomcat service
	 * and customizes it. Sets the Connector as the Tomcat instance's Connector. Registers
	 * the Connector's Executor with the Tomcat instance. Disables auto deployment on the
	 * Tomcat host. Configures the Tomcat engine. Adds additional Tomcat Connectors to the
	 * Tomcat service and registers their Executors. Prepares the Tomcat host's context
	 * with the provided initializers. Returns a WebServer instance created from the
	 * configured Tomcat instance.
	 * @param initializers the ServletContextInitializers to configure the WebServer with
	 * @return a WebServer instance configured with the given ServletContextInitializers
	 */
	@Override
	public WebServer getWebServer(ServletContextInitializer... initializers) {
		if (this.disableMBeanRegistry) {
			Registry.disableRegistry();
		}
		Tomcat tomcat = new Tomcat();
		File baseDir = (this.baseDirectory != null) ? this.baseDirectory : createTempDir("tomcat");
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		for (LifecycleListener listener : this.serverLifecycleListeners) {
			tomcat.getServer().addLifecycleListener(listener);
		}
		Connector connector = new Connector(this.protocol);
		connector.setThrowOnFailure(true);
		tomcat.getService().addConnector(connector);
		customizeConnector(connector);
		tomcat.setConnector(connector);
		registerConnectorExecutor(tomcat, connector);
		tomcat.getHost().setAutoDeploy(false);
		configureEngine(tomcat.getEngine());
		for (Connector additionalConnector : this.additionalTomcatConnectors) {
			tomcat.getService().addConnector(additionalConnector);
			registerConnectorExecutor(tomcat, additionalConnector);
		}
		prepareContext(tomcat.getHost(), initializers);
		return getTomcatWebServer(tomcat);
	}

	/**
	 * Registers the given Connector's Executor with the provided Tomcat instance.
	 * @param tomcat the Tomcat instance to register the Executor with
	 * @param connector the Connector whose Executor needs to be registered
	 */
	private void registerConnectorExecutor(Tomcat tomcat, Connector connector) {
		if (connector.getProtocolHandler().getExecutor() instanceof Executor executor) {
			tomcat.getService().addExecutor(executor);
		}
	}

	/**
	 * Configures the engine with the specified background processor delay and valves.
	 * @param engine the engine to be configured
	 */
	private void configureEngine(Engine engine) {
		engine.setBackgroundProcessorDelay(this.backgroundProcessorDelay);
		for (Valve valve : this.engineValves) {
			engine.getPipeline().addValve(valve);
		}
	}

	/**
	 * Prepares the context for the given host and servlet context initializers.
	 * @param host the host for the context
	 * @param initializers the servlet context initializers
	 */
	protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
		File documentRoot = getValidDocumentRoot();
		TomcatEmbeddedContext context = new TomcatEmbeddedContext();
		if (documentRoot != null) {
			context.setResources(new LoaderHidingResourceRoot(context));
		}
		context.setName(getContextPath());
		context.setDisplayName(getDisplayName());
		context.setPath(getContextPath());
		File docBase = (documentRoot != null) ? documentRoot : createTempDir("tomcat-docbase");
		context.setDocBase(docBase.getAbsolutePath());
		context.addLifecycleListener(new FixContextListener());
		ClassLoader parentClassLoader = (this.resourceLoader != null) ? this.resourceLoader.getClassLoader()
				: ClassUtils.getDefaultClassLoader();
		context.setParentClassLoader(parentClassLoader);
		resetDefaultLocaleMapping(context);
		addLocaleMappings(context);
		try {
			context.setCreateUploadTargets(true);
		}
		catch (NoSuchMethodError ex) {
			// Tomcat is < 8.5.39. Continue.
		}
		configureTldPatterns(context);
		WebappLoader loader = new WebappLoader();
		loader.setLoaderInstance(new TomcatEmbeddedWebappClassLoader(parentClassLoader));
		loader.setDelegate(true);
		context.setLoader(loader);
		if (isRegisterDefaultServlet()) {
			addDefaultServlet(context);
		}
		if (shouldRegisterJspServlet()) {
			addJspServlet(context);
			addJasperInitializer(context);
		}
		context.addLifecycleListener(new StaticResourceConfigurer(context));
		ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
		host.addChild(context);
		configureContext(context, initializersToUse);
		postProcessContext(context);
	}

	/**
	 * Override Tomcat's default locale mappings to align with other servers. See
	 * {@code org.apache.catalina.util.CharsetMapperDefault.properties}.
	 * @param context the context to reset
	 */
	private void resetDefaultLocaleMapping(TomcatEmbeddedContext context) {
		context.addLocaleEncodingMappingParameter(Locale.ENGLISH.toString(), DEFAULT_CHARSET.displayName());
		context.addLocaleEncodingMappingParameter(Locale.FRENCH.toString(), DEFAULT_CHARSET.displayName());
		context.addLocaleEncodingMappingParameter(Locale.JAPANESE.toString(), DEFAULT_CHARSET.displayName());
	}

	/**
	 * Adds locale mappings to the given TomcatEmbeddedContext.
	 * @param context the TomcatEmbeddedContext to add the locale mappings to
	 */
	private void addLocaleMappings(TomcatEmbeddedContext context) {
		getLocaleCharsetMappings().forEach(
				(locale, charset) -> context.addLocaleEncodingMappingParameter(locale.toString(), charset.toString()));
	}

	/**
	 * Configures the TLD patterns for the given TomcatEmbeddedContext.
	 * @param context the TomcatEmbeddedContext to configure
	 */
	private void configureTldPatterns(TomcatEmbeddedContext context) {
		StandardJarScanFilter filter = new StandardJarScanFilter();
		filter.setTldSkip(StringUtils.collectionToCommaDelimitedString(this.tldSkipPatterns));
		filter.setTldScan(StringUtils.collectionToCommaDelimitedString(this.tldScanPatterns));
		context.getJarScanner().setJarScanFilter(filter);
	}

	/**
	 * Adds a default servlet to the given context.
	 * @param context the context to which the default servlet will be added
	 */
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
		context.addServletMappingDecoded("/", "default");
	}

	/**
	 * Adds a JSP servlet to the given context.
	 * @param context the context to add the JSP servlet to
	 */
	private void addJspServlet(Context context) {
		Wrapper jspServlet = context.createWrapper();
		jspServlet.setName("jsp");
		jspServlet.setServletClass(getJsp().getClassName());
		jspServlet.addInitParameter("fork", "false");
		getJsp().getInitParameters().forEach(jspServlet::addInitParameter);
		jspServlet.setLoadOnStartup(3);
		context.addChild(jspServlet);
		context.addServletMappingDecoded("*.jsp", "jsp");
		context.addServletMappingDecoded("*.jspx", "jsp");
	}

	/**
	 * Adds the JasperInitializer to the given TomcatEmbeddedContext. This initializer is
	 * responsible for initializing the Jasper JSP engine.
	 * @param context the TomcatEmbeddedContext to add the initializer to
	 */
	private void addJasperInitializer(TomcatEmbeddedContext context) {
		try {
			ServletContainerInitializer initializer = (ServletContainerInitializer) ClassUtils
				.forName("org.apache.jasper.servlet.JasperInitializer", null)
				.getDeclaredConstructor()
				.newInstance();
			context.addServletContainerInitializer(initializer, null);
		}
		catch (Exception ex) {
			// Probably not Tomcat 8
		}
	}

	// Needs to be protected so it can be used by subclasses
	protected void customizeConnector(Connector connector) {
		int port = Math.max(getPort(), 0);
		connector.setPort(port);
		if (StringUtils.hasText(getServerHeader())) {
			connector.setProperty("server", getServerHeader());
		}
		if (connector.getProtocolHandler() instanceof AbstractProtocol) {
			customizeProtocol((AbstractProtocol<?>) connector.getProtocolHandler());
		}
		invokeProtocolHandlerCustomizers(connector.getProtocolHandler());
		if (getUriEncoding() != null) {
			connector.setURIEncoding(getUriEncoding().name());
		}
		if (getHttp2() != null && getHttp2().isEnabled()) {
			connector.addUpgradeProtocol(new Http2Protocol());
		}
		if (Ssl.isEnabled(getSsl())) {
			customizeSsl(connector);
		}
		TomcatConnectorCustomizer compression = new CompressionConnectorCustomizer(getCompression());
		compression.customize(connector);
		for (TomcatConnectorCustomizer customizer : this.tomcatConnectorCustomizers) {
			customizer.customize(connector);
		}
	}

	/**
	 * Customizes the protocol of the given AbstractProtocol object.
	 * @param protocol the AbstractProtocol object to be customized
	 *
	 * @since version 1.0
	 */
	private void customizeProtocol(AbstractProtocol<?> protocol) {
		if (getAddress() != null) {
			protocol.setAddress(getAddress());
		}
	}

	/**
	 * Invokes the protocol handler customizers for the given protocol handler.
	 * @param protocolHandler the protocol handler to customize
	 */
	@SuppressWarnings("unchecked")
	private void invokeProtocolHandlerCustomizers(ProtocolHandler protocolHandler) {
		LambdaSafe
			.callbacks(TomcatProtocolHandlerCustomizer.class, this.tomcatProtocolHandlerCustomizers, protocolHandler)
			.invoke((customizer) -> customizer.customize(protocolHandler));
	}

	/**
	 * Customizes the SSL configuration for the given connector.
	 * @param connector the connector to customize
	 */
	private void customizeSsl(Connector connector) {
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, connector, getSsl().getClientAuth());
		customizer.customize(getSslBundle());
		String sslBundleName = getSsl().getBundle();
		if (StringUtils.hasText(sslBundleName)) {
			getSslBundles().addBundleUpdateHandler(sslBundleName, customizer::update);
		}
	}

	/**
	 * Configure the Tomcat {@link Context}.
	 * @param context the Tomcat context
	 * @param initializers initializers to apply
	 */
	protected void configureContext(Context context, ServletContextInitializer[] initializers) {
		TomcatStarter starter = new TomcatStarter(initializers);
		if (context instanceof TomcatEmbeddedContext embeddedContext) {
			embeddedContext.setStarter(starter);
			embeddedContext.setFailCtxIfServletStartFails(true);
		}
		context.addServletContainerInitializer(starter, NO_CLASSES);
		for (LifecycleListener lifecycleListener : this.contextLifecycleListeners) {
			context.addLifecycleListener(lifecycleListener);
		}
		for (Valve valve : this.contextValves) {
			context.getPipeline().addValve(valve);
		}
		for (ErrorPage errorPage : getErrorPages()) {
			org.apache.tomcat.util.descriptor.web.ErrorPage tomcatErrorPage = new org.apache.tomcat.util.descriptor.web.ErrorPage();
			tomcatErrorPage.setLocation(errorPage.getPath());
			tomcatErrorPage.setErrorCode(errorPage.getStatusCode());
			tomcatErrorPage.setExceptionType(errorPage.getExceptionName());
			context.addErrorPage(tomcatErrorPage);
		}
		setMimeMappings(context);
		configureSession(context);
		configureCookieProcessor(context);
		new DisableReferenceClearingContextCustomizer().customize(context);
		for (String webListenerClassName : getWebListenerClassNames()) {
			context.addApplicationListener(webListenerClassName);
		}
		for (TomcatContextCustomizer customizer : this.tomcatContextCustomizers) {
			customizer.customize(context);
		}
	}

	/**
	 * Configures the session settings for the given context.
	 * @param context the context to configure
	 */
	private void configureSession(Context context) {
		long sessionTimeout = getSessionTimeoutInMinutes();
		context.setSessionTimeout((int) sessionTimeout);
		Boolean httpOnly = getSession().getCookie().getHttpOnly();
		if (httpOnly != null) {
			context.setUseHttpOnly(httpOnly);
		}
		if (getSession().isPersistent()) {
			Manager manager = context.getManager();
			if (manager == null) {
				manager = new StandardManager();
				context.setManager(manager);
			}
			configurePersistSession(manager);
		}
		else {
			context.addLifecycleListener(new DisablePersistSessionListener());
		}
	}

	/**
	 * Sets the MIME mappings for the given context.
	 * @param context the context to set the MIME mappings for
	 */
	private void setMimeMappings(Context context) {
		if (context instanceof TomcatEmbeddedContext embeddedContext) {
			embeddedContext.setMimeMappings(getMimeMappings());
			return;
		}
		for (MimeMappings.Mapping mapping : getMimeMappings()) {
			context.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
		}
	}

	/**
	 * Configures the cookie processor for the given context.
	 * @param context the context for which the cookie processor is to be configured
	 */
	private void configureCookieProcessor(Context context) {
		SameSite sessionSameSite = getSession().getCookie().getSameSite();
		List<CookieSameSiteSupplier> suppliers = new ArrayList<>();
		if (sessionSameSite != null) {
			suppliers.add(CookieSameSiteSupplier.of(sessionSameSite)
				.whenHasName(() -> SessionConfig.getSessionCookieName(context)));
		}
		if (!CollectionUtils.isEmpty(getCookieSameSiteSuppliers())) {
			suppliers.addAll(getCookieSameSiteSuppliers());
		}
		if (!suppliers.isEmpty()) {
			context.setCookieProcessor(new SuppliedSameSiteCookieProcessor(suppliers));
		}
	}

	/**
	 * Configures the persistence of HTTP session state using the provided manager.
	 * @param manager the manager to be configured for session persistence
	 * @throws IllegalArgumentException if the provided manager is not an instance of
	 * StandardManager
	 */
	private void configurePersistSession(Manager manager) {
		Assert.state(manager instanceof StandardManager,
				() -> "Unable to persist HTTP session state using manager type " + manager.getClass().getName());
		File dir = getValidSessionStoreDir();
		File file = new File(dir, "SESSIONS.ser");
		((StandardManager) manager).setPathname(file.getAbsolutePath());
	}

	/**
	 * Returns the session timeout in minutes.
	 * @return the session timeout in minutes
	 */
	private long getSessionTimeoutInMinutes() {
		Duration sessionTimeout = getSession().getTimeout();
		if (isZeroOrLess(sessionTimeout)) {
			return 0;
		}
		return Math.max(sessionTimeout.toMinutes(), 1);
	}

	/**
	 * Checks if the given session timeout is zero or less.
	 * @param sessionTimeout the duration of the session timeout
	 * @return {@code true} if the session timeout is zero or less, {@code false}
	 * otherwise
	 */
	private boolean isZeroOrLess(Duration sessionTimeout) {
		return sessionTimeout == null || sessionTimeout.isNegative() || sessionTimeout.isZero();
	}

	/**
	 * Post process the Tomcat {@link Context} before it's used with the Tomcat Server.
	 * Subclasses can override this method to apply additional processing to the
	 * {@link Context}.
	 * @param context the Tomcat {@link Context}
	 */
	protected void postProcessContext(Context context) {
	}

	/**
	 * Factory method called to create the {@link TomcatWebServer}. Subclasses can
	 * override this method to return a different {@link TomcatWebServer} or apply
	 * additional processing to the Tomcat server.
	 * @param tomcat the Tomcat server.
	 * @return a new {@link TomcatWebServer} instance
	 */
	protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
		return new TomcatWebServer(tomcat, getPort() >= 0, getShutdown());
	}

	/**
	 * Set the resource loader to be used by this factory.
	 * @param resourceLoader the resource loader to be set
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Sets the base directory for the web server.
	 * @param baseDirectory the base directory to set
	 */
	@Override
	public void setBaseDirectory(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	/**
	 * Returns a mutable set of the patterns that match jars to ignore for TLD scanning.
	 * @return the list of jars to ignore for TLD scanning
	 */
	public Set<String> getTldSkipPatterns() {
		return this.tldSkipPatterns;
	}

	/**
	 * Set the patterns that match jars to ignore for TLD scanning. See Tomcat's
	 * catalina.properties for typical values. Defaults to a list drawn from that source.
	 * @param patterns the jar patterns to skip when scanning for TLDs etc
	 */
	public void setTldSkipPatterns(Collection<String> patterns) {
		Assert.notNull(patterns, "Patterns must not be null");
		this.tldSkipPatterns = new LinkedHashSet<>(patterns);
	}

	/**
	 * Add patterns that match jars to ignore for TLD scanning. See Tomcat's
	 * catalina.properties for typical values.
	 * @param patterns the additional jar patterns to skip when scanning for TLDs etc
	 */
	public void addTldSkipPatterns(String... patterns) {
		Assert.notNull(patterns, "Patterns must not be null");
		this.tldSkipPatterns.addAll(Arrays.asList(patterns));
	}

	/**
	 * The Tomcat protocol to use when create the {@link Connector}.
	 * @param protocol the protocol
	 * @see Connector#Connector(String)
	 */
	public void setProtocol(String protocol) {
		Assert.hasLength(protocol, "Protocol must not be empty");
		this.protocol = protocol;
	}

	/**
	 * Set {@link Valve}s that should be applied to the Tomcat {@link Engine}. Calling
	 * this method will replace any existing valves.
	 * @param engineValves the valves to set
	 */
	public void setEngineValves(Collection<? extends Valve> engineValves) {
		Assert.notNull(engineValves, "Valves must not be null");
		this.engineValves = new ArrayList<>(engineValves);
	}

	/**
	 * Returns a mutable collection of the {@link Valve}s that will be applied to the
	 * Tomcat {@link Engine}.
	 * @return the engine valves that will be applied
	 */
	public Collection<Valve> getEngineValves() {
		return this.engineValves;
	}

	/**
	 * Adds engine valves to the Tomcat servlet web server factory.
	 * @param engineValves the engine valves to be added
	 * @throws IllegalArgumentException if the engine valves are null
	 */
	@Override
	public void addEngineValves(Valve... engineValves) {
		Assert.notNull(engineValves, "Valves must not be null");
		this.engineValves.addAll(Arrays.asList(engineValves));
	}

	/**
	 * Set {@link Valve}s that should be applied to the Tomcat {@link Context}. Calling
	 * this method will replace any existing valves.
	 * @param contextValves the valves to set
	 */
	public void setContextValves(Collection<? extends Valve> contextValves) {
		Assert.notNull(contextValves, "Valves must not be null");
		this.contextValves = new ArrayList<>(contextValves);
	}

	/**
	 * Returns a mutable collection of the {@link Valve}s that will be applied to the
	 * Tomcat {@link Context}.
	 * @return the context valves that will be applied
	 * @see #getEngineValves()
	 */
	public Collection<Valve> getContextValves() {
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
	 * Set {@link LifecycleListener}s that should be applied to the Tomcat
	 * {@link Context}. Calling this method will replace any existing listeners.
	 * @param contextLifecycleListeners the listeners to set
	 */
	public void setContextLifecycleListeners(Collection<? extends LifecycleListener> contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners, "ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners = new ArrayList<>(contextLifecycleListeners);
	}

	/**
	 * Returns a mutable collection of the {@link LifecycleListener}s that will be applied
	 * to the Tomcat {@link Context}.
	 * @return the context lifecycle listeners that will be applied
	 */
	public Collection<LifecycleListener> getContextLifecycleListeners() {
		return this.contextLifecycleListeners;
	}

	/**
	 * Add {@link LifecycleListener}s that should be added to the Tomcat {@link Context}.
	 * @param contextLifecycleListeners the listeners to add
	 */
	public void addContextLifecycleListeners(LifecycleListener... contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners, "ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners.addAll(Arrays.asList(contextLifecycleListeners));
	}

	/**
	 * Set {@link TomcatContextCustomizer}s that should be applied to the Tomcat
	 * {@link Context}. Calling this method will replace any existing customizers.
	 * @param tomcatContextCustomizers the customizers to set
	 */
	public void setTomcatContextCustomizers(Collection<? extends TomcatContextCustomizer> tomcatContextCustomizers) {
		Assert.notNull(tomcatContextCustomizers, "TomcatContextCustomizers must not be null");
		this.tomcatContextCustomizers = new LinkedHashSet<>(tomcatContextCustomizers);
	}

	/**
	 * Returns a mutable collection of the {@link TomcatContextCustomizer}s that will be
	 * applied to the Tomcat {@link Context}.
	 * @return the listeners that will be applied
	 */
	public Collection<TomcatContextCustomizer> getTomcatContextCustomizers() {
		return this.tomcatContextCustomizers;
	}

	/**
	 * Add customizers to the Tomcat context.
	 * @param tomcatContextCustomizers the customizers to add (must not be null)
	 */
	@Override
	public void addContextCustomizers(TomcatContextCustomizer... tomcatContextCustomizers) {
		Assert.notNull(tomcatContextCustomizers, "TomcatContextCustomizers must not be null");
		this.tomcatContextCustomizers.addAll(Arrays.asList(tomcatContextCustomizers));
	}

	/**
	 * Set {@link TomcatConnectorCustomizer}s that should be applied to the Tomcat
	 * {@link Connector}. Calling this method will replace any existing customizers.
	 * @param tomcatConnectorCustomizers the customizers to set
	 */
	public void setTomcatConnectorCustomizers(
			Collection<? extends TomcatConnectorCustomizer> tomcatConnectorCustomizers) {
		Assert.notNull(tomcatConnectorCustomizers, "TomcatConnectorCustomizers must not be null");
		this.tomcatConnectorCustomizers = new LinkedHashSet<>(tomcatConnectorCustomizers);
	}

	/**
	 * Add customizers to the Tomcat connector.
	 * @param tomcatConnectorCustomizers the customizers to be added (must not be null)
	 */
	@Override
	public void addConnectorCustomizers(TomcatConnectorCustomizer... tomcatConnectorCustomizers) {
		Assert.notNull(tomcatConnectorCustomizers, "TomcatConnectorCustomizers must not be null");
		this.tomcatConnectorCustomizers.addAll(Arrays.asList(tomcatConnectorCustomizers));
	}

	/**
	 * Returns a mutable collection of the {@link TomcatConnectorCustomizer}s that will be
	 * applied to the Tomcat {@link Connector}.
	 * @return the customizers that will be applied
	 */
	public Collection<TomcatConnectorCustomizer> getTomcatConnectorCustomizers() {
		return this.tomcatConnectorCustomizers;
	}

	/**
	 * Set {@link TomcatProtocolHandlerCustomizer}s that should be applied to the Tomcat
	 * {@link Connector}. Calling this method will replace any existing customizers.
	 * @param tomcatProtocolHandlerCustomizer the customizers to set
	 * @since 2.2.0
	 */
	public void setTomcatProtocolHandlerCustomizers(
			Collection<? extends TomcatProtocolHandlerCustomizer<?>> tomcatProtocolHandlerCustomizer) {
		Assert.notNull(tomcatProtocolHandlerCustomizer, "TomcatProtocolHandlerCustomizers must not be null");
		this.tomcatProtocolHandlerCustomizers = new LinkedHashSet<>(tomcatProtocolHandlerCustomizer);
	}

	/**
	 * Add {@link TomcatProtocolHandlerCustomizer}s that should be added to the Tomcat
	 * {@link Connector}.
	 * @param tomcatProtocolHandlerCustomizers the customizers to add
	 * @since 2.2.0
	 */
	@Override
	public void addProtocolHandlerCustomizers(TomcatProtocolHandlerCustomizer<?>... tomcatProtocolHandlerCustomizers) {
		Assert.notNull(tomcatProtocolHandlerCustomizers, "TomcatProtocolHandlerCustomizers must not be null");
		this.tomcatProtocolHandlerCustomizers.addAll(Arrays.asList(tomcatProtocolHandlerCustomizers));
	}

	/**
	 * Returns a mutable collection of the {@link TomcatProtocolHandlerCustomizer}s that
	 * will be applied to the Tomcat {@link Connector}.
	 * @return the customizers that will be applied
	 * @since 2.2.0
	 */
	public Collection<TomcatProtocolHandlerCustomizer<?>> getTomcatProtocolHandlerCustomizers() {
		return this.tomcatProtocolHandlerCustomizers;
	}

	/**
	 * Add {@link Connector}s in addition to the default connector, e.g. for SSL or AJP.
	 * <p>
	 * {@link #getTomcatConnectorCustomizers Connector customizers} are not applied to
	 * connectors added this way.
	 * @param connectors the connectors to add
	 */
	public void addAdditionalTomcatConnectors(Connector... connectors) {
		Assert.notNull(connectors, "Connectors must not be null");
		this.additionalTomcatConnectors.addAll(Arrays.asList(connectors));
	}

	/**
	 * Returns a mutable collection of the {@link Connector}s that will be added to the
	 * Tomcat.
	 * @return the additionalTomcatConnectors
	 */
	public List<Connector> getAdditionalTomcatConnectors() {
		return this.additionalTomcatConnectors;
	}

	/**
	 * Sets the encoding to be used for parsing URIs.
	 * @param uriEncoding the encoding to be used for parsing URIs
	 */
	@Override
	public void setUriEncoding(Charset uriEncoding) {
		this.uriEncoding = uriEncoding;
	}

	/**
	 * Returns the character encoding to use for URL decoding.
	 * @return the URI encoding
	 */
	public Charset getUriEncoding() {
		return this.uriEncoding;
	}

	/**
	 * Sets the delay for the background processor.
	 * @param delay the delay in milliseconds
	 */
	@Override
	public void setBackgroundProcessorDelay(int delay) {
		this.backgroundProcessorDelay = delay;
	}

	/**
	 * Set whether the factory should disable Tomcat's MBean registry prior to creating
	 * the server.
	 * @param disableMBeanRegistry whether to disable the MBean registry
	 * @since 2.2.0
	 */
	public void setDisableMBeanRegistry(boolean disableMBeanRegistry) {
		this.disableMBeanRegistry = disableMBeanRegistry;
	}

	/**
	 * {@link LifecycleListener} to disable persistence in the {@link StandardManager}. A
	 * {@link LifecycleListener} is used so not to interfere with Tomcat's default manager
	 * creation logic.
	 */
	private static final class DisablePersistSessionListener implements LifecycleListener {

		/**
		 * This method is called when a lifecycle event occurs. It disables session
		 * persistence by setting the pathname to null in the StandardManager.
		 * @param event The lifecycle event that occurred.
		 */
		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			if (event.getType().equals(Lifecycle.START_EVENT)) {
				Context context = (Context) event.getLifecycle();
				Manager manager = context.getManager();
				if (manager instanceof StandardManager standardManager) {
					standardManager.setPathname(null);
				}
			}
		}

	}

	/**
	 * StaticResourceConfigurer class.
	 */
	private final class StaticResourceConfigurer implements LifecycleListener {

		private static final String WEB_APP_MOUNT = "/";

		private static final String INTERNAL_PATH = "/META-INF/resources";

		private final Context context;

		/**
		 * Constructs a new StaticResourceConfigurer with the specified context.
		 * @param context the context to be used by the StaticResourceConfigurer
		 */
		private StaticResourceConfigurer(Context context) {
			this.context = context;
		}

		/**
		 * This method is an implementation of the lifecycleEvent method from the
		 * LifecycleListener interface. It is called when a lifecycle event occurs.
		 * @param event The LifecycleEvent object representing the event that occurred.
		 */
		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				addResourceJars(getUrlsOfJarsWithMetaInfResources());
			}
		}

		/**
		 * Adds the resource jars to the resource sets.
		 * @param resourceJarUrls the list of URLs pointing to the resource jars
		 */
		private void addResourceJars(List<URL> resourceJarUrls) {
			for (URL url : resourceJarUrls) {
				String path = url.getPath();
				if (path.endsWith(".jar") || path.endsWith(".jar!/")) {
					String jar = url.toString();
					if (!jar.startsWith("jar:")) {
						// A jar file in the file system. Convert to Jar URL.
						jar = "jar:" + jar + "!/";
					}
					addResourceSet(jar);
				}
				else {
					addResourceSet(url.toString());
				}
			}
		}

		/**
		 * Adds a resource set to the web resource root.
		 * @param resource the resource to be added
		 */
		private void addResourceSet(String resource) {
			try {
				if (isInsideClassicNestedJar(resource)) {
					addClassicNestedResourceSet(resource);
					return;
				}
				WebResourceRoot root = this.context.getResources();
				URL url = new URL(resource);
				if (isInsideNestedJar(resource)) {
					root.addJarResources(new NestedJarResourceSet(url, root, WEB_APP_MOUNT, INTERNAL_PATH));
				}
				else {
					root.createWebResourceSet(ResourceSetType.RESOURCE_JAR, WEB_APP_MOUNT, url, INTERNAL_PATH);
				}
			}
			catch (Exception ex) {
				// Ignore (probably not a directory)
			}
		}

		/**
		 * Adds a classic nested resource set to the context's resources.
		 * @param resource the URL of the nested resource set
		 * @throws MalformedURLException if the URL is malformed
		 */
		private void addClassicNestedResourceSet(String resource) throws MalformedURLException {
			// It's a nested jar but we now don't want the suffix because Tomcat
			// is going to try and locate it as a root URL (not the resource
			// inside it)
			URL url = new URL(resource.substring(0, resource.length() - 2));
			this.context.getResources()
				.createWebResourceSet(ResourceSetType.RESOURCE_JAR, WEB_APP_MOUNT, url, INTERNAL_PATH);
		}

		/**
		 * Checks if the given resource is inside a classic nested JAR file.
		 * @param resource the resource to check
		 * @return true if the resource is inside a classic nested JAR file, false
		 * otherwise
		 */
		private boolean isInsideClassicNestedJar(String resource) {
			return !isInsideNestedJar(resource) && resource.indexOf("!/") < resource.lastIndexOf("!/");
		}

		/**
		 * Checks if a resource is inside a nested JAR file.
		 * @param resource the resource to check
		 * @return true if the resource is inside a nested JAR file, false otherwise
		 */
		private boolean isInsideNestedJar(String resource) {
			return resource.startsWith("jar:nested:");
		}

	}

	/**
	 * LoaderHidingResourceRoot class.
	 */
	private static final class LoaderHidingResourceRoot extends StandardRoot {

		/**
		 * Constructs a new LoaderHidingResourceRoot with the specified
		 * TomcatEmbeddedContext.
		 * @param context the TomcatEmbeddedContext to be associated with this
		 * LoaderHidingResourceRoot
		 */
		private LoaderHidingResourceRoot(TomcatEmbeddedContext context) {
			super(context);
		}

		/**
		 * Creates a new WebResourceSet for the main resources. This method overrides the
		 * createMainResourceSet method in the parent class. It creates a new
		 * LoaderHidingWebResourceSet by passing the result of the
		 * super.createMainResourceSet() method as a parameter.
		 * @return the newly created WebResourceSet for the main resources
		 */
		@Override
		protected WebResourceSet createMainResourceSet() {
			return new LoaderHidingWebResourceSet(super.createMainResourceSet());
		}

	}

	/**
	 * LoaderHidingWebResourceSet class.
	 */
	private static final class LoaderHidingWebResourceSet extends AbstractResourceSet {

		private final WebResourceSet delegate;

		private final Method initInternal;

		/**
		 * Constructs a new LoaderHidingWebResourceSet with the specified delegate
		 * WebResourceSet.
		 * @param delegate the delegate WebResourceSet to be used
		 * @throws IllegalStateException if an exception occurs while initializing the
		 * internal method
		 */
		private LoaderHidingWebResourceSet(WebResourceSet delegate) {
			this.delegate = delegate;
			try {
				this.initInternal = LifecycleBase.class.getDeclaredMethod("initInternal");
				this.initInternal.setAccessible(true);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
		 * Retrieves the web resource for the given path.
		 * @param path the path of the resource to retrieve
		 * @return the web resource for the given path
		 */
		@Override
		public WebResource getResource(String path) {
			if (path.startsWith("/org/springframework/boot")) {
				return new EmptyResource(getRoot(), path);
			}
			return this.delegate.getResource(path);
		}

		/**
		 * Returns an array of strings representing the files and directories in the
		 * specified path.
		 * @param path the path of the directory to list
		 * @return an array of strings representing the files and directories in the
		 * specified path
		 */
		@Override
		public String[] list(String path) {
			return this.delegate.list(path);
		}

		/**
		 * Returns a set of web application paths excluding those that start with
		 * "/org/springframework/boot".
		 * @param path the base path to list web application paths from
		 * @return a set of web application paths
		 */
		@Override
		public Set<String> listWebAppPaths(String path) {
			return this.delegate.listWebAppPaths(path)
				.stream()
				.filter((webAppPath) -> !webAppPath.startsWith("/org/springframework/boot"))
				.collect(Collectors.toSet());
		}

		/**
		 * Creates a new directory at the specified path.
		 * @param path the path of the directory to be created
		 * @return true if the directory was successfully created, false otherwise
		 */
		@Override
		public boolean mkdir(String path) {
			return this.delegate.mkdir(path);
		}

		/**
		 * Writes the contents of the given input stream to the specified path.
		 * @param path the path where the contents will be written
		 * @param is the input stream containing the contents to be written
		 * @param overwrite a boolean indicating whether to overwrite the existing file if
		 * it already exists
		 * @return true if the contents were successfully written, false otherwise
		 */
		@Override
		public boolean write(String path, InputStream is, boolean overwrite) {
			return this.delegate.write(path, is, overwrite);
		}

		/**
		 * Returns the base URL of the web resource set.
		 * @return the base URL of the web resource set
		 */
		@Override
		public URL getBaseUrl() {
			return this.delegate.getBaseUrl();
		}

		/**
		 * Sets the read-only flag for the LoaderHidingWebResourceSet.
		 * @param readOnly true if the LoaderHidingWebResourceSet should be read-only,
		 * false otherwise
		 */
		@Override
		public void setReadOnly(boolean readOnly) {
			this.delegate.setReadOnly(readOnly);
		}

		/**
		 * Returns a boolean value indicating whether the LoaderHidingWebResourceSet is
		 * read-only.
		 * @return true if the LoaderHidingWebResourceSet is read-only, false otherwise.
		 */
		@Override
		public boolean isReadOnly() {
			return this.delegate.isReadOnly();
		}

		/**
		 * Calls the garbage collector to free up memory. This method delegates the call
		 * to the gc() method of the delegate object.
		 */
		@Override
		public void gc() {
			this.delegate.gc();
		}

		/**
		 * Initializes the internal state of the LoaderHidingWebResourceSet.
		 * @throws LifecycleException if an error occurs during initialization
		 */
		@Override
		protected void initInternal() throws LifecycleException {
			if (this.delegate instanceof LifecycleBase) {
				try {
					ReflectionUtils.invokeMethod(this.initInternal, this.delegate);
				}
				catch (Exception ex) {
					throw new LifecycleException(ex);
				}
			}
		}

	}

	/**
	 * {@link Rfc6265CookieProcessor} that supports {@link CookieSameSiteSupplier
	 * supplied} {@link SameSite} values.
	 */
	private static class SuppliedSameSiteCookieProcessor extends Rfc6265CookieProcessor {

		private final List<CookieSameSiteSupplier> suppliers;

		/**
		 * Constructs a new SuppliedSameSiteCookieProcessor with the given list of
		 * CookieSameSiteSupplier objects.
		 * @param suppliers the list of CookieSameSiteSupplier objects to be used by the
		 * cookie processor
		 */
		SuppliedSameSiteCookieProcessor(List<CookieSameSiteSupplier> suppliers) {
			this.suppliers = suppliers;
		}

		/**
		 * Generates the header for the given cookie and HTTP servlet request. If the
		 * SameSite attribute is present in the cookie, it uses the Rfc6265CookieProcessor
		 * to generate the header with the SameSite attribute value. If the SameSite
		 * attribute is not present, it delegates the generation to the superclass.
		 * @param cookie the cookie for which the header needs to be generated
		 * @param request the HTTP servlet request
		 * @return the generated header string
		 */
		@Override
		public String generateHeader(Cookie cookie, HttpServletRequest request) {
			SameSite sameSite = getSameSite(cookie);
			if (sameSite == null) {
				return super.generateHeader(cookie, request);
			}
			Rfc6265CookieProcessor delegate = new Rfc6265CookieProcessor();
			delegate.setSameSiteCookies(sameSite.attributeValue());
			return delegate.generateHeader(cookie, request);
		}

		/**
		 * Returns the SameSite attribute value for the given cookie.
		 * @param cookie the cookie to get the SameSite attribute value for
		 * @return the SameSite attribute value for the given cookie, or null if not found
		 */
		private SameSite getSameSite(Cookie cookie) {
			for (CookieSameSiteSupplier supplier : this.suppliers) {
				SameSite sameSite = supplier.getSameSite(cookie);
				if (sameSite != null) {
					return sameSite;
				}
			}
			return null;
		}

	}

}
