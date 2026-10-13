/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.health;

import org.jspecify.annotations.Nullable;
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

	static ResultSummary createResultSummary(String serverAddress, @Nullable String databaseName) {
		ServerInfo serverInfo = mock(ServerInfo.class);
		given(serverInfo.address()).willReturn(serverAddress);
		DatabaseInfo databaseInfo = mock(DatabaseInfo.class);
		given(databaseInfo.name()).willReturn(databaseName);
		ResultSummary resultSummary = mock(ResultSummary.class);
		given(resultSummary.server()).willReturn(serverInfo);
		given(resultSummary.database()).willReturn(databaseInfo);
		return resultSummary;
	}

}
