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

import org.junit.jupiter.api.Test;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SecurityContextHolderStrategyRegistrar}.
 *
 * @author Jonatan Ivanov
 */
class SecurityContextHolderStrategyRegistrarTest {

	@Test
	void whenSecurityContextHolderStrategyIsPresentItShouldBeRegistered() {
		SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
		new SecurityContextHolderStrategyRegistrar(strategy);
		assertThat(SecurityContextHolder.getContextHolderStrategy()).isSameAs(strategy);
	}

	@Test
	void whenNoSecurityContextHolderStrategyIsPresentItShouldNotFail() {
		new SecurityContextHolderStrategyRegistrar(null);
		assertThat(SecurityContextHolder.getContextHolderStrategy()).isNotNull();
	}

}
