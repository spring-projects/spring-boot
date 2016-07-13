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

package org.springframework.boot.context.embedded.undertow;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowMessages;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MimeMappings.Mapping;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.Ssl.ClientAuth;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * {@link EmbeddedServletContainerFactory} that can be used to create
 * {@link UndertowEmbeddedServletContainer}s.
 * <p>
 * Unless explicitly configured otherwise, the factory will create containers that listen
 * for HTTP requests on port 8080.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Marcos Barbero
 * @author Eddú Meléndez
 * @since 1.2.0
 * @see UndertowEmbeddedServletContainer
 */
public class UndertowEmbeddedServletContainerFactory
		extends AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {

	private static final Set<Class<?>> NO_CLASSES = Collections.emptySet();

	private List<UndertowBuilderCustomizer> builderCustomizers = new ArrayList<UndertowBuilderCustomizer>();

	private List<UndertowDeploymentInfoCustomizer> deploymentInfoCustomizers = new ArrayList<UndertowDeploymentInfoCustomizer>();

	private ResourceLoader resourceLoader;

	private Integer bufferSize;

	private Integer buffersPerRegion;

	private Integer ioThreads;

	private Integer workerThreads;

	private Boolean directBuffers;

	private File accessLogDirectory;

	private String accessLogPattern;

	private boolean accessLogEnabled = false;

	private boolean useForwardHeaders;

	/**
	 * Create a new {@link UndertowEmbeddedServletContainerFactory} instance.
	 */
	public UndertowEmbeddedServletContainerFactory() {
		super();
		getJspServlet().setRegistered(false);
	}

	/**
	 * Create a new {@link UndertowEmbeddedServletContainerFactory} that listens for
	 * requests using the specified port.
	 * @param port the port to listen on
	 */
	public UndertowEmbeddedServletContainerFactory(int port) {
		super(port);
		getJspServlet().setRegistered(false);
	}

	/**
	 * Create a new {@link UndertowEmbeddedServletContainerFactory} with the specified
	 * context path and port.
	 * @param contextPath the root context path
	 * @param port the port to listen on
	 */
	public UndertowEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
		getJspServlet().setRegistered(false);
	}

	/**
	 * Set {@link UndertowBuilderCustomizer}s that should be applied to the Undertow
	 * {@link Builder}. Calling this method will replace any existing customizers.
	 * @param customizers the customizers to set
	 */
	public void setBuilderCustomizers(
			Collection<? extends UndertowBuilderCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers = new ArrayList<UndertowBuilderCustomizer>(customizers);
	}

	/**
	 * Returns a mutable collection of the {@link UndertowBuilderCustomizer}s that will be
	 * applied to the Undertow {@link Builder} .
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
		return this.builderCustomizers;
	}

	/**
	 * Add {@link UndertowBuilderCustomizer}s that should be used to customize the
	 * Undertow {@link Builder}.
	 * @param customizers the customizers to add
	 */
	public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.builderCustomizers.addAll(Arrays.asList(customizers));
	}

	/**
	 * Set {@link UndertowDeploymentInfoCustomizer}s that should be applied to the
	 * Undertow {@link DeploymentInfo}. Calling this method will replace any existing
	 * customizers.
	 * @param customizers the customizers to set
	 */
	public void setDeploymentInfoCustomizers(
			Collection<? extends UndertowDeploymentInfoCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.deploymentInfoCustomizers = new ArrayList<UndertowDeploymentInfoCustomizer>(
				customizers);
	}

	/**
	 * Returns a mutable collection of the {@link UndertowDeploymentInfoCustomizer}s that
	 * will be applied to the Undertow {@link DeploymentInfo} .
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowDeploymentInfoCustomizer> getDeploymentInfoCustomizers() {
		return this.deploymentInfoCustomizers;
	}

	/**
	 * Add {@link UndertowDeploymentInfoCustomizer}s that should be used to customize the
	 * Undertow {@link DeploymentInfo}.
	 * @param customizers the customizers to add
	 */
	public void addDeploymentInfoCustomizers(
			UndertowDeploymentInfoCustomizer... customizers) {
		Assert.notNull(customizers, "UndertowDeploymentInfoCustomizers must not be null");
		this.deploymentInfoCustomizers.addAll(Arrays.asList(customizers));
	}

	@Override
	public EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers) {
		DeploymentManager manager = createDeploymentManager(initializers);
		int port = getPort();
		Builder builder = createBuilder(port);
		return getUndertowEmbeddedServletContainer(builder, manager, port);
	}

	private Builder createBuilder(int port) {
		Builder builder = Undertow.builder();
		if (this.bufferSize != null) {
			builder.setBufferSize(this.bufferSize);
		}
		if (this.buffersPerRegion != null) {
			builder.setBuffersPerRegion(this.buffersPerRegion);
		}
		if (this.ioThreads != null) {
			builder.setIoThreads(this.ioThreads);
		}
		if (this.workerThreads != null) {
			builder.setWorkerThreads(this.workerThreads);
		}
		if (this.directBuffers != null) {
			builder.setDirectBuffers(this.directBuffers);
		}
		if (getSsl() != null && getSsl().isEnabled()) {
			configureSsl(getSsl(), port, builder);
		}
		else {
			builder.addHttpListener(port, getListenAddress());
		}
		for (UndertowBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
		return builder;
	}

	private void configureSsl(Ssl ssl, int port, Builder builder) {
		try {
			SSLContext sslContext = SSLContext.getInstance(ssl.getProtocol());
			sslContext.init(getKeyManagers(), getTrustManagers(), null);
			builder.addHttpsListener(port, getListenAddress(), sslContext);
			builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE,
					getSslClientAuthMode(ssl));
			if (ssl.getEnabledProtocols() != null) {
				builder.setSocketOption(Options.SSL_ENABLED_PROTOCOLS,
						Sequence.of(ssl.getEnabledProtocols()));
			}
			if (ssl.getCiphers() != null) {
				builder.setSocketOption(Options.SSL_ENABLED_CIPHER_SUITES,
						Sequence.of(ssl.getCiphers()));
			}
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
		catch (KeyManagementException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private String getListenAddress() {
		if (getAddress() == null) {
			return "0.0.0.0";
		}
		return getAddress().getHostAddress();
	}

	private SslClientAuthMode getSslClientAuthMode(Ssl ssl) {
		if (ssl.getClientAuth() == ClientAuth.NEED) {
			return SslClientAuthMode.REQUIRED;
		}
		if (ssl.getClientAuth() == ClientAuth.WANT) {
			return SslClientAuthMode.REQUESTED;
		}
		return SslClientAuthMode.NOT_REQUESTED;
	}

	private KeyManager[] getKeyManagers() {
		try {
			KeyStore keyStore = getKeyStore();
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			Ssl ssl = getSsl();
			char[] keyPassword = (ssl.getKeyPassword() != null
					? ssl.getKeyPassword().toCharArray() : null);
			if (keyPassword == null && ssl.getKeyStorePassword() != null) {
				keyPassword = ssl.getKeyStorePassword().toCharArray();
			}
			keyManagerFactory.init(keyStore, keyPassword);
			return keyManagerFactory.getKeyManagers();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private KeyStore getKeyStore() throws Exception {
		if (getSslStoreProvider() != null) {
			return getSslStoreProvider().getKeyStore();
		}
		Ssl ssl = getSsl();
		return loadKeyStore(ssl.getKeyStoreType(), ssl.getKeyStore(),
				ssl.getKeyStorePassword());
	}

	private TrustManager[] getTrustManagers() {
		try {
			KeyStore store = getTrustStore();
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(store);
			return trustManagerFactory.getTrustManagers();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private KeyStore getTrustStore() throws Exception {
		if (getSslStoreProvider() != null) {
			return getSslStoreProvider().getTrustStore();
		}
		Ssl ssl = getSsl();
		return loadKeyStore(ssl.getTrustStoreType(), ssl.getTrustStore(),
				ssl.getTrustStorePassword());
	}

	private KeyStore loadKeyStore(String type, String resource, String password)
			throws Exception {
		type = (type == null ? "JKS" : type);
		if (resource == null) {
			return null;
		}
		KeyStore store = KeyStore.getInstance(type);
		URL url = ResourceUtils.getURL(resource);
		store.load(url.openStream(), password == null ? null : password.toCharArray());
		return store;
	}

	private DeploymentManager createDeploymentManager(
			ServletContextInitializer... initializers) {
		DeploymentInfo deployment = Servlets.deployment();
		registerServletContainerInitializerToDriveServletContextInitializers(deployment,
				initializers);
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
		configureMimeMappings(deployment);
		for (UndertowDeploymentInfoCustomizer customizer : this.deploymentInfoCustomizers) {
			customizer.customize(deployment);
		}
		if (isAccessLogEnabled()) {
			configureAccessLog(deployment);
		}
		if (isPersistSession()) {
			File dir = getValidSessionStoreDir();
			deployment.setSessionPersistenceManager(new FileSessionPersistence(dir));
		}
		DeploymentManager manager = Servlets.newContainer().addDeployment(deployment);
		manager.deploy();
		SessionManager sessionManager = manager.getDeployment().getSessionManager();
		int sessionTimeout = (getSessionTimeout() > 0 ? getSessionTimeout() : -1);
		sessionManager.setDefaultSessionTimeout(sessionTimeout);
		return manager;
	}

	private void configureAccessLog(DeploymentInfo deploymentInfo) {
		deploymentInfo.addInitialHandlerChainWrapper(new HandlerWrapper() {

			@Override
			public HttpHandler wrap(HttpHandler handler) {
				return createAccessLogHandler(handler);
			}

		});
	}

	private AccessLogHandler createAccessLogHandler(HttpHandler handler) {
		try {
			createAccessLogDirectoryIfNecessary();
			AccessLogReceiver accessLogReceiver = new DefaultAccessLogReceiver(
					createWorker(), this.accessLogDirectory, "access_log.");
			String formatString = (this.accessLogPattern != null) ? this.accessLogPattern
					: "common";
			return new AccessLogHandler(handler, accessLogReceiver, formatString,
					Undertow.class.getClassLoader());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to create AccessLogHandler", ex);
		}
	}

	private void createAccessLogDirectoryIfNecessary() {
		Assert.state(this.accessLogDirectory != null, "Access log directory is not set");
		if (!this.accessLogDirectory.isDirectory() && !this.accessLogDirectory.mkdirs()) {
			throw new IllegalStateException("Failed to create access log directory '"
					+ this.accessLogDirectory + "'");
		}
	}

	private XnioWorker createWorker() throws IOException {
		Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
		return xnio.createWorker(
				OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap());
	}

	private void registerServletContainerInitializerToDriveServletContextInitializers(
			DeploymentInfo deployment, ServletContextInitializer... initializers) {
		ServletContextInitializer[] mergedInitializers = mergeInitializers(initializers);
		Initializer initializer = new Initializer(mergedInitializers);
		deployment.addServletContainerInitalizer(new ServletContainerInitializerInfo(
				Initializer.class,
				new ImmediateInstanceFactory<ServletContainerInitializer>(initializer),
				NO_CLASSES));
	}

	private ClassLoader getServletClassLoader() {
		if (this.resourceLoader != null) {
			return this.resourceLoader.getClassLoader();
		}
		return getClass().getClassLoader();
	}

	private ResourceManager getDocumentRootResourceManager() {
		File root = getCanonicalDocumentRoot();
		if (root.isDirectory()) {
			return new FileResourceManager(root, 0);
		}
		if (root.isFile()) {
			return new JarResourceManager(root);
		}
		return ResourceManager.EMPTY_RESOURCE_MANAGER;
	}

	/**
	 * Return the document root in canonical form. Undertow uses File#getCanonicalFile()
	 * to determine whether a resource has been requested using the proper case but on
	 * Windows {@code java.io.tmpdir} may be set as a tilde-compressed pathname.
	 * @return the canonical document root
	 */
	private File getCanonicalDocumentRoot() {
		try {
			File root = getValidDocumentRoot();
			root = (root != null ? root : createTempDir("undertow-docbase"));
			return root.getCanonicalFile();
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot get canonical document root", e);
		}
	}

	private void configureErrorPages(DeploymentInfo servletBuilder) {
		for (ErrorPage errorPage : getErrorPages()) {
			servletBuilder.addErrorPage(getUndertowErrorPage(errorPage));
		}
	}

	private io.undertow.servlet.api.ErrorPage getUndertowErrorPage(ErrorPage errorPage) {
		if (errorPage.getStatus() != null) {
			return new io.undertow.servlet.api.ErrorPage(errorPage.getPath(),
					errorPage.getStatusCode());
		}
		if (errorPage.getException() != null) {
			return new io.undertow.servlet.api.ErrorPage(errorPage.getPath(),
					errorPage.getException());
		}
		return new io.undertow.servlet.api.ErrorPage(errorPage.getPath());
	}

	private void configureMimeMappings(DeploymentInfo servletBuilder) {
		for (Mapping mimeMapping : getMimeMappings()) {
			servletBuilder.addMimeMapping(new MimeMapping(mimeMapping.getExtension(),
					mimeMapping.getMimeType()));
		}
	}

	/**
	 * Factory method called to create the {@link UndertowEmbeddedServletContainer}.
	 * Subclasses can override this method to return a different
	 * {@link UndertowEmbeddedServletContainer} or apply additional processing to the
	 * {@link Builder} and {@link DeploymentManager} used to bootstrap Undertow
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param port the port that Undertow should listen on
	 * @return a new {@link UndertowEmbeddedServletContainer} instance
	 */
	protected UndertowEmbeddedServletContainer getUndertowEmbeddedServletContainer(
			Builder builder, DeploymentManager manager, int port) {
		return new UndertowEmbeddedServletContainer(builder, manager, getContextPath(),
				isUseForwardHeaders(), port >= 0, getCompression(), getServerHeader());
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void setBufferSize(Integer bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void setBuffersPerRegion(Integer buffersPerRegion) {
		this.buffersPerRegion = buffersPerRegion;
	}

	public void setIoThreads(Integer ioThreads) {
		this.ioThreads = ioThreads;
	}

	public void setWorkerThreads(Integer workerThreads) {
		this.workerThreads = workerThreads;
	}

	public void setDirectBuffers(Boolean directBuffers) {
		this.directBuffers = directBuffers;
	}

	public void setAccessLogDirectory(File accessLogDirectory) {
		this.accessLogDirectory = accessLogDirectory;
	}

	public void setAccessLogPattern(String accessLogPattern) {
		this.accessLogPattern = accessLogPattern;
	}

	public void setAccessLogEnabled(boolean accessLogEnabled) {
		this.accessLogEnabled = accessLogEnabled;
	}

	public boolean isAccessLogEnabled() {
		return this.accessLogEnabled;
	}

	protected final boolean isUseForwardHeaders() {
		return this.useForwardHeaders;
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
	 * Undertow {@link ResourceManager} for JAR resources.
	 */
	private static class JarResourceManager implements ResourceManager {

		private final String jarPath;

		JarResourceManager(File jarFile) {
			this(jarFile.getAbsolutePath());
		}

		JarResourceManager(String jarPath) {
			this.jarPath = jarPath;
		}

		@Override
		public Resource getResource(String path) throws IOException {
			URL url = new URL("jar:file:" + this.jarPath + "!" + path);
			URLResource resource = new URLResource(url, url.openConnection(), path);
			if (resource.getContentLength() < 0) {
				return null;
			}
			return resource;
		}

		@Override
		public boolean isResourceChangeListenerSupported() {
			return false;
		}

		@Override
		public void registerResourceChangeListener(ResourceChangeListener listener) {
			throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();

		}

		@Override
		public void removeResourceChangeListener(ResourceChangeListener listener) {
			throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
		}

		@Override
		public void close() throws IOException {
		}

	}

	/**
	 * {@link ServletContainerInitializer} to initialize {@link ServletContextInitializer
	 * ServletContextInitializers}.
	 */
	private static class Initializer implements ServletContainerInitializer {

		private final ServletContextInitializer[] initializers;

		Initializer(ServletContextInitializer[] initializers) {
			this.initializers = initializers;
		}

		@Override
		public void onStartup(Set<Class<?>> classes, ServletContext servletContext)
				throws ServletException {
			for (ServletContextInitializer initializer : this.initializers) {
				initializer.onStartup(servletContext);
			}
		}

	}

}
