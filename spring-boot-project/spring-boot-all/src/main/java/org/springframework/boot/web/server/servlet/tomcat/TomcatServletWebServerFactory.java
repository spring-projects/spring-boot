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

package org.springframework.boot.web.server.servlet.tomcat;

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
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.scan.StandardJarScanFilter;

import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ContextPath;
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.boot.web.server.servlet.DocumentRoot;
import org.springframework.boot.web.server.servlet.ServletContextInitializer;
import org.springframework.boot.web.server.servlet.ServletContextInitializers;
import org.springframework.boot.web.server.servlet.ServletWebServerSettings;
import org.springframework.boot.web.server.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.tomcat.DisableReferenceClearingContextCustomizer;
import org.springframework.boot.web.server.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.server.tomcat.TomcatEmbeddedContext;
import org.springframework.boot.web.server.tomcat.TomcatEmbeddedWebappClassLoader;
import org.springframework.boot.web.server.tomcat.TomcatStarter;
import org.springframework.boot.web.server.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.tomcat.TomcatWebServerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigurableServletWebServerFactory} that can be used to create
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
 * @author Scott Frederick
 * @since 4.0.0
 * @see #setPort(int)
 * @see #setContextLifecycleListeners(Collection)
 * @see TomcatWebServer
 */
public class TomcatServletWebServerFactory extends TomcatWebServerFactory
		implements ConfigurableTomcatWebServerFactory, ConfigurableServletWebServerFactory, ResourceLoaderAware {

	private static final Log logger = LogFactory.getLog(TomcatServletWebServerFactory.class);

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Set<Class<?>> NO_CLASSES = Collections.emptySet();

	private final ServletWebServerSettings settings = new ServletWebServerSettings();

	private ResourceLoader resourceLoader;

	private Set<String> tldSkipPatterns = new LinkedHashSet<>(TldPatterns.DEFAULT_SKIP);

	private final Set<String> tldScanPatterns = new LinkedHashSet<>(TldPatterns.DEFAULT_SCAN);

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
		super(port);
		this.settings.setContextPath(ContextPath.of(contextPath));
	}

	@Override
	public WebServer getWebServer(ServletContextInitializer... initializers) {
		Tomcat tomcat = createTomcat();
		prepareContext(tomcat.getHost(), initializers);
		return getTomcatWebServer(tomcat);
	}

	protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
		DocumentRoot documentRoot = new DocumentRoot(logger);
		documentRoot.setDirectory(this.settings.getDocumentRoot());
		File documentRootFile = documentRoot.getValidDirectory();
		TomcatEmbeddedContext context = new TomcatEmbeddedContext();
		WebResourceRoot resourceRoot = (documentRootFile != null) ? new LoaderHidingResourceRoot(context)
				: new StandardRoot(context);
		ignoringNoSuchMethodError(() -> resourceRoot.setReadOnly(true));
		context.setResources(resourceRoot);
		String contextPath = this.settings.getContextPath().toString();
		context.setName(contextPath);
		context.setDisplayName(this.settings.getDisplayName());
		context.setPath(contextPath);
		File docBase = (documentRootFile != null) ? documentRootFile : createTempDir("tomcat-docbase");
		context.setDocBase(docBase.getAbsolutePath());
		context.addLifecycleListener(new FixContextListener());
		ClassLoader parentClassLoader = (this.resourceLoader != null) ? this.resourceLoader.getClassLoader()
				: ClassUtils.getDefaultClassLoader();
		context.setParentClassLoader(parentClassLoader);
		resetDefaultLocaleMapping(context);
		addLocaleMappings(context);
		context.setCreateUploadTargets(true);
		configureTldPatterns(context);
		WebappLoader loader = new WebappLoader();
		loader.setLoaderInstance(new TomcatEmbeddedWebappClassLoader(parentClassLoader));
		loader.setDelegate(true);
		context.setLoader(loader);
		if (this.settings.isRegisterDefaultServlet()) {
			addDefaultServlet(context);
		}
		if (shouldRegisterJspServlet()) {
			addJspServlet(context);
			addJasperInitializer(context);
		}
		context.addLifecycleListener(new StaticResourceConfigurer(context));
		ServletContextInitializers initializersToUse = ServletContextInitializers.from(this.settings, initializers);
		host.addChild(context);
		configureContext(context, initializersToUse);
		postProcessContext(context);
	}

	private void ignoringNoSuchMethodError(Runnable method) {
		try {
			method.run();
		}
		catch (NoSuchMethodError ex) {
		}
	}

	private boolean shouldRegisterJspServlet() {
		return this.settings.getJsp() != null && this.settings.getJsp().getRegistered()
				&& ClassUtils.isPresent(this.settings.getJsp().getClassName(), getClass().getClassLoader());
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

	private void addLocaleMappings(TomcatEmbeddedContext context) {
		this.settings.getLocaleCharsetMappings()
			.forEach((locale, charset) -> context.addLocaleEncodingMappingParameter(locale.toString(),
					charset.toString()));
	}

	private void configureTldPatterns(TomcatEmbeddedContext context) {
		StandardJarScanFilter filter = new StandardJarScanFilter();
		filter.setTldSkip(StringUtils.collectionToCommaDelimitedString(this.tldSkipPatterns));
		filter.setTldScan(StringUtils.collectionToCommaDelimitedString(this.tldScanPatterns));
		context.getJarScanner().setJarScanFilter(filter);
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
		context.addServletMappingDecoded("/", "default");
	}

	private void addJspServlet(Context context) {
		Wrapper jspServlet = context.createWrapper();
		jspServlet.setName("jsp");
		jspServlet.setServletClass(this.settings.getJsp().getClassName());
		jspServlet.addInitParameter("fork", "false");
		this.settings.getJsp().getInitParameters().forEach(jspServlet::addInitParameter);
		jspServlet.setLoadOnStartup(3);
		context.addChild(jspServlet);
		context.addServletMappingDecoded("*.jsp", "jsp");
		context.addServletMappingDecoded("*.jspx", "jsp");
	}

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

	/**
	 * Configure the Tomcat {@link Context}.
	 * @param context the Tomcat context
	 * @param initializers initializers to apply
	 */
	protected void configureContext(Context context, Iterable<ServletContextInitializer> initializers) {
		TomcatStarter starter = new TomcatStarter(initializers);
		if (context instanceof TomcatEmbeddedContext embeddedContext) {
			embeddedContext.setStarter(starter);
			embeddedContext.setFailCtxIfServletStartFails(true);
		}
		context.addServletContainerInitializer(starter, NO_CLASSES);
		for (LifecycleListener lifecycleListener : this.getContextLifecycleListeners()) {
			context.addLifecycleListener(lifecycleListener);
		}
		for (Valve valve : this.getContextValves()) {
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
		for (String webListenerClassName : getSettings().getWebListenerClassNames()) {
			context.addApplicationListener(webListenerClassName);
		}
		for (TomcatContextCustomizer customizer : this.getContextCustomizers()) {
			customizer.customize(context);
		}
	}

	private void configureSession(Context context) {
		long sessionTimeout = getSessionTimeoutInMinutes();
		context.setSessionTimeout((int) sessionTimeout);
		Boolean httpOnly = this.settings.getSession().getCookie().getHttpOnly();
		if (httpOnly != null) {
			context.setUseHttpOnly(httpOnly);
		}
		if (this.settings.getSession().isPersistent()) {
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

	private void setMimeMappings(Context context) {
		MimeMappings mimeMappings = this.settings.getMimeMappings();
		if (context instanceof TomcatEmbeddedContext embeddedContext) {
			embeddedContext.setMimeMappings(mimeMappings);
			return;
		}
		for (MimeMappings.Mapping mapping : mimeMappings) {
			context.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
		}
	}

	private void configureCookieProcessor(Context context) {
		SameSite sessionSameSite = this.settings.getSession().getCookie().getSameSite();
		List<CookieSameSiteSupplier> suppliers = new ArrayList<>();
		if (sessionSameSite != null) {
			suppliers.add(CookieSameSiteSupplier.of(sessionSameSite)
				.whenHasName(() -> SessionConfig.getSessionCookieName(context)));
		}
		if (!CollectionUtils.isEmpty(this.settings.getCookieSameSiteSuppliers())) {
			suppliers.addAll(this.settings.getCookieSameSiteSuppliers());
		}
		if (!suppliers.isEmpty()) {
			context.setCookieProcessor(new SuppliedSameSiteCookieProcessor(suppliers));
		}
	}

	private void configurePersistSession(Manager manager) {
		Assert.state(manager instanceof StandardManager,
				() -> "Unable to persist HTTP session state using manager type " + manager.getClass().getName());
		File dir = this.settings.getSession().getSessionStoreDirectory().getValidDirectory(true);
		File file = new File(dir, "SESSIONS.ser");
		((StandardManager) manager).setPathname(file.getAbsolutePath());
	}

	private long getSessionTimeoutInMinutes() {
		Duration sessionTimeout = this.settings.getSession().getTimeout();
		if (isZeroOrLess(sessionTimeout)) {
			return 0;
		}
		return Math.max(sessionTimeout.toMinutes(), 1);
	}

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

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Returns a mutable set of the patterns that match jars to ignore for TLD scanning.
	 * @return the set of jars to ignore for TLD scanning
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
		Assert.notNull(patterns, "'patterns' must not be null");
		this.tldSkipPatterns = new LinkedHashSet<>(patterns);
	}

	/**
	 * Add patterns that match jars to ignore for TLD scanning. See Tomcat's
	 * catalina.properties for typical values.
	 * @param patterns the additional jar patterns to skip when scanning for TLDs etc
	 */
	public void addTldSkipPatterns(String... patterns) {
		Assert.notNull(patterns, "'patterns' must not be null");
		this.tldSkipPatterns.addAll(Arrays.asList(patterns));
	}

	@Override
	public ServletWebServerSettings getSettings() {
		return this.settings;
	}

	/**
	 * {@link LifecycleListener} to disable persistence in the {@link StandardManager}. A
	 * {@link LifecycleListener} is used so not to interfere with Tomcat's default manager
	 * creation logic.
	 */
	private static final class DisablePersistSessionListener implements LifecycleListener {

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

	private final class StaticResourceConfigurer implements LifecycleListener {

		private static final String WEB_APP_MOUNT = "/";

		private static final String INTERNAL_PATH = "/META-INF/resources";

		private final Context context;

		private StaticResourceConfigurer(Context context) {
			this.context = context;
		}

		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			if (event.getType().equals(Lifecycle.BEFORE_INIT_EVENT)) {
				addResourceJars(TomcatServletWebServerFactory.this.getSettings().getStaticResourceUrls());
			}
		}

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
			for (WebResourceSet resources : this.context.getResources().getJarResources()) {
				resources.setReadOnly(true);
			}
		}

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

		private void addClassicNestedResourceSet(String resource) throws MalformedURLException {
			// It's a nested jar but we now don't want the suffix because Tomcat
			// is going to try and locate it as a root URL (not the resource
			// inside it)
			URL url = new URL(resource.substring(0, resource.length() - 2));
			this.context.getResources()
				.createWebResourceSet(ResourceSetType.RESOURCE_JAR, WEB_APP_MOUNT, url, INTERNAL_PATH);
		}

		private boolean isInsideClassicNestedJar(String resource) {
			return !isInsideNestedJar(resource) && resource.indexOf("!/") < resource.lastIndexOf("!/");
		}

		private boolean isInsideNestedJar(String resource) {
			return resource.startsWith("jar:nested:");
		}

	}

	private static final class LoaderHidingResourceRoot extends StandardRoot {

		private LoaderHidingResourceRoot(TomcatEmbeddedContext context) {
			super(context);
		}

		@Override
		protected WebResourceSet createMainResourceSet() {
			return new LoaderHidingWebResourceSet(super.createMainResourceSet());
		}

	}

	private static final class LoaderHidingWebResourceSet extends AbstractResourceSet {

		private final WebResourceSet delegate;

		private final Method initInternal;

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

		@Override
		public WebResource getResource(String path) {
			if (path.startsWith("/org/springframework/boot")) {
				return new EmptyResource(getRoot(), path);
			}
			return this.delegate.getResource(path);
		}

		@Override
		public String[] list(String path) {
			return this.delegate.list(path);
		}

		@Override
		public Set<String> listWebAppPaths(String path) {
			return this.delegate.listWebAppPaths(path)
				.stream()
				.filter((webAppPath) -> !webAppPath.startsWith("/org/springframework/boot"))
				.collect(Collectors.toSet());
		}

		@Override
		public boolean mkdir(String path) {
			return this.delegate.mkdir(path);
		}

		@Override
		public boolean write(String path, InputStream is, boolean overwrite) {
			return this.delegate.write(path, is, overwrite);
		}

		@Override
		public URL getBaseUrl() {
			return this.delegate.getBaseUrl();
		}

		@Override
		public void setReadOnly(boolean readOnly) {
			this.delegate.setReadOnly(readOnly);
		}

		@Override
		public boolean isReadOnly() {
			return this.delegate.isReadOnly();
		}

		@Override
		public void gc() {
			this.delegate.gc();
		}

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

		SuppliedSameSiteCookieProcessor(List<CookieSameSiteSupplier> suppliers) {
			this.suppliers = suppliers;
		}

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
