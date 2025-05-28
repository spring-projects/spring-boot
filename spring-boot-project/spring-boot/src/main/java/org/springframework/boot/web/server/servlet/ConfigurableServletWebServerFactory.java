/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.servlet;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * A configurable {@link ServletWebServerFactory}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @since 4.0.0
 * @see ServletWebServerFactory
 * @see WebServerFactoryCustomizer
 */
public interface ConfigurableServletWebServerFactory
		extends ConfigurableWebServerFactory, ServletWebServerFactory, WebListenerRegistry {

	ServletWebServerSettings getSettings();

	/**
	 * Sets the context path for the web server. The context should start with a "/"
	 * character but not end with a "/" character. The default context path can be
	 * specified using an empty string.
	 * @param contextPath the context path to set
	 */
	default void setContextPath(String contextPath) {
		getSettings().setContextPath(ContextPath.of(contextPath));
	}

	/**
	 * Returns the context path for the servlet web server.
	 * @return the context path
	 */
	default String getContextPath() {
		return getSettings().getContextPath().toString();
	}

	/**
	 * Sets the display name of the application deployed in the web server.
	 * @param displayName the displayName to set
	 * @since 4.0.0
	 */
	default void setDisplayName(String displayName) {
		getSettings().setDisplayName(displayName);
	}

	/**
	 * Sets the configuration that will be applied to the container's HTTP session
	 * support.
	 * @param session the session configuration
	 */
	default void setSession(Session session) {
		getSettings().setSession(session);
	}

	/**
	 * Set if the DefaultServlet should be registered. Defaults to {@code false} since
	 * 2.4.
	 * @param registerDefaultServlet if the default servlet should be registered
	 */
	default void setRegisterDefaultServlet(boolean registerDefaultServlet) {
		getSettings().setRegisterDefaultServlet(registerDefaultServlet);
	}

	/**
	 * Sets the mime-type mappings.
	 * @param mimeMappings the mime type mappings (defaults to
	 * {@link MimeMappings#DEFAULT})
	 */
	default void setMimeMappings(MimeMappings mimeMappings) {
		getSettings().setMimeMappings(mimeMappings);
	}

	/**
	 * Adds mime-type mappings.
	 * @param mimeMappings the mime type mappings to add
	 * @since 4.0.0
	 */
	default void addMimeMappings(MimeMappings mimeMappings) {
		getSettings().addMimeMappings(mimeMappings);
	}

	/**
	 * Sets the document root directory which will be used by the web context to serve
	 * static files.
	 * @param documentRoot the document root or {@code null} if not required
	 */
	default void setDocumentRoot(File documentRoot) {
		getSettings().setDocumentRoot(documentRoot);
	}

	/**
	 * Sets {@link ServletContextInitializer} that should be applied in addition to
	 * {@link ServletWebServerFactory#getWebServer(ServletContextInitializer...)}
	 * parameters. This method will replace any previously set or added initializers.
	 * @param initializers the initializers to set
	 * @see #addInitializers
	 */
	default void setInitializers(List<? extends ServletContextInitializer> initializers) {
		getSettings().setInitializers(initializers);
	}

	/**
	 * Add {@link ServletContextInitializer}s to those that should be applied in addition
	 * to {@link ServletWebServerFactory#getWebServer(ServletContextInitializer...)}
	 * parameters.
	 * @param initializers the initializers to add
	 * @see #setInitializers
	 */
	default void addInitializers(ServletContextInitializer... initializers) {
		getSettings().addInitializers(initializers);
	}

	/**
	 * Sets the configuration that will be applied to the server's JSP servlet.
	 * @param jsp the JSP servlet configuration
	 */
	default void setJsp(Jsp jsp) {
		getSettings().setJsp(jsp);
	}

	/**
	 * Sets the Locale to Charset mappings.
	 * @param localeCharsetMappings the Locale to Charset mappings
	 */
	default void setLocaleCharsetMappings(Map<Locale, Charset> localeCharsetMappings) {
		getSettings().setLocaleCharsetMappings(localeCharsetMappings);
	}

	/**
	 * Sets the init parameters that are applied to the container's
	 * {@link ServletContext}.
	 * @param initParameters the init parameters
	 */
	default void setInitParameters(Map<String, String> initParameters) {
		getSettings().setInitParameters(initParameters);
	}

	/**
	 * Sets {@link CookieSameSiteSupplier CookieSameSiteSuppliers} that should be used to
	 * obtain the {@link SameSite} attribute of any added cookie. This method will replace
	 * any previously set or added suppliers.
	 * @param cookieSameSiteSuppliers the suppliers to add
	 * @see #addCookieSameSiteSuppliers
	 */
	default void setCookieSameSiteSuppliers(List<? extends CookieSameSiteSupplier> cookieSameSiteSuppliers) {
		getSettings().setCookieSameSiteSuppliers(cookieSameSiteSuppliers);
	}

	/**
	 * Add {@link CookieSameSiteSupplier CookieSameSiteSuppliers} to those that should be
	 * used to obtain the {@link SameSite} attribute of any added cookie.
	 * @param cookieSameSiteSuppliers the suppliers to add
	 * @see #setCookieSameSiteSuppliers
	 */
	default void addCookieSameSiteSuppliers(CookieSameSiteSupplier... cookieSameSiteSuppliers) {
		getSettings().addCookieSameSiteSuppliers(cookieSameSiteSuppliers);
	}

	@Override
	default void addWebListeners(String... webListenerClassNames) {
		getSettings().addWebListenerClassNames(webListenerClassNames);
	}

}
