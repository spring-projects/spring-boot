/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BuildpackReference}.
 *
 * @author Phillip Webb
 */
class BuildpackReferenceTests {

	@Test
	void ofWhenValueIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackReference.of(""))
			.withMessage("'value' must not be empty");
	}

	@Test
	void ofCreatesInstance() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference).isNotNull();
	}

	@Test
	void toStringReturnsValue() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference).hasToString("test");
	}

	@Test
	void equalsAndHashCode() {
		BuildpackReference a = BuildpackReference.of("test1");
		BuildpackReference b = BuildpackReference.of("test1");
		BuildpackReference c = BuildpackReference.of("test2");
		assertThat(a).isEqualTo(a).isEqualTo(b).isNotEqualTo(c);
		assertThat(a).hasSameHashCodeAs(b);
	}

	@Test
	void hasPrefixWhenPrefixMatchReturnsTrue() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference.hasPrefix("te")).isTrue();
	}

	@Test
	void hasPrefixWhenPrefixMismatchReturnsFalse() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference.hasPrefix("st")).isFalse();
	}

	@Test
	void getSubReferenceWhenPrefixMatchReturnsSubReference() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference.getSubReference("te")).isEqualTo("st");
	}

	@Test
	void getSubReferenceWhenPrefixMismatchReturnsNull() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference.getSubReference("st")).isNull();
	}

	@Test
	void asPathWhenFileUrlReturnsPath() {
		BuildpackReference reference = BuildpackReference.of("file:///test.dat");
		assertThat(reference.asPath()).isEqualTo(Paths.get("/test.dat"));
	}

	@Test
	void asPathWhenPathReturnsPath() {
		BuildpackReference reference = BuildpackReference.of("/test.dat");
		assertThat(reference.asPath()).isEqualTo(Paths.get("/test.dat"));
	}

}
