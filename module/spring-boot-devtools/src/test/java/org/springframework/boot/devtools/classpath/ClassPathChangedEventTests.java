/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.classpath;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devtools.filewatch.ChangedFiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassPathChangedEvent}.
 *
 * @author Phillip Webb
 */
class ClassPathChangedEventTests {

	private final Object source = new Object();

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void changeSetMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassPathChangedEvent(this.source, null, false))
			.withMessageContaining("'changeSet' must not be null");
	}

	@Test
	void getChangeSet() {
		Set<ChangedFiles> changeSet = new LinkedHashSet<>();
		ClassPathChangedEvent event = new ClassPathChangedEvent(this.source, changeSet, false);
		assertThat(event.getChangeSet()).isSameAs(changeSet);
	}

	@Test
	void getRestartRequired() {
		Set<ChangedFiles> changeSet = new LinkedHashSet<>();
		ClassPathChangedEvent event;
		event = new ClassPathChangedEvent(this.source, changeSet, false);
		assertThat(event.isRestartRequired()).isFalse();
		event = new ClassPathChangedEvent(this.source, changeSet, true);
		assertThat(event.isRestartRequired()).isTrue();
	}

}
