package org.springframework.boot.context.embedded.undertow;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;
import static org.xnio.SslClientAuthMode.NOT_REQUESTED;
import static org.xnio.SslClientAuthMode.REQUESTED;
import static org.xnio.SslClientAuthMode.REQUIRED;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

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
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.boot.context.embedded.MimeMappings.Mapping;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.Ssl.ClientAuth;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SocketUtils;

/**
 * @author Ivan Sopov
 */
public class UndertowEmbeddedServletContainerFactory extends
		AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {

	private ResourceLoader resourceLoader;

	private Integer bufferSize;
	private Integer buffersPerRegion;
	private Integer ioThreads;
	private Integer workerThreads;
	private Boolean directBuffers;

	/**
	 * Create a new
	 * {@link org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory}
	 * instance.
	 */
	public UndertowEmbeddedServletContainerFactory() {
		super();
	}

	/**
	 * Create a new
	 * {@link org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory}
	 * that listens for requests using the specified port.
	 *
	 * @param port the port to listen on
	 */
	public UndertowEmbeddedServletContainerFactory(int port) {
		super(port);
	}

	/**
	 * Create a new
	 * {@link org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory}
	 * with the specified context path and port.
	 *
	 * @param contextPath root the context path
	 * @param port the port to listen on
	 */
	public UndertowEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
	}

	@Override
	public EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers) {
		DeploymentInfo servletBuilder = deployment();

		servletBuilder.addListener(new ListenerInfo(
				UndertowSpringServletContextListener.class,
				new UndertowSpringServletContextListenerFactory(
						new UndertowSpringServletContextListener(
								mergeInitializers(initializers)))));

		if (resourceLoader != null) {
			servletBuilder.setClassLoader(resourceLoader.getClassLoader());
		}
		else {
			servletBuilder.setClassLoader(getClass().getClassLoader());
		}
		servletBuilder.setContextPath(getContextPath());
		servletBuilder.setDeploymentName("spring-boot");
		if (isRegisterDefaultServlet()) {
			servletBuilder.addServlet(servlet("default", DefaultServlet.class));
		}
		if (isRegisterJspServlet()) {
			logger.error("JSPs are not supported with Undertow");
		}
		for (ErrorPage springErrorPage : getErrorPages()) {
			if (springErrorPage.getStatus() != null) {
				io.undertow.servlet.api.ErrorPage undertowErrorpage = new io.undertow.servlet.api.ErrorPage(
						springErrorPage.getPath(), springErrorPage.getStatusCode());
				servletBuilder.addErrorPage(undertowErrorpage);
			}
			else if (springErrorPage.getException() != null) {
				io.undertow.servlet.api.ErrorPage undertowErrorpage = new io.undertow.servlet.api.ErrorPage(
						springErrorPage.getPath(), springErrorPage.getException());
				servletBuilder.addErrorPage(undertowErrorpage);
			}
			else {
				io.undertow.servlet.api.ErrorPage undertowErrorpage = new io.undertow.servlet.api.ErrorPage(
						springErrorPage.getPath());
				servletBuilder.addErrorPage(undertowErrorpage);
			}
		}
		servletBuilder.setServletStackTraces(ServletStackTraces.NONE);

		File root = getValidDocumentRoot();
		if (root != null && root.isDirectory()) {
			servletBuilder.setResourceManager(new FileResourceManager(root, 0));
		}
		else if (root != null && root.isFile()) {
			servletBuilder.setResourceManager(new JarResourcemanager(root));
		}
		else if (resourceLoader != null) {
			servletBuilder.setResourceManager(new ClassPathResourceManager(resourceLoader
					.getClassLoader(), ""));
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

		Builder builder = Undertow.builder();
		if (bufferSize != null) {
			builder.setBufferSize(bufferSize);
		}
		if (buffersPerRegion != null) {
			builder.setBuffersPerRegion(buffersPerRegion);
		}
		if (ioThreads != null) {
			builder.setIoThreads(ioThreads);
		}
		if (workerThreads != null) {
			builder.setWorkerThreads(workerThreads);
		}
		if (directBuffers != null) {
			builder.setDirectBuffers(directBuffers);
		}

		int realPort = getPort();
		if (realPort == 0) {
			realPort = SocketUtils.findAvailableTcpPort(40000);
		}
		if (getSsl() == null) {
			builder.addHttpListener(realPort, "0.0.0.0");
		}
		else {
			try {
				Ssl ssl = getSsl();
				SSLContext sslContext = SSLContext.getInstance(ssl.getProtocol());
				sslContext.init(getKeyManagers(), getTrustManagers(), null);
				builder.addHttpsListener(realPort, "0.0.0.0", sslContext);
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
			catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			catch (KeyManagementException e) {
				throw new RuntimeException(e);
			}
		}
		return new UndertowEmbeddedServletContainer(builder, manager, getContextPath(),
				realPort, realPort > 0);

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
		catch (Exception e) {
			throw new RuntimeException(e);
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
		catch (Exception e) {
			throw new RuntimeException(e);
		}

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
			URL url = new URL("jar:file:" + jarPath + "!" + path);
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
					listener);
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
		public void contextInitialized(ServletContextEvent sce) {
			try {
				for (ServletContextInitializer initializer : initializers) {
					initializer.onStartup(sce.getServletContext());
				}
			}
			catch (ServletException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			// no code
		}
	}

}
