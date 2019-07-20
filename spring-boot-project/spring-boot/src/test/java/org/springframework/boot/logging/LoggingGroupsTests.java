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

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggingGroups}
 *
 * @author HaiTao Zhang
 */
public class LoggingGroupsTests {

	private LoggingSystem loggingSystem = mock(LoggingSystem.class);

	@Test
	void setLoggerGroupWithTheConfiguredLevelToAllMembers() {
		LoggingGroups loggingGroups = new LoggingGroups(this.loggingSystem);
		loggingGroups.setLoggerGroup("test", Arrays.asList("test.member", "test.member2"));
		loggingGroups.setLoggerGroupLevel("test", LogLevel.DEBUG);
		verify(this.loggingSystem).setLogLevel("test.member2", LogLevel.DEBUG);
		verify(this.loggingSystem).setLogLevel("test.member", LogLevel.DEBUG);
	}

}
