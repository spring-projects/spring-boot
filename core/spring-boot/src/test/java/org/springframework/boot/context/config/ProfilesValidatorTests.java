/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link ProfilesValidator}.
 *
 * @author Phillip Webb
 * @author Sijun Yang
 */
class ProfilesValidatorTests {

	private static final Bindable<String> STRING = Bindable.of(String.class);

	private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);

	private static final Bindable<Map<String, String>> STRING_STRING_MAP = Bindable.mapOf(String.class, String.class);

	@Test
	void validateWhenValid() {
		assertValid("test");
		assertValid("dev-test");
		assertValid("dev-test_123");
		assertValid("dev-테스트_123");
		assertValid("d-e_v-t-.e_@@s+t");
	}

	@Test
	void validateWhenInvalidThrowsException() {
		assertInvalid("-dev");
		assertInvalid("_dev");
		assertInvalid("+dev");
		assertInvalid(".dev");
		assertInvalid("dev_");
		assertInvalid("dev*test");
	}

	@Test
	void validateWhenInvalidBoundStringThrowsException() {
		assertInvalid(Map.of("profile", "dev*test"), STRING);
	}

	@Test
	void validateWhenInvalidBoundCollectionThrowsException() {
		assertInvalid(Map.of("profile", "dev*test"), STRING_LIST);
	}

	@Test
	void validateWhenInvalidBoundCollectionFromIndexedThrowsException() {
		assertInvalid(Map.of("profile[0]", "ok,", "profile[1]", "dev*test"), STRING_LIST);
	}

	@Test
	void validateWhenInvalidBoundMapFromIndexedThrowsException() {
		assertInvalid(Map.of("profile.foo", "dev*test"), STRING_STRING_MAP);
	}

	@Test
	void validateWhenInvalidThrowsUsefulExceptionMessage() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind(Map.of("profile", "b*d")))
			.havingCause()
			.withMessageContaining(
					"Profile 'b*d' must contain a letter, digit or allowed char ('-', '_', '.', '+', '@')");
	}

	@Test
	void validateWhenInvalidStartCharacterThrowsUsefulExceptionMessage() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind(Map.of("profile", "_bad")))
			.havingCause()
			.withMessageContaining("Profile '_bad' must start and end with a letter or digit");
	}

	@Test
	void validateWithWrappedExceptionMessageWhenValid() {
		assertThatNoException().isThrownBy(() -> ProfilesValidator.get(new Binder()).validate("ok", () -> "context"));
	}

	@Test
	void validateWithWrappedExceptionMessageWhenInvalidThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> ProfilesValidator.get(new Binder()).validate("b*d", () -> "context"))
			.withMessage("context")
			.havingCause()
			.withMessageContaining("must contain a letter");
	}

	private void assertValid(String value) {
		assertThatNoException().isThrownBy(() -> bind(Map.of("profile", value)));
	}

	private void assertInvalid(String value) {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind(Map.of("profile", value)));
	}

	private <T> void assertInvalid(Map<String, String> map, Bindable<T> target) {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind(map, target));
	}

	private void bind(Map<?, ?> map) {
		bind(map, STRING);
	}

	private <T> void bind(Map<?, ?> map, Bindable<T> target) {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		Binder binder = new Binder(source);
		ProfilesValidator validator = ProfilesValidator.get(binder);
		binder.bind("profile", target, validator);
	}

}
