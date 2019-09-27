/*
 * Copyright 2012-2019 the original author or authors.
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
