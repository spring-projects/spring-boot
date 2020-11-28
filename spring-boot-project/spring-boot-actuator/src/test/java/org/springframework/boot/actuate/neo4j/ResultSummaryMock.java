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

package org.springframework.boot.actuate.neo4j;

import org.neo4j.driver.summary.DatabaseInfo;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.ServerInfo;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test utility to mock {@link ResultSummary}.
 *
 * @author Stephane Nicoll
 */
final class ResultSummaryMock {

	private ResultSummaryMock() {
	}

	static ResultSummary createResultSummary(String serverVersion, String serverAddress, String databaseName) {
		ServerInfo serverInfo = mock(ServerInfo.class);
		given(serverInfo.version()).willReturn(serverVersion);
		given(serverInfo.address()).willReturn(serverAddress);
		DatabaseInfo databaseInfo = mock(DatabaseInfo.class);
		given(databaseInfo.name()).willReturn(databaseName);
		ResultSummary resultSummary = mock(ResultSummary.class);
		given(resultSummary.server()).willReturn(serverInfo);
		given(resultSummary.database()).willReturn(databaseInfo);
		return resultSummary;
	}

}
