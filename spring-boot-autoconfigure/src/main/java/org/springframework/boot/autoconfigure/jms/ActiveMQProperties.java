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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for ActiveMQ
 * 
 * @author Greg Turnquist
 */
@ConfigurationProperties(prefix = "spring.activemq")
public class ActiveMQProperties {

	private String brokerUrl = "tcp://localhost:61616";

	private boolean inMemory = true;

	private boolean pooled = false;

	private String user;

	private String password;

	// Will override brokerURL if inMemory is set to true
	public String getBrokerUrl() {
		if (this.inMemory) {
			return "vm://localhost";
		}
		return this.brokerUrl;
	}

	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

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

}
