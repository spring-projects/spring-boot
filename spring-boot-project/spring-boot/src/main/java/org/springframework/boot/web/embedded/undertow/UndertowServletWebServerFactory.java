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

package org.springframework.boot.web.embedded.undertow;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.core.DeploymentImpl;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.MimeMappings.Mapping;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link ServletWebServerFactory} that can be used to create
 * {@link UndertowServletWebServer}s.
 * <p>
 * Unless explicitly configured otherwise, the factory will create servers that listen for
 * HTTP requests on port 8080.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Marcos Barbero
 * @author Eddú Meléndez
 * @since 2.0.0
 * @see UndertowServletWebServer
 */
public class UndertowServletWebServerFactory extends AbstractServletWebServerFactory
		implements ConfigurableUndertowWebServerFactory, ResourceLoaderAware {

	private static final Pattern ENCODED_SLASH = Pattern.compile("%2F", Pattern.LITERAL);

	private static final Set<Class<?>> NO_CLASSES = Collections.emptySet();

	private final UndertowWebServerFactoryDelegate delegate = new UndertowWebServerFactoryDelegate();

	private Set<UndertowDeploymentInfoCustomizer> deploymentInfoCustomizers = new LinkedHashSet<>();

	private ResourceLoader resourceLoader;

	private boolean eagerFilterInit = true;

	private boolean preservePathOnForward = false;

	/**
	 * Create a new {@link UndertowServletWebServerFactory} instance.
	 */
	public UndertowServletWebServerFactory() {
		getJsp().setRegistered(false);
	}

	/**
	 * Create a new {@link UndertowServletWebServerFactory} that listens for requests
	 * using the specified port.
	 * @param port the port to listen on
	 */
	public UndertowServletWebServerFactory(int port) {
		super(port);
		getJsp().setRegistered(false);
	}

	/**
	 * Create a new {@link UndertowServletWebServerFactory} with the specified context
	 * path and port.
	 * @param contextPath the root context path
	 * @param port the port to listen on
	 */
	public UndertowServletWebServerFactory(String contextPath, int port) {
		super(contextPath, port);
		getJsp().setRegistered(false);
	}

	/**
     * Set the customizers to be applied to the Undertow builder.
     * 
     * @param customizers the customizers to be applied
     */
    @Override
	public void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers) {
		this.delegate.setBuilderCustomizers(customizers);
	}

	/**
     * Add customizers to the Undertow builder.
     * 
     * @param customizers the customizers to add
     */
    @Override
	public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		this.delegate.addBuilderCustomizers(customizers);
	}

	/**
	 * Returns a mutable collection of the {@link UndertowBuilderCustomizer}s that will be
	 * applied to the Undertow {@link Builder}.
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
		return this.delegate.getBuilderCustomizers();
	}

	/**
     * Sets the buffer size for the server.
     * 
     * @param bufferSize the buffer size to be set
     */
    @Override
	public void setBufferSize(Integer bufferSize) {
		this.delegate.setBufferSize(bufferSize);
	}

	/**
     * Sets the number of I/O threads to be used by the server.
     * 
     * @param ioThreads the number of I/O threads
     */
    @Override
	public void setIoThreads(Integer ioThreads) {
		this.delegate.setIoThreads(ioThreads);
	}

	/**
     * Sets the number of worker threads to be used by the server.
     * 
     * @param workerThreads the number of worker threads
     */
    @Override
	public void setWorkerThreads(Integer workerThreads) {
		this.delegate.setWorkerThreads(workerThreads);
	}

	/**
     * Sets whether to use direct buffers for the underlying server.
     * 
     * @param directBuffers a boolean value indicating whether to use direct buffers
     */
    @Override
	public void setUseDirectBuffers(Boolean directBuffers) {
		this.delegate.setUseDirectBuffers(directBuffers);
	}

	/**
     * Sets the directory where the access logs will be stored.
     * 
     * @param accessLogDirectory the directory where the access logs will be stored
     */
    @Override
	public void setAccessLogDirectory(File accessLogDirectory) {
		this.delegate.setAccessLogDirectory(accessLogDirectory);
	}

	/**
     * Sets the access log pattern for the Undertow servlet web server factory.
     * 
     * @param accessLogPattern the access log pattern to be set
     */
    @Override
	public void setAccessLogPattern(String accessLogPattern) {
		this.delegate.setAccessLogPattern(accessLogPattern);
	}

	/**
     * Sets the access log prefix for the Undertow servlet web server factory.
     * 
     * @param accessLogPrefix the access log prefix to be set
     */
    @Override
	public void setAccessLogPrefix(String accessLogPrefix) {
		this.delegate.setAccessLogPrefix(accessLogPrefix);
	}

	/**
     * Returns the access log prefix used by the UndertowServletWebServerFactory.
     * 
     * @return the access log prefix
     */
    public String getAccessLogPrefix() {
		return this.delegate.getAccessLogPrefix();
	}

	/**
     * Sets the access log suffix for the Undertow server.
     * 
     * @param accessLogSuffix the access log suffix to be set
     */
    @Override
	public void setAccessLogSuffix(String accessLogSuffix) {
		this.delegate.setAccessLogSuffix(accessLogSuffix);
	}

	/**
     * Sets whether access log is enabled for the Undertow servlet web server.
     * 
     * @param accessLogEnabled true if access log is enabled, false otherwise
     */
    @Override
	public void setAccessLogEnabled(boolean accessLogEnabled) {
		this.delegate.setAccessLogEnabled(accessLogEnabled);
	}

	/**
     * Returns a boolean value indicating whether the access log is enabled.
     *
     * @return {@code true} if the access log is enabled, {@code false} otherwise
     */
    public boolean isAccessLogEnabled() {
		return this.delegate.isAccessLogEnabled();
	}

	/**
     * Sets whether to rotate the access log.
     * 
     * @param accessLogRotate true to rotate the access log, false otherwise
     */
    @Override
	public void setAccessLogRotate(boolean accessLogRotate) {
		this.delegate.setAccessLogRotate(accessLogRotate);
	}

	/**
     * Sets whether to use forward headers.
     * 
     * @param useForwardHeaders true to use forward headers, false otherwise
     */
    @Override
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.delegate.setUseForwardHeaders(useForwardHeaders);
	}

	/**
     * Returns whether to use forward headers.
     * 
     * @return {@code true} if forward headers are used, {@code false} otherwise
     */
    protected final boolean isUseForwardHeaders() {
		return this.delegate.isUseForwardHeaders();
	}

	/**
	 * Set {@link UndertowDeploymentInfoCustomizer}s that should be applied to the
	 * Undertow {@link DeploymentInfo}. Calling this method will replace any existing
	 * customizers.
	 * @param customizers the customizers to set
	 */
	public void setDeploymentInfoCustomizers(Collection<? extends UndertowDeploymentInfoCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.deploymentInfoCustomizers = new LinkedHashSet<>(customizers);
	}

	/**
	 * Add {@link UndertowDeploymentInfoCustomizer}s that should be used to customize the
	 * Undertow {@link DeploymentInfo}.
	 * @param customizers the customizers to add
	 */
	public void addDeploymentInfoCustomizers(UndertowDeploymentInfoCustomizer... customizers) {
		Assert.notNull(customizers, "UndertowDeploymentInfoCustomizers must not be null");
		this.deploymentInfoCustomizers.addAll(Arrays.asList(customizers));
	}

	/**
	 * Returns a mutable collection of the {@link UndertowDeploymentInfoCustomizer}s that
	 * will be applied to the Undertow {@link DeploymentInfo}.
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowDeploymentInfoCustomizer> getDeploymentInfoCustomizers() {
		return this.deploymentInfoCustomizers;
	}

	/**
     * Set the resource loader to be used for loading resources.
     * 
     * @param resourceLoader the resource loader to be used
     */
    @Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Return if filters should be eagerly initialized.
	 * @return {@code true} if filters are eagerly initialized, otherwise {@code false}.
	 * @since 2.4.0
	 */
	public boolean isEagerFilterInit() {
		return this.eagerFilterInit;
	}

	/**
	 * Set whether filters should be eagerly initialized.
	 * @param eagerFilterInit {@code true} if filters are eagerly initialized, otherwise
	 * {@code false}.
	 * @since 2.4.0
	 */
	public void setEagerFilterInit(boolean eagerFilterInit) {
		this.eagerFilterInit = eagerFilterInit;
	}

	/**
	 * Return whether the request path should be preserved on forward.
	 * @return {@code true} if the path should be preserved when a request is forwarded,
	 * otherwise {@code false}.
	 * @since 2.4.0
	 */
	public boolean isPreservePathOnForward() {
		return this.preservePathOnForward;
	}

	/**
	 * Set whether the request path should be preserved on forward.
	 * @param preservePathOnForward {@code true} if the path should be preserved when a
	 * request is forwarded, otherwise {@code false}.
	 * @since 2.4.0
	 */
	public void setPreservePathOnForward(boolean preservePathOnForward) {
		this.preservePathOnForward = preservePathOnForward;
	}

	/**
     * Returns a WebServer instance for the Undertow server.
     * 
     * @param initializers the ServletContextInitializers to be applied to the server
     * @return the WebServer instance
     */
    @Override
	public WebServer getWebServer(ServletContextInitializer... initializers) {
		Builder builder = this.delegate.createBuilder(this, this::getSslBundle);
		DeploymentManager manager = createManager(initializers);
		return getUndertowWebServer(builder, manager, getPort());
	}

	/**
     * Creates a DeploymentManager for the Undertow servlet container.
     * 
     * @param initializers the initializers to be driven by the servlet container
     * @return the created DeploymentManager
     */
    private DeploymentManager createManager(ServletContextInitializer... initializers) {
		DeploymentInfo deployment = Servlets.deployment();
		registerServletContainerInitializerToDriveServletContextInitializers(deployment, initializers);
		deployment.setClassLoader(getServletClassLoader());
		deployment.setContextPath(getContextPath());
		deployment.setDisplayName(getDisplayName());
		deployment.setDeploymentName("spring-boot");
		if (isRegisterDefaultServlet()) {
			deployment.addServlet(Servlets.servlet("default", DefaultServlet.class));
		}
		configureErrorPages(deployment);
		deployment.setServletStackTraces(ServletStackTraces.NONE);
		deployment.setResourceManager(getDocumentRootResourceManager());
		deployment.setTempDir(createTempDir("undertow"));
		deployment.setEagerFilterInit(this.eagerFilterInit);
		deployment.setPreservePathOnForward(this.preservePathOnForward);
		configureMimeMappings(deployment);
		configureWebListeners(deployment);
		for (UndertowDeploymentInfoCustomizer customizer : this.deploymentInfoCustomizers) {
			customizer.customize(deployment);
		}
		if (getSession().isPersistent()) {
			File dir = getValidSessionStoreDir();
			deployment.setSessionPersistenceManager(new FileSessionPersistence(dir));
		}
		addLocaleMappings(deployment);
		DeploymentManager manager = Servlets.newContainer().addDeployment(deployment);
		manager.deploy();
		if (manager.getDeployment() instanceof DeploymentImpl managerDeployment) {
			removeSuperfluousMimeMappings(managerDeployment, deployment);
		}
		SessionManager sessionManager = manager.getDeployment().getSessionManager();
		Duration timeoutDuration = getSession().getTimeout();
		int sessionTimeout = (isZeroOrLess(timeoutDuration) ? -1 : (int) timeoutDuration.getSeconds());
		sessionManager.setDefaultSessionTimeout(sessionTimeout);
		return manager;
	}

	/**
     * Configures the web listeners for the given deployment.
     * 
     * @param deployment the deployment info to configure the web listeners for
     * @throws IllegalStateException if failed to load a web listener class
     */
    private void configureWebListeners(DeploymentInfo deployment) {
		for (String className : getWebListenerClassNames()) {
			try {
				deployment.addListener(new ListenerInfo(loadWebListenerClass(className)));
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Failed to load web listener class '" + className + "'", ex);
			}
		}
	}

	/**
     * Loads a web listener class based on the provided class name.
     * 
     * @param className the fully qualified name of the web listener class to load
     * @return the loaded web listener class
     * @throws ClassNotFoundException if the specified class cannot be found
     */
    @SuppressWarnings("unchecked")
	private Class<? extends EventListener> loadWebListenerClass(String className) throws ClassNotFoundException {
		return (Class<? extends EventListener>) getServletClassLoader().loadClass(className);
	}

	/**
     * Checks if the given timeout duration is zero or less.
     * 
     * @param timeoutDuration the timeout duration to be checked
     * @return {@code true} if the timeout duration is zero or less, {@code false} otherwise
     */
    private boolean isZeroOrLess(Duration timeoutDuration) {
		return timeoutDuration == null || timeoutDuration.isZero() || timeoutDuration.isNegative();
	}

	/**
     * Adds locale mappings to the given deployment.
     * 
     * @param deployment the DeploymentInfo object to which the locale mappings will be added
     */
    private void addLocaleMappings(DeploymentInfo deployment) {
		getLocaleCharsetMappings()
			.forEach((locale, charset) -> deployment.addLocaleCharsetMapping(locale.toString(), charset.toString()));
	}

	/**
     * Registers the given ServletContextInitializers to drive the ServletContext initializers for the specified DeploymentInfo.
     * 
     * @param deployment the DeploymentInfo to register the ServletContextInitializers with
     * @param initializers the ServletContextInitializers to register
     */
    private void registerServletContainerInitializerToDriveServletContextInitializers(DeploymentInfo deployment,
			ServletContextInitializer... initializers) {
		ServletContextInitializer[] mergedInitializers = mergeInitializers(initializers);
		Initializer initializer = new Initializer(mergedInitializers);
		deployment.addServletContainerInitializer(new ServletContainerInitializerInfo(Initializer.class,
				new ImmediateInstanceFactory<ServletContainerInitializer>(initializer), NO_CLASSES));
	}

	/**
     * Returns the class loader to be used for loading servlets.
     * 
     * If a resource loader is set, the class loader of the resource loader is returned.
     * Otherwise, the class loader of the UndertowServletWebServerFactory class is returned.
     * 
     * @return the class loader to be used for loading servlets
     */
    private ClassLoader getServletClassLoader() {
		if (this.resourceLoader != null) {
			return this.resourceLoader.getClassLoader();
		}
		return getClass().getClassLoader();
	}

	/**
     * Returns the ResourceManager for the document root.
     * 
     * @return The ResourceManager for the document root.
     */
    private ResourceManager getDocumentRootResourceManager() {
		File root = getValidDocumentRoot();
		File docBase = getCanonicalDocumentRoot(root);
		List<URL> metaInfResourceUrls = getUrlsOfJarsWithMetaInfResources();
		List<URL> resourceJarUrls = new ArrayList<>();
		List<ResourceManager> managers = new ArrayList<>();
		ResourceManager rootManager = (docBase.isDirectory() ? new FileResourceManager(docBase, 0)
				: new JarResourceManager(docBase));
		if (root != null) {
			rootManager = new LoaderHidingResourceManager(rootManager);
		}
		managers.add(rootManager);
		for (URL url : metaInfResourceUrls) {
			if ("file".equals(url.getProtocol())) {
				try {
					File file = new File(url.toURI());
					if (file.isFile()) {
						resourceJarUrls.add(new URL("jar:" + url + "!/"));
					}
					else {
						managers.add(new FileResourceManager(new File(file, "META-INF/resources"), 0));
					}
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			else {
				resourceJarUrls.add(url);
			}
		}
		managers.add(new MetaInfResourcesResourceManager(resourceJarUrls));
		return new CompositeResourceManager(managers.toArray(new ResourceManager[0]));
	}

	/**
     * Returns the canonical document root for the given docBase.
     * If docBase is null, a temporary directory is created and used as the document root.
     * 
     * @param docBase the base directory for the document root, can be null
     * @return the canonical document root file
     * @throws IllegalStateException if the canonical document root cannot be obtained
     */
    private File getCanonicalDocumentRoot(File docBase) {
		try {
			File root = (docBase != null) ? docBase : createTempDir("undertow-docbase");
			return root.getCanonicalFile();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot get canonical document root", ex);
		}
	}

	/**
     * Configures the error pages for the deployment.
     * 
     * @param deployment the DeploymentInfo object to configure
     */
    private void configureErrorPages(DeploymentInfo deployment) {
		for (ErrorPage errorPage : getErrorPages()) {
			deployment.addErrorPage(getUndertowErrorPage(errorPage));
		}
	}

	/**
     * Returns an Undertow ErrorPage object based on the provided ErrorPage object.
     * 
     * @param errorPage the ErrorPage object to convert
     * @return the converted Undertow ErrorPage object
     */
    private io.undertow.servlet.api.ErrorPage getUndertowErrorPage(ErrorPage errorPage) {
		if (errorPage.getStatus() != null) {
			return new io.undertow.servlet.api.ErrorPage(errorPage.getPath(), errorPage.getStatusCode());
		}
		if (errorPage.getException() != null) {
			return new io.undertow.servlet.api.ErrorPage(errorPage.getPath(), errorPage.getException());
		}
		return new io.undertow.servlet.api.ErrorPage(errorPage.getPath());
	}

	/**
     * Configures the MIME mappings for the given deployment.
     * 
     * @param deployment the DeploymentInfo object to configure the MIME mappings for
     */
    private void configureMimeMappings(DeploymentInfo deployment) {
		for (Mapping mimeMapping : getMimeMappings()) {
			deployment.addMimeMapping(new MimeMapping(mimeMapping.getExtension(), mimeMapping.getMimeType()));
		}
	}

	/**
     * Removes superfluous mime mappings from the deployment.
     * 
     * @param deployment the deployment to remove mime mappings from
     * @param deploymentInfo the deployment info containing the mime mappings
     */
    private void removeSuperfluousMimeMappings(DeploymentImpl deployment, DeploymentInfo deploymentInfo) {
		// DeploymentManagerImpl will always add MimeMappings.DEFAULT_MIME_MAPPINGS
		// but we only want ours
		Map<String, String> mappings = new HashMap<>();
		for (MimeMapping mapping : deploymentInfo.getMimeMappings()) {
			mappings.put(mapping.getExtension().toLowerCase(Locale.ENGLISH), mapping.getMimeType());
		}
		deployment.setMimeExtensionMappings(mappings);
	}

	/**
	 * Factory method called to create the {@link UndertowServletWebServer}. Subclasses
	 * can override this method to return a different {@link UndertowServletWebServer} or
	 * apply additional processing to the {@link Builder} and {@link DeploymentManager}
	 * used to bootstrap Undertow
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param port the port that Undertow should listen on
	 * @return a new {@link UndertowServletWebServer} instance
	 */
	protected UndertowServletWebServer getUndertowWebServer(Builder builder, DeploymentManager manager, int port) {
		List<HttpHandlerFactory> initialHandlerFactories = new ArrayList<>();
		initialHandlerFactories.add(new DeploymentManagerHttpHandlerFactory(manager));
		HttpHandlerFactory cooHandlerFactory = getCookieHandlerFactory(manager.getDeployment());
		if (cooHandlerFactory != null) {
			initialHandlerFactories.add(cooHandlerFactory);
		}
		List<HttpHandlerFactory> httpHandlerFactories = this.delegate.createHttpHandlerFactories(this,
				initialHandlerFactories.toArray(new HttpHandlerFactory[0]));
		return new UndertowServletWebServer(builder, httpHandlerFactories, getContextPath(), port >= 0);
	}

	/**
     * Returns the HttpHandlerFactory for handling cookies with SameSite attribute.
     * 
     * @param deployment the Deployment object representing the web application deployment
     * @return the HttpHandlerFactory for handling cookies with SameSite attribute, or null if no SameSite attribute is configured
     */
    private HttpHandlerFactory getCookieHandlerFactory(Deployment deployment) {
		SameSite sessionSameSite = getSession().getCookie().getSameSite();
		List<CookieSameSiteSupplier> suppliers = new ArrayList<>();
		if (sessionSameSite != null) {
			String sessionCookieName = deployment.getServletContext().getSessionCookieConfig().getName();
			suppliers.add(CookieSameSiteSupplier.of(sessionSameSite).whenHasName(sessionCookieName));
		}
		if (!CollectionUtils.isEmpty(getCookieSameSiteSuppliers())) {
			suppliers.addAll(getCookieSameSiteSuppliers());
		}
		return (!suppliers.isEmpty()) ? (next) -> new SuppliedSameSiteCookieHandler(next, suppliers) : null;
	}

	/**
	 * {@link ServletContainerInitializer} to initialize {@link ServletContextInitializer
	 * ServletContextInitializers}.
	 */
	private static class Initializer implements ServletContainerInitializer {

		private final ServletContextInitializer[] initializers;

		/**
         * Constructs a new Initializer with the specified array of ServletContextInitializers.
         * 
         * @param initializers the array of ServletContextInitializers to be used
         */
        Initializer(ServletContextInitializer[] initializers) {
			this.initializers = initializers;
		}

		/**
         * Called when the application starts up.
         * 
         * @param classes         the set of classes to be initialized
         * @param servletContext the servlet context of the application
         * @throws ServletException if an error occurs during startup
         */
        @Override
		public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
			for (ServletContextInitializer initializer : this.initializers) {
				initializer.onStartup(servletContext);
			}
		}

	}

	/**
	 * {@link ResourceManager} that exposes resource in {@code META-INF/resources}
	 * directory of nested (in {@code BOOT-INF/lib} or {@code WEB-INF/lib}) jars.
	 */
	private static final class MetaInfResourcesResourceManager implements ResourceManager {

		private final List<URL> metaInfResourceJarUrls;

		/**
         * Constructs a new MetaInfResourcesResourceManager with the specified list of meta-inf resource jar URLs.
         * 
         * @param metaInfResourceJarUrls the list of meta-inf resource jar URLs
         */
        private MetaInfResourcesResourceManager(List<URL> metaInfResourceJarUrls) {
			this.metaInfResourceJarUrls = metaInfResourceJarUrls;
		}

		/**
         * Closes the resource manager.
         * 
         * @throws IOException if an I/O error occurs while closing the resource manager
         */
        @Override
		public void close() throws IOException {
		}

		/**
         * Retrieves a resource from the specified path.
         * 
         * @param path The path of the resource to retrieve.
         * @return The resource if found, or null if not found.
         */
        @Override
		public Resource getResource(String path) {
			for (URL url : this.metaInfResourceJarUrls) {
				URLResource resource = getMetaInfResource(url, path);
				if (resource != null) {
					return resource;
				}
			}
			return null;
		}

		/**
         * Returns whether the resource change listener is supported.
         * 
         * @return {@code false} if the resource change listener is not supported, {@code true} otherwise.
         */
        @Override
		public boolean isResourceChangeListenerSupported() {
			return false;
		}

		/**
         * Registers a resource change listener.
         *
         * @param listener the resource change listener to be registered
         */
        @Override
		public void registerResourceChangeListener(ResourceChangeListener listener) {
		}

		/**
         * Removes the specified resource change listener from this MetaInfResourcesResourceManager.
         * 
         * @param listener the resource change listener to be removed
         */
        @Override
		public void removeResourceChangeListener(ResourceChangeListener listener) {

		}

		/**
         * Retrieves a resource from the META-INF/resources directory of a JAR file.
         * 
         * @param resourceJar the URL of the JAR file containing the resource
         * @param path the path of the resource within the META-INF/resources directory
         * @return the URLResource object representing the resource, or null if the resource is not found or an error occurs
         */
        private URLResource getMetaInfResource(URL resourceJar, String path) {
			try {
				String urlPath = URLEncoder.encode(ENCODED_SLASH.matcher(path).replaceAll("/"), StandardCharsets.UTF_8);
				URL resourceUrl = new URL(resourceJar + "META-INF/resources" + urlPath);
				URLResource resource = new URLResource(resourceUrl, path);
				if (resource.getContentLength() < 0) {
					return null;
				}
				return resource;
			}
			catch (Exception ex) {
				return null;
			}
		}

	}

	/**
	 * {@link ResourceManager} to hide Spring Boot loader classes.
	 */
	private static final class LoaderHidingResourceManager implements ResourceManager {

		private final ResourceManager delegate;

		/**
         * Constructs a new LoaderHidingResourceManager with the specified delegate ResourceManager.
         * 
         * @param delegate the delegate ResourceManager to be used by this LoaderHidingResourceManager
         */
        private LoaderHidingResourceManager(ResourceManager delegate) {
			this.delegate = delegate;
		}

		/**
         * Retrieves a resource from the specified path.
         * 
         * @param path the path of the resource to retrieve
         * @return the retrieved resource, or null if the path starts with "/org/springframework/boot"
         * @throws IOException if an I/O error occurs while retrieving the resource
         */
        @Override
		public Resource getResource(String path) throws IOException {
			if (path.startsWith("/org/springframework/boot")) {
				return null;
			}
			return this.delegate.getResource(path);
		}

		/**
         * Returns whether the resource change listener is supported by the underlying delegate.
         * 
         * @return {@code true} if the resource change listener is supported, {@code false} otherwise
         */
        @Override
		public boolean isResourceChangeListenerSupported() {
			return this.delegate.isResourceChangeListenerSupported();
		}

		/**
         * Registers a resource change listener with the LoaderHidingResourceManager.
         * 
         * @param listener the resource change listener to be registered
         */
        @Override
		public void registerResourceChangeListener(ResourceChangeListener listener) {
			this.delegate.registerResourceChangeListener(listener);
		}

		/**
         * Removes the specified resource change listener from this LoaderHidingResourceManager.
         * 
         * @param listener the resource change listener to be removed
         */
        @Override
		public void removeResourceChangeListener(ResourceChangeListener listener) {
			this.delegate.removeResourceChangeListener(listener);
		}

		/**
         * Closes the resource manager by closing the delegate resource.
         *
         * @throws IOException if an I/O error occurs while closing the delegate resource
         */
        @Override
		public void close() throws IOException {
			this.delegate.close();
		}

	}

	/**
	 * {@link HttpHandler} to apply {@link CookieSameSiteSupplier supplied}
	 * {@link SameSite} cookie values.
	 */
	private static class SuppliedSameSiteCookieHandler implements HttpHandler {

		private final HttpHandler next;

		private final List<CookieSameSiteSupplier> suppliers;

		/**
         * Constructs a new instance of SuppliedSameSiteCookieHandler with the specified HttpHandler and list of CookieSameSiteSupplier.
         *
         * @param next the HttpHandler to be invoked after processing the cookie
         * @param suppliers the list of CookieSameSiteSupplier to be used for supplying the SameSite attribute value for cookies
         */
        SuppliedSameSiteCookieHandler(HttpHandler next, List<CookieSameSiteSupplier> suppliers) {
			this.next = next;
			this.suppliers = suppliers;
		}

		/**
         * Handles the HTTP server exchange request.
         * 
         * @param exchange the HTTP server exchange object
         * @throws Exception if an error occurs while handling the request
         */
        @Override
		public void handleRequest(HttpServerExchange exchange) throws Exception {
			exchange.addResponseCommitListener(this::beforeCommit);
			this.next.handleRequest(exchange);
		}

		/**
         * Sets the SameSite attribute for each cookie in the response.
         * 
         * @param exchange the HTTP server exchange object
         */
        private void beforeCommit(HttpServerExchange exchange) {
			for (Cookie cookie : exchange.responseCookies()) {
				SameSite sameSite = getSameSite(asServletCookie(cookie));
				if (sameSite != null) {
					cookie.setSameSiteMode(sameSite.attributeValue());
				}
			}
		}

		/**
         * Converts a standard Cookie object to a jakarta.servlet.http.Cookie object.
         * 
         * @param cookie the standard Cookie object to be converted
         * @return the converted jakarta.servlet.http.Cookie object
         * @since 1.0
         * @deprecated This method is marked for removal and should not be used in new code.
         */
        @SuppressWarnings("removal")
		private jakarta.servlet.http.Cookie asServletCookie(Cookie cookie) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			jakarta.servlet.http.Cookie result = new jakarta.servlet.http.Cookie(cookie.getName(), cookie.getValue());
			map.from(cookie::getComment).to(result::setComment);
			map.from(cookie::getDomain).to(result::setDomain);
			map.from(cookie::getMaxAge).to(result::setMaxAge);
			map.from(cookie::getPath).to(result::setPath);
			result.setSecure(cookie.isSecure());
			result.setVersion(cookie.getVersion());
			result.setHttpOnly(cookie.isHttpOnly());
			return result;
		}

		/**
         * Returns the SameSite attribute of the given cookie.
         * 
         * @param cookie the cookie to retrieve the SameSite attribute from
         * @return the SameSite attribute of the cookie, or null if not found
         */
        private SameSite getSameSite(jakarta.servlet.http.Cookie cookie) {
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
