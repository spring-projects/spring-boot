/*
 * Copyright 2012-2016 the original author or authors.
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
	public void isEqualToWhenStringIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenNullActualShouldFail() throws Exception {
		assertThat(forJson(null)).isEqualTo(SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenStringIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT);
	}

	@Test
	public void isEqualToWhenResourcePathIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo("lenient-same.json");
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenResourcePathIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo("different.json");
	}

	@Test
	public void isEqualToWhenBytesAreMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenBytesAreNotMatchingShouldFail() throws Exception {
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
	public void isEqualToWhenInputStreamIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(createInputStream(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenInputStreamIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(createInputStream(DIFFERENT));
	}

	@Test
	public void isEqualToWhenResourceIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(createResource(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenResourceIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualTo(createResource(DIFFERENT));
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenNullActualShouldFail() throws Exception {
		assertThat(forJson(null)).isEqualToJson(SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenStringIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT);
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json");
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json");
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassIsMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass());
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathAndClassIsNotMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass());
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenBytesAreNotMatchingShouldFail() throws Exception {
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
	public void isEqualToJsonWhenInputStreamIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenInputStreamIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT));
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourceIsNotMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT));
	}

	@Test
	public void isStrictlyEqualToJsonWhenStringIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenStringIsNotMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(LENIENT_SAME);
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourcePathIsMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("source.json");
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenResourcePathIsNotMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("lenient-same.json");
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourcePathAndClassIsMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("source.json", getClass());
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenResourcePathAndClassIsNotMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson("lenient-same.json",
				getClass());
	}

	@Test
	public void isStrictlyEqualToJsonWhenBytesAreMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(SOURCE.getBytes());
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenBytesAreNotMatchingShouldFail()
			throws Exception {
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
	public void isStrictlyEqualToJsonWhenInputStreamIsMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createInputStream(SOURCE));
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenInputStreamIsNotMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE))
				.isStrictlyEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isStrictlyEqualToJsonWhenResourceIsMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createResource(SOURCE));
	}

	@Test(expected = AssertionError.class)
	public void isStrictlyEqualToJsonWhenResourceIsNotMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isStrictlyEqualToJson(createResource(LENIENT_SAME));
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingAndLenientShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME, JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenStringIsNotMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT, JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json",
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathIsNotMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json",
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassIsMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathAndClassIsNotMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingAndLenientShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes(),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenBytesAreNotMatchingAndLenientShouldFail()
			throws Exception {
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
	public void isEqualToJsonWhenInputStreamIsMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenInputStreamIsNotMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourceIsNotMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isEqualToJsonWhenStringIsMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME, COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenStringIsNotMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(DIFFERENT, COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourcePathIsMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathIsNotMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json", COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourcePathAndClassAreMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("lenient-same.json", getClass(),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourcePathAndClassAreNotMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson("different.json", getClass(),
				COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenBytesAreMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(LENIENT_SAME.getBytes(), COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenBytesAreNotMatchingAndComparatorShouldFail()
			throws Exception {
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
	public void isEqualToJsonWhenInputStreamIsMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(LENIENT_SAME),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenInputStreamIsNotMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createInputStream(DIFFERENT),
				COMPARATOR);
	}

	@Test
	public void isEqualToJsonWhenResourceIsMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(LENIENT_SAME),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToJsonWhenResourceIsNotMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isEqualToJson(createResource(DIFFERENT), COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenStringIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME);
	}

	@Test
	public void isNotEqualToWhenNullActualShouldPass() throws Exception {
		assertThat(forJson(null)).isNotEqualTo(SOURCE);
	}

	@Test
	public void isNotEqualToWhenStringIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenResourcePathIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo("lenient-same.json");
	}

	@Test
	public void isNotEqualToWhenResourcePathIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo("different.json");
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenBytesAreMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME.getBytes());
	}

	@Test
	public void isNotEqualToWhenBytesAreNotMatchingShouldPass() throws Exception {
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
	public void isNotEqualToWhenInputStreamIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToWhenInputStreamIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(createInputStream(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToWhenResourceIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(createResource(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToWhenResourceIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualTo(createResource(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenStringIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME);
	}

	@Test
	public void isNotEqualToJsonWhenNullActualShouldPass() throws Exception {
		assertThat(forJson(null)).isNotEqualToJson(SOURCE);
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json");
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json");
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", getClass());
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass());
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenBytesAreMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes());
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingShouldPass() throws Exception {
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
	public void isNotEqualToJsonWhenInputStreamIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourceIsMatchingShouldFail() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(LENIENT_SAME));
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsNotMatchingShouldPass() throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT));
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenStringIsMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(SOURCE);
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenStringIsNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(LENIENT_SAME);
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenResourcePathIsMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("source.json");
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourcePathIsNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("lenient-same.json");
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenResourcePathAndClassAreMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("source.json", getClass());
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourcePathAndClassAreNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson("lenient-same.json",
				getClass());
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenBytesAreMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(SOURCE.getBytes());
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenBytesAreNotMatchingShouldPass()
			throws Exception {
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
	public void isNotStrictlyEqualToJsonWhenInputStreamIsMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createInputStream(SOURCE));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenInputStreamIsNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE))
				.isNotStrictlyEqualToJson(createInputStream(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isNotStrictlyEqualToJsonWhenResourceIsMatchingShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualToJson(createResource(SOURCE));
	}

	@Test
	public void isNotStrictlyEqualToJsonWhenResourceIsNotMatchingShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE))
				.isNotStrictlyEqualToJson(createResource(LENIENT_SAME));
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenStringIsMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME,
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT, JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathIsMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json",
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json",
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass(),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenBytesAreMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes(),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingAndLenientShouldPass()
			throws Exception {
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
	public void isNotEqualToJsonWhenInputStreamIsMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourceIsMatchingAndLenientShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(LENIENT_SAME),
				JSONCompareMode.LENIENT);
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsNotMatchingAndLenientShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT),
				JSONCompareMode.LENIENT);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenStringIsMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME, COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenStringIsNotMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(DIFFERENT, COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathIsMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathIsNotMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourcePathAndClassAreMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("lenient-same.json", getClass(),
				COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenResourcePathAndClassAreNotMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson("different.json", getClass(),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenBytesAreMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(LENIENT_SAME.getBytes(), COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenBytesAreNotMatchingAndComparatorShouldPass()
			throws Exception {
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
	public void isNotEqualToJsonWhenInputStreamIsMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(LENIENT_SAME),
				COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenInputStreamIsNotMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createInputStream(DIFFERENT),
				COMPARATOR);
	}

	@Test(expected = AssertionError.class)
	public void isNotEqualToJsonWhenResourceIsMatchingAndComparatorShouldFail()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(LENIENT_SAME),
				COMPARATOR);
	}

	@Test
	public void isNotEqualToJsonWhenResourceIsNotMatchingAndComparatorShouldPass()
			throws Exception {
		assertThat(forJson(SOURCE)).isNotEqualToJson(createResource(DIFFERENT),
				COMPARATOR);
	}

	@Test
	public void hasJsonPathValue() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.str");
	}

	@Test
	public void hasJsonPathValueForAnEmptyArray() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.emptyArray");
	}

	@Test
	public void hasJsonPathValueForAnEmptyMap() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathValue("$.emptyMap");
	}

	@Test
	public void hasJsonPathValueForIndefinitePathWithResults() throws Exception {
		assertThat(forJson(SIMPSONS))
				.hasJsonPathValue("$.familyMembers[?(@.name == 'Bart')]");
	}

	@Test
	public void hasJsonPathValueForIndefinitePathWithEmptyResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("No value at JSON path \"" + expression + "\"");
		assertThat(forJson(SIMPSONS)).hasJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveJsonPathValue() throws Exception {
		assertThat(forJson(TYPES)).doesNotHaveJsonPathValue("$.bogus");
	}

	@Test
	public void doesNotHaveJsonPathValueForAnEmptyArray() throws Exception {
		String expression = "$.emptyArray";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected no value at JSON path \"" + expression + "\" but found: []");
		assertThat(forJson(TYPES)).doesNotHaveJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveJsonPathValueForAnEmptyMap() throws Exception {
		String expression = "$.emptyMap";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected no value at JSON path \"" + expression + "\" but found: {}");
		assertThat(forJson(TYPES)).doesNotHaveJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveJsonPathValueForIndefinitePathWithResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected no value at JSON path \"" + expression
				+ "\" but found: [{\"name\":\"Bart\"}]");
		assertThat(forJson(SIMPSONS)).doesNotHaveJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveJsonPathValueForIndefinitePathWithEmptyResults()
			throws Exception {
		assertThat(forJson(SIMPSONS))
				.doesNotHaveJsonPathValue("$.familyMembers[?(@.name == 'Dilbert')]");
	}

	@Test
	public void hasEmptyJsonPathValueForAnEmptyString() throws Exception {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyString");
	}

	@Test
	public void hasEmptyJsonPathValueForAnEmptyArray() throws Exception {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyArray");
	}

	@Test
	public void hasEmptyJsonPathValueForAnEmptyMap() throws Exception {
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue("$.emptyMap");
	}

	@Test
	public void hasEmptyJsonPathValueForIndefinitePathWithEmptyResults()
			throws Exception {
		assertThat(forJson(SIMPSONS))
				.hasEmptyJsonPathValue("$.familyMembers[?(@.name == 'Dilbert')]");
	}

	@Test
	public void hasEmptyJsonPathValueForIndefinitePathWithResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected an empty value at JSON path \"" + expression
				+ "\" but found: [{\"name\":\"Bart\"}]");
		assertThat(forJson(SIMPSONS)).hasEmptyJsonPathValue(expression);
	}

	@Test
	public void hasEmptyJsonPathValueForWhitespace() throws Exception {
		String expression = "$.whitespace";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected an empty value at JSON path \"" + expression
				+ "\" but found: '    '");
		assertThat(forJson(TYPES)).hasEmptyJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForString() throws Exception {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.str");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForNumber() throws Exception {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.num");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForBoolean() throws Exception {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.bool");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForArray() throws Exception {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.arr");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForMap() throws Exception {
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue("$.colorMap");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForIndefinitePathWithResults()
			throws Exception {
		assertThat(forJson(SIMPSONS))
				.doesNotHaveEmptyJsonPathValue("$.familyMembers[?(@.name == 'Bart')]");
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForIndefinitePathWithEmptyResults()
			throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a non-empty value at JSON path \""
				+ expression + "\" but found: []");
		assertThat(forJson(SIMPSONS)).doesNotHaveEmptyJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForAnEmptyString() throws Exception {
		String expression = "$.emptyString";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a non-empty value at JSON path \""
				+ expression + "\" but found: ''");
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForForAnEmptyArray() throws Exception {
		String expression = "$.emptyArray";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a non-empty value at JSON path \""
				+ expression + "\" but found: []");
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression);
	}

	@Test
	public void doesNotHaveEmptyJsonPathValueForAnEmptyMap() throws Exception {
		String expression = "$.emptyMap";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a non-empty value at JSON path \""
				+ expression + "\" but found: {}");
		assertThat(forJson(TYPES)).doesNotHaveEmptyJsonPathValue(expression);
	}

	@Test
	public void hasJsonPathStringValue() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathStringValue("$.str");
	}

	@Test
	public void hasJsonPathStringValueForAnEmptyString() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathStringValue("$.emptyString");
	}

	@Test
	public void hasJsonPathStringValueForNonString() throws Exception {
		String expression = "$.bool";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a string at JSON path \"" + expression + "\" but found: true");
		assertThat(forJson(TYPES)).hasJsonPathStringValue(expression);
	}

	@Test
	public void hasJsonPathNumberValue() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathNumberValue("$.num");
	}

	@Test
	public void hasJsonPathNumberValueForNonNumber() throws Exception {
		String expression = "$.bool";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a number at JSON path \"" + expression + "\" but found: true");
		assertThat(forJson(TYPES)).hasJsonPathNumberValue(expression);
	}

	@Test
	public void hasJsonPathBooleanValue() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathBooleanValue("$.bool");
	}

	@Test
	public void hasJsonPathBooleanValueForNonBoolean() throws Exception {
		String expression = "$.num";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a boolean at JSON path \"" + expression + "\" but found: 5");
		assertThat(forJson(TYPES)).hasJsonPathBooleanValue(expression);
	}

	@Test
	public void hasJsonPathArrayValue() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathArrayValue("$.arr");
	}

	@Test
	public void hasJsonPathArrayValueForAnEmptyArray() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathArrayValue("$.emptyArray");
	}

	@Test
	public void hasJsonPathArrayValueForNonArray() throws Exception {
		String expression = "$.str";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).hasJsonPathArrayValue(expression);
	}

	@Test
	public void hasJsonPathMapValue() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathMapValue("$.colorMap");
	}

	@Test
	public void hasJsonPathMapValueForAnEmptyMap() throws Exception {
		assertThat(forJson(TYPES)).hasJsonPathMapValue("$.emptyMap");
	}

	@Test
	public void hasJsonPathMapValueForNonMap() throws Exception {
		String expression = "$.str";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).hasJsonPathMapValue(expression);
	}

	@Test
	public void extractingJsonPathValue() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathValue("@.str").isEqualTo("foo");
	}

	@Test
	public void extractingJsonPathValueForMissing() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathStringValue() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.str")
				.isEqualTo("foo");
	}

	@Test
	public void extractingJsonPathStringValueForMissing() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathStringValueForEmptyString() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathStringValue("@.emptyString")
				.isEmpty();
	}

	@Test
	public void extractingJsonPathStringValueForWrongType() throws Exception {
		String expression = "$.num";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a string at JSON path \"" + expression + "\" but found: 5");
		assertThat(forJson(TYPES)).extractingJsonPathStringValue(expression);
	}

	@Test
	public void extractingJsonPathNumberValue() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathNumberValue("@.num").isEqualTo(5);
	}

	@Test
	public void extractingJsonPathNumberValueForMissing() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathNumberValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathNumberValueForWrongType() throws Exception {
		String expression = "$.str";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a number at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).extractingJsonPathNumberValue(expression);
	}

	@Test
	public void extractingJsonPathBooleanValue() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathBooleanValue("@.bool").isTrue();
	}

	@Test
	public void extractingJsonPathBooleanValueForMissing() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathBooleanValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathBooleanValueForWrongType() throws Exception {
		String expression = "$.str";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Expected a boolean at JSON path \"" + expression
				+ "\" but found: 'foo'");
		assertThat(forJson(TYPES)).extractingJsonPathBooleanValue(expression);
	}

	@Test
	public void extractingJsonPathArrayValue() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.arr")
				.containsExactly(42);
	}

	@Test
	public void extractingJsonPathArrayValueForMissing() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathArrayValueForEmpty() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.emptyArray").isEmpty();
	}

	@Test
	public void extractingJsonPathArrayValueForWrongType() throws Exception {
		String expression = "$.str";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).extractingJsonPathArrayValue(expression);
	}

	@Test
	public void extractingJsonPathMapValue() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.colorMap")
				.contains(entry("red", "rojo"));
	}

	@Test
	public void extractingJsonPathMapValueForMissing() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.bogus").isNull();
	}

	@Test
	public void extractingJsonPathMapValueForEmpty() throws Exception {
		assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.emptyMap").isEmpty();
	}

	@Test
	public void extractingJsonPathMapValueForWrongType() throws Exception {
		String expression = "$.str";
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
		assertThat(forJson(TYPES)).extractingJsonPathMapValue(expression);
	}

	@Test
	public void isNullWhenActualIsNullShouldPass() throws Exception {
		assertThat(forJson(null)).isNull();
	}

	private File createFile(String content) throws IOException {
		File file = this.tempFolder.newFile("file.json");
		FileCopyUtils.copy(content.getBytes(), file);
		return file;
	}

	private InputStream createInputStream(String content) throws IOException {
		return new ByteArrayInputStream(content.getBytes());
	}

	private Resource createResource(String content) throws IOException {
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

	private AssertProvider<JsonContentAssert> forJson(final String json) {
		return new AssertProvider<JsonContentAssert>() {

			@Override
			public JsonContentAssert assertThat() {
				return new JsonContentAssert(JsonContentAssertTests.class, json);
			}

		};
	}

}
