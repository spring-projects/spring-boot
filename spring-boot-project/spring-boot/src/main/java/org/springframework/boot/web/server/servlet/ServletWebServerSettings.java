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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.Assert;

/**
 * Settings for a servlet {@link WebServer} to be created by a
 * {@link ConfigurableServletWebServerFactory}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class ServletWebServerSettings {

	private ContextPath contextPath = ContextPath.DEFAULT;

	private String displayName;

	private Session session = new Session();

	private boolean registerDefaultServlet;

	private MimeMappings mimeMappings = MimeMappings.lazyCopy(MimeMappings.DEFAULT);

	private File documentRoot;

	private List<ServletContextInitializer> initializers = new ArrayList<>();

	private Jsp jsp = new Jsp();

	private Map<Locale, Charset> localeCharsetMappings = new HashMap<>();

	private Map<String, String> initParameters = new HashMap<>();

	private List<CookieSameSiteSupplier> cookieSameSiteSuppliers = new ArrayList<>();

	private final Set<String> webListenerClassNames = new HashSet<>();

	private final StaticResourceJars staticResourceJars = new StaticResourceJars();

	public ContextPath getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(ContextPath contextPath) {
		this.contextPath = contextPath;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Session getSession() {
		return this.session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public boolean isRegisterDefaultServlet() {
		return this.registerDefaultServlet;
	}

	public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
		this.registerDefaultServlet = registerDefaultServlet;
	}

	public MimeMappings getMimeMappings() {
		return this.mimeMappings;
	}

	public File getDocumentRoot() {
		return this.documentRoot;
	}

	public void setDocumentRoot(File documentRoot) {
		this.documentRoot = documentRoot;
	}

	public List<? extends ServletContextInitializer> getInitializers() {
		return this.initializers;
	}

	public void setJsp(Jsp jsp) {
		this.jsp = jsp;
	}

	public Jsp getJsp() {
		return this.jsp;
	}

	public Map<Locale, Charset> getLocaleCharsetMappings() {
		return this.localeCharsetMappings;
	}

	public Map<String, String> getInitParameters() {
		return this.initParameters;
	}

	public List<? extends CookieSameSiteSupplier> getCookieSameSiteSuppliers() {
		return this.cookieSameSiteSuppliers;
	}

	public void setMimeMappings(MimeMappings mimeMappings) {
		Assert.notNull(mimeMappings, "'mimeMappings' must not be null");
		this.mimeMappings = new MimeMappings(mimeMappings);
	}

	public void addMimeMappings(MimeMappings mimeMappings) {
		mimeMappings.forEach((mapping) -> this.mimeMappings.add(mapping.getExtension(), mapping.getMimeType()));
	}

	public void setInitializers(List<? extends ServletContextInitializer> initializers) {
		Assert.notNull(initializers, "'initializers' must not be null");
		this.initializers = new ArrayList<>(initializers);
	}

	public void addInitializers(ServletContextInitializer... initializers) {
		Assert.notNull(initializers, "'initializers' must not be null");
		this.initializers.addAll(Arrays.asList(initializers));
	}

	public void setLocaleCharsetMappings(Map<Locale, Charset> localeCharsetMappings) {
		Assert.notNull(localeCharsetMappings, "'localeCharsetMappings' must not be null");
		this.localeCharsetMappings = localeCharsetMappings;
	}

	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	public void setCookieSameSiteSuppliers(List<? extends CookieSameSiteSupplier> cookieSameSiteSuppliers) {
		Assert.notNull(cookieSameSiteSuppliers, "'cookieSameSiteSuppliers' must not be null");
		this.cookieSameSiteSuppliers = new ArrayList<>(cookieSameSiteSuppliers);
	}

	public void addCookieSameSiteSuppliers(CookieSameSiteSupplier... cookieSameSiteSuppliers) {
		Assert.notNull(cookieSameSiteSuppliers, "'cookieSameSiteSuppliers' must not be null");
		this.cookieSameSiteSuppliers.addAll(Arrays.asList(cookieSameSiteSuppliers));
	}

	public void addWebListenerClassNames(String... webListenerClassNames) {
		this.webListenerClassNames.addAll(Arrays.asList(webListenerClassNames));
	}

	public Set<String> getWebListenerClassNames() {
		return this.webListenerClassNames;
	}

	public List<URL> getStaticResourceUrls() {
		return this.staticResourceJars.getUrls();
	}

}
