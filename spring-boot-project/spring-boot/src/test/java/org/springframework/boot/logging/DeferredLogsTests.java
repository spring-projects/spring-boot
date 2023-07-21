/*
 * Copyright 2012-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DeferredLogs}.
 *
 * @author Phillip Webb
 */
class DeferredLogsTests {

	@Test
	void switchOverAllSwitchesLoggersWithOrderedOutput() {
		Log log1 = mock(Log.class);
		Log log2 = mock(Log.class);
		DeferredLogs loggers = new DeferredLogs();
		Log dlog1 = loggers.getLog(log1);
		Log dlog2 = loggers.getLog(log2);
		dlog1.info("a");
		dlog2.info("b");
		dlog1.info("c");
		dlog2.info("d");
		then(log1).shouldHaveNoInteractions();
		then(log2).shouldHaveNoInteractions();
		loggers.switchOverAll();
		InOrder ordered = inOrder(log1, log2);
		then(log1).should(ordered).info("a", null);
		then(log2).should(ordered).info("b", null);
		then(log1).should(ordered).info("c", null);
		then(log2).should(ordered).info("d", null);
		then(log1).shouldHaveNoMoreInteractions();
		then(log2).shouldHaveNoMoreInteractions();
		dlog1.info("e");
		dlog2.info("f");
		then(log1).should(ordered).info("e", null);
		then(log2).should(ordered).info("f", null);
	}

}
