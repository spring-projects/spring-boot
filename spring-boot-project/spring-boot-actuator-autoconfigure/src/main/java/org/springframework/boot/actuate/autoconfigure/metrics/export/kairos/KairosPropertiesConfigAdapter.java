/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.kairos;

import io.micrometer.kairos.KairosConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link KairosProperties} to an {@link KairosConfig}.
 *
 * @author Stephane Nicoll
 */
class KairosPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<KairosProperties>
		implements KairosConfig {

	KairosPropertiesConfigAdapter(KairosProperties properties) {
		super(properties);
	}

	@Override
	public String uri() {
		return get(KairosProperties::getUri, KairosConfig.super::uri);
	}

	@Override
	public String userName() {
		return get(KairosProperties::getUserName, KairosConfig.super::userName);
	}

	@Override
	public String password() {
		return get(KairosProperties::getPassword, KairosConfig.super::password);
	}

}
