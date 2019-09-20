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

package org.springframework.boot.actuate.diagnostics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DiagnosticsEndpoint}.
 *
 * @author Luis De Bello
 */
public class DiagnosticsEndpointTests {

	private static DiagnosticsEndpoint diagnosticsEndpoint;

	@BeforeAll
	static void createDiagnosticsEndpoint() {
		diagnosticsEndpoint = new DiagnosticsEndpoint();
	}

	@Test
	void checkOperationsAreNotNull() {
		String operations = diagnosticsEndpoint.getOperations();
		assertThat(operations).isNotNull();
	}

	@Test
	void checkOperationsDoesNotContainsErrorMessage() {
		String operations = diagnosticsEndpoint.getOperations();
		assertThat(operations).doesNotContain(VMDiagnostics.GET_OPERATIONS_ERROR_MESSAGE);
	}

	@Test
	void checkOperationDetails() throws IOException {
		String operations = diagnosticsEndpoint.getOperations();
		try (BufferedReader reader = new BufferedReader(new StringReader(operations))) {
			String firstOperation = reader.readLine();
			String firstOperationDetails = diagnosticsEndpoint.getOperationDetails(firstOperation);
			assertThat(firstOperationDetails).isNotNull();
		}
	}

	@Test
	void checkOperationDetailsDoesNotContainsErrorMessage() throws IOException {
		String operations = diagnosticsEndpoint.getOperations();
		try (BufferedReader reader = new BufferedReader(new StringReader(operations))) {
			String firstOperation = reader.readLine();
			String firstOperationDetails = diagnosticsEndpoint.getOperationDetails(firstOperation);
			assertThat(firstOperationDetails).doesNotContain(VMDiagnostics.NO_HELP_AVAILABLE_ERROR_MESSAGE);
		}
	}

}
