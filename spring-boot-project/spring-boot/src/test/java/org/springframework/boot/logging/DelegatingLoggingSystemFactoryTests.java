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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DelegatingLoggingSystemFactory}.
 *
 * @author Phillip Webb
 */
class DelegatingLoggingSystemFactoryTests {

	private ClassLoader classLoader = getClass().getClassLoader();

	@Test
	void getLoggingSystemWhenDelegatesFunctionIsNullReturnsNull() {
		DelegatingLoggingSystemFactory factory = new DelegatingLoggingSystemFactory(null);
		assertThat(factory.getLoggingSystem(this.classLoader)).isNull();
	}

	@Test
	void getLoggingSystemWhenDelegatesFunctionReturnsNullReturnsNull() {
		DelegatingLoggingSystemFactory factory = new DelegatingLoggingSystemFactory((cl) -> null);
		assertThat(factory.getLoggingSystem(this.classLoader)).isNull();
	}

	@Test
	void getLoggingSystemReturnsFirstNonNullLoggingSystem() {
		List<LoggingSystemFactory> delegates = new ArrayList<>();
		delegates.add(mock(LoggingSystemFactory.class));
		delegates.add(mock(LoggingSystemFactory.class));
		delegates.add(mock(LoggingSystemFactory.class));
		LoggingSystem result = mock(LoggingSystem.class);
		given(delegates.get(1).getLoggingSystem(this.classLoader)).willReturn(result);
		DelegatingLoggingSystemFactory factory = new DelegatingLoggingSystemFactory((cl) -> delegates);
		assertThat(factory.getLoggingSystem(this.classLoader)).isSameAs(result);
		then(delegates.get(0)).should().getLoggingSystem(this.classLoader);
		then(delegates.get(1)).should().getLoggingSystem(this.classLoader);
		then(delegates.get(2)).shouldHaveNoInteractions();
	}

}
