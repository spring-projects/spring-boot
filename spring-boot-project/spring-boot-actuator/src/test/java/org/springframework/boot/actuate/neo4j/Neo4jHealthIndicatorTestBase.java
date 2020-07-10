/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.actuate.neo4j;

import org.mockito.Mock;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.summary.DatabaseInfo;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.ServerInfo;

import static org.mockito.Mockito.when;

/**
 * Contains some shared mocks for the health indicator tests.
 *
 * @author Michael J. Simons
 */
abstract class Neo4jHealthIndicatorTestBase {

	@Mock
	protected Driver driver;

	@Mock
	protected ResultSummary resultSummary;

	@Mock
	protected ServerInfo serverInfo;

	@Mock
	protected DatabaseInfo databaseInfo;

	@Mock
	protected Record record;

	protected void prepareSharedMocks() {

		when(serverInfo.version()).thenReturn("4711");
		when(serverInfo.address()).thenReturn("Zu Hause");
		when(databaseInfo.name()).thenReturn("n/a");
		when(resultSummary.server()).thenReturn(serverInfo);
		when(resultSummary.database()).thenReturn(databaseInfo);
		when(record.get("edition")).thenReturn(Values.value("ultimate collectors edition"));
	}

}
