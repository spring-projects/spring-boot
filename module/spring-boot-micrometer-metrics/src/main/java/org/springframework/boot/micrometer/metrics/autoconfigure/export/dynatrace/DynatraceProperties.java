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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.dynatrace;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Dynatrace
 * metrics export.
 *
 * @author Andy Wilkinson
 * @author Georg Pirklbauer
 * @since 4.0.0
 */
@ConfigurationProperties("management.dynatrace.metrics.export")
public class DynatraceProperties extends StepRegistryProperties {

	private final V1 v1 = new V1();

	private final V2 v2 = new V2();

	/**
	 * Dynatrace authentication token.
	 */
	private @Nullable String apiToken;

	/**
	 * URI to ship metrics to. Should be used for SaaS, self-managed instances or to
	 * en-route through an internal proxy.
	 */
	private @Nullable String uri;

	public @Nullable String getApiToken() {
		return this.apiToken;
	}

	public void setApiToken(@Nullable String apiToken) {
		this.apiToken = apiToken;
	}

	public @Nullable String getUri() {
		return this.uri;
	}

	public void setUri(@Nullable String uri) {
		this.uri = uri;
	}

	public V1 getV1() {
		return this.v1;
	}

	public V2 getV2() {
		return this.v2;
	}

	public static class V1 {

		/**
		 * ID of the custom device that is exporting metrics to Dynatrace.
		 */
		private @Nullable String deviceId;

		/**
		 * Group for exported metrics. Used to specify custom device group name in the
		 * Dynatrace UI.
		 */
		private @Nullable String group;

		/**
		 * Technology type for exported metrics. Used to group metrics under a logical
		 * technology name in the Dynatrace UI.
		 */
		private String technologyType = "java";

		public @Nullable String getDeviceId() {
			return this.deviceId;
		}

		public void setDeviceId(@Nullable String deviceId) {
			this.deviceId = deviceId;
		}

		public @Nullable String getGroup() {
			return this.group;
		}

		public void setGroup(@Nullable String group) {
			this.group = group;
		}

		public String getTechnologyType() {
			return this.technologyType;
		}

		public void setTechnologyType(String technologyType) {
			this.technologyType = technologyType;
		}

	}

	public static class V2 {

		/**
		 * Default dimensions that are added to all metrics in the form of key-value
		 * pairs. These are overwritten by Micrometer tags if they use the same key.
		 */
		private @Nullable Map<String, String> defaultDimensions;

		/**
		 * Whether to enable Dynatrace metadata export.
		 */
		private boolean enrichWithDynatraceMetadata = true;

		/**
		 * Prefix string that is added to all exported metrics.
		 */
		private @Nullable String metricKeyPrefix;

		/**
		 * Whether to fall back to the built-in micrometer instruments for Timer and
		 * DistributionSummary.
		 */
		private boolean useDynatraceSummaryInstruments = true;

		/**
		 * Whether to export meter metadata (unit and description) to the Dynatrace
		 * backend.
		 */
		private boolean exportMeterMetadata = true;

		public @Nullable Map<String, String> getDefaultDimensions() {
			return this.defaultDimensions;
		}

		public void setDefaultDimensions(@Nullable Map<String, String> defaultDimensions) {
			this.defaultDimensions = defaultDimensions;
		}

		public boolean isEnrichWithDynatraceMetadata() {
			return this.enrichWithDynatraceMetadata;
		}

		public void setEnrichWithDynatraceMetadata(Boolean enrichWithDynatraceMetadata) {
			this.enrichWithDynatraceMetadata = enrichWithDynatraceMetadata;
		}

		public @Nullable String getMetricKeyPrefix() {
			return this.metricKeyPrefix;
		}

		public void setMetricKeyPrefix(@Nullable String metricKeyPrefix) {
			this.metricKeyPrefix = metricKeyPrefix;
		}

		public boolean isUseDynatraceSummaryInstruments() {
			return this.useDynatraceSummaryInstruments;
		}

		public void setUseDynatraceSummaryInstruments(boolean useDynatraceSummaryInstruments) {
			this.useDynatraceSummaryInstruments = useDynatraceSummaryInstruments;
		}

		public boolean isExportMeterMetadata() {
			return this.exportMeterMetadata;
		}

		public void setExportMeterMetadata(boolean exportMeterMetadata) {
			this.exportMeterMetadata = exportMeterMetadata;
		}

	}

}
