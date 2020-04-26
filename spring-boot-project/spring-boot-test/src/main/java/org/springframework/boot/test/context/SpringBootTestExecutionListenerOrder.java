/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.boot.test.context;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestExecutionListener;

/**
 * Aggregates the orders of the {@link TestExecutionListener}s defined by Spring Boot
 * Test.
 *
 * @author Andrei Dragnea
 * @since 2.3.0
 * @see org.springframework.core.Ordered
 * @see TestExecutionListener
 */
public enum SpringBootTestExecutionListenerOrder {

	/**
	 * Order of
	 * {@link org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener}.
	 */
	RESET_MOCKS(Ordered.LOWEST_PRECEDENCE - 100),

	/**
	 * Order of
	 * {@link org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener}.
	 */
	MOCKITO(1_950);

	private final int value;

	SpringBootTestExecutionListenerOrder(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

}
