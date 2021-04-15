/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.zabbix;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Zabbix metrics
 * export.
 */
@ConfigurationProperties(prefix = "management.metrics.export.zabbix")
public class ZabbixProperties extends StepRegistryProperties {

	/**
	 * The name of the host for the instance that is monitored.
	 */
	private String host = "instance";

	/**
	 * The hostname of the Zabbix installation where the metrics will be shipped.
	 */
	private String instanceHost = "localhost";

	/**
	 * The port of the Zabbix installation where the metrics will be shipped.
	 */
	private Integer instancePort = 8649;

	/**
	 * Prefix to be prepended to all metric keys sent to Zabbix.
	 */
	private String namePrefix = "";

	/**
	 * Postfix to be appended to all metric keys sent to Zabbix.
	 */
	private String nameSuffix = "";

	public String getHost() {
		return host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public String getInstanceHost() {
		return instanceHost;
	}

	public void setInstanceHost(final String instanceHost) {
		this.instanceHost = instanceHost;
	}

	public Integer getInstancePort() {
		return instancePort;
	}

	public void setInstancePort(final Integer instancePort) {
		this.instancePort = instancePort;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(final String namePrefix) {
		this.namePrefix = namePrefix;
	}

	public String getNameSuffix() {
		return nameSuffix;
	}

	public void setNameSuffix(final String nameSuffix) {
		this.nameSuffix = nameSuffix;
	}

}