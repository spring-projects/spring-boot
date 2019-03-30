/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.assertj.core.api.AssertProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link JsonContentAssert}. Some tests here are based on Spring Framework
 * tests for {@link JsonPathExpectationsHelper}.
 *
 * @author Phillip Webb
 */
public class JsonContentAssertTests {

	private static final String SOURCE = loadJson("source.json");

	private static final String LENIENT_SAME = loadJson("lenient-same.json");

	private static final String DIFFERENT = loadJson("different.json");

	private static final String TYPES = loadJson("types.json");

	private static final String SIMPSONS = loadJson("simpsons.json");

	private static JSONComparator COMPARATOR = new DefaultComparator(
			JSONCompareMode.LENIENT);

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void isEqualToWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME);
	}

	@Test
	public void isEqualToWhenNullActualShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(null)).isEqualTo(SOURCE));
	}

	@Test
	public void isEqualToWhenStringIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT));
	}

	@Test
	public void isEqualToWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo("lenient-same.json");
	}

	@Test
	public void isEqualToWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualTo("different.json"));
	}

	@Test
	public void isEqualToWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME.getBytes());
	}

	@Test
	public void isEqualToWhenBytesAreNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT.getBytes()));
	}

	@Test
	public void isEqualToWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(createFile(LENIENT_SAME));
	}

	@Test
	public void isEqualToWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualTo(createFile(DIFFERENT)));
	}

	@Test
	public void isEqualToWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isEqualToWhenInputStreamIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualTo(createInputStream(DIFFERENT)));
	}

	@Test
	public void isEqualToWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(createResource(LENIENT_SAME));
	}

	@Test
	public void isEqualToWhenResourceIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualTo(createResource(DIFFERENT)));
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME);
	}

	@Test
	public void isEqualToJsonWhenNullActualShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(null)).isEqualToJson(SOURCE));
	}

	@Test
	public void isEqualToJsonWhenStringIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT));
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json");
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson("different.json"));
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass());
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson("different.json", getClass()));
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test
	public void isEqualToJsonWhenBytesAreNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT.getBytes()));
	}

	@Test
	public void isEqualToJsonWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME));
	}

	@Test
	public void isEqualToJsonWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson(createFile(DIFFERENT)));
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson(createInputStream(DIFFERENT)));
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME));
	}

	@Test
	public void isEqualToJsonWhenResourceIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson(createResource(DIFFERENT)));
	}

	@Test
	public void isStrictlyEqualToJsonWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(SOURCE);
	}

	@Test
	public void isStrictlyEqualToJsonWhenStringIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isStrictlyEqualToJson(LENIENT_SAME));
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("source.json");
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isStrictlyEqualToJson("lenient-same.json"));
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourcePathAndClassIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("source.json", getClass());
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourcePathAndClassIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isStrictlyEqualToJson("lenient-same.json", getClass()));
	}

	@Test
	public void isStrictlyEqualToJsonWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(SOURCE.getBytes());
	}

	@Test
	public void isStrictlyEqualToJsonWhenBytesAreNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isStrictlyEqualToJson(LENIENT_SAME.getBytes()));
	}

	@Test
	public void isStrictlyEqualToJsonWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createFile(SOURCE));
	}

	@Test
	public void isStrictlyEqualToJsonWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isStrictlyEqualToJson(createFile(LENIENT_SAME)));
	}

	@Test
	public void isStrictlyEqualToJsonWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createInputStream(SOURCE));
	}

	@Test
	public void isStrictlyEqualToJsonWhenInputStreamIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isStrictlyEqualToJson(createInputStream(LENIENT_SAME)));
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createResource(SOURCE));
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourceIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isStrictlyEqualToJson(createResource(LENIENT_SAME)));
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME, JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenStringIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT,
						JSONCompareMode.LENIENT));
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json",
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson("different.json", JSONCompareMode.LENIENT));
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson("different.json",
						getClass(), JSONCompareMode.LENIENT));
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenBytesAreNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson(DIFFERENT.getBytes(), JSONCompareMode.LENIENT));
	}

	@Test
	public void isEqualToJsonWhenFileIsMatchingAndLenientShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenFileIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson(createFile(DIFFERENT), JSONCompareMode.LENIENT));
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(
						createInputStream(DIFFERENT), JSONCompareMode.LENIENT));
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourceIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT),
						JSONCompareMode.LENIENT));
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME, COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenStringIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT, COMPARATOR));
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson("different.json", COMPARATOR));
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassAreMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass(),
				COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassAreNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson("different.json", getClass(), COMPARATOR));
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes(), COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenBytesAreNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson(DIFFERENT.getBytes(), COMPARATOR));
	}

	@Test
	public void isEqualToJsonWhenFileIsMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME), COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenFileIsNotMatchingAndComparatorShouldFail()
			throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson(createFile(DIFFERENT), COMPARATOR));
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME),
				COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson(createInputStream(DIFFERENT), COMPARATOR));
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME),
				COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourceIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isEqualToJson(createResource(DIFFERENT), COMPARATOR));
	}

	@Test
	public void isNotEqualToWhenStringIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToWhenNullActualShouldPass() {
		assertThat(forJson(null)).isNotEqualTo(SOURCE);
	}

	@Test
	public void isNotEqualToWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT);
	}

	@Test
	public void isNotEqualToWhenResourcePathIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualTo("lenient-same.json"));
	}

	@Test
	public void isNotEqualToWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo("different.json");
	}

	@Test
	public void isNotEqualToWhenBytesAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME.getBytes()));
	}

	@Test
	public void isNotEqualToWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT.getBytes());
	}

	@Test
	public void isNotEqualToWhenFileIsMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualTo(createFile(LENIENT_SAME)));
	}

	@Test
	public void isNotEqualToWhenFileIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(createFile(DIFFERENT));
	}

	@Test
	public void isNotEqualToWhenInputStreamIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualTo(createInputStream(LENIENT_SAME)));
	}

	@Test
	public void isNotEqualToWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createInputStream(DIFFERENT));
	}

	@Test
	public void isNotEqualToWhenResourceIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualTo(createResource(LENIENT_SAME)));
	}

	@Test
	public void isNotEqualToWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createResource(DIFFERENT));
	}

	@Test
	public void isNotEqualToJsonWhenStringIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToJsonWhenNullActualShouldPass() {
		assertThat(forJson(null)).isNotEqualToJson(SOURCE);
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json"));
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json");
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson("lenient-same.json", getClass()));
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass());
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(LENIENT_SAME.getBytes()));
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes());
	}

	@Test
	public void isNotEqualToJsonWhenFileIsMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(createFile(LENIENT_SAME)));
	}

	@Test
	public void isNotEqualToJsonWhenFileIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT));
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(createInputStream(LENIENT_SAME)));
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT));
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(createResource(LENIENT_SAME)));
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenStringIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(SOURCE));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(LENIENT_SAME);
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourcePathIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotStrictlyEqualToJson("source.json"));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("lenient-same.json");
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourcePathAndClassAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotStrictlyEqualToJson("source.json", getClass()));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourcePathAndClassAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("lenient-same.json",
				getClass());
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenBytesAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotStrictlyEqualToJson(SOURCE.getBytes()));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenFileIsMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotStrictlyEqualToJson(createFile(SOURCE)));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenFileIsNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createFile(LENIENT_SAME));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenInputStreamIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotStrictlyEqualToJson(createInputStream(SOURCE)));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE))
				.isNotStrictlyEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourceIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotStrictlyEqualToJson(createResource(SOURCE)));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE))
				.isNotStrictlyEqualToJson(createResource(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToJsonWhenStringIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(LENIENT_SAME, JSONCompareMode.LENIENT));
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT, JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson("lenient-same.json", JSONCompareMode.LENIENT));
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json",
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json",
						getClass(), JSONCompareMode.LENIENT));
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(
						LENIENT_SAME.getBytes(), JSONCompareMode.LENIENT));
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenFileIsMatchingAndLenientShouldFail()
			throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(
						createFile(LENIENT_SAME), JSONCompareMode.LENIENT));
	}

	@Test
	public void isNotEqualToJsonWhenFileIsNotMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(
						createInputStream(LENIENT_SAME), JSONCompareMode.LENIENT));
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(
						createResource(LENIENT_SAME), JSONCompareMode.LENIENT));
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenStringIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(LENIENT_SAME, COMPARATOR));
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT, COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson("lenient-same.json", COMPARATOR));
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson("lenient-same.json", getClass(), COMPARATOR));
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass(),
				COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(LENIENT_SAME.getBytes(), COMPARATOR));
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes(), COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenFileIsMatchingAndComparatorShouldFail()
			throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(createFile(LENIENT_SAME), COMPARATOR));
	}

	@Test
	public void isNotEqualToJsonWhenFileIsNotMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT), COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(createInputStream(LENIENT_SAME), COMPARATOR));
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT),
				COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE))
						.isNotEqualToJson(createResource(LENIENT_SAME), COMPARATOR));
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT),
				COMPARATOR);
	}

	@Test
	public void hasJsonPathValue() {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.str");
	}

	@Test
	public void hasJsonPathValueForAnEmptyArray() {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.emptyArray");
	}

	@Test
	public void hasJsonPathValueForAnEmptyMap() {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.emptyMap");
	}

	@Test
	public void hasJsonPathValueForIndefinitePathWithResults() {
		assertThat(forJson(SIMPSONS))
				.hasJsonPathValue("$.familyMembers[?(@.name == 'Bart')]");
	}

	@Test
	public void hasJsonPathValueForIndefinitePathWithEmptyResults() {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(
						() -> assertThat(forJson(SIMPSONS)).hasJsonPathValue(expression))
				.withMessageContaining("No value at JSON path \"" + expression + "\"");
	}

	@Test
	public void doesNotHaveJsonPathValue() {
		assertThat(forJson(TYPES)).doesNotHaveJsonPathValue("$.bogus");
	}

	@Test
	public void doesNotHaveJsonPathValueForAnEmptyArray() {
		String expression = "$.emptyArray";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).doesNotHaveJsonPathValue(expression))
				.withMessageContaining("Expected no value at JSON path \"" + expression
						+ "\" but found: []");
	}

	@Test
	public void doesNotHaveJsonPathValueForAnEmptyMap() {
		String expression = "$.emptyMap";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).doesNotHaveJsonPathValue(expression))
				.withMessageContaining("Expected no value at JSON path \"" + expression
						+ "\" but found: {}");
	}

	@Test
	public void doesNotHaveJsonPathValueForIndefinitePathWithResults() {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SIMPSONS)).doesNotHaveJsonPathValue(expression))
				.withMessageContaining("Expected no value at JSON path \"" + expression
						+ "\" but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	public void doesNotHaveJsonPathValueForIndefinitePathWithEmptyResults() {
		assertThat(forJson(SIMPSONS))
				.doesNotHaveJsonPathValue("$.familyMembers[?(@.name == 'Dilbert')]");
	}

	@Test
	public void hasEmptyJsonPathValueForAnEmptyString() {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyString");
	}

	@Test
	public void hasEmptyJsonPathValueForAnEmptyArray() {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyArray");
	}

	@Test
	public void hasEmptyJsonPathValueForAnEmptyMap() {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyMap");
	}

	@Test
	public void hasEmptyJsonPathValueForIndefinitePathWithEmptyResults() {
		assertThat(forJson(SIMPSONS))
				.hasEmptyJsonPathValue("$.familyMembers[?(@.name == 'Dilbert')]");
	}

	@Test
	public void hasEmptyJsonPathValueForIndefinitePathWithResults() {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SIMPSONS)).hasEmptyJsonPathValue(expression))
				.withMessageContaining("Expected an empty value at JSON path \""
						+ expression + "\" but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	public void hasEmptyJsonPathValueForWhitespace() {
		String expression = "$.whitespace";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).hasEmptyJsonPathValue(expression))
				.withMessageContaining("Expected an empty value at JSON path \""
						+ expression + "\" but found: '    '");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForString() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.str");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForNumber() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.num");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForBoolean() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.bool");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForArray() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.arr");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForMap() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.colorMap");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForIndefinitePathWithResults() {
		assertThat(forJson(SIMPSONS))
				.doesNotHaveEmptyJsonPathValue("$.familyMembers[?(@.name == 'Bart')]");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForIndefinitePathWithEmptyResults() {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SIMPSONS))
						.doesNotHaveEmptyJsonPathValue(expression))
				.withMessageContaining("Expected a non-empty value at JSON path \""
						+ expression + "\" but found: []");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForAnEmptyString() {
		String expression = "$.emptyString";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES))
						.doesNotHaveEmptyJsonPathValue(expression))
				.withMessageContaining("Expected a non-empty value at JSON path \""
						+ expression + "\" but found: ''");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForForAnEmptyArray() {
		String expression = "$.emptyArray";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES))
						.doesNotHaveEmptyJsonPathValue(expression))
				.withMessageContaining("Expected a non-empty value at JSON path \""
						+ expression + "\" but found: []");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForAnEmptyMap() {
		String expression = "$.emptyMap";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES))
						.doesNotHaveEmptyJsonPathValue(expression))
				.withMessageContaining("Expected a non-empty value at JSON path \""
						+ expression + "\" but found: {}");
	}

	@Test
	public void hasJsonPathStringValue() {
		assertThat(forJson(TYPES)).hasJsonPathStringValue("$.str");
	}

	@Test
	public void hasJsonPathStringValueForAnEmptyString() {
		assertThat(forJson(TYPES)).hasJsonPathStringValue("$.emptyString");
	}

	@Test
	public void hasJsonPathStringValueForNonString() {
		String expression = "$.bool";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).hasJsonPathStringValue(expression))
				.withMessageContaining("Expected a string at JSON path \"" + expression
						+ "\" but found: true");
	}

	@Test
	public void hasJsonPathNumberValue() {
		assertThat(forJson(TYPES)).hasJsonPathNumberValue("$.num");
	}

	@Test
	public void hasJsonPathNumberValueForNonNumber() {
		String expression = "$.bool";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).hasJsonPathNumberValue(expression))
				.withMessageContaining("Expected a number at JSON path \"" + expression
						+ "\" but found: true");
	}

	@Test
	public void hasJsonPathBooleanValue() {
		assertThat(forJson(TYPES)).hasJsonPathBooleanValue("$.bool");
	}

	@Test
	public void hasJsonPathBooleanValueForNonBoolean() {
		String expression = "$.num";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).hasJsonPathBooleanValue(expression))
				.withMessageContaining("Expected a boolean at JSON path \"" + expression
						+ "\" but found: 5");
	}

	@Test
	public void hasJsonPathArrayValue() {
		assertThat(forJson(TYPES)).hasJsonPathArrayValue("$.arr");
	}

	@Test
	public void hasJsonPathArrayValueForAnEmptyArray() {
		assertThat(forJson(TYPES)).hasJsonPathArrayValue("$.emptyArray");
	}

	@Test
	public void hasJsonPathArrayValueForNonArray() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).hasJsonPathArrayValue(expression))
				.withMessageContaining("Expected an array at JSON path \"" + expression
						+ "\" but found: 'foo'");
	}

	@Test
	public void hasJsonPathMapValue() {
		assertThat(forJson(TYPES)).hasJsonPathMapValue("$.colorMap");
	}

	@Test
	public void hasJsonPathMapValueForAnEmptyMap() {
		assertThat(forJson(TYPES)).hasJsonPathMapValue("$.emptyMap");
	}

	@Test
	public void hasJsonPathMapValueForNonMap() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(
						() -> assertThat(forJson(TYPES)).hasJsonPathMapValue(expression))
				.withMessageContaining("Expected a map at JSON path \"" + expression
						+ "\" but found: 'foo'");
	}

	@Test
	public void extractingJsonPathValue() {
		assertThat(forJson(TYPES)).extractingJsonPathValue("@.str").isEqualTo("foo");
	}

	@Test
	public void extractingJsonPathValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathStringValue() {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.str")
				.isEqualTo("foo");
	}

	@Test
	public void extractingJsonPathStringValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathStringValueForEmptyString() {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.emptyString")
				.isEmpty();
	}

	@Test
	public void extractingJsonPathStringValueForWrongType() {
		String expression = "$.num";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES))
						.extractingJsonPathStringValue(expression))
				.withMessageContaining("Expected a string at JSON path \"" + expression
						+ "\" but found: 5");
	}

	@Test
	public void extractingJsonPathNumberValue() {
		assertThat(forJson(TYPES)).extractingJsonPathNumberValue("@.num").isEqualTo(5);
	}

	@Test
	public void extractingJsonPathNumberValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathNumberValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathNumberValueForWrongType() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES))
						.extractingJsonPathNumberValue(expression))
				.withMessageContaining("Expected a number at JSON path \"" + expression
						+ "\" but found: 'foo'");
	}

	@Test
	public void extractingJsonPathBooleanValue() {
		assertThat(forJson(TYPES)).extractingJsonPathBooleanValue("@.bool").isTrue();
	}

	@Test
	public void extractingJsonPathBooleanValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathBooleanValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathBooleanValueForWrongType() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES))
						.extractingJsonPathBooleanValue(expression))
				.withMessageContaining("Expected a boolean at JSON path \"" + expression
						+ "\" but found: 'foo'");
	}

	@Test
	public void extractingJsonPathArrayValue() {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.arr")
				.containsExactly(42);
	}

	@Test
	public void extractingJsonPathArrayValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathArrayValueForEmpty() {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.emptyArray").isEmpty();
	}

	@Test
	public void extractingJsonPathArrayValueForWrongType() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).extractingJsonPathArrayValue(expression))
				.withMessageContaining("Expected an array at JSON path \"" + expression
						+ "\" but found: 'foo'");
	}

	@Test
	public void extractingJsonPathMapValue() {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.colorMap")
				.contains(entry("red", "rojo"));
	}

	@Test
	public void extractingJsonPathMapValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathMapValueForEmpty() {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.emptyMap").isEmpty();
	}

	@Test
	public void extractingJsonPathMapValueForWrongType() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(TYPES)).extractingJsonPathMapValue(expression))
				.withMessageContaining("Expected a map at JSON path \"" + expression
						+ "\" but found: 'foo'");
	}

	@Test
	public void isNullWhenActualIsNullShouldPass() {
		assertThat(forJson(null)).isNull();
	}

	private File createFile(String content) throws IOException {
		File file = this.temp.newFile("example.json");
		FileCopyUtils.copy(content.getBytes(), file);
		return file;
	}

	private InputStream createInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes());
	}

	private Resource createResource(String content) {
		return new ByteArrayResource(content.getBytes());
	}

	private static String loadJson(String path) {
		try {
			ClassPathResource resource = new ClassPathResource(path,
					JsonContentAssertTests.class);
			return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

	}

	private AssertProvider<JsonContentAssert> forJson(String json) {
		return () -> new JsonContentAssert(JsonContentAssertTests.class, json);
	}

}
