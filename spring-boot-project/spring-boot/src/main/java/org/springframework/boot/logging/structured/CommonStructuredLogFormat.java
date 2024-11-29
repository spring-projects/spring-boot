/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.structured;

/**
 * Common structured log formats supported by Spring Boot.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.4.0
 */
public enum CommonStructuredLogFormat {

	/**
	 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">Elasic Common
	 * Schema</a> (ECS) log format.
	 */
	ELASTIC_COMMON_SCHEMA("ecs"),

	/**
	 * <a href="https://go2docs.graylog.org/current/getting_in_log_data/gelf.html">Graylog
	 * Extended Log Format</a> (GELF) log format.
	 */
	GRAYLOG_EXTENDED_LOG_FORMAT("gelf"),

	/**
	 * The <a href=
	 * "https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#standard-fields">Logstash</a>
	 * log format.
	 */
	LOGSTASH("logstash");

	private final String id;

	CommonStructuredLogFormat(String id) {
		this.id = id;
	}

	/**
	 * Return the ID for this format.
	 * @return the format identifier
	 */
	String getId() {
		return this.id;
	}

	/**
	 * Find the {@link CommonStructuredLogFormat} for the given ID.
	 * @param id the format identifier
	 * @return the associated {@link CommonStructuredLogFormat} or {@code null}
	 */
	static CommonStructuredLogFormat forId(String id) {
		for (CommonStructuredLogFormat candidate : values()) {
			if (candidate.getId().equalsIgnoreCase(id)) {
				return candidate;
			}
		}
		return null;
	}

}
