/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Function that takes a {@link SanitizableData} and applies sanitization to the value, if
 * necessary. Can be used by a {@link Sanitizer} to determine the sanitized value.
 * <p>
 * This interface also provides convenience methods that can help build a
 * {@link SanitizingFunction} instances, for example to return from a {@code @Bean}
 * method. See {@link #sanitizeValue()} for an example.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.6.0
 * @see Sanitizer
 */
@FunctionalInterface
public interface SanitizingFunction {

	/**
	 * Apply the sanitizing function to the given data.
	 * @param data the data to sanitize
	 * @return the sanitized data or the original instance is no sanitization is applied
	 */
	SanitizableData apply(SanitizableData data);

	/**
	 * Return an optional filter that determines if the sanitizing function applies.
	 * @return a predicate used to filter functions or {@code null} if no filter is
	 * declared
	 * @since 3.5.0
	 * @see #applyUnlessFiltered(SanitizableData)
	 */
	default Predicate<SanitizableData> filter() {
		return null;
	}

	/**
	 * Apply the sanitizing function as long as the filter passes or there is no filter.
	 * @param data the data to sanitize
	 * @return the sanitized data or the original instance is no sanitization is applied
	 * @since 3.5.0
	 */
	default SanitizableData applyUnlessFiltered(SanitizableData data) {
		return (filter() == null || filter().test(data)) ? apply(data) : data;
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data is
	 * likely to contain a sensitive value. This method can help construct a useful
	 * sanitizing function, but may not catch all sensitive data so care should be taken
	 * to test the results for your specific environment.
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifLikelySensitive() {
		return ifLikelyCredential().ifLikelyUri().ifLikelySensitiveProperty().ifVcapServices();
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data is
	 * likely to contain a credential. This method can help construct a useful sanitizing
	 * function, but may not catch all sensitive data so care should be taken to test the
	 * results for your specific environment.
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifLikelyCredential() {
		return ifKeyEndsWith("password", "secret", "key", "token").ifKeyContains("credentials");
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data is
	 * likely to contain a URI. This method can help construct a useful sanitizing
	 * function, but may not catch all sensitive data so care should be taken to test the
	 * results for your specific environment.
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifLikelyUri() {
		return ifKeyEndsWith("uri", "uris", "url", "urls", "address", "addresses");
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data is
	 * likely to contain a sensitive property value. This method can help construct a
	 * useful sanitizing function, but may not catch all sensitive data so care should be
	 * taken to test the results for your specific environment.
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifLikelySensitiveProperty() {
		return ifKeyMatches("sun.java.command", "^spring[._]application[._]json$");
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data is for
	 * VCAP services.
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */

	default SanitizingFunction ifVcapServices() {
		return ifKeyEquals("vcap_services").ifKeyMatches("^vcap\\.services.*$");
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data key is
	 * equal to any of the given values (ignoring case).
	 * @param values the case insensitive values that the key can equal
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifKeyEquals(String... values) {
		Assert.notNull(values, "'values' must not be null");
		return ifKeyMatchesIgnoringCase(String::equals, values);
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data key ends
	 * with any of the given values (ignoring case).
	 * @param suffixes the case insensitive suffixes that they key can end with
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifKeyEndsWith(String... suffixes) {
		Assert.notNull(suffixes, "'suffixes' must not be null");
		return ifKeyMatchesIgnoringCase(String::endsWith, suffixes);
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data key
	 * contains any of the given values (ignoring case).
	 * @param values the case insensitive values that the key can contain
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifKeyContains(String... values) {
		Assert.notNull(values, "'values' must not be null");
		return ifKeyMatchesIgnoringCase(String::contains, values);
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data key and
	 * any of the values match the given predicate. The predicate is only called with
	 * lower case values.
	 * @param predicate the predicate used to check the key against a value. The key is
	 * the first argument and the value is the second. Both are converted to lower case
	 * @param values the case insensitive values that the key can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifKeyMatchesIgnoringCase(BiPredicate<String, String> predicate, String... values) {
		Assert.notNull(predicate, "'predicate' must not be null");
		Assert.notNull(values, "'values' must not be null");
		return ifMatches(Arrays.stream(values).map((value) -> onKeyIgnoringCase(predicate, value)).toList());
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data key
	 * matches any of the given regex patterns (ignoring case).
	 * @param regexes the case insensitive regexes that the key can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifKeyMatches(String... regexes) {
		Assert.notNull(regexes, "'regexes' must not be null");
		return ifKeyMatches(Arrays.stream(regexes).map(this::caseInsensitivePattern).toArray(Pattern[]::new));
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data key
	 * matches any of the given patterns.
	 * @param patterns the patterns that the key can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifKeyMatches(Pattern... patterns) {
		Assert.notNull(patterns, "'patterns' must not be null");
		return ifKeyMatches(Arrays.stream(patterns).map(Pattern::asMatchPredicate).toList());
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data key
	 * matches any of the given predicates.
	 * @param predicates the predicates that the key can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifKeyMatches(List<Predicate<String>> predicates) {
		Assert.notNull(predicates, "'predicates' must not be null");
		return ifMatches(predicates.stream().map(this::onKey).toList());
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data key
	 * matches any of the given predicate.
	 * @param predicate the predicate that the key can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifKeyMatches(Predicate<String> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return ifMatches(onKey(predicate));
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data string
	 * value matches any of the given regex patterns (ignoring case).
	 * @param regexes the case insensitive regexes that the values string can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifValueStringMatches(String... regexes) {
		Assert.notNull(regexes, "'regexes' must not be null");
		return ifValueStringMatches(Arrays.stream(regexes).map(this::caseInsensitivePattern).toArray(Pattern[]::new));
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data string
	 * value matches any of the given patterns.
	 * @param patterns the patterns that the value string can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifValueStringMatches(Pattern... patterns) {
		Assert.notNull(patterns, "'patterns' must not be null");
		return ifValueStringMatches(Arrays.stream(patterns).map(Pattern::asMatchPredicate).toList());
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data string
	 * value matches any of the given predicates.
	 * @param predicates the predicates that the value string can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */

	default SanitizingFunction ifValueStringMatches(List<Predicate<String>> predicates) {
		Assert.notNull(predicates, "'predicates' must not be null");
		return ifMatches(predicates.stream().map(this::onValueString).toList());
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data value
	 * matches any of the given predicates.
	 * @param predicates the predicates that the value can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifValueMatches(List<Predicate<Object>> predicates) {
		Assert.notNull(predicates, "'predicates' must not be null");
		return ifMatches(predicates.stream().map(this::onValue).toList());
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data string
	 * value matches the given predicate.
	 * @param predicate the predicate that the value string can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */

	default SanitizingFunction ifValueStringMatches(Predicate<String> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return ifMatches(onValueString(predicate));
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data value
	 * matches the given predicate.
	 * @param predicate the predicate that the value can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifValueMatches(Predicate<Object> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return ifMatches((data) -> predicate.test(data.getValue()));
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data matches
	 * any of the given predicates.
	 * @param predicates the predicates that the data can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifMatches(List<Predicate<SanitizableData>> predicates) {
		Assert.notNull(predicates, "'predicates' must not be null");
		Predicate<SanitizableData> combined = null;
		for (Predicate<SanitizableData> predicate : predicates) {
			combined = (combined != null) ? combined.or(predicate) : predicate;
		}
		return ifMatches(combined);
	}

	/**
	 * Return a new function with a filter that <em>also</em> applies if the data matches
	 * the given predicate.
	 * @param predicate the predicate that the data can match
	 * @return a new sanitizing function with an updated {@link #filter()}
	 * @since 3.5.0
	 * @see #filter()
	 * @see #sanitizeValue()
	 */
	default SanitizingFunction ifMatches(Predicate<SanitizableData> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		Predicate<SanitizableData> filter = (filter() != null) ? filter().or(predicate) : predicate;
		return new SanitizingFunction() {

			@Override
			public Predicate<SanitizableData> filter() {
				return filter;
			}

			@Override
			public SanitizableData apply(SanitizableData data) {
				return SanitizingFunction.this.apply(data);
			}

		};
	}

	private Pattern caseInsensitivePattern(String regex) {
		Assert.notNull(regex, "'regex' must not be null");
		return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	private Predicate<SanitizableData> onKeyIgnoringCase(BiPredicate<String, String> predicate, String value) {
		Assert.notNull(predicate, "'predicate' must not be null");
		Assert.notNull(value, "'value' must not be null");
		String lowerCaseValue = value.toLowerCase(Locale.getDefault());
		return (data) -> nullSafeTest(data.getLowerCaseKey(),
				(lowerCaseKey) -> predicate.test(lowerCaseKey, lowerCaseValue));
	}

	private Predicate<SanitizableData> onKey(Predicate<String> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return (data) -> nullSafeTest(data.getKey(), predicate);
	}

	private Predicate<SanitizableData> onValue(Predicate<Object> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return (data) -> nullSafeTest(data.getValue(), predicate);
	}

	private Predicate<SanitizableData> onValueString(Predicate<String> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return (data) -> nullSafeTest((data.getValue() != null) ? data.getValue().toString() : null, predicate);
	}

	private <T> boolean nullSafeTest(T value, Predicate<T> predicate) {
		return value != null && predicate.test(value);
	}

	/**
	 * Factory method to return a {@link SanitizingFunction} that sanitizes the value.
	 * This method is often chained with one or more {@code if...} methods. For example:
	 * <pre class="code">
	 * return SanitizingFunction.sanitizeValue()
	 * 	.ifKeyContains("password", "secret")
	 * 	.ifValueStringMatches("^gh._[a-zA-Z0-9]{36}$");
	 * </pre>
	 * @return a {@link SanitizingFunction} that sanitizes values.
	 */
	static SanitizingFunction sanitizeValue() {
		return SanitizableData::withSanitizedValue;
	}

	/**
	 * Helper method that can be used working with a sanitizingFunction as a lambda. For
	 * example: <pre class="code">
	 * SanitizingFunction.of((data) -> data.withValue("----")).ifKeyContains("password");
	 * </pre>
	 * @param sanitizingFunction the sanitizing function lambda
	 * @return a {@link SanitizingFunction} for further method calls
	 * @since 3.5.0
	 */
	static SanitizingFunction of(SanitizingFunction sanitizingFunction) {
		Assert.notNull(sanitizingFunction, "'sanitizingFunction' must not be null");
		return sanitizingFunction;
	}

}
