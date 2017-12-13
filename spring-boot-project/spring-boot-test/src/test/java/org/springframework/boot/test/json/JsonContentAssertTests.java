/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.rules.ExpectedException;
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
	public final ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void isEqualToWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenNullActualShouldFail() {
		assertThat(forJson(null)).isEqualTo(SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenStringIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT);
	}

	@Test
	public void isEqualToWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo("lenient-same.json");
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenResourcePathIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualTo("different.json");
	}

	@Test
	public void isEqualToWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenBytesAreNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT.getBytes());
	}

	@Test
	public void isEqualToWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(createFile(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(createFile(DIFFERENT));
	}

	@Test
	public void isEqualToWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(createInputStream(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenInputStreamIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualTo(createInputStream(DIFFERENT));
	}

	@Test
	public void isEqualToWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(createResource(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenResourceIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualTo(createResource(DIFFERENT));
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenNullActualShouldFail() {
		assertThat(forJson(null)).isEqualToJson(SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenStringIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT);
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json");
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json");
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass());
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathAndClassIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass());
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenBytesAreNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT.getBytes());
	}

	@Test
	public void isEqualToJsonWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(DIFFERENT));
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenInputStreamIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT));
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourceIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT));
	}

	@Test
	public void isStrictlyEqualToJsonWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenStringIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(LENIENT_SAME);
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("source.json");
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenResourcePathIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("lenient-same.json");
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourcePathAndClassIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("source.json", getClass());
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenResourcePathAndClassIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("lenient-same.json",
				getClass());
	}

	@Test
	public void isStrictlyEqualToJsonWhenBytesAreMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(SOURCE.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenBytesAreNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test
	public void isStrictlyEqualToJsonWhenFileIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createFile(SOURCE));
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenFileIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createFile(LENIENT_SAME));
	}

	@Test
	public void isStrictlyEqualToJsonWhenInputStreamIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createInputStream(SOURCE));
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenInputStreamIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE))
				.isStrictlyEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourceIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createResource(SOURCE));
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenResourceIsNotMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createResource(LENIENT_SAME));
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME, JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenStringIsNotMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT, JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json",
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathIsNotMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json",
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathAndClassIsNotMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes(),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenBytesAreNotMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT.getBytes(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenFileIsMatchingAndLenientShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenFileIsNotMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenInputStreamIsNotMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourceIsNotMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME, COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenStringIsNotMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT, COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathIsNotMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json", COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassAreMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass(),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathAndClassAreNotMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass(),
				COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes(), COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenBytesAreNotMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT.getBytes(), COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenFileIsMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(LENIENT_SAME), COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenFileIsNotMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createFile(DIFFERENT), COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenInputStreamIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenInputStreamIsNotMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT),
				COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourceIsNotMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT), COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenStringIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME);
	}

	@Test
	public void isNotEqualToWhenNullActualShouldPass() {
		assertThat(forJson(null)).isNotEqualTo(SOURCE);
	}

	@Test
	public void isNotEqualToWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenResourcePathIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualTo("lenient-same.json");
	}

	@Test
	public void isNotEqualToWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo("different.json");
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenBytesAreMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME.getBytes());
	}

	@Test
	public void isNotEqualToWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenFileIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(createFile(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToWhenFileIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(createFile(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenInputStreamIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createInputStream(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenResourceIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createResource(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createResource(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenStringIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME);
	}

	@Test
	public void isNotEqualToJsonWhenNullActualShouldPass() {
		assertThat(forJson(null)).isNotEqualToJson(SOURCE);
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json");
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json");
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", getClass());
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass());
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenBytesAreMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenFileIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToJsonWhenFileIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenInputStreamIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourceIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenStringIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(SOURCE);
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(LENIENT_SAME);
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenResourcePathIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("source.json");
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("lenient-same.json");
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenResourcePathAndClassAreMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("source.json", getClass());
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourcePathAndClassAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("lenient-same.json",
				getClass());
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenBytesAreMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(SOURCE.getBytes());
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenBytesAreNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenFileIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createFile(SOURCE));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenFileIsNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createFile(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenInputStreamIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createInputStream(SOURCE));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenInputStreamIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE))
				.isNotStrictlyEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenResourceIsMatchingShouldFail() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createResource(SOURCE));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourceIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE))
				.isNotStrictlyEqualToJson(createResource(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenStringIsMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME,
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT, JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathIsMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json",
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json",
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenBytesAreMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes(),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenFileIsMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenFileIsNotMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenInputStreamIsMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourceIsMatchingAndLenientShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenStringIsMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME, COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT, COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathIsMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", getClass(),
				COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass(),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenBytesAreMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes(), COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT.getBytes(), COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenFileIsMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(LENIENT_SAME),
				COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenFileIsNotMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createFile(DIFFERENT), COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenInputStreamIsMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(LENIENT_SAME),
				COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourceIsMatchingAndComparatorShouldFail() {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(LENIENT_SAME),
				COMPARATOR);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("No value at JSON path \"" + expression + "\"");
		assertThat(forJson(SIMPSONS)).hasJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveJsonPathValue() {
		assertThat(forJson(TYPES)).doesNotHaveJsonPathValue("$.bogus");
	}

	@Test
	public void doesNotHaveJsonPathValueForAnEmptyArray() {
		String expression = "$.emptyArray";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected no value at JSON path \"" + expression + "\" but found: []");
		assertThat(forJson(TYPES)).doesNotHaveJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveJsonPathValueForAnEmptyMap() {
		String expression = "$.emptyMap";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected no value at JSON path \"" + expression + "\" but found: {}");
		assertThat(forJson(TYPES)).doesNotHaveJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveJsonPathValueForIndefinitePathWithResults() {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected no value at JSON path \"" + expression
				+ "\" but found: [{\"name\":\"Bart\"}]");
		assertThat(forJson(SIMPSONS)).doesNotHaveJsonPathValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected an empty value at JSON path \"" + expression
				+ "\" but found: [{\"name\":\"Bart\"}]");
		assertThat(forJson(SIMPSONS)).hasEmptyJsonPathValue(expression);
	}

	@Test
	public void hasEmptyJsonPathValueForWhitespace() {
		String expression = "$.whitespace";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected an empty value at JSON path \"" + expression
				+ "\" but found: '    '");
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a non-empty value at JSON path \""
				+ expression + "\" but found: []");
		assertThat(forJson(SIMPSONS)).doesNotHaveEmptyJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForAnEmptyString() {
		String expression = "$.emptyString";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a non-empty value at JSON path \""
				+ expression + "\" but found: ''");
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForForAnEmptyArray() {
		String expression = "$.emptyArray";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a non-empty value at JSON path \""
				+ expression + "\" but found: []");
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForAnEmptyMap() {
		String expression = "$.emptyMap";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a non-empty value at JSON path \""
				+ expression + "\" but found: {}");
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a string at JSON path \"" + expression + "\" but found: true");
		assertThat(forJson(TYPES)).hasJsonPathStringValue(expression);
	}

	@Test
	public void hasJsonPathNumberValue() {
		assertThat(forJson(TYPES)).hasJsonPathNumberValue("$.num");
	}

	@Test
	public void hasJsonPathNumberValueForNonNumber() {
		String expression = "$.bool";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a number at JSON path \"" + expression + "\" but found: true");
		assertThat(forJson(TYPES)).hasJsonPathNumberValue(expression);
	}

	@Test
	public void hasJsonPathBooleanValue() {
		assertThat(forJson(TYPES)).hasJsonPathBooleanValue("$.bool");
	}

	@Test
	public void hasJsonPathBooleanValueForNonBoolean() {
		String expression = "$.num";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a boolean at JSON path \"" + expression + "\" but found: 5");
		assertThat(forJson(TYPES)).hasJsonPathBooleanValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).hasJsonPathArrayValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).hasJsonPathMapValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a string at JSON path \"" + expression + "\" but found: 5");
		assertThat(forJson(TYPES)).extractingJsonPathStringValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a number at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).extractingJsonPathNumberValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a boolean at JSON path \"" + expression
				+ "\" but found: 'foo'");
		assertThat(forJson(TYPES)).extractingJsonPathBooleanValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue(expression);
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
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).extractingJsonPathMapValue(expression);
	}

	@Test
	public void isNullWhenActualIsNullShouldPass() {
		assertThat(forJson(null)).isNull();
	}

	private File createFile(String content) throws IOException {
		File file = this.tempFolder.newFile("file.json");
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
