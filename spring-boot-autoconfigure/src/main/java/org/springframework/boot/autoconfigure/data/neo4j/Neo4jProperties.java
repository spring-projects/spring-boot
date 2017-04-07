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
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.data.neo4j")
public class Neo4jProperties implements ApplicationContextAware {

	static final String EMBEDDED_DRIVER = "org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver";

	static final String HTTP_DRIVER = "org.neo4j.ogm.drivers.http.driver.HttpDriver";

	static final String DEFAULT_BOLT_URI = "bolt://localhost:7687";

	static final String BOLT_DRIVER = "org.neo4j.ogm.drivers.bolt.driver.BoltDriver";
	private final Embedded embedded = new Embedded();
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
	private Indexes indexes = new Indexes();

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

	public Embedded getEmbedded() {
		return this.embedded;
	}

	public Indexes getIndexes() {
		return this.indexes;
	}

	public void setIndexes(Indexes indexes) {
		this.indexes = indexes;
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

		builder.autoIndex(this.indexes.getAuto().name());

		if (AutoIndexMode.DUMP == this.indexes.getAuto()) {
			builder.generatedIndexesOutputDir(this.indexes.getDump().getDir());
			builder.generatedIndexesOutputFilename(this.indexes.getDump().getFilename());
		}
	}

	private void configureUriWithDefaults(Builder builder) {
		if (!getEmbedded().isEnabled()
				|| !ClassUtils.isPresent(EMBEDDED_DRIVER, this.classLoader)) {
			builder.uri(DEFAULT_BOLT_URI);
		}
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

	public static class Indexes {

		private AutoIndexMode auto = AutoIndexMode.NONE;

		private Dump dump = new Dump();

		public AutoIndexMode getAuto() {
			return this.auto;
		}

		public void setAuto(AutoIndexMode auto) {
			this.auto = auto;
		}

		public Dump getDump() {
			return this.dump;
		}

		public void setDump(Dump dump) {
			this.dump = dump;
		}

		public static class Dump {

			/**
			 * Generated Indexes Output Dir.
			 */
			private String dir = ".";

			/**
			 * Generated Indexes Output Filename.
			 */
			private String filename = "generated_indexes.cql";

			public String getDir() {
				return this.dir;
			}

			public void setDir(String dir) {
				this.dir = dir;
			}

			public String getFilename() {
				return this.filename;
			}

			public void setFilename(String filename) {
				this.filename = filename;
			}
		}
	}
}
