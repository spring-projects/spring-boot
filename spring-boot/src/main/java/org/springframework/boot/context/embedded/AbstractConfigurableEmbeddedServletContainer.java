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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@link ConfigurableEmbeddedServletContainer} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Ivan Sopov
 * @author Eddú Meléndez
 * @see AbstractEmbeddedServletContainerFactory
 */
public abstract class AbstractConfigurableEmbeddedServletContainer
		implements ConfigurableEmbeddedServletContainer {

	private static final int DEFAULT_SESSION_TIMEOUT = (int) TimeUnit.MINUTES
			.toSeconds(30);

	private String contextPath = "";

	private String displayName;

	private boolean registerDefaultServlet = true;

	private int port = 8080;

	private List<ServletContextInitializer> initializers = new ArrayList<ServletContextInitializer>();

	private File documentRoot;

	private Set<ErrorPage> errorPages = new LinkedHashSet<ErrorPage>();

	private MimeMappings mimeMappings = new MimeMappings(MimeMappings.DEFAULT);

	private InetAddress address;

	private int sessionTimeout = DEFAULT_SESSION_TIMEOUT;

	private boolean persistSession;

	private File sessionStoreDir;

	private Ssl ssl;

	private SslStoreProvider sslStoreProvider;

	private JspServlet jspServlet = new JspServlet();

	private Compression compression;

	private String serverHeader;

	/**
	 * Create a new {@link AbstractConfigurableEmbeddedServletContainer} instance.
	 */
	public AbstractConfigurableEmbeddedServletContainer() {
	}

	/**
	 * Create a new {@link AbstractConfigurableEmbeddedServletContainer} instance with the
	 * specified port.
	 * @param port the port number for the embedded servlet container
	 */
	public AbstractConfigurableEmbeddedServletContainer(int port) {
		this.port = port;
	}

	/**
	 * Create a new {@link AbstractConfigurableEmbeddedServletContainer} instance with the
	 * specified context path and port.
	 * @param contextPath the context path for the embedded servlet container
	 * @param port the port number for the embedded servlet container
	 */
	public AbstractConfigurableEmbeddedServletContainer(String contextPath, int port) {
		checkContextPath(contextPath);
		this.contextPath = contextPath;
		this.port = port;
	}

	@Override
	public void setContextPath(String contextPath) {
		checkContextPath(contextPath);
		this.contextPath = contextPath;
	}

	private void checkContextPath(String contextPath) {
		Assert.notNull(contextPath, "ContextPath must not be null");
		if (contextPath.length() > 0) {
			if ("/".equals(contextPath)) {
				throw new IllegalArgumentException(
						"Root ContextPath must be specified using an empty string");
			}
			if (!contextPath.startsWith("/") || contextPath.endsWith("/")) {
				throw new IllegalArgumentException(
						"ContextPath must start with '/' and not end with '/'");
			}
		}
	}

	/**
	 * Returns the context path for the embedded servlet container. The path will start
	 * with "/" and not end with "/". The root context is represented by an empty string.
	 * @return the context path
	 */
	public String getContextPath() {
		return this.contextPath;
	}

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * The port that the embedded server listens on.
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	@Override
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * Return the address that the embedded container binds to.
	 * @return the address
	 */
	public InetAddress getAddress() {
		return this.address;
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	@Override
	public void setSessionTimeout(int sessionTimeout, TimeUnit timeUnit) {
		Assert.notNull(timeUnit, "TimeUnit must not be null");
		this.sessionTimeout = (int) timeUnit.toSeconds(sessionTimeout);
	}

	/**
	 * Return the session timeout in seconds.
	 * @return the timeout in seconds
	 */
	public int getSessionTimeout() {
		return this.sessionTimeout;
	}

	@Override
	public void setPersistSession(boolean persistSession) {
		this.persistSession = persistSession;
	}

	public boolean isPersistSession() {
		return this.persistSession;
	}

	@Override
	public void setSessionStoreDir(File sessionStoreDir) {
		this.sessionStoreDir = sessionStoreDir;
	}

	public File getSessionStoreDir() {
		return this.sessionStoreDir;
	}

	@Override
	public void setInitializers(List<? extends ServletContextInitializer> initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers = new ArrayList<ServletContextInitializer>(initializers);
	}

	@Override
	public void addInitializers(ServletContextInitializer... initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers.addAll(Arrays.asList(initializers));
	}

	@Override
	public void setDocumentRoot(File documentRoot) {
		this.documentRoot = documentRoot;
	}

	/**
	 * Returns the document root which will be used by the web context to serve static
	 * files.
	 * @return the document root
	 */
	public File getDocumentRoot() {
		return this.documentRoot;
	}

	@Override
	public void setErrorPages(Set<? extends ErrorPage> errorPages) {
		Assert.notNull(errorPages, "ErrorPages must not be null");
		this.errorPages = new LinkedHashSet<ErrorPage>(errorPages);
	}

	@Override
	public void addErrorPages(ErrorPage... errorPages) {
		Assert.notNull(errorPages, "ErrorPages must not be null");
		this.errorPages.addAll(Arrays.asList(errorPages));
	}

	/**
	 * Returns a mutable set of {@link ErrorPage ErrorPages} that will be used when
	 * handling exceptions.
	 * @return the error pages
	 */
	public Set<ErrorPage> getErrorPages() {
		return this.errorPages;
	}

	@Override
	public void setMimeMappings(MimeMappings mimeMappings) {
		this.mimeMappings = new MimeMappings(mimeMappings);
	}

	/**
	 * Returns the mime-type mappings.
	 * @return the mimeMappings the mime-type mappings.
	 */
	public MimeMappings getMimeMappings() {
		return this.mimeMappings;
	}

	@Override
	public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
		this.registerDefaultServlet = registerDefaultServlet;
	}

	/**
	 * Flag to indicate that the default servlet should be registered.
	 * @return true if the default servlet is to be registered
	 */
	public boolean isRegisterDefaultServlet() {
		return this.registerDefaultServlet;
	}

	@Override
	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	@Override
	public void setSslStoreProvider(SslStoreProvider sslStoreProvider) {
		this.sslStoreProvider = sslStoreProvider;
	}

	public SslStoreProvider getSslStoreProvider() {
		return this.sslStoreProvider;
	}

	@Override
	public void setJspServlet(JspServlet jspServlet) {
		this.jspServlet = jspServlet;
	}

	public JspServlet getJspServlet() {
		return this.jspServlet;
	}

	public Compression getCompression() {
		return this.compression;
	}

	@Override
	public void setCompression(Compression compression) {
		this.compression = compression;
	}

	public String getServerHeader() {
		return this.serverHeader;
	}

	@Override
	public void setServerHeader(String serverHeader) {
		this.serverHeader = serverHeader;
	}

	/**
	 * Utility method that can be used by subclasses wishing to combine the specified
	 * {@link ServletContextInitializer} parameters with those defined in this instance.
	 * @param initializers the initializers to merge
	 * @return a complete set of merged initializers (with the specified parameters
	 * appearing first)
	 */
	protected final ServletContextInitializer[] mergeInitializers(
			ServletContextInitializer... initializers) {
		List<ServletContextInitializer> mergedInitializers = new ArrayList<ServletContextInitializer>();
		mergedInitializers.addAll(Arrays.asList(initializers));
		mergedInitializers.addAll(this.initializers);
		return mergedInitializers
				.toArray(new ServletContextInitializer[mergedInitializers.size()]);
	}

	/**
	 * Returns whether or not the JSP servlet should be registered with the embedded
	 * container.
	 * @return {@code true} if the container should be registered, otherwise {@code false}
	 */
	protected boolean shouldRegisterJspServlet() {
		return this.jspServlet != null && this.jspServlet.getRegistered() && ClassUtils
				.isPresent(this.jspServlet.getClassName(), getClass().getClassLoader());
	}

}
