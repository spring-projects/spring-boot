/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

/**
 * Configuration properties for ActiveMQ
 * 
 * @author Greg Turnquist
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "spring.activemq")
public class ActiveMQProperties {

	public static final String DEFAULT_EMBEDDED_BROKER_URL = "vm://localhost?broker.persistent=false";

	public static final String DEFAULT_NETWORK_BROKER_URL = "tcp://localhost:61616";

	private String brokerUrl = null;

	private boolean inMemory = true;

	private boolean pooled = false;

	private String user;

	private String password;

	/**
	 * Determine the broker url to use for the specified {@link Environment}.
	 * <p>If no broker url is specified through configuration, a default
	 * broker is provided, that is {@value #DEFAULT_EMBEDDED_BROKER_URL} if
	 * the {@code inMemory} flag is {@code null} or {@code true},
	 * {@value #DEFAULT_NETWORK_BROKER_URL} otherwise.
	 *
	 * @param environment the environment to extract configuration from
	 * @return the broker url to use
	 */
	public static String determineBrokerUrl(Environment environment) {
		PropertyResolver resolver = new RelaxedPropertyResolver(environment, "spring.activemq.");
		String brokerUrl = resolver.getProperty("brokerUrl");
		Boolean inMemory = resolver.getProperty("inMemory", Boolean.class);
		return determineBrokerUrl(brokerUrl, inMemory);
	}

	public String getBrokerUrl() {
		return determineBrokerUrl(this.brokerUrl, this.inMemory);
	}

	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

	/**
	 * Specify if the default broker url should be in memory. Ignored
	 * if an explicit broker has been specified.
	 */
	public boolean isInMemory() {
		return this.inMemory;
	}

	public void setInMemory(boolean inMemory) {
		this.inMemory = inMemory;
	}

	public boolean isPooled() {
		return this.pooled;
	}

	public void setPooled(boolean pooled) {
		this.pooled = pooled;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}


	/**
	 * @see #determineBrokerUrl(Environment)
	 */
	private static String determineBrokerUrl(String brokerUrl, Boolean inMemory) {
		boolean embedded = (inMemory != null) ? inMemory : true;

		if (brokerUrl != null) {
			return brokerUrl;
		}
		else if (embedded) {
			return DEFAULT_EMBEDDED_BROKER_URL;
		}
		else {
			return DEFAULT_NETWORK_BROKER_URL;
		}
	}

}
