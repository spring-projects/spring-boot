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

package org.springframework.boot.actuate.autoconfigure.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Micrometer-based metrics.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties("spring.metrics")
public class MetricsProperties {

	private Web web = new Web();

	/**
	 * Whether or not auto-configured MeterRegistry implementations should be bound to the
	 * global static registry on Metrics. For testing, set this to 'false' to maximize
	 * test independence.
	 */
	private boolean useGlobalRegistry = true;

	public boolean isUseGlobalRegistry() {
		return this.useGlobalRegistry;
	}

	public void setUseGlobalRegistry(boolean useGlobalRegistry) {
		this.useGlobalRegistry = useGlobalRegistry;
	}

	public Web getWeb() {
		return this.web;
	}

	public static class Web {

		private Client client = new Client();

		private Server server = new Server();

		public Client getClient() {
			return this.client;
		}

		public void setClient(Client client) {
			this.client = client;
		}

		public Server getServer() {
			return this.server;
		}

		public void setServer(Server server) {
			this.server = server;
		}

		public static class Client {

			/**
			 * Whether or not instrumented requests record percentiles histogram buckets
			 * by default.
			 */
			private boolean recordRequestPercentiles;

			/**
			 * Name of the metric for sent requests.
			 */
			private String requestsMetricName = "http.client.requests";

			public boolean isRecordRequestPercentiles() {
				return this.recordRequestPercentiles;
			}

			public void setRecordRequestPercentiles(boolean recordRequestPercentiles) {
				this.recordRequestPercentiles = recordRequestPercentiles;
			}

			public String getRequestsMetricName() {
				return this.requestsMetricName;
			}

			public void setRequestsMetricName(String requestsMetricName) {
				this.requestsMetricName = requestsMetricName;
			}

		}

		public static class Server {

			/**
			 * Whether or not requests handled by Spring MVC or WebFlux should be
			 * automatically timed. If the number of time series emitted grows too large
			 * on account of request mapping timings, disable this and use 'Timed' on a
			 * per request mapping basis as needed.
			 */
			private boolean autoTimeRequests = true;

			/**
			 * Whether or not instrumented requests record percentiles histogram buckets
			 * by default. Can be overridden by adding '@Timed' to a request endpoint and
			 * setting 'percentiles' to true.
			 */
			private boolean recordRequestPercentiles;

			/**
			 * Name of the metric for received requests.
			 */
			private String requestsMetricName = "http.server.requests";

			public boolean isAutoTimeRequests() {
				return this.autoTimeRequests;
			}

			public void setAutoTimeRequests(boolean autoTimeRequests) {
				this.autoTimeRequests = autoTimeRequests;
			}

			public boolean isRecordRequestPercentiles() {
				return this.recordRequestPercentiles;
			}

			public void setRecordRequestPercentiles(boolean recordRequestPercentiles) {
				this.recordRequestPercentiles = recordRequestPercentiles;
			}

			public String getRequestsMetricName() {
				return this.requestsMetricName;
			}

			public void setRequestsMetricName(String requestsMetricName) {
				this.requestsMetricName = requestsMetricName;
			}

		}

	}

}
