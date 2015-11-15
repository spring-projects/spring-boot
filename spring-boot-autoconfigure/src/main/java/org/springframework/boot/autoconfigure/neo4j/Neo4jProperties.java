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

package org.springframework.boot.autoconfigure.neo4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Configuration properties for Neo4j.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Josh Long
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Michael Hunger
 */
@ConfigurationProperties(prefix = Neo4jProperties.DATA_NEO4J_PREFIX)
public class Neo4jProperties {

	public static final String DATA_NEO4J_PREFIX = "spring.data.neo4j";

	/**
	 * Default port used when the configured port is not set.
	 */
	public static final int DEFAULT_PORT = 7474;
	/**
	 * Default host is localhost
	 */
	public static final String DEFAULT_HOST = "localhost";
	/**
	 * default URL is http on localhost and port 7474
	 */
	public static final String DEFAULT_URL = "http://"+DEFAULT_HOST+":"+DEFAULT_PORT;

	/**
	 * Neo4j server protocol.
	 */
	private String protocol = "http";

	/**
	 * Neo4j server host.
	 */
	private String host;

	/**
	 * Neo4j server port.
	 */
	private Integer port = null;

	/**
	 * Neo4j database URI. When set, host and port are ignored.
	 */
	private String url;

	/**
	 * Login user of the neo4j server.
	 */
	private String username;

	/**
	 * Login password of the neo4j server.
	 */
	private char[] password;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public char[] getPassword() {
		return this.password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public void clearPassword() {
		if (this.password == null) {
			return;
		}
		for (int i = 0; i < this.password.length; i++) {
			this.password[i] = 0;
		}
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * Creates a {@link Neo4jServer} representation using the given
	 * {@code environment}. If the configured port is zero, the value of the
	 * {@code local.neo4j.port} property retrieved from the {@code environment} is used
	 * to configure the server.
	 *
	 * @param environment the environment
	 * @return the Neo4j server representation
	 * @throws UnknownHostException if the configured host is unknown
	 */
	public Neo4jServer createNeo4jServer(Environment environment) throws UnknownHostException, MalformedURLException {
		try {
			String url = hasCustomAddress() ? new URL(this.protocol,determineHost(environment),determinePort(environment),"").toString() :
					     hasCustomUrl() ? this.url : DEFAULT_URL;
			if (hasCustomCredentials()) {
				return new RemoteServer(url,this.username,String.valueOf(this.password));
			} else {
				return new RemoteServer(url);
			}
		}
		finally {
			clearPassword();
		}
	}

	private boolean hasCustomAddress() {
		return this.host != null || this.port != null;
	}
	private boolean hasCustomUrl() {
		return this.url != null;
	}

	private boolean hasCustomCredentials() {
		return this.username != null && this.password != null;
	}

	private int determinePort(Environment environment) {
		if (this.port == null) {
			return DEFAULT_PORT;
		}
		if (this.port != 0) {
			return this.port;
		}
		if (environment != null) {
            String localPort = environment.getProperty("local.neo4j.port");
            if (localPort != null) {
                return Integer.valueOf(localPort);
            }
        }
		throw new IllegalStateException(
                "spring.data.neo4j.port=0 and no local neo4j port configuration "
                        + "is available");
	}
	private String determineHost(Environment environment) {
		if (this.host != null) {
			return this.host;
		}

		if (environment != null) {
            String localHost = environment.getProperty("local.neo4j.host");
            if (localHost != null) {
                return localHost;
            }
        }
		return DEFAULT_HOST;
	}

	public static Neo4jProperties fromEnvironment(Environment env) {
		Neo4jProperties props = new Neo4jProperties();
        if (env.containsProperty(DATA_NEO4J_PREFIX+".url"))
            props.url = env.getProperty(DATA_NEO4J_PREFIX+".url");
        if (env.containsProperty(DATA_NEO4J_PREFIX+".host"))
		    props.host = env.getProperty(DATA_NEO4J_PREFIX+".host");
        if (env.containsProperty(DATA_NEO4J_PREFIX+".port"))
		    props.port = Integer.parseInt(env.getProperty(DATA_NEO4J_PREFIX + ".port"));
        if (env.containsProperty(DATA_NEO4J_PREFIX+".protocol"))
		    props.protocol = env.getProperty(DATA_NEO4J_PREFIX + ".protocol");
        if (env.containsProperty(DATA_NEO4J_PREFIX+".user"))
		    props.username = env.getProperty(DATA_NEO4J_PREFIX + ".user");
        if (env.containsProperty(DATA_NEO4J_PREFIX+".password"))
		    props.password = env.getProperty(DATA_NEO4J_PREFIX + ".password").toCharArray();
		return props;
	}
}
