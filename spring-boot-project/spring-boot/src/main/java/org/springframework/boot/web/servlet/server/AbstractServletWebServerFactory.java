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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Cookie;
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
public abstract class AbstractServletWebServerFactory extends AbstractConfigurableWebServerFactory
		implements ConfigurableServletWebServerFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	private String contextPath = "";

	private String displayName;

	private Session session = new Session();

	private boolean registerDefaultServlet = false;

	private MimeMappings mimeMappings = MimeMappings.lazyCopy(MimeMappings.DEFAULT);

	private List<ServletContextInitializer> initializers = new ArrayList<>();

	private Jsp jsp = new Jsp();

	private Map<Locale, Charset> localeCharsetMappings = new HashMap<>();

	private Map<String, String> initParameters = Collections.emptyMap();

	private List<CookieSameSiteSupplier> cookieSameSiteSuppliers = new ArrayList<>();

	private final DocumentRoot documentRoot = new DocumentRoot(this.logger);

	private final StaticResourceJars staticResourceJars = new StaticResourceJars();

	private final Set<String> webListenerClassNames = new HashSet<>();

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

	/**
	 * Sets the context path for the web server.
	 * @param contextPath the context path to be set
	 * @throws IllegalArgumentException if the context path is invalid
	 */
	@Override
	public void setContextPath(String contextPath) {
		checkContextPath(contextPath);
		this.contextPath = contextPath;
	}

	/**
	 * Checks if the given context path is valid.
	 * @param contextPath the context path to be checked
	 * @throws IllegalArgumentException if the context path is null, empty, or does not
	 * meet the required format
	 */
	private void checkContextPath(String contextPath) {
		Assert.notNull(contextPath, "ContextPath must not be null");
		if (!contextPath.isEmpty()) {
			if ("/".equals(contextPath)) {
				throw new IllegalArgumentException("Root ContextPath must be specified using an empty string");
			}
			if (!contextPath.startsWith("/") || contextPath.endsWith("/")) {
				throw new IllegalArgumentException("ContextPath must start with '/' and not end with '/'");
			}
		}
	}

	/**
	 * Returns the display name of the AbstractServletWebServerFactory.
	 * @return the display name of the AbstractServletWebServerFactory
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Sets the display name of the web server.
	 * @param displayName the display name to be set
	 */
	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Flag to indicate that the default servlet should be registered.
	 * @return true if the default servlet is to be registered
	 */
	public boolean isRegisterDefaultServlet() {
		return this.registerDefaultServlet;
	}

	/**
	 * Sets whether to register the default servlet.
	 * @param registerDefaultServlet true to register the default servlet, false otherwise
	 */
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

	/**
	 * Set the MIME mappings for the servlet web server factory.
	 * @param mimeMappings the MIME mappings to be set (must not be null)
	 * @throws IllegalArgumentException if the mimeMappings parameter is null
	 */
	@Override
	public void setMimeMappings(MimeMappings mimeMappings) {
		Assert.notNull(mimeMappings, "MimeMappings must not be null");
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

	/**
	 * Sets the document root directory for the web server.
	 * @param documentRoot the file representing the document root directory
	 */
	@Override
	public void setDocumentRoot(File documentRoot) {
		this.documentRoot.setDirectory(documentRoot);
	}

	/**
	 * Set the initializers for the servlet context.
	 * @param initializers the list of initializers to set
	 * @throws IllegalArgumentException if the initializers are null
	 */
	@Override
	public void setInitializers(List<? extends ServletContextInitializer> initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers = new ArrayList<>(initializers);
	}

	/**
	 * Adds the specified initializers to the list of initializers for the servlet
	 * context.
	 * @param initializers the initializers to be added (must not be null)
	 * @throws IllegalArgumentException if the initializers parameter is null
	 */
	@Override
	public void addInitializers(ServletContextInitializer... initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers.addAll(Arrays.asList(initializers));
	}

	/**
	 * Returns the Jsp object associated with this AbstractServletWebServerFactory.
	 * @return the Jsp object associated with this AbstractServletWebServerFactory
	 */
	public Jsp getJsp() {
		return this.jsp;
	}

	/**
	 * Sets the JSP configuration for the web server factory.
	 * @param jsp the JSP configuration to set
	 */
	@Override
	public void setJsp(Jsp jsp) {
		this.jsp = jsp;
	}

	/**
	 * Returns the current session associated with this servlet web server factory.
	 * @return the current session associated with this servlet web server factory
	 */
	public Session getSession() {
		return this.session;
	}

	/**
	 * Sets the session for this AbstractServletWebServerFactory.
	 * @param session the session to be set
	 */
	@Override
	public void setSession(Session session) {
		this.session = session;
	}

	/**
	 * Return the Locale to Charset mappings.
	 * @return the charset mappings
	 */
	public Map<Locale, Charset> getLocaleCharsetMappings() {
		return this.localeCharsetMappings;
	}

	/**
	 * Set the locale to charset mappings for this web server factory.
	 * @param localeCharsetMappings the locale to charset mappings
	 * @throws IllegalArgumentException if localeCharsetMappings is null
	 */
	@Override
	public void setLocaleCharsetMappings(Map<Locale, Charset> localeCharsetMappings) {
		Assert.notNull(localeCharsetMappings, "localeCharsetMappings must not be null");
		this.localeCharsetMappings = localeCharsetMappings;
	}

	/**
	 * Sets the initial parameters for the servlet web server factory.
	 * @param initParameters a map containing the initial parameters
	 */
	@Override
	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	/**
	 * Returns the initialization parameters of the servlet web server factory.
	 * @return a map containing the initialization parameters as key-value pairs
	 */
	public Map<String, String> getInitParameters() {
		return this.initParameters;
	}

	/**
	 * Sets the list of {@link CookieSameSiteSupplier} instances to be used for
	 * determining the SameSite attribute of cookies.
	 * @param cookieSameSiteSuppliers the list of {@link CookieSameSiteSupplier} instances
	 * @throws IllegalArgumentException if the cookieSameSiteSuppliers is null
	 */
	@Override
	public void setCookieSameSiteSuppliers(List<? extends CookieSameSiteSupplier> cookieSameSiteSuppliers) {
		Assert.notNull(cookieSameSiteSuppliers, "CookieSameSiteSuppliers must not be null");
		this.cookieSameSiteSuppliers = new ArrayList<>(cookieSameSiteSuppliers);
	}

	/**
	 * Adds the given CookieSameSiteSuppliers to the list of suppliers.
	 * @param cookieSameSiteSuppliers the CookieSameSiteSuppliers to be added (must not be
	 * null)
	 * @throws IllegalArgumentException if cookieSameSiteSuppliers is null
	 */
	@Override
	public void addCookieSameSiteSuppliers(CookieSameSiteSupplier... cookieSameSiteSuppliers) {
		Assert.notNull(cookieSameSiteSuppliers, "CookieSameSiteSuppliers must not be null");
		this.cookieSameSiteSuppliers.addAll(Arrays.asList(cookieSameSiteSuppliers));
	}

	/**
	 * Returns the list of CookieSameSiteSupplier objects.
	 * @return the list of CookieSameSiteSupplier objects
	 */
	public List<CookieSameSiteSupplier> getCookieSameSiteSuppliers() {
		return this.cookieSameSiteSuppliers;
	}

	/**
	 * Utility method that can be used by subclasses wishing to combine the specified
	 * {@link ServletContextInitializer} parameters with those defined in this instance.
	 * @param initializers the initializers to merge
	 * @return a complete set of merged initializers (with the specified parameters
	 * appearing first)
	 */
	protected final ServletContextInitializer[] mergeInitializers(ServletContextInitializer... initializers) {
		List<ServletContextInitializer> mergedInitializers = new ArrayList<>();
		mergedInitializers.add((servletContext) -> this.initParameters.forEach(servletContext::setInitParameter));
		mergedInitializers.add(new SessionConfiguringInitializer(this.session));
		mergedInitializers.addAll(Arrays.asList(initializers));
		mergedInitializers.addAll(this.initializers);
		return mergedInitializers.toArray(new ServletContextInitializer[0]);
	}

	/**
	 * Returns whether the JSP servlet should be registered with the web server.
	 * @return {@code true} if the servlet should be registered, otherwise {@code false}
	 */
	protected boolean shouldRegisterJspServlet() {
		return this.jsp != null && this.jsp.getRegistered()
				&& ClassUtils.isPresent(this.jsp.getClassName(), getClass().getClassLoader());
	}

	/**
	 * Returns the absolute document root when it points to a valid directory, logging a
	 * warning and returning {@code null} otherwise.
	 * @return the valid document root
	 */
	protected final File getValidDocumentRoot() {
		return this.documentRoot.getValidDirectory();
	}

	/**
	 * Returns the URLs of the JAR files that contain META-INF resources.
	 * @return the list of URLs of JAR files with META-INF resources
	 */
	protected final List<URL> getUrlsOfJarsWithMetaInfResources() {
		return this.staticResourceJars.getUrls();
	}

	/**
	 * Returns the valid session store directory.
	 * @return the valid session store directory
	 */
	protected final File getValidSessionStoreDir() {
		return getValidSessionStoreDir(true);
	}

	/**
	 * Returns the valid session store directory.
	 * @param mkdirs a boolean value indicating whether to create the directory if it
	 * doesn't exist
	 * @return the valid session store directory
	 */
	protected final File getValidSessionStoreDir(boolean mkdirs) {
		return this.session.getSessionStoreDirectory().getValidDirectory(mkdirs);
	}

	/**
	 * Adds the specified web listener classes to the list of web listeners for this
	 * servlet web server factory.
	 * @param webListenerClassNames the names of the web listener classes to be added
	 */
	@Override
	public void addWebListeners(String... webListenerClassNames) {
		this.webListenerClassNames.addAll(Arrays.asList(webListenerClassNames));
	}

	/**
	 * Returns the set of web listener class names.
	 * @return the set of web listener class names
	 */
	protected final Set<String> getWebListenerClassNames() {
		return this.webListenerClassNames;
	}

	/**
	 * {@link ServletContextInitializer} to apply appropriate parts of the {@link Session}
	 * configuration.
	 */
	private static class SessionConfiguringInitializer implements ServletContextInitializer {

		private final Session session;

		/**
		 * Initializes a new instance of the SessionConfiguringInitializer class with the
		 * specified session.
		 * @param session The session to be configured.
		 */
		SessionConfiguringInitializer(Session session) {
			this.session = session;
		}

		/**
		 * This method is called during the startup of the application. It configures the
		 * session tracking modes and session cookie for the servlet context.
		 * @param servletContext the servlet context
		 * @throws ServletException if an error occurs during the initialization
		 */
		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			if (this.session.getTrackingModes() != null) {
				servletContext.setSessionTrackingModes(unwrap(this.session.getTrackingModes()));
			}
			configureSessionCookie(servletContext.getSessionCookieConfig());
		}

		/**
		 * Configures the session cookie with the provided SessionCookieConfig.
		 * @param config the SessionCookieConfig to configure the session cookie
		 */
		private void configureSessionCookie(SessionCookieConfig config) {
			Cookie cookie = this.session.getCookie();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(cookie::getName).to(config::setName);
			map.from(cookie::getDomain).to(config::setDomain);
			map.from(cookie::getPath).to(config::setPath);
			map.from(cookie::getHttpOnly).to(config::setHttpOnly);
			map.from(cookie::getSecure).to(config::setSecure);
			map.from(cookie::getMaxAge).asInt(Duration::getSeconds).to(config::setMaxAge);
		}

		/**
		 * Unwraps a set of SessionTrackingMode objects from the Servlet API to a set of
		 * SessionTrackingMode objects from the Jakarta Servlet API.
		 * @param modes the set of SessionTrackingMode objects from the Servlet API to be
		 * unwrapped
		 * @return the set of SessionTrackingMode objects from the Jakarta Servlet API
		 */
		private Set<jakarta.servlet.SessionTrackingMode> unwrap(Set<Session.SessionTrackingMode> modes) {
			if (modes == null) {
				return null;
			}
			Set<jakarta.servlet.SessionTrackingMode> result = new LinkedHashSet<>();
			for (Session.SessionTrackingMode mode : modes) {
				result.add(jakarta.servlet.SessionTrackingMode.valueOf(mode.name()));
			}
			return result;
		}

	}

}
