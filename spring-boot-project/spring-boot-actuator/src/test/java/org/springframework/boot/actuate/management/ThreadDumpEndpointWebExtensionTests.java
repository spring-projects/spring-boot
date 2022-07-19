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

package org.springframework.boot.actuate.management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.management.ThreadDumpEndpoint.ThreadDumpDescriptor;
import org.springframework.boot.actuate.management.ThreadDumpEndpoint.ThreadDumperUnavailableException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadDumpEndpointWebExtension}.
 *
 * @author Moritz Halbritter
 */
class ThreadDumpEndpointWebExtensionTests {

	private ThreadDumpEndpointWebExtension extension;

	private ThreadDumpEndpoint delegateMock;

	@BeforeEach
	void setUp() {
		this.delegateMock = Mockito.mock(ThreadDumpEndpoint.class);
		this.extension = new ThreadDumpEndpointWebExtension(this.delegateMock);
	}

	@Test
	void shouldHandleThreadDumperUnavailable() {
		Mockito.when(this.delegateMock.threadDump())
				.thenThrow(new ThreadDumperUnavailableException("No thread dumper available"));
		WebEndpointResponse<ThreadDumpDescriptor> response = this.extension.threadDump();
		assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
	}

	@Test
	void shouldHandleThreadDumperUnavailableText() {
		Mockito.when(this.delegateMock.textThreadDump())
				.thenThrow(new ThreadDumperUnavailableException("No thread dumper available"));
		WebEndpointResponse<String> response = this.extension.textThreadDump();
		assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
	}

}
