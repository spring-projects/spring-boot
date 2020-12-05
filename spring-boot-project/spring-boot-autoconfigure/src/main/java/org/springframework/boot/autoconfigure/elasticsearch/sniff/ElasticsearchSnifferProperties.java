/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.elasticsearch.sniff;

import org.elasticsearch.client.sniff.NodesSniffer;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration properties for Elasticsearch sniffer.
 *
 */
@ConfigurationProperties(prefix = "spring.elasticsearch.sniff")
public class ElasticsearchSnifferProperties {

	/**
	 * Sniffer interval millis
	 */
	private Duration sniffIntervalMillis = Duration.ofMillis(1L);
	/**
	 * Sniffer failure delay millis.
	 */
	private Duration sniffFailureDelayMillis = Duration.ofMillis(1L);

	public Duration getSniffIntervalMillis() {
		return sniffIntervalMillis;
	}

	public void setSniffIntervalMillis(Duration sniffIntervalMillis) {
		this.sniffIntervalMillis = sniffIntervalMillis;
	}

	public Duration getSniffFailureDelayMillis() {
		return sniffFailureDelayMillis;
	}

	public void setSniffFailureDelayMillis(Duration sniffFailureDelayMillis) {
		this.sniffFailureDelayMillis = sniffFailureDelayMillis;
	}
}
