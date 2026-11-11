/*
 * Copyright 2012-present the original author or authors.
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

	private final ClassLoader classLoader = getClass().getClassLoader();

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
