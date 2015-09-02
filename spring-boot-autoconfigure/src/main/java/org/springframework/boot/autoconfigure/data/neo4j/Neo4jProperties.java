/*
 * Copyright 2012-2015 the original author or authors.
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

import org.neo4j.ogm.config.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Neo4j.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Josh Long
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Michael Hunger
 * @author Vince Bickers
 */
@ConfigurationProperties(prefix = "spring.data.neo4j")
public class Neo4jProperties {

	// if you don't set this up somewhere, this is what we'll use by default
	private String driver = "org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver";
	private String compiler;
	private String URI;
	private String username;
	private String password;

	public String getDriver() {
		return this.driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getCompiler() {
		return this.compiler;
	}

	public void setCompiler(String compiler) {
		this.compiler = compiler;
	}

	public String getURI() {
		return this.URI;
	}

	public void setURI(String URI) {
		this.URI = URI;
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

	public Configuration configure() {
		Configuration configuration = new Configuration();

		if (this.driver != null) {
			configuration.driverConfiguration().setDriverClassName(this.driver);
		}

		if (this.URI != null) {
			configuration.driverConfiguration().setURI(this.URI);
		}

		if (this.username != null && this.password != null) {
			configuration.driverConfiguration().setCredentials(this.username, this.password);
		}

		if (this.compiler != null) {
			configuration.compilerConfiguration().setCompilerClassName(this.compiler);
		}

		return configuration;
	}
}
