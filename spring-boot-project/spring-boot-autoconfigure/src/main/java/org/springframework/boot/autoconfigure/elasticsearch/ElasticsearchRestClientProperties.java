/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.elasticsearch;

import java.time.Duration;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties specific to Elasticsearch's {@link RestClient} and
 * {@link RestHighLevelClient}.
 *
 * @author Brian Clozel
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "spring.elasticsearch.restclient")
public class ElasticsearchRestClientProperties {

	private final Sniffer sniffer = new Sniffer();

	public Sniffer getSniffer() {
		return this.sniffer;
	}

	public static class Sniffer {

		/**
		 * Interval between consecutive ordinary sniff executions.
		 */
		private Duration interval = Duration.ofMinutes(5);

		/**
		 * Delay of a sniff execution scheduled after a failure.
		 */
		private Duration delayAfterFailure = Duration.ofMinutes(1);

		public Duration getInterval() {
			return this.interval;
		}

		public void setInterval(Duration interval) {
			this.interval = interval;
		}

		public Duration getDelayAfterFailure() {
			return this.delayAfterFailure;
		}

		public void setDelayAfterFailure(Duration delayAfterFailure) {
			this.delayAfterFailure = delayAfterFailure;
		}

	}

}
