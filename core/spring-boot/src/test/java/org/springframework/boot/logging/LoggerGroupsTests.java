/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggerGroups}
 *
 * @author HaiTao Zhang
 * @author Madhura Bhave
 */
class LoggerGroupsTests {

	@Test
	void putAllShouldAddLoggerGroups() {
		Map<String, List<String>> groups = Collections.singletonMap("test",
				Arrays.asList("test.member", "test.member2"));
		LoggerGroups loggerGroups = new LoggerGroups();
		loggerGroups.putAll(groups);
		LoggerGroup group = loggerGroups.get("test");
		assertThat(group).isNotNull();
		assertThat(group.getMembers()).containsExactly("test.member", "test.member2");
	}

	@Test
	void iteratorShouldReturnLoggerGroups() {
		LoggerGroups groups = createLoggerGroups();
		assertThat(groups).hasSize(3);
		assertThat(groups).extracting("name").containsExactlyInAnyOrder("test0", "test1", "test2");
	}

	private LoggerGroups createLoggerGroups() {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		groups.put("test0", Arrays.asList("test0.member", "test0.member2"));
		groups.put("test1", Arrays.asList("test1.member", "test1.member2"));
		groups.put("test2", Arrays.asList("test2.member", "test2.member2"));
		return new LoggerGroups(groups);
	}

}
