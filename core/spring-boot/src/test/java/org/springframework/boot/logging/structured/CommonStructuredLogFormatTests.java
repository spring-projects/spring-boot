/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.structured;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommonStructuredLogFormat}.
 *
 * @author Phillip Webb
 */
class CommonStructuredLogFormatTests {

	@Test
	void forIdReturnsCommonStructuredLogFormat() {
		assertThat(CommonStructuredLogFormat.forId("ecs")).isEqualTo(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA);
		assertThat(CommonStructuredLogFormat.forId("logstash")).isEqualTo(CommonStructuredLogFormat.LOGSTASH);
	}

	@Test
	void forIdWhenIdIsInDifferentCaseReturnsCommonStructuredLogFormat() {
		assertThat(CommonStructuredLogFormat.forId("ECS")).isEqualTo(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA);
		assertThat(CommonStructuredLogFormat.forId("logSTAsh")).isEqualTo(CommonStructuredLogFormat.LOGSTASH);
	}

	@Test
	void forIdWhenNotKnownReturnsNull() {
		assertThat(CommonStructuredLogFormat.forId("madeup")).isNull();
	}

}
