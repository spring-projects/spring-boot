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

import io.micrometer.zabbix.ZabbixConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link ZabbixProperties} to a {@link ZabbixConfig}.
 */
class ZabbixPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<ZabbixProperties>
		implements ZabbixConfig {

	ZabbixPropertiesConfigAdapter(ZabbixProperties properties) {
		super(properties);
	}

	@Override
	public String instanceHost() {
		return get(ZabbixProperties::getInstanceHost, ZabbixConfig.super::instanceHost);
	}

	@Override
	public int instancePort() {
		return get(ZabbixProperties::getInstancePort, ZabbixConfig.super::instancePort);
	}

	@Override
	public String host() {
		return get(ZabbixProperties::getHost, ZabbixConfig.super::host);
	}

	@Override
	public String namePrefix() {
		return get(ZabbixProperties::getNamePrefix, ZabbixConfig.super::namePrefix);
	}

	@Override
	public String nameSuffix() {
		return get(ZabbixProperties::getNameSuffix, ZabbixConfig.super::nameSuffix);
	}

}