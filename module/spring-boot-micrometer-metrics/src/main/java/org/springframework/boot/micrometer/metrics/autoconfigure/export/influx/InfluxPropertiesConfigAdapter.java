/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.influx;

import io.micrometer.influx.InfluxApiVersion;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxConsistency;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link InfluxProperties} to an {@link InfluxConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class InfluxPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<InfluxProperties>
		implements InfluxConfig {

	InfluxPropertiesConfigAdapter(InfluxProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.influx.metrics.export";
	}

	@Override
	public String db() {
		return getRequired(InfluxProperties::getDb, InfluxConfig.super::db);
	}

	@Override
	public InfluxConsistency consistency() {
		return getRequired(InfluxProperties::getConsistency, InfluxConfig.super::consistency);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String userName() {
		return get(InfluxProperties::getUserName, InfluxConfig.super::userName);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String password() {
		return get(InfluxProperties::getPassword, InfluxConfig.super::password);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String retentionPolicy() {
		return get(InfluxProperties::getRetentionPolicy, InfluxConfig.super::retentionPolicy);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable Integer retentionReplicationFactor() {
		return get(InfluxProperties::getRetentionReplicationFactor, InfluxConfig.super::retentionReplicationFactor);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String retentionDuration() {
		return get(InfluxProperties::getRetentionDuration, InfluxConfig.super::retentionDuration);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String retentionShardDuration() {
		return get(InfluxProperties::getRetentionShardDuration, InfluxConfig.super::retentionShardDuration);
	}

	@Override
	public String uri() {
		return getRequired(InfluxProperties::getUri, InfluxConfig.super::uri);
	}

	@Override
	public boolean compressed() {
		return getRequired(InfluxProperties::isCompressed, InfluxConfig.super::compressed);
	}

	@Override
	public boolean autoCreateDb() {
		return getRequired(InfluxProperties::isAutoCreateDb, InfluxConfig.super::autoCreateDb);
	}

	@Override
	public InfluxApiVersion apiVersion() {
		return getRequired(InfluxProperties::getApiVersion, InfluxConfig.super::apiVersion);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String org() {
		return get(InfluxProperties::getOrg, InfluxConfig.super::org);
	}

	@Override
	public String bucket() {
		return getRequired(InfluxProperties::getBucket, InfluxConfig.super::bucket);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String token() {
		return get(InfluxProperties::getToken, InfluxConfig.super::token);
	}

}
