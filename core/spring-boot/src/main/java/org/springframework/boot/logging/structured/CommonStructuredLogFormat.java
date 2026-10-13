/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.structured;

import org.jspecify.annotations.Nullable;

/**
 * Common structured log formats supported by Spring Boot.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.4.0
 */
public enum CommonStructuredLogFormat {

	/**
	 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">Elastic Common
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
	static @Nullable CommonStructuredLogFormat forId(String id) {
		for (CommonStructuredLogFormat candidate : values()) {
			if (candidate.getId().equalsIgnoreCase(id)) {
				return candidate;
			}
		}
		return null;
	}

}
