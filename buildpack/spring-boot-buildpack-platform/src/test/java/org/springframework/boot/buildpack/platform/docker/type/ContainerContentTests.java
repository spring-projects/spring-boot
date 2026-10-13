/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.io.TarArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ContainerContent}.
 *
 * @author Phillip Webb
 */
class ContainerContentTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenArchiveIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerContent.of(null))
			.withMessage("'archive' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenDestinationPathIsNullThrowsException() {
		TarArchive archive = mock(TarArchive.class);
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerContent.of(archive, null))
			.withMessage("'destinationPath' must not be empty");
	}

	@Test
	void ofWhenDestinationPathIsEmptyThrowsException() {
		TarArchive archive = mock(TarArchive.class);
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerContent.of(archive, ""))
			.withMessage("'destinationPath' must not be empty");
	}

	@Test
	void ofCreatesContainerContent() {
		TarArchive archive = mock(TarArchive.class);
		ContainerContent content = ContainerContent.of(archive);
		assertThat(content.getArchive()).isSameAs(archive);
		assertThat(content.getDestinationPath()).isEqualTo("/");
	}

	@Test
	void ofWithDestinationPathCreatesContainerContent() {
		TarArchive archive = mock(TarArchive.class);
		ContainerContent content = ContainerContent.of(archive, "/test");
		assertThat(content.getArchive()).isSameAs(archive);
		assertThat(content.getDestinationPath()).isEqualTo("/test");
	}

}
