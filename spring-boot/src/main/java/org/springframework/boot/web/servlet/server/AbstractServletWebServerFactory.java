/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@link ConfigurableServletWebServerFactory} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Ivan Sopov
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @since 2.0.0
 */
public abstract class AbstractServletWebServerFactory
		extends AbstractConfigurableWebServerFactory
		implements ConfigurableServletWebServerFactory {

	private static final int DEFAULT_SESSION_TIMEOUT = (int) TimeUnit.MINUTES
			.toSeconds(30);

	protected final Log logger = LogFactory.getLog(getClass());

	private String contextPath = "";

	private String displayName;

	private int sessionTimeout = DEFAULT_SESSION_TIMEOUT;

	private boolean persistSession;

	private boolean registerDefaultServlet = true;

	private MimeMappings mimeMappings = new MimeMappings(MimeMappings.DEFAULT);

	private List<ServletContextInitializer> initializers = new ArrayList<>();

	private Jsp jsp = new Jsp();

	private Map<Locale, Charset> localeCharsetMappings = new HashMap<>();

	private final SessionStoreDirectory sessionStoreDir = new SessionStoreDirectory();

	private final DocumentRoot documentRoot = new DocumentRoot(this.logger);

	private final StaticResourceJars staticResourceJars = new StaticResourceJars();

	/**
	 * Create a new {@link AbstractServletWebServerFactory} instance.
	 */
	public AbstractServletWebServerFactory() {
	}

	/**
	 * Create a new {@link AbstractServletWebServerFactory} instance with the specified
	 * port.
	 * @param port the port number for the web server
	 */
	public AbstractServletWebServerFactory(int port) {
		super(port);
	}

	/**
	 * Create a new {@link AbstractServletWebServerFactory} instance with the specified
	 * context path and port.
	 * @param contextPath the context path for the web server
	 * @param port the port number for the web server
	 */
	public AbstractServletWebServerFactory(String contextPath, int port) {
		super(port);
		checkContextPath(contextPath);
		this.contextPath = contextPath;
	}

	/**
	 * Returns the context path for the web server. The path will start with "/" and not
	 * end with "/". The root context is represented by an empty string.
	 * @return the context path
	 */
	public String getContextPath() {
		return this.contextPath;
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

	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Return the session timeout in seconds.
	 * @return the timeout in seconds
	 */
	public int getSessionTimeout() {
		return this.sessionTimeout;
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

	public boolean isPersistSession() {
		return this.persistSession;
	}

	@Override
	public void setPersistSession(boolean persistSession) {
		this.persistSession = persistSession;
	}

	public File getSessionStoreDir() {
		return this.sessionStoreDir.getDirectory();
	}

	@Override
	public void setSessionStoreDir(File sessionStoreDir) {
		this.sessionStoreDir.setDirectory(sessionStoreDir);
	}

	/**
	 * Flag to indicate that the default servlet should be registered.
	 * @return true if the default servlet is to be registered
	 */
	public boolean isRegisterDefaultServlet() {
		return this.registerDefaultServlet;
	}

	@Override
	public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
		this.registerDefaultServlet = registerDefaultServlet;
	}

	/**
	 * Returns the mime-type mappings.
	 * @return the mimeMappings the mime-type mappings.
	 */
	public MimeMappings getMimeMappings() {
		return this.mimeMappings;
	}

	@Override
	public void setMimeMappings(MimeMappings mimeMappings) {
		this.mimeMappings = new MimeMappings(mimeMappings);
	}

	/**
	 * Returns the document root which will be used by the web context to serve static
	 * files.
	 * @return the document root
	 */
	public File getDocumentRoot() {
		return this.documentRoot.getDirectory();
	}

	@Override
	public void setDocumentRoot(File documentRoot) {
		this.documentRoot.setDirectory(documentRoot);
	}

	@Override
	public void setInitializers(List<? extends ServletContextInitializer> initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers = new ArrayList<>(initializers);
	}

	@Override
	public void addInitializers(ServletContextInitializer... initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers.addAll(Arrays.asList(initializers));
	}

	public Jsp getJsp() {
		return this.jsp;
	}

	@Override
	public void setJsp(Jsp jsp) {
		this.jsp = jsp;
	}

	/**
	 * Return the Locale to Charset mappings.
	 * @return the charset mappings
	 */
	public Map<Locale, Charset> getLocaleCharsetMappings() {
		return this.localeCharsetMappings;
	}

	@Override
	public void setLocaleCharsetMappings(Map<Locale, Charset> localeCharsetMappings) {
		Assert.notNull(localeCharsetMappings, "localeCharsetMappings must not be null");
		this.localeCharsetMappings = localeCharsetMappings;
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
		List<ServletContextInitializer> mergedInitializers = new ArrayList<>();
		mergedInitializers.addAll(Arrays.asList(initializers));
		mergedInitializers.addAll(this.initializers);
		return mergedInitializers
				.toArray(new ServletContextInitializer[mergedInitializers.size()]);
	}

	/**
	 * Returns whether or not the JSP servlet should be registered with the web server.
	 * @return {@code true} if the servlet should be registered, otherwise {@code false}
	 */
	protected boolean shouldRegisterJspServlet() {
		return this.jsp != null && this.jsp.getRegistered() && ClassUtils
				.isPresent(this.jsp.getClassName(), getClass().getClassLoader());
	}

	/**
	 * Returns the absolute document root when it points to a valid directory, logging a
	 * warning and returning {@code null} otherwise.
	 * @return the valid document root
	 */
	protected final File getValidDocumentRoot() {
		return this.documentRoot.getValidDirectory();
	}

	protected final List<URL> getUrlsOfJarsWithMetaInfResources() {
		return this.staticResourceJars.getUrls();
	}

	protected final File getValidSessionStoreDir() {
		return this.sessionStoreDir.getValidDirectory(true);
	}

	protected final File getValidSessionStoreDir(boolean mkdirs) {
		return this.sessionStoreDir.getValidDirectory(mkdirs);
	}

}
