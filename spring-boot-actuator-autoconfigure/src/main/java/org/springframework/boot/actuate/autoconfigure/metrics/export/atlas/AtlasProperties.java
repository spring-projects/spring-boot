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

package org.springframework.boot.actuate.autoconfigure.metrics.export.atlas;

import java.time.Duration;

import com.netflix.spectator.atlas.AtlasConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.RegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Atlas metrics export.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
@ConfigurationProperties(prefix = "metrics.atlas")
public class AtlasProperties extends RegistryProperties implements AtlasConfig {

	@Override
	protected String prefix() {
		return "atlas";
	}

	public void setStep(Duration step) {
		set("step", step);
	}

	public void setMeterTTL(Duration meterTTL) {
		set("meterTTL", meterTTL);
	}

	public void setEnabled(Boolean enabled) {
		set("enabled", enabled);
	}

	public void setNumThreads(Integer numThreads) {
		set("numThreads", numThreads);
	}

	public void setUri(String uri) {
		set("uri", uri);
	}

	public void setLwcEnabled(boolean lwcEnabled) {
		set("lwcEnabled", lwcEnabled);
	}

	public void setConfigRefreshFrequency(Duration configRefreshFrequency) {
		set("configRefreshFrequency", configRefreshFrequency);
	}

	public void setConfigTTL(Duration configTTL) {
		set("configTTL", configTTL);
	}

	public void setConfigUri(String configUri) {
		set("configUri", configUri);
	}

	public void setEvalUri(String evalUri) {
		set("evalUri", evalUri);
	}

	public void setConnectTimeout(Duration connectTimeout) {
		set("connectTimeout", connectTimeout);
	}

	public void setReadTimeout(Duration readTimeout) {
		set("readTimeout", readTimeout);
	}

	public void setBatchSize(Integer batchSize) {
		set("batchSize", batchSize);
	}

}
