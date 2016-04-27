/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.remote.client;

import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

import org.junit.Test;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LocalDebugPortAvailableCondition}.
 *
 * @author Phillip Webb
 */
public class LocalDebugPortAvailableConditionTests {

	private int port = SocketUtils.findAvailableTcpPort();

	private LocalDebugPortAvailableCondition condition = new LocalDebugPortAvailableCondition();

	@Test
	public void portAvailable() throws Exception {
		ConditionOutcome outcome = getOutcome();
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).isEqualTo("Local debug port available");
	}

	@Test
	public void portInUse() throws Exception {
		final ServerSocket serverSocket = ServerSocketFactory.getDefault()
				.createServerSocket(this.port);
		ConditionOutcome outcome = getOutcome();
		serverSocket.close();
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).isEqualTo("Local debug port unavailable");
	}

	private ConditionOutcome getOutcome() {
		MockEnvironment environment = new MockEnvironment();
		EnvironmentTestUtils.addEnvironment(environment,
				"spring.devtools.remote.debug.local-port:" + this.port);
		ConditionContext context = mock(ConditionContext.class);
		given(context.getEnvironment()).willReturn(environment);
		ConditionOutcome outcome = this.condition.getMatchOutcome(context, null);
		return outcome;
	}

}
