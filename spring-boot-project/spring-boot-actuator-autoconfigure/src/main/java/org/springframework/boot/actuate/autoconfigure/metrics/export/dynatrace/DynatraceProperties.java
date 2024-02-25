/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace;

import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Dynatrace
 * metrics export.
 *
 * @author Andy Wilkinson
 * @author Georg Pirklbauer
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "management.dynatrace.metrics.export")
public class DynatraceProperties extends StepRegistryProperties {

	private final V1 v1 = new V1();

	private final V2 v2 = new V2();

	/**
	 * Dynatrace authentication token.
	 */
	private String apiToken;

	/**
	 * URI to ship metrics to. Should be used for SaaS, self-managed instances or to
	 * en-route through an internal proxy.
	 */
	private String uri;

	/**
	 * Returns the API token.
	 * @return the API token
	 */
	public String getApiToken() {
		return this.apiToken;
	}

	/**
	 * Sets the API token for authentication.
	 * @param apiToken the API token to be set
	 */
	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	/**
	 * Returns the URI of the DynatraceProperties object.
	 * @return the URI of the DynatraceProperties object
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * Sets the URI for the DynatraceProperties.
	 * @param uri the URI to be set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Returns the V1 object.
	 * @return the V1 object
	 */
	public V1 getV1() {
		return this.v1;
	}

	/**
	 * Returns the V2 object.
	 * @return the V2 object
	 */
	public V2 getV2() {
		return this.v2;
	}

	/**
	 * V1 class.
	 */
	public static class V1 {

		/**
		 * ID of the custom device that is exporting metrics to Dynatrace.
		 */
		private String deviceId;

		/**
		 * Group for exported metrics. Used to specify custom device group name in the
		 * Dynatrace UI.
		 */
		private String group;

		/**
		 * Technology type for exported metrics. Used to group metrics under a logical
		 * technology name in the Dynatrace UI.
		 */
		private String technologyType = "java";

		/**
		 * Returns the device ID.
		 * @return the device ID
		 */
		public String getDeviceId() {
			return this.deviceId;
		}

		/**
		 * Sets the device ID.
		 * @param deviceId the device ID to be set
		 */
		public void setDeviceId(String deviceId) {
			this.deviceId = deviceId;
		}

		/**
		 * Returns the group of the object.
		 * @return the group of the object
		 */
		public String getGroup() {
			return this.group;
		}

		/**
		 * Sets the group for the object.
		 * @param group the group to be set
		 */
		public void setGroup(String group) {
			this.group = group;
		}

		/**
		 * Returns the technology type of the object.
		 * @return the technology type
		 */
		public String getTechnologyType() {
			return this.technologyType;
		}

		/**
		 * Sets the technology type for the V1 class.
		 * @param technologyType the technology type to be set
		 */
		public void setTechnologyType(String technologyType) {
			this.technologyType = technologyType;
		}

	}

	/**
	 * V2 class.
	 */
	public static class V2 {

		/**
		 * Default dimensions that are added to all metrics in the form of key-value
		 * pairs. These are overwritten by Micrometer tags if they use the same key.
		 */
		private Map<String, String> defaultDimensions;

		/**
		 * Whether to enable Dynatrace metadata export.
		 */
		private boolean enrichWithDynatraceMetadata = true;

		/**
		 * Prefix string that is added to all exported metrics.
		 */
		private String metricKeyPrefix;

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

		/**
		 * Returns the default dimensions as a map of key-value pairs.
		 * @return the default dimensions as a map of key-value pairs
		 */
		public Map<String, String> getDefaultDimensions() {
			return this.defaultDimensions;
		}

		/**
		 * Sets the default dimensions for the V2 class.
		 * @param defaultDimensions a map containing the default dimensions as key-value
		 * pairs
		 */
		public void setDefaultDimensions(Map<String, String> defaultDimensions) {
			this.defaultDimensions = defaultDimensions;
		}

		/**
		 * Returns a boolean value indicating whether the data is enriched with Dynatrace
		 * metadata.
		 * @return true if the data is enriched with Dynatrace metadata, false otherwise
		 */
		public boolean isEnrichWithDynatraceMetadata() {
			return this.enrichWithDynatraceMetadata;
		}

		/**
		 * Sets the flag to enrich the data with Dynatrace metadata.
		 * @param enrichWithDynatraceMetadata true to enrich the data with Dynatrace
		 * metadata, false otherwise
		 */
		public void setEnrichWithDynatraceMetadata(Boolean enrichWithDynatraceMetadata) {
			this.enrichWithDynatraceMetadata = enrichWithDynatraceMetadata;
		}

		/**
		 * Returns the metric key prefix.
		 * @return the metric key prefix
		 */
		public String getMetricKeyPrefix() {
			return this.metricKeyPrefix;
		}

		/**
		 * Sets the metric key prefix.
		 * @param metricKeyPrefix the metric key prefix to be set
		 */
		public void setMetricKeyPrefix(String metricKeyPrefix) {
			this.metricKeyPrefix = metricKeyPrefix;
		}

		/**
		 * Returns the value of the useDynatraceSummaryInstruments property.
		 * @return the value of the useDynatraceSummaryInstruments property
		 */
		public boolean isUseDynatraceSummaryInstruments() {
			return this.useDynatraceSummaryInstruments;
		}

		/**
		 * Sets the flag indicating whether to use Dynatrace summary instruments.
		 * @param useDynatraceSummaryInstruments true to use Dynatrace summary
		 * instruments, false otherwise
		 */
		public void setUseDynatraceSummaryInstruments(boolean useDynatraceSummaryInstruments) {
			this.useDynatraceSummaryInstruments = useDynatraceSummaryInstruments;
		}

		/**
		 * Returns the value of the exportMeterMetadata flag.
		 * @return true if the exportMeterMetadata flag is set, false otherwise.
		 */
		public boolean isExportMeterMetadata() {
			return this.exportMeterMetadata;
		}

		/**
		 * Sets the flag to export meter metadata.
		 * @param exportMeterMetadata true to export meter metadata, false otherwise
		 */
		public void setExportMeterMetadata(boolean exportMeterMetadata) {
			this.exportMeterMetadata = exportMeterMetadata;
		}

	}

}
