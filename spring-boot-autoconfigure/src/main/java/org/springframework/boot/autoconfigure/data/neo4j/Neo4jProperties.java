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

package org.springframework.boot.autoconfigure.data.neo4j;

import java.net.URI;
import java.net.URISyntaxException;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.DriverConfiguration;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

/**
 * Configuration properties for Neo4j.
 *
 * @author Stephane Nicoll
 * @author Michael Hunger
 * @author Vince Bickers
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.data.neo4j")
public class Neo4jProperties implements ApplicationContextAware {

	static final String EMBEDDED_DRIVER = "org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver";

	static final String HTTP_DRIVER = "org.neo4j.ogm.drivers.http.driver.HttpDriver";

	static final String DEFAULT_HTTP_URI = "http://localhost:7474";

	/**
	 * URI used by the driver. Auto-detected by default.
	 */
	private String uri;

	/**
	 * Login user of the server.
	 */
	private String username;

	/**
	 * Login password of the server.
	 */
	private String password;

	/**
	 * Compiler to use.
	 */
	private String compiler;

	private final Embedded embedded = new Embedded();

	private ClassLoader classLoader = Neo4jProperties.class.getClassLoader();

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCompiler() {
		return this.compiler;
	}

	public void setCompiler(String compiler) {
		this.compiler = compiler;
	}

	public Embedded getEmbedded() {
		return this.embedded;
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.classLoader = ctx.getClassLoader();
	}

	/**
	 * Create a {@link Configuration} based on the state of this instance.
	 * @return a configuration
	 */
	public Configuration createConfiguration() {
		Configuration configuration = new Configuration();
		configureDriver(configuration.driverConfiguration());
		if (this.compiler != null) {
			configuration.compilerConfiguration().setCompilerClassName(this.compiler);
		}
		return configuration;
	}

	private void configureDriver(DriverConfiguration driverConfiguration) {
		if (this.uri != null) {
			configureDriverFromUri(driverConfiguration, this.uri);
		}
		else {
			configureDriverWithDefaults(driverConfiguration);
		}
		if (this.username != null && this.password != null) {
			driverConfiguration.setCredentials(this.username, this.password);
		}
	}

	private void configureDriverFromUri(DriverConfiguration driverConfiguration,
			String uri) {
		driverConfiguration.setDriverClassName(deduceDriverFromUri());
		driverConfiguration.setURI(uri);
	}

	private String deduceDriverFromUri() {
		try {
			URI uri = new URI(this.uri);
			String scheme = uri.getScheme();
			if (scheme == null || scheme.equals("file")) {
				return EMBEDDED_DRIVER;
			}
			if ("http".equals(scheme)) {
				return HTTP_DRIVER;
			}
			throw new IllegalArgumentException(
					"Could not deduce driver to use based on URI '" + uri + "'");
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException(
					"Invalid URI for spring.data.neo4j.uri '" + this.uri + "'", ex);
		}
	}

	private void configureDriverWithDefaults(DriverConfiguration driverConfiguration) {
		if (getEmbedded().isEnabled()
				&& ClassUtils.isPresent(EMBEDDED_DRIVER, this.classLoader)) {
			driverConfiguration.setDriverClassName(EMBEDDED_DRIVER);
			return;
		}
		driverConfiguration.setDriverClassName(HTTP_DRIVER);
		driverConfiguration.setURI(DEFAULT_HTTP_URI);
	}

	public static class Embedded {

		/**
		 * Enable embedded mode if the embedded driver is available.
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
