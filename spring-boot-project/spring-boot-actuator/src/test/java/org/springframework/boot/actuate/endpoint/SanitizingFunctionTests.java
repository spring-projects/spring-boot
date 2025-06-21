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

package org.springframework.boot.actuate.endpoint;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.assertj.core.api.Condition;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SanitizingFunction}.
 *
 * @author Phillip Webb
 */
class SanitizingFunctionTests {

	private static final SanitizableData data = data("key");

	@Test
	void applyUnlessFilteredWhenHasNoFilterReturnsFiltered() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue();
		assertThat(function.apply(data)).has(sanitizedValue());
		assertThat(function.applyUnlessFiltered(data)).has(sanitizedValue());
	}

	@Test
	void applyUnlessFilteredWhenHasFilterTestingTrueReturnsFiltered() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifMatches((data) -> true);
		assertThat(function.apply(data)).has(sanitizedValue());
		assertThat(function.applyUnlessFiltered(data)).has(sanitizedValue());
	}

	@Test
	void applyUnlessFilteredWhenHasFilterTestingFalseReturnsUnfiltered() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifMatches((data) -> false);
		assertThat(function.apply(data)).has(sanitizedValue());
		assertThat(function.applyUnlessFiltered(data)).has(unsanitizedValue());
	}

	@Test
	void ifLikelySensitiveFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifLikelySensitive();
		assertThat(function).satisfies(this::likelyCredentialChecks, this::likelyUriChecks,
				this::likelySensitivePropertyChecks, this::vcapServicesChecks);
	}

	@Test
	void ifLikelyCredentialFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifLikelyCredential();
		assertThat(function).satisfies(this::likelyCredentialChecks);
	}

	private void likelyCredentialChecks(SanitizingFunction function) {
		assertThatApplyingToKey(function, "password").has(sanitizedValue());
		assertThatApplyingToKey(function, "database.password").has(sanitizedValue());
		assertThatApplyingToKey(function, "PASSWORD").has(sanitizedValue());
		assertThatApplyingToKey(function, "secret").has(sanitizedValue());
		assertThatApplyingToKey(function, "key").has(sanitizedValue());
		assertThatApplyingToKey(function, "token").has(sanitizedValue());
		assertThatApplyingToKey(function, "credentials").has(sanitizedValue());
		assertThatApplyingToKey(function, "thecredentialssecret").has(sanitizedValue());
		assertThatApplyingToKey(function, "some.credentials.here").has(sanitizedValue());
		assertThatApplyingToKey(function, "test").has(unsanitizedValue());
	}

	@Test
	void ifLikelyUriFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifLikelyUri();
		assertThat(function).satisfies(this::likelyUriChecks);
	}

	private void likelyUriChecks(SanitizingFunction function) {
		assertThatApplyingToKey(function, "uri").has(sanitizedValue());
		assertThatApplyingToKey(function, "URI").has(sanitizedValue());
		assertThatApplyingToKey(function, "database.uri").has(sanitizedValue());
		assertThatApplyingToKey(function, "uris").has(sanitizedValue());
		assertThatApplyingToKey(function, "url").has(sanitizedValue());
		assertThatApplyingToKey(function, "urls").has(sanitizedValue());
		assertThatApplyingToKey(function, "address").has(sanitizedValue());
		assertThatApplyingToKey(function, "addresses").has(sanitizedValue());
		assertThatApplyingToKey(function, "test").has(unsanitizedValue());
	}

	@Test
	void ifLikelySensitivePropertyFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifLikelySensitiveProperty();
		assertThat(function).satisfies(this::likelySensitivePropertyChecks);
	}

	private void likelySensitivePropertyChecks(SanitizingFunction function) {
		assertThatApplyingToKey(function, "sun.java.command").has(sanitizedValue());
		assertThatApplyingToKey(function, "spring.application.json").has(sanitizedValue());
		assertThatApplyingToKey(function, "SPRING_APPLICATION_JSON").has(sanitizedValue());
		assertThatApplyingToKey(function, "some.other.json").has(unsanitizedValue());
	}

	@Test
	void ifVcapServicesFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifVcapServices();
		assertThat(function).satisfies(this::vcapServicesChecks);
	}

	private void vcapServicesChecks(SanitizingFunction function) {
		assertThatApplyingToKey(function, "vcap_services").has(sanitizedValue());
		assertThatApplyingToKey(function, "vcap.services").has(sanitizedValue());
		assertThatApplyingToKey(function, "vcap.services.whatever").has(sanitizedValue());
		assertThatApplyingToKey(function, "notvcap.services").has(unsanitizedValue());
	}

	@Test
	void ifKeyEqualsFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifKeyEquals("spring", "test");
		assertThatApplyingToKey(function, "spring").has(sanitizedValue());
		assertThatApplyingToKey(function, "SPRING").has(sanitizedValue());
		assertThatApplyingToKey(function, "SpRiNg").has(sanitizedValue());
		assertThatApplyingToKey(function, "test").has(sanitizedValue());
		assertThatApplyingToKey(function, "boot").has(unsanitizedValue());
		assertThatApplyingToKey(function, "xspring").has(unsanitizedValue());
		assertThatApplyingToKey(function, "springx").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifKeyEndsWithFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifKeyEndsWith("boot", "test");
		assertThatApplyingToKey(function, "springboot").has(sanitizedValue());
		assertThatApplyingToKey(function, "SPRINGboot").has(sanitizedValue());
		assertThatApplyingToKey(function, "springBOOT").has(sanitizedValue());
		assertThatApplyingToKey(function, "boot").has(sanitizedValue());
		assertThatApplyingToKey(function, "atest").has(sanitizedValue());
		assertThatApplyingToKey(function, "bootx").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifKeyContainsFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifKeyContains("oo", "ee");
		assertThatApplyingToKey(function, "oo").has(sanitizedValue());
		assertThatApplyingToKey(function, "OO").has(sanitizedValue());
		assertThatApplyingToKey(function, "bOOt").has(sanitizedValue());
		assertThatApplyingToKey(function, "boot").has(sanitizedValue());
		assertThatApplyingToKey(function, "beet").has(sanitizedValue());
		assertThatApplyingToKey(function, "spring").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifKeyMatchesIgnoringCaseFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifKeyMatchesIgnoringCase((key, value) -> key.startsWith(value) && key.endsWith(value), "x", "y");
		assertThatApplyingToKey(function, "xtestx").has(sanitizedValue());
		assertThatApplyingToKey(function, "XtestX").has(sanitizedValue());
		assertThatApplyingToKey(function, "YY").has(sanitizedValue());
		assertThatApplyingToKey(function, "xy").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifKeyMatchesWithRegexFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifKeyMatches("^sp.*$", "^bo.*$");
		assertThatApplyingToKey(function, "spring").has(sanitizedValue());
		assertThatApplyingToKey(function, "spin").has(sanitizedValue());
		assertThatApplyingToKey(function, "SPRING").has(sanitizedValue());
		assertThatApplyingToKey(function, "BOOT").has(sanitizedValue());
		assertThatApplyingToKey(function, "xspring").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifKeyMatchesWithPatternFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifKeyMatches(Pattern.compile("^sp.*$"));
		assertThatApplyingToKey(function, "spring").has(sanitizedValue());
		assertThatApplyingToKey(function, "spin").has(sanitizedValue());
		assertThatApplyingToKey(function, "SPRING").has(unsanitizedValue());
		assertThatApplyingToKey(function, "xspring").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifKeyMatchesWithPredicatesFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifKeyMatches(List.of((key) -> key.startsWith("sp"), (key) -> key.startsWith("BO")));
		assertThatApplyingToKey(function, "spring").has(sanitizedValue());
		assertThatApplyingToKey(function, "spin").has(sanitizedValue());
		assertThatApplyingToKey(function, "BO").has(sanitizedValue());
		assertThatApplyingToKey(function, "SPRING").has(unsanitizedValue());
		assertThatApplyingToKey(function, "boot").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifKeyMatchesWithPredicateFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifKeyMatches((key) -> key.startsWith("sp"));
		assertThatApplyingToKey(function, "spring").has(sanitizedValue());
		assertThatApplyingToKey(function, "spin").has(sanitizedValue());
		assertThatApplyingToKey(function, "boot").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifValueStringMatchesWithRegexesFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue().ifValueStringMatches("^sp.*$", "^bo.*$");
		assertThatApplyingToValue(function, "spring").has(sanitizedValue());
		assertThatApplyingToValue(function, "SPRING").has(sanitizedValue());
		assertThatApplyingToValue(function, "boot").has(sanitizedValue());
		assertThatApplyingToValue(function, "other").has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifValueStringMatchesWithPatternsFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifValueStringMatches(Pattern.compile("^sp.*$"));
		assertThatApplyingToValue(function, "spring").has(sanitizedValue());
		assertThatApplyingToValue(function, "spin").has(sanitizedValue());
		assertThatApplyingToValue(function, "SPRING").has(unsanitizedValue());
		assertThatApplyingToValue(function, "xspring").has(unsanitizedValue());
		assertThatApplyingToValue(function, null).has(unsanitizedValue());
	}

	@Test
	void ifValueStringStringMatchesWithPredicatesFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifValueStringMatches(List.of((value) -> value.startsWith("sp"), (value) -> value.startsWith("BO")));
		assertThatApplyingToValue(function, "spring").has(sanitizedValue());
		assertThatApplyingToValue(function, "spin").has(sanitizedValue());
		assertThatApplyingToValue(function, "BO").has(sanitizedValue());
		assertThatApplyingToValue(function, "SPRING").has(unsanitizedValue());
		assertThatApplyingToValue(function, "boot").has(unsanitizedValue());
		assertThatApplyingToValue(function, null).has(unsanitizedValue());
	}

	@Test
	void ifValueStringMatchesWithPredicateFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifValueStringMatches((value) -> value.startsWith("sp"));
		assertThatApplyingToValue(function, "spring").has(sanitizedValue());
		assertThatApplyingToValue(function, "spin").has(sanitizedValue());
		assertThatApplyingToValue(function, "boot").has(unsanitizedValue());
		assertThatApplyingToValue(function, null).has(unsanitizedValue());
	}

	@Test
	void ifValueMatchesWithPredicatesFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifValueMatches(List.of((value) -> value instanceof String string && string.startsWith("sp"),
					(value) -> value instanceof String string && string.startsWith("BO")));
		assertThatApplyingToValue(function, "spring").has(sanitizedValue());
		assertThatApplyingToValue(function, "spin").has(sanitizedValue());
		assertThatApplyingToValue(function, "BO").has(sanitizedValue());
		assertThatApplyingToValue(function, "SPRING").has(unsanitizedValue());
		assertThatApplyingToValue(function, "boot").has(unsanitizedValue());
		assertThatApplyingToValue(function, 123).has(unsanitizedValue());
		assertThatApplyingToValue(function, null).has(unsanitizedValue());
	}

	@Test
	void ifValueMatchesWithPredicateFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifValueMatches((value) -> value instanceof String string && string.startsWith("sp"));
		assertThatApplyingToValue(function, "spring").has(sanitizedValue());
		assertThatApplyingToValue(function, "spin").has(sanitizedValue());
		assertThatApplyingToValue(function, "boot").has(unsanitizedValue());
		assertThatApplyingToValue(function, 123).has(unsanitizedValue());
		assertThatApplyingToKey(function, null).has(unsanitizedValue());
	}

	@Test
	void ifMatchesPredicatesFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifMatches(List.of((data) -> data.getKey().startsWith("sp") && "boot".equals(data.getValue()),
					(data) -> data.getKey().startsWith("sp") && "framework".equals(data.getValue())));
		assertThatApplying(function, data("spring", "boot")).is(sanitizedValue());
		assertThatApplying(function, data("spring", "framework")).is(sanitizedValue());
		assertThatApplying(function, data("spring", "data")).is(unsanitizedValue());
		assertThatApplying(function, data("spring", null)).is(unsanitizedValue());
	}

	@Test
	void ifMatchesPredicateFiltersExpected() {
		SanitizingFunction function = SanitizingFunction.sanitizeValue()
			.ifMatches((data) -> data.getKey().startsWith("sp") && "boot".equals(data.getValue()));
		assertThatApplying(function, data("spring", "boot")).is(sanitizedValue());
		assertThatApplying(function, data("spring", "framework")).is(unsanitizedValue());
		assertThatApplying(function, data("spring", "data")).is(unsanitizedValue());
		assertThatApplying(function, data("spring", null)).is(unsanitizedValue());
	}

	@Test
	void ofAllowsChainingFromLambda() {
		SanitizingFunction function = SanitizingFunction.of((data) -> data.withValue("----")).ifKeyContains("password");
		assertThat(function.applyUnlessFiltered(data("username", "spring")).getValue()).isEqualTo("spring");
		assertThat(function.applyUnlessFiltered(data("password", "boot")).getValue()).isEqualTo("----");
	}

	private ObjectAssert<SanitizableData> assertThatApplyingToKey(SanitizingFunction function, String key) {
		return assertThatApplying(function, data(key));
	}

	private ObjectAssert<SanitizableData> assertThatApplyingToValue(SanitizingFunction function, Object value) {
		return assertThatApplying(function, data("key", value));
	}

	private ObjectAssert<SanitizableData> assertThatApplying(SanitizingFunction function, SanitizableData data) {
		return assertThat(function.applyUnlessFiltered(data)).as("%s:%s", data.getKey(), data.getValue());
	}

	private Condition<SanitizableData> sanitizedValue() {
		return new Condition<>((data) -> Objects.equals(data.getValue(), SanitizableData.SANITIZED_VALUE),
				"sanitized value");
	}

	private Condition<SanitizableData> unsanitizedValue() {
		return new Condition<>((data) -> !Objects.equals(data.getValue(), SanitizableData.SANITIZED_VALUE),
				"unsanitized value");
	}

	private static SanitizableData data(String key) {
		return data(key, "value");
	}

	private static SanitizableData data(String key, Object value) {
		return new SanitizableData(null, key, value);
	}

}
