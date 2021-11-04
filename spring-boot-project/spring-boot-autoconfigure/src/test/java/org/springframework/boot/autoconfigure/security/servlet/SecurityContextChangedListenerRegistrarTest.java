/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.context.ListeningSecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextChangedListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SecurityContextChangedListenerRegistrar}.
 *
 * @author Jonatan Ivanov
 */
class SecurityContextChangedListenerRegistrarTest {

	private final SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();

	@AfterEach
	void tearDown() {
		SecurityContextHolder.setContextHolderStrategy(this.strategy);
	}

	@Test
	void whenASecurityContextChangedListenersIsProvidedThenAListeningSecurityContextHolderStrategyShouldBeRegistered() {
		List<SecurityContextChangedListener> listeners = new ArrayList<>();
		listeners.add(mock(SecurityContextChangedListener.class));

		new SecurityContextChangedListenerRegistrar(listeners);
		assertThat(SecurityContextHolder.getContextHolderStrategy())
				.isInstanceOf(ListeningSecurityContextHolderStrategy.class);
	}

	@Test
	void whenNoSecurityContextChangedListenerIsProvidedThenNoListeningSecurityContextHolderStrategyShouldBeRegistered() {
		List<SecurityContextChangedListener> listeners = new ArrayList<>();
		SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
		SecurityContextHolder.setContextHolderStrategy(strategy);

		new SecurityContextChangedListenerRegistrar(listeners);
		assertThat(SecurityContextHolder.getContextHolderStrategy()).isSameAs(strategy);
	}

}
