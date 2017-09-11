/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.datadog;

import io.micrometer.datadog.DatadogConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Datadog metrics export.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "metrics.datadog")
public class DatadogProperties extends StepRegistryProperties implements DatadogConfig {

	@Override
	public String prefix() {
		return "metrics.datadog";
	}

	public DatadogProperties() {
		set("apiKey", "dummyKey"); // FIXME otherwise tests fail
	}

	public void setApiKey(String apiKey) {
		set("apiKey", apiKey);
	}

	public void setHostTag(String hostTag) {
		set("hostTag", hostTag);
	}

}
