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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.elastic;

import io.micrometer.elastic.ElasticConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link ElasticProperties} to an {@link ElasticConfig}.
 *
 * @author Andy Wilkinson
 */
class ElasticPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<ElasticProperties>
		implements ElasticConfig {

	ElasticPropertiesConfigAdapter(ElasticProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.elastic.metrics.export";
	}

	@Override
	public String host() {
		return getRequired(ElasticProperties::getHost, ElasticConfig.super::host);
	}

	@Override
	public String index() {
		return getRequired(ElasticProperties::getIndex, ElasticConfig.super::index);
	}

	@Override
	public String indexDateFormat() {
		return getRequired(ElasticProperties::getIndexDateFormat, ElasticConfig.super::indexDateFormat);
	}

	@Override
	public String indexDateSeparator() {
		return getRequired(ElasticProperties::getIndexDateSeparator, ElasticConfig.super::indexDateSeparator);
	}

	@Override
	public String timestampFieldName() {
		return getRequired(ElasticProperties::getTimestampFieldName, ElasticConfig.super::timestampFieldName);
	}

	@Override
	public boolean autoCreateIndex() {
		return getRequired(ElasticProperties::isAutoCreateIndex, ElasticConfig.super::autoCreateIndex);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String userName() {
		return get(ElasticProperties::getUserName, ElasticConfig.super::userName);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String password() {
		return get(ElasticProperties::getPassword, ElasticConfig.super::password);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String pipeline() {
		return get(ElasticProperties::getPipeline, ElasticConfig.super::pipeline);
	}

	@Override
	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public @Nullable String apiKeyCredentials() {
		return get(ElasticProperties::getApiKeyCredentials, ElasticConfig.super::apiKeyCredentials);
	}

	@Override
	public boolean enableSource() {
		return getRequired(ElasticProperties::isEnableSource, ElasticConfig.super::enableSource);
	}

}
