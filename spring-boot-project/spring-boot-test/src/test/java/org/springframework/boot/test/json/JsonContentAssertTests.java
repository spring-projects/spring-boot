/*
 * Copyright 2012-2019 the original author or authors.
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
import java.nio.file.Path;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
class JsonContentAssertTests {

	private static final String SOURCE = loadJson("source.json");

	private static final String LENIENT_SAME = loadJson("lenient-same.json");

	private static final String DIFFERENT = loadJson("different.json");

	private static final String TYPES = loadJson("types.json");

	private static final String SIMPSONS = loadJson("simpsons.json");

	private static final String NULLS = loadJson("nulls.json");

	private static JSONComparator COMPARATOR = new DefaultComparator(JSONCompareMode.LENIENT);

	@TempDir
	public Path tempDir;

	private File temp;

	@BeforeEach
	void setup() {
		this.temp = new File(this.tempDir.toFile(), "file.json");
	}

	@Test
	void isEqualToWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME);
	}

	@Test
	void isEqualToWhenNullActualShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forJson(null)).isEqualTo(SOURCE));
	}

	@Test
	void isEqualToWhenStringIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT));
	}

	@Test
	void isEqualToWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo("lenient-same.json");
	}

	@Test
	void isEqualToWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo("different.json"));
	}

	@Test
	void isEqualToWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME.getBytes());
	}

	@Test
	void isEqualToWhenBytesAreNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT.getBytes()));
	}

	@Test
	void isEqualToWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(createFile(LENIENT_SAME));
	}

	@Test
	void isEqualToWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(createFile(DIFFERENT)));
	}

	@Test
	void isEqualToWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(createInputStream(LENIENT_SAME));
	}

	@Test
	void isEqualToWhenInputStreamIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(createInputStream(DIFFERENT)));
	}

	@Test
	void isEqualToWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(createResource(LENIENT_SAME));
	}

	@Test
	void isEqualToWhenResourceIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(createResource(DIFFERENT)));
	}

	@Test
	void isEqualToJsonWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME);
	}

	@Test
	void isEqualToJsonWhenNullActualShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(null)).isEqualToJson(SOURCE));
	}

	@Test
	void isEqualToJsonWhenStringIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT));
	}

	@Test
	void isEqualToJsonWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json");
	}

	@Test
	void isEqualToJsonWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson("different.json"));
	}

	@Test
	void isEqualToJsonWhenResourcePathAndClassIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass());
	}

	@Test
	void isEqualToJsonWhenResourcePathAndClassIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass()));
	}

	@Test
	void isEqualToJsonWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test
	void isEqualToJsonWhenBytesAreNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT.getBytes()));
	}

	@Test
	void isEqualToJsonWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME));
	}

	@Test
	void isEqualToJsonWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(createFile(DIFFERENT)));
	}

	@Test
	void isEqualToJsonWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test
	void isEqualToJsonWhenInputStreamIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT)));
	}

	@Test
	void isEqualToJsonWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME));
	}

	@Test
	void isEqualToJsonWhenResourceIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT)));
	}

	@Test
	void isStrictlyEqualToJsonWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(SOURCE);
	}

	@Test
	void isStrictlyEqualToJsonWhenStringIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualToJson(LENIENT_SAME));
	}

	@Test
	void isStrictlyEqualToJsonWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("source.json");
	}

	@Test
	void isStrictlyEqualToJsonWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualToJson("lenient-same.json"));
	}

	@Test
	void isStrictlyEqualToJsonWhenResourcePathAndClassIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("source.json", getClass());
	}

	@Test
	void isStrictlyEqualToJsonWhenResourcePathAndClassIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualToJson("lenient-same.json", getClass()));
	}

	@Test
	void isStrictlyEqualToJsonWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(SOURCE.getBytes());
	}

	@Test
	void isStrictlyEqualToJsonWhenBytesAreNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualToJson(LENIENT_SAME.getBytes()));
	}

	@Test
	void isStrictlyEqualToJsonWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createFile(SOURCE));
	}

	@Test
	void isStrictlyEqualToJsonWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createFile(LENIENT_SAME)));
	}

	@Test
	void isStrictlyEqualToJsonWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createInputStream(SOURCE));
	}

	@Test
	void isStrictlyEqualToJsonWhenInputStreamIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createInputStream(LENIENT_SAME)));
	}

	@Test
	void isStrictlyEqualToJsonWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createResource(SOURCE));
	}

	@Test
	void isStrictlyEqualToJsonWhenResourceIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createResource(LENIENT_SAME)));
	}

	@Test
	void isEqualToJsonWhenStringIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME, JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToJsonWhenStringIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT, JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToJsonWhenResourcePathIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToJsonWhenResourcePathIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson("different.json", JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToJsonWhenResourcePathAndClassIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass(), JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToJsonWhenResourcePathAndClassIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass(), JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToJsonWhenBytesAreMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes(), JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToJsonWhenBytesAreNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT.getBytes(), JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToJsonWhenFileIsMatchingAndLenientShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME), JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToJsonWhenFileIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson(createFile(DIFFERENT), JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToJsonWhenInputStreamIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME), JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToJsonWhenInputStreamIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT), JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToJsonWhenResourceIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME), JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToJsonWhenResourceIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT), JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToJsonWhenStringIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME, COMPARATOR);
	}

	@Test
	void isEqualToJsonWhenStringIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT, COMPARATOR));
	}

	@Test
	void isEqualToJsonWhenResourcePathIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", COMPARATOR);
	}

	@Test
	void isEqualToJsonWhenResourcePathIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson("different.json", COMPARATOR));
	}

	@Test
	void isEqualToJsonWhenResourcePathAndClassAreMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass(), COMPARATOR);
	}

	@Test
	void isEqualToJsonWhenResourcePathAndClassAreNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass(), COMPARATOR));
	}

	@Test
	void isEqualToJsonWhenBytesAreMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes(), COMPARATOR);
	}

	@Test
	void isEqualToJsonWhenBytesAreNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT.getBytes(), COMPARATOR));
	}

	@Test
	void isEqualToJsonWhenFileIsMatchingAndComparatorShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME), COMPARATOR);
	}

	@Test
	void isEqualToJsonWhenFileIsNotMatchingAndComparatorShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(createFile(DIFFERENT), COMPARATOR));
	}

	@Test
	void isEqualToJsonWhenInputStreamIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME), COMPARATOR);
	}

	@Test
	void isEqualToJsonWhenInputStreamIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT), COMPARATOR));
	}

	@Test
	void isEqualToJsonWhenResourceIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME), COMPARATOR);
	}

	@Test
	void isEqualToJsonWhenResourceIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT), COMPARATOR));
	}

	@Test
	void isNotEqualToWhenStringIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME));
	}

	@Test
	void isNotEqualToWhenNullActualShouldPass() {
		assertThat(forJson(null)).isNotEqualTo(SOURCE);
	}

	@Test
	void isNotEqualToWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT);
	}

	@Test
	void isNotEqualToWhenResourcePathIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo("lenient-same.json"));
	}

	@Test
	void isNotEqualToWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo("different.json");
	}

	@Test
	void isNotEqualToWhenBytesAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME.getBytes()));
	}

	@Test
	void isNotEqualToWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT.getBytes());
	}

	@Test
	void isNotEqualToWhenFileIsMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(createFile(LENIENT_SAME)));
	}

	@Test
	void isNotEqualToWhenFileIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(createFile(DIFFERENT));
	}

	@Test
	void isNotEqualToWhenInputStreamIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(createInputStream(LENIENT_SAME)));
	}

	@Test
	void isNotEqualToWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createInputStream(DIFFERENT));
	}

	@Test
	void isNotEqualToWhenResourceIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(createResource(LENIENT_SAME)));
	}

	@Test
	void isNotEqualToWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createResource(DIFFERENT));
	}

	@Test
	void isNotEqualToJsonWhenStringIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME));
	}

	@Test
	void isNotEqualToJsonWhenNullActualShouldPass() {
		assertThat(forJson(null)).isNotEqualToJson(SOURCE);
	}

	@Test
	void isNotEqualToJsonWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT);
	}

	@Test
	void isNotEqualToJsonWhenResourcePathIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json"));
	}

	@Test
	void isNotEqualToJsonWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json");
	}

	@Test
	void isNotEqualToJsonWhenResourcePathAndClassAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", getClass()));
	}

	@Test
	void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass());
	}

	@Test
	void isNotEqualToJsonWhenBytesAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes()));
	}

	@Test
	void isNotEqualToJsonWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes());
	}

	@Test
	void isNotEqualToJsonWhenFileIsMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(LENIENT_SAME)));
	}

	@Test
	void isNotEqualToJsonWhenFileIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT));
	}

	@Test
	void isNotEqualToJsonWhenInputStreamIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(LENIENT_SAME)));
	}

	@Test
	void isNotEqualToJsonWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT));
	}

	@Test
	void isNotEqualToJsonWhenResourceIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(LENIENT_SAME)));
	}

	@Test
	void isNotEqualToJsonWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenStringIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(SOURCE));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(LENIENT_SAME);
	}

	@Test
	void isNotStrictlyEqualToJsonWhenResourcePathIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("source.json"));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("lenient-same.json");
	}

	@Test
	void isNotStrictlyEqualToJsonWhenResourcePathAndClassAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("source.json", getClass()));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenResourcePathAndClassAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("lenient-same.json", getClass());
	}

	@Test
	void isNotStrictlyEqualToJsonWhenBytesAreMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(SOURCE.getBytes()));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test
	void isNotStrictlyEqualToJsonWhenFileIsMatchingShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createFile(SOURCE)));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenFileIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createFile(LENIENT_SAME));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenInputStreamIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createInputStream(SOURCE)));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenResourceIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createResource(SOURCE)));
	}

	@Test
	void isNotStrictlyEqualToJsonWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createResource(LENIENT_SAME));
	}

	@Test
	void isNotEqualToJsonWhenStringIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME, JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToJsonWhenStringIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT, JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToJsonWhenResourcePathIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToJsonWhenResourcePathIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToJsonWhenResourcePathAndClassAreMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forJson(SOURCE))
				.isNotEqualToJson("lenient-same.json", getClass(), JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass(), JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToJsonWhenBytesAreMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes(), JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToJsonWhenBytesAreNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes(), JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToJsonWhenFileIsMatchingAndLenientShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(LENIENT_SAME), JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToJsonWhenFileIsNotMatchingAndLenientShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT), JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToJsonWhenInputStreamIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forJson(SOURCE))
				.isNotEqualToJson(createInputStream(LENIENT_SAME), JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToJsonWhenInputStreamIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT), JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToJsonWhenResourceIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forJson(SOURCE))
				.isNotEqualToJson(createResource(LENIENT_SAME), JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToJsonWhenResourceIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT), JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToJsonWhenStringIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME, COMPARATOR));
	}

	@Test
	void isNotEqualToJsonWhenStringIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT, COMPARATOR);
	}

	@Test
	void isNotEqualToJsonWhenResourcePathIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", COMPARATOR));
	}

	@Test
	void isNotEqualToJsonWhenResourcePathIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", COMPARATOR);
	}

	@Test
	void isNotEqualToJsonWhenResourcePathAndClassAreMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", getClass(), COMPARATOR));
	}

	@Test
	void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass(), COMPARATOR);
	}

	@Test
	void isNotEqualToJsonWhenBytesAreMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes(), COMPARATOR));
	}

	@Test
	void isNotEqualToJsonWhenBytesAreNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes(), COMPARATOR);
	}

	@Test
	void isNotEqualToJsonWhenFileIsMatchingAndComparatorShouldFail() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(LENIENT_SAME), COMPARATOR));
	}

	@Test
	void isNotEqualToJsonWhenFileIsNotMatchingAndComparatorShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT), COMPARATOR);
	}

	@Test
	void isNotEqualToJsonWhenInputStreamIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(LENIENT_SAME), COMPARATOR));
	}

	@Test
	void isNotEqualToJsonWhenInputStreamIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT), COMPARATOR);
	}

	@Test
	void isNotEqualToJsonWhenResourceIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(LENIENT_SAME), COMPARATOR));
	}

	@Test
	void isNotEqualToJsonWhenResourceIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT), COMPARATOR);
	}

	@Test
	void hasJsonPathForPresentAndNotNull() {
		assertThat(forJson(NULLS)).hasJsonPath("valuename");
	}

	@Test
	void hasJsonPathForPresentAndNull() {
		assertThat(forJson(NULLS)).hasJsonPath("nullname");
	}

	@Test
	void hasJsonPathForNotPresent() {
		String expression = "missing";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(NULLS)).hasJsonPath(expression))
				.withMessageContaining("No JSON path \"" + expression + "\" found");
	}

	@Test
	void hasJsonPathValue() {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.str");
	}

	@Test
	void hasJsonPathValueForAnEmptyArray() {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.emptyArray");
	}

	@Test
	void hasJsonPathValueForAnEmptyMap() {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.emptyMap");
	}

	@Test
	void hasJsonPathValueForANullValue() {
		String expression = "nullname";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(NULLS)).hasJsonPathValue(expression))
				.withMessageContaining("No value at JSON path \"" + expression + "\"");
	}

	@Test
	void hasJsonPathValueForMissingValue() {
		String expression = "missing";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(NULLS)).hasJsonPathValue(expression))
				.withMessageContaining("No value at JSON path \"" + expression + "\"");
	}

	@Test
	void hasJsonPathValueForIndefinitePathWithResults() {
		assertThat(forJson(SIMPSONS)).hasJsonPathValue("$.familyMembers[?(@.name == 'Bart')]");
	}

	@Test
	void hasJsonPathValueForIndefinitePathWithEmptyResults() {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SIMPSONS)).hasJsonPathValue(expression))
				.withMessageContaining("No value at JSON path \"" + expression + "\"");
	}

	@Test
	void doesNotHaveJsonPathForMissing() {
		assertThat(forJson(NULLS)).doesNotHaveJsonPath("missing");
	}

	@Test
	void doesNotHaveJsonPathForNull() {
		String expression = "nullname";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(NULLS)).doesNotHaveJsonPath(expression))
				.withMessageContaining("Expecting no JSON path \"" + expression + "\"");
	}

	@Test
	void doesNotHaveJsonPathForPresent() {
		String expression = "valuename";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(NULLS)).doesNotHaveJsonPath(expression))
				.withMessageContaining("Expecting no JSON path \"" + expression + "\"");
	}

	@Test
	void doesNotHaveJsonPathValue() {
		assertThat(forJson(TYPES)).doesNotHaveJsonPathValue("$.bogus");
	}

	@Test
	void doesNotHaveJsonPathValueForAnEmptyArray() {
		String expression = "$.emptyArray";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).doesNotHaveJsonPathValue(expression))
				.withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void doesNotHaveJsonPathValueForAnEmptyMap() {
		String expression = "$.emptyMap";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).doesNotHaveJsonPathValue(expression))
				.withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: {}");
	}

	@Test
	void doesNotHaveJsonPathValueForIndefinitePathWithResults() {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SIMPSONS)).doesNotHaveJsonPathValue(expression))
				.withMessageContaining(
						"Expected no value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	void doesNotHaveJsonPathValueForIndefinitePathWithEmptyResults() {
		assertThat(forJson(SIMPSONS)).doesNotHaveJsonPathValue("$.familyMembers[?(@.name == 'Dilbert')]");
	}

	@Test
	void doesNotHaveJsonPathValueForNull() {
		assertThat(forJson(NULLS)).doesNotHaveJsonPathValue("nullname");
	}

	@Test
	void hasEmptyJsonPathValueForAnEmptyString() {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyString");
	}

	@Test
	void hasEmptyJsonPathValueForAnEmptyArray() {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyArray");
	}

	@Test
	void hasEmptyJsonPathValueForAnEmptyMap() {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyMap");
	}

	@Test
	void hasEmptyJsonPathValueForIndefinitePathWithEmptyResults() {
		assertThat(forJson(SIMPSONS)).hasEmptyJsonPathValue("$.familyMembers[?(@.name == 'Dilbert')]");
	}

	@Test
	void hasEmptyJsonPathValueForIndefinitePathWithResults() {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SIMPSONS)).hasEmptyJsonPathValue(expression))
				.withMessageContaining(
						"Expected an empty value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	void hasEmptyJsonPathValueForWhitespace() {
		String expression = "$.whitespace";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).hasEmptyJsonPathValue(expression))
				.withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: '    '");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForString() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.str");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForNumber() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.num");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForBoolean() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.bool");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForArray() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.arr");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForMap() {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.colorMap");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForIndefinitePathWithResults() {
		assertThat(forJson(SIMPSONS)).doesNotHaveEmptyJsonPathValue("$.familyMembers[?(@.name == 'Bart')]");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForIndefinitePathWithEmptyResults() {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SIMPSONS)).doesNotHaveEmptyJsonPathValue(expression))
				.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForAnEmptyString() {
		String expression = "$.emptyString";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression))
				.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: ''");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForForAnEmptyArray() {
		String expression = "$.emptyArray";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression))
				.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void doesNotHaveEmptyJsonPathValueForAnEmptyMap() {
		String expression = "$.emptyMap";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression))
				.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: {}");
	}

	@Test
	void hasJsonPathStringValue() {
		assertThat(forJson(TYPES)).hasJsonPathStringValue("$.str");
	}

	@Test
	void hasJsonPathStringValueForAnEmptyString() {
		assertThat(forJson(TYPES)).hasJsonPathStringValue("$.emptyString");
	}

	@Test
	void hasJsonPathStringValueForNonString() {
		String expression = "$.bool";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).hasJsonPathStringValue(expression))
				.withMessageContaining("Expected a string at JSON path \"" + expression + "\" but found: true");
	}

	@Test
	void hasJsonPathNumberValue() {
		assertThat(forJson(TYPES)).hasJsonPathNumberValue("$.num");
	}

	@Test
	void hasJsonPathNumberValueForNonNumber() {
		String expression = "$.bool";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).hasJsonPathNumberValue(expression))
				.withMessageContaining("Expected a number at JSON path \"" + expression + "\" but found: true");
	}

	@Test
	void hasJsonPathBooleanValue() {
		assertThat(forJson(TYPES)).hasJsonPathBooleanValue("$.bool");
	}

	@Test
	void hasJsonPathBooleanValueForNonBoolean() {
		String expression = "$.num";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).hasJsonPathBooleanValue(expression))
				.withMessageContaining("Expected a boolean at JSON path \"" + expression + "\" but found: 5");
	}

	@Test
	void hasJsonPathArrayValue() {
		assertThat(forJson(TYPES)).hasJsonPathArrayValue("$.arr");
	}

	@Test
	void hasJsonPathArrayValueForAnEmptyArray() {
		assertThat(forJson(TYPES)).hasJsonPathArrayValue("$.emptyArray");
	}

	@Test
	void hasJsonPathArrayValueForNonArray() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).hasJsonPathArrayValue(expression))
				.withMessageContaining("Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
	}

	@Test
	void hasJsonPathMapValue() {
		assertThat(forJson(TYPES)).hasJsonPathMapValue("$.colorMap");
	}

	@Test
	void hasJsonPathMapValueForAnEmptyMap() {
		assertThat(forJson(TYPES)).hasJsonPathMapValue("$.emptyMap");
	}

	@Test
	void hasJsonPathMapValueForNonMap() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).hasJsonPathMapValue(expression))
				.withMessageContaining("Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
	}

	@Test
	void extractingJsonPathValue() {
		assertThat(forJson(TYPES)).extractingJsonPathValue("@.str").isEqualTo("foo");
	}

	@Test
	void extractingJsonPathValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathValue("@.bogus").isNull();
	}

	@Test
	void extractingJsonPathStringValue() {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.str").isEqualTo("foo");
	}

	@Test
	void extractingJsonPathStringValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.bogus").isNull();
	}

	@Test
	void extractingJsonPathStringValueForEmptyString() {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.emptyString").isEmpty();
	}

	@Test
	void extractingJsonPathStringValueForWrongType() {
		String expression = "$.num";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).extractingJsonPathStringValue(expression))
				.withMessageContaining("Expected a string at JSON path \"" + expression + "\" but found: 5");
	}

	@Test
	void extractingJsonPathNumberValue() {
		assertThat(forJson(TYPES)).extractingJsonPathNumberValue("@.num").isEqualTo(5);
	}

	@Test
	void extractingJsonPathNumberValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathNumberValue("@.bogus").isNull();
	}

	@Test
	void extractingJsonPathNumberValueForWrongType() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).extractingJsonPathNumberValue(expression))
				.withMessageContaining("Expected a number at JSON path \"" + expression + "\" but found: 'foo'");
	}

	@Test
	void extractingJsonPathBooleanValue() {
		assertThat(forJson(TYPES)).extractingJsonPathBooleanValue("@.bool").isTrue();
	}

	@Test
	void extractingJsonPathBooleanValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathBooleanValue("@.bogus").isNull();
	}

	@Test
	void extractingJsonPathBooleanValueForWrongType() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).extractingJsonPathBooleanValue(expression))
				.withMessageContaining("Expected a boolean at JSON path \"" + expression + "\" but found: 'foo'");
	}

	@Test
	void extractingJsonPathArrayValue() {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.arr").containsExactly(42);
	}

	@Test
	void extractingJsonPathArrayValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.bogus").isNull();
	}

	@Test
	void extractingJsonPathArrayValueForEmpty() {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.emptyArray").isEmpty();
	}

	@Test
	void extractingJsonPathArrayValueForWrongType() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).extractingJsonPathArrayValue(expression))
				.withMessageContaining("Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
	}

	@Test
	void extractingJsonPathMapValue() {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.colorMap").contains(entry("red", "rojo"));
	}

	@Test
	void extractingJsonPathMapValueForMissing() {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.bogus").isNull();
	}

	@Test
	void extractingJsonPathMapValueForEmpty() {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.emptyMap").isEmpty();
	}

	@Test
	void extractingJsonPathMapValueForWrongType() {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(TYPES)).extractingJsonPathMapValue(expression))
				.withMessageContaining("Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
	}

	@Test
	void isNullWhenActualIsNullShouldPass() {
		assertThat(forJson(null)).isNull();
	}

	private File createFile(String content) throws IOException {
		File file = this.temp;
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
			ClassPathResource resource = new ClassPathResource(path, JsonContentAssertTests.class);
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
