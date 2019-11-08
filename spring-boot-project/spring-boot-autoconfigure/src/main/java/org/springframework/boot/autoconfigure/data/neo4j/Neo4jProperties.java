/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import org.neo4j.ogm.config.AutoIndexMode;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.Configuration.Builder;

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
 * @author Aurélien Leboulanger
 * @author Michael Simons
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.data.neo4j")
public class Neo4jProperties implements ApplicationContextAware {

	static final String EMBEDDED_DRIVER = "org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver";

	static final String HTTP_DRIVER = "org.neo4j.ogm.drivers.http.driver.HttpDriver";

	static final String DEFAULT_BOLT_URI = "bolt://localhost:7687";

	static final String BOLT_DRIVER = "org.neo4j.ogm.drivers.bolt.driver.BoltDriver";

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
	 * Auto index mode.
	 */
	private AutoIndexMode autoIndex = AutoIndexMode.NONE;

	/**
	 * Register OpenSessionInViewInterceptor. Binds a Neo4j Session to the thread for the
	 * entire processing of the request.",
	 */
	private Boolean openInView;

	/**
	 * Whether to use Neo4j native types wherever possible.
	 */
	private boolean useNativeTypes = false;

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

	public AutoIndexMode getAutoIndex() {
		return this.autoIndex;
	}

	public void setAutoIndex(AutoIndexMode autoIndex) {
		this.autoIndex = autoIndex;
	}

	public Boolean getOpenInView() {
		return this.openInView;
	}

	public void setOpenInView(Boolean openInView) {
		this.openInView = openInView;
	}

	public boolean isUseNativeTypes() {
		return this.useNativeTypes;
	}

	public void setUseNativeTypes(boolean useNativeTypes) {
		this.useNativeTypes = useNativeTypes;
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
		Builder builder = new Builder();
		configure(builder);
		return builder.build();
	}

	private void configure(Builder builder) {
		if (this.uri != null) {
			builder.uri(this.uri);
		}
		else {
			configureUriWithDefaults(builder);
		}
		if (this.username != null && this.password != null) {
			builder.credentials(this.username, this.password);
		}
		builder.autoIndex(this.getAutoIndex().getName());
		if (this.useNativeTypes) {
			builder.useNativeTypes();
		}
	}

	private void configureUriWithDefaults(Builder builder) {
		if (!getEmbedded().isEnabled() || !ClassUtils.isPresent(EMBEDDED_DRIVER, this.classLoader)) {
			builder.uri(DEFAULT_BOLT_URI);
		}
	}

	public static class Embedded {

		/**
		 * Whether to enable embedded mode if the embedded driver is available.
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
