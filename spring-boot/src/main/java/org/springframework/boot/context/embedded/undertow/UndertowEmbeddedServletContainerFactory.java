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

package org.springframework.boot.context.embedded.undertow;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceHandle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.boot.context.embedded.MimeMappings.Mapping;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.Ssl.ClientAuth;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SocketUtils;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;
import static org.xnio.SslClientAuthMode.NOT_REQUESTED;
import static org.xnio.SslClientAuthMode.REQUESTED;
import static org.xnio.SslClientAuthMode.REQUIRED;

/**
 * {@link EmbeddedServletContainerFactory} that can be used to create
 * {@link UndertowEmbeddedServletContainer}s.
 * <p>
 * Unless explicitly configured otherwise, the factory will create containers that listen
 * for HTTP requests on port 8080.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @since 1.2.0
 * @see UndertowEmbeddedServletContainer
 */
public class UndertowEmbeddedServletContainerFactory extends
		AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {

	private List<UndertowBuilderCustomizer> undertowBuilderCustomizers = new ArrayList<UndertowBuilderCustomizer>();

	private ResourceLoader resourceLoader;

	private Integer bufferSize;

	private Integer buffersPerRegion;

	private Integer ioThreads;

	private Integer workerThreads;

	private Boolean directBuffers;

	/**
	 * Create a new {@link UndertowEmbeddedServletContainerFactory} instance.
	 */
	public UndertowEmbeddedServletContainerFactory() {
		super();
		setRegisterJspServlet(false);
	}

	/**
	 * Create a new {@link UndertowEmbeddedServletContainerFactory} that listens for
	 * requests using the specified port.
	 * @param port the port to listen on
	 */
	public UndertowEmbeddedServletContainerFactory(int port) {
		super(port);
		setRegisterJspServlet(false);
	}

	/**
	 * Create a new {@link UndertowEmbeddedServletContainerFactory} with the specified
	 * context path and port.
	 * @param contextPath root the context path
	 * @param port the port to listen on
	 */
	public UndertowEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
		setRegisterJspServlet(false);
	}

	/**
	 * Set {@link UndertowBuilderCustomizer}s that should be applied to the Undertow
	 * {@link Builder}. Calling this method will replace any existing customizers.
	 * @param undertowBuilderCustomizers the customizers to set
	 */
	public void setUndertowBuilderCustomizers(
			Collection<? extends UndertowBuilderCustomizer> undertowBuilderCustomizers) {
		Assert.notNull(undertowBuilderCustomizers,
				"undertowBuilderCustomizers must not be null");
		this.undertowBuilderCustomizers = new ArrayList<UndertowBuilderCustomizer>(
				undertowBuilderCustomizers);
	}

	/**
	 * Returns a mutable collection of the {@link UndertowBuilderCustomizer}s that will be
	 * applied to the Undertow {@link Builder} .
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowBuilderCustomizer> getUndertowBuilderCustomizers() {
		return this.undertowBuilderCustomizers;
	}

	/**
	 * Add {@link UndertowBuilderCustomizer}s that should be used to customize the
	 * Undertow {@link Builder}.
	 * @param undertowBuilderCustomizers the customizers to add
	 */
	public void addUndertowBuilderCustomizers(
			UndertowBuilderCustomizer... undertowBuilderCustomizers) {
		Assert.notNull(undertowBuilderCustomizers,
				"undertowBuilderCustomizers must not be null");
		this.undertowBuilderCustomizers.addAll(Arrays.asList(undertowBuilderCustomizers));
	}

	@Override
	public EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers) {
		DeploymentManager manager = createDeploymentManager(initializers);

		int port = getPort();
		if (port == 0) {
			port = SocketUtils.findAvailableTcpPort(40000);
		}

		Builder builder = createBuilder(port);

		return new UndertowEmbeddedServletContainer(builder, manager, getContextPath(),
				port, port >= 0);
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

		if (getSsl() == null) {
			builder.addHttpListener(port, "0.0.0.0");
		}
		else {
			configureSsl(port, builder);
		}
		for (UndertowBuilderCustomizer customizer : this.undertowBuilderCustomizers) {
			customizer.customize(builder);
		}
		return builder;
	}

	private void configureSsl(int port, Builder builder) {
		try {
			Ssl ssl = getSsl();
			SSLContext sslContext = SSLContext.getInstance(ssl.getProtocol());
			sslContext.init(getKeyManagers(), getTrustManagers(), null);
			builder.addHttpsListener(port, "0.0.0.0", sslContext);
			if (ssl.getClientAuth() == ClientAuth.NEED) {
				builder.setSocketOption(SSL_CLIENT_AUTH_MODE, REQUIRED);
			}
			else if (ssl.getClientAuth() == ClientAuth.WANT) {
				builder.setSocketOption(SSL_CLIENT_AUTH_MODE, REQUESTED);
			}
			else {
				builder.setSocketOption(SSL_CLIENT_AUTH_MODE, NOT_REQUESTED);
			}
		}
		catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
		catch (KeyManagementException ex) {
			throw new RuntimeException(ex);
		}
	}

	private KeyManager[] getKeyManagers() {
		try {
			Ssl ssl = getSsl();

			String keyStoreType = ssl.getKeyStoreType();
			if (keyStoreType == null) {
				keyStoreType = "JKS";
			}
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			URL url = ResourceUtils.getURL(ssl.getKeyStore());
			keyStore.load(url.openStream(), ssl.getKeyStorePassword().toCharArray());

			// Get key manager to provide client credentials.
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			char[] keyPassword = ssl.getKeyPassword() != null ? ssl.getKeyPassword()
					.toCharArray() : ssl.getKeyStorePassword().toCharArray();
			keyManagerFactory.init(keyStore, keyPassword);
			return keyManagerFactory.getKeyManagers();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private TrustManager[] getTrustManagers() {
		try {
			Ssl ssl = getSsl();

			String trustStoreType = ssl.getTrustStoreType();
			if (trustStoreType == null) {
				trustStoreType = "JKS";
			}
			String trustStore = ssl.getTrustStore();
			if (trustStore == null) {
				return null;
			}
			KeyStore trustedKeyStore = KeyStore.getInstance(trustStoreType);
			URL url = ResourceUtils.getURL(trustStore);
			trustedKeyStore.load(url.openStream(), ssl.getTrustStorePassword()
					.toCharArray());

			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(trustedKeyStore);
			return trustManagerFactory.getTrustManagers();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private DeploymentManager createDeploymentManager(
			ServletContextInitializer... initializers) {
		DeploymentInfo servletBuilder = deployment();

		servletBuilder.addListener(new ListenerInfo(
				UndertowSpringServletContextListener.class,
				new UndertowSpringServletContextListenerFactory(
						new UndertowSpringServletContextListener(
								mergeInitializers(initializers)))));

		if (this.resourceLoader != null) {
			servletBuilder.setClassLoader(this.resourceLoader.getClassLoader());
		}
		else {
			servletBuilder.setClassLoader(getClass().getClassLoader());
		}
		servletBuilder.setContextPath(getContextPath());
		servletBuilder.setDeploymentName("spring-boot");
		if (isRegisterDefaultServlet()) {
			servletBuilder.addServlet(servlet("default", DefaultServlet.class));
		}

		configureErrorPages(servletBuilder);
		servletBuilder.setServletStackTraces(ServletStackTraces.NONE);

		File root = getValidDocumentRoot();
		if (root != null && root.isDirectory()) {
			servletBuilder.setResourceManager(new FileResourceManager(root, 0));
		}
		else if (root != null && root.isFile()) {
			servletBuilder.setResourceManager(new JarResourcemanager(root));
		}
		else if (this.resourceLoader != null) {
			servletBuilder.setResourceManager(new ClassPathResourceManager(
					this.resourceLoader.getClassLoader(), ""));
		}
		else {
			servletBuilder.setResourceManager(new ClassPathResourceManager(getClass()
					.getClassLoader(), ""));
		}
		for (Mapping mimeMapping : getMimeMappings()) {
			servletBuilder.addMimeMapping(new MimeMapping(mimeMapping.getExtension(),
					mimeMapping.getMimeType()));
		}

		DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);

		manager.deploy();

		manager.getDeployment().getSessionManager()
				.setDefaultSessionTimeout(getSessionTimeout());
		return manager;
	}

	private void configureErrorPages(DeploymentInfo servletBuilder) {
		for (ErrorPage errorPage : getErrorPages()) {
			if (errorPage.getStatus() != null) {
				io.undertow.servlet.api.ErrorPage undertowErrorpage = new io.undertow.servlet.api.ErrorPage(
						errorPage.getPath(), errorPage.getStatusCode());
				servletBuilder.addErrorPage(undertowErrorpage);
			}
			else if (errorPage.getException() != null) {
				io.undertow.servlet.api.ErrorPage undertowErrorpage = new io.undertow.servlet.api.ErrorPage(
						errorPage.getPath(), errorPage.getException());
				servletBuilder.addErrorPage(undertowErrorpage);
			}
			else {
				io.undertow.servlet.api.ErrorPage undertowErrorpage = new io.undertow.servlet.api.ErrorPage(
						errorPage.getPath());
				servletBuilder.addErrorPage(undertowErrorpage);
			}
		}
	}

	/**
	 * Factory method called to create the {@link UndertowEmbeddedServletContainer}.
	 * Subclasses can override this method to return a different
	 * {@link UndertowEmbeddedServletContainer} or apply additional processing to the
	 * {@link Builder} and {@link DeploymentManager} used to bootstrap Undertow
	 *
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param port the port that Undertow should listen on
	 * @return a new {@link UndertowEmbeddedServletContainer} instance
	 */
	protected UndertowEmbeddedServletContainer getUndertowEmbeddedServletContainer(
			Builder builder, DeploymentManager manager, int port) {
		return new UndertowEmbeddedServletContainer(builder, manager, getContextPath(),
				port, port >= 0);
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

	@Override
	public void setRegisterJspServlet(boolean registerJspServlet) {
		Assert.isTrue(!registerJspServlet, "Undertow does not support JSPs");
		super.setRegisterJspServlet(registerJspServlet);
	}

	private static class JarResourcemanager implements ResourceManager {
		private final String jarPath;

		public JarResourcemanager(File jarFile) {
			this(jarFile.getAbsolutePath());
		}

		public JarResourcemanager(String jarPath) {
			this.jarPath = jarPath;
		}

		@Override
		public void close() throws IOException {
			// no code
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

	}

	private static class UndertowSpringServletContextListenerFactory implements
			InstanceFactory<UndertowSpringServletContextListener> {

		private final UndertowSpringServletContextListener listener;

		public UndertowSpringServletContextListenerFactory(
				UndertowSpringServletContextListener listener) {
			this.listener = listener;
		}

		@Override
		public InstanceHandle<UndertowSpringServletContextListener> createInstance()
				throws InstantiationException {
			return new ImmediateInstanceHandle<UndertowSpringServletContextListener>(
					this.listener);
		}

	}

	private static class UndertowSpringServletContextListener implements
			ServletContextListener {
		private final ServletContextInitializer[] initializers;

		public UndertowSpringServletContextListener(
				ServletContextInitializer... initializers) {
			this.initializers = initializers;
		}

		@Override
		public void contextInitialized(ServletContextEvent event) {
			try {
				for (ServletContextInitializer initializer : this.initializers) {
					initializer.onStartup(event.getServletContext());
				}
			}
			catch (ServletException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			// no code
		}
	}

}
