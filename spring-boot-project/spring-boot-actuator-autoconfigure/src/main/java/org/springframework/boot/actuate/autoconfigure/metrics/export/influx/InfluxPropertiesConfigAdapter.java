/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.influx;

import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxConsistency;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link InfluxProperties} to an {@link InfluxConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class InfluxPropertiesConfigAdapter extends
		StepRegistryPropertiesConfigAdapter<InfluxProperties> implements InfluxConfig {

	InfluxPropertiesConfigAdapter(InfluxProperties properties) {
		super(properties);
	}

	@Override
	public String db() {
		return get(InfluxProperties::getDb, InfluxConfig.super::db);
	}

	@Override
	public InfluxConsistency consistency() {
		return get(InfluxProperties::getConsistency, InfluxConfig.super::consistency);
	}

	@Override
	public String userName() {
		return get(InfluxProperties::getUserName, InfluxConfig.super::userName);
	}

	@Override
	public String password() {
		return get(InfluxProperties::getPassword, InfluxConfig.super::password);
	}

	@Override
	public String retentionPolicy() {
		return get(InfluxProperties::getRetentionPolicy,
				InfluxConfig.super::retentionPolicy);
	}

	@Override
	public Integer retentionReplicationFactor() {
		return get(InfluxProperties::getRetentionReplicationFactor,
				InfluxConfig.super::retentionReplicationFactor);
	}

	@Override
	public String retentionDuration() {
		return get(InfluxProperties::getRetentionDuration,
				InfluxConfig.super::retentionDuration);
	}

	@Override
	public String retentionShardDuration() {
		return get(InfluxProperties::getRetentionShardDuration,
				InfluxConfig.super::retentionShardDuration);
	}

	@Override
	public String uri() {
		return get(InfluxProperties::getUri, InfluxConfig.super::uri);
	}

	@Override
	public boolean compressed() {
		return get(InfluxProperties::isCompressed, InfluxConfig.super::compressed);
	}

	@Override
	public boolean autoCreateDb() {
		return get(InfluxProperties::isAutoCreateDb, InfluxConfig.super::autoCreateDb);
	}

}
