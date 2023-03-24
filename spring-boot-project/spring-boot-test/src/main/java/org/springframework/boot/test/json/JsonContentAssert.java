/*
 * Copyright 2012-2022 the original author or authors.
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

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.MapAssert;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import org.springframework.core.io.Resource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * AssertJ {@link Assert} for {@link JsonContent}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Diego Berrueta
 * @author Camille Vienot
 * @since 1.4.0
 */
public class JsonContentAssert extends AbstractAssert<JsonContentAssert, CharSequence> {

	private final JsonLoader loader;

	private final Configuration configuration;

	/**
	 * Create a new {@link JsonContentAssert} instance that will load resources as UTF-8.
	 * @param resourceLoadClass the source class used to load resources
	 * @param json the actual JSON content
	 */
	public JsonContentAssert(Class<?> resourceLoadClass, CharSequence json) {
		this(resourceLoadClass, null, json);
	}

	/**
	 * Create a new {@link JsonContentAssert} instance that will load resources in the
	 * given {@code charset}.
	 * @param resourceLoadClass the source class used to load resources
	 * @param charset the charset of the JSON resources
	 * @param json the actual JSON content
	 * @since 1.4.1
	 */
	public JsonContentAssert(Class<?> resourceLoadClass, Charset charset, CharSequence json) {
		this(resourceLoadClass, charset, json, Configuration.defaultConfiguration());
	}

	/**
	 * Create a new {@link JsonContentAssert} instance that will load resources in the
	 * given {@code charset}.
	 * @param resourceLoadClass the source class used to load resources
	 * @param charset the charset of the JSON resources
	 * @param json the actual JSON content
	 * @param configuration the json-path configuration
	 */
	JsonContentAssert(Class<?> resourceLoadClass, Charset charset, CharSequence json, Configuration configuration) {
		super(json, JsonContentAssert.class);
		this.configuration = configuration;
		this.loader = new JsonLoader(resourceLoadClass, charset);
	}

	/**
	 * Overridden version of {@code isEqualTo} to perform JSON tests based on the object
	 * type.
	 * @see org.assertj.core.api.AbstractAssert#isEqualTo(java.lang.Object)
	 */
	@Override
	public JsonContentAssert isEqualTo(Object expected) {
		if (expected == null || expected instanceof CharSequence) {
			return isEqualToJson((CharSequence) expected);
		}
		if (expected instanceof byte[] bytes) {
			return isEqualToJson(bytes);
		}
		if (expected instanceof File file) {
			return isEqualToJson(file);
		}
		if (expected instanceof InputStream inputStream) {
			return isEqualToJson(inputStream);
		}
		if (expected instanceof Resource resource) {
			return isEqualToJson(resource);
		}
		failWithMessage("Unsupported type for JSON assert %s", expected.getClass());
		return null;
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#LENIENT leniently} equal
	 * to the specified JSON. The {@code expected} value can contain the JSON itself or,
	 * if it ends with {@code .json}, the name of a resource to be loaded using
	 * {@code resourceLoadClass}.
	 * @param expected the expected JSON or the name of a resource containing the expected
	 * JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(CharSequence expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#LENIENT leniently} equal
	 * to the specified JSON resource.
	 * @param path the name of a resource containing the expected JSON
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(String path, Class<?> resourceLoadClass) {
		String expectedJson = this.loader.getJson(path, resourceLoadClass);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#LENIENT leniently} equal
	 * to the specified JSON bytes.
	 * @param expected the expected JSON bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(byte[] expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#LENIENT leniently} equal
	 * to the specified JSON file.
	 * @param expected a file containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(File expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#LENIENT leniently} equal
	 * to the specified JSON input stream.
	 * @param expected an input stream containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(InputStream expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#LENIENT leniently} equal
	 * to the specified JSON resource.
	 * @param expected a resource containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(Resource expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#STRICT strictly} equal to
	 * the specified JSON. The {@code expected} value can contain the JSON itself or, if
	 * it ends with {@code .json}, the name of a resource to be loaded using
	 * {@code resourceLoadClass}.
	 * @param expected the expected JSON or the name of a resource containing the expected
	 * JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isStrictlyEqualToJson(CharSequence expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#STRICT strictly} equal to
	 * the specified JSON resource.
	 * @param path the name of a resource containing the expected JSON
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isStrictlyEqualToJson(String path, Class<?> resourceLoadClass) {
		String expectedJson = this.loader.getJson(path, resourceLoadClass);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#STRICT strictly} equal to
	 * the specified JSON bytes.
	 * @param expected the expected JSON bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isStrictlyEqualToJson(byte[] expected) {
		return assertNotFailed(compare(this.loader.getJson(expected), JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#STRICT strictly} equal to
	 * the specified JSON file.
	 * @param expected a file containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isStrictlyEqualToJson(File expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#STRICT strictly} equal to
	 * the specified JSON input stream.
	 * @param expected an input stream containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isStrictlyEqualToJson(InputStream expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is {@link JSONCompareMode#STRICT strictly} equal to
	 * the specified JSON resource.
	 * @param expected a resource containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isStrictlyEqualToJson(Resource expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON. The {@code expected}
	 * value can contain the JSON itself or, if it ends with {@code .json}, the name of a
	 * resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected JSON or the name of a resource containing the expected
	 * JSON
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(CharSequence expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON resource.
	 * @param path the name of a resource containing the expected JSON
	 * @param resourceLoadClass the source class used to load the resource
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(String path, Class<?> resourceLoadClass, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(path, resourceLoadClass);
		return assertNotFailed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON bytes.
	 * @param expected the expected JSON bytes
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(byte[] expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON file.
	 * @param expected a file containing the expected JSON
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(File expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON input stream.
	 * @param expected an input stream containing the expected JSON
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(InputStream expected, JSONCompareMode compareMode) {
		return assertNotFailed(compare(this.loader.getJson(expected), compareMode));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON resource.
	 * @param expected a resource containing the expected JSON
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(Resource expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON. The {@code expected}
	 * value can contain the JSON itself or, if it ends with {@code .json}, the name of a
	 * resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected JSON or the name of a resource containing the expected
	 * JSON
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(CharSequence expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON resource.
	 * @param path the name of a resource containing the expected JSON
	 * @param resourceLoadClass the source class used to load the resource
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(String path, Class<?> resourceLoadClass, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(path, resourceLoadClass);
		return assertNotFailed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON bytes.
	 * @param expected the expected JSON bytes
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(byte[] expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON file.
	 * @param expected a file containing the expected JSON
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(File expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON input stream.
	 * @param expected an input stream containing the expected JSON
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(InputStream expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is equal to the specified JSON resource.
	 * @param expected a resource containing the expected JSON
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is not equal to the given one
	 */
	public JsonContentAssert isEqualToJson(Resource expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, comparator));
	}

	/**
	 * Overridden version of {@code isNotEqualTo} to perform JSON tests based on the
	 * object type.
	 * @see org.assertj.core.api.AbstractAssert#isEqualTo(java.lang.Object)
	 */
	@Override
	public JsonContentAssert isNotEqualTo(Object expected) {
		if (expected == null || expected instanceof CharSequence) {
			return isNotEqualToJson((CharSequence) expected);
		}
		if (expected instanceof byte[] bytes) {
			return isNotEqualToJson(bytes);
		}
		if (expected instanceof File file) {
			return isNotEqualToJson(file);
		}
		if (expected instanceof InputStream inputStream) {
			return isNotEqualToJson(inputStream);
		}
		if (expected instanceof Resource resource) {
			return isNotEqualToJson(resource);
		}
		failWithMessage("Unsupported type for JSON assert %s", expected.getClass());
		return null;
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#LENIENT leniently}
	 * equal to the specified JSON. The {@code expected} value can contain the JSON itself
	 * or, if it ends with {@code .json}, the name of a resource to be loaded using
	 * {@code resourceLoadClass}.
	 * @param expected the expected JSON or the name of a resource containing the expected
	 * JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(CharSequence expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#LENIENT leniently}
	 * equal to the specified JSON resource.
	 * @param path the name of a resource containing the expected JSON
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(String path, Class<?> resourceLoadClass) {
		String expectedJson = this.loader.getJson(path, resourceLoadClass);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#LENIENT leniently}
	 * equal to the specified JSON bytes.
	 * @param expected the expected JSON bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(byte[] expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#LENIENT leniently}
	 * equal to the specified JSON file.
	 * @param expected a file containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(File expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#LENIENT leniently}
	 * equal to the specified JSON input stream.
	 * @param expected an input stream containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(InputStream expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#LENIENT leniently}
	 * equal to the specified JSON resource.
	 * @param expected a resource containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(Resource expected) {
		return assertNotPassed(compare(this.loader.getJson(expected), JSONCompareMode.LENIENT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#STRICT strictly} equal
	 * to the specified JSON. The {@code expected} value can contain the JSON itself or,
	 * if it ends with {@code .json}, the name of a resource to be loaded using
	 * {@code resourceLoadClass}.
	 * @param expected the expected JSON or the name of a resource containing the expected
	 * JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotStrictlyEqualToJson(CharSequence expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#STRICT strictly} equal
	 * to the specified JSON resource.
	 * @param path the name of a resource containing the expected JSON
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotStrictlyEqualToJson(String path, Class<?> resourceLoadClass) {
		String expectedJson = this.loader.getJson(path, resourceLoadClass);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#STRICT strictly} equal
	 * to the specified JSON bytes.
	 * @param expected the expected JSON bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotStrictlyEqualToJson(byte[] expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#STRICT strictly} equal
	 * to the specified JSON file.
	 * @param expected a file containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotStrictlyEqualToJson(File expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#STRICT strictly} equal
	 * to the specified JSON input stream.
	 * @param expected an input stream containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotStrictlyEqualToJson(InputStream expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is not {@link JSONCompareMode#STRICT strictly} equal
	 * to the specified JSON resource.
	 * @param expected a resource containing the expected JSON
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotStrictlyEqualToJson(Resource expected) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, JSONCompareMode.STRICT));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected JSON or the name of a resource containing the expected
	 * JSON
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(CharSequence expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON resource.
	 * @param path the name of a resource containing the expected JSON
	 * @param resourceLoadClass the source class used to load the resource
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(String path, Class<?> resourceLoadClass, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(path, resourceLoadClass);
		return assertNotPassed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON bytes.
	 * @param expected the expected JSON bytes
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(byte[] expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON file.
	 * @param expected a file containing the expected JSON
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(File expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON input stream.
	 * @param expected an input stream containing the expected JSON
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(InputStream expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON resource.
	 * @param expected a resource containing the expected JSON
	 * @param compareMode the compare mode used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(Resource expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, compareMode));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected JSON or the name of a resource containing the expected
	 * JSON
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(CharSequence expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON resource.
	 * @param path the name of a resource containing the expected JSON
	 * @param resourceLoadClass the source class used to load the resource
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(String path, Class<?> resourceLoadClass, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(path, resourceLoadClass);
		return assertNotPassed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON bytes.
	 * @param expected the expected JSON bytes
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(byte[] expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON file.
	 * @param expected a file containing the expected JSON
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(File expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON input stream.
	 * @param expected an input stream containing the expected JSON
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(InputStream expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, comparator));
	}

	/**
	 * Verifies that the actual value is not equal to the specified JSON resource.
	 * @param expected a resource containing the expected JSON
	 * @param comparator the comparator used when checking
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual JSON value is equal to the given one
	 */
	public JsonContentAssert isNotEqualToJson(Resource expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, comparator));
	}

	/**
	 * Verify that the JSON path is present without checking if it has a value.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is missing
	 * @since 2.2.0
	 * @see #hasJsonPathValue(CharSequence, Object...)
	 */
	public JsonContentAssert hasJsonPath(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertHasPath();
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path produces a non-null result. If
	 * the JSON path expression is not {@linkplain JsonPath#isDefinite() definite}, this
	 * method verifies that the value at the given path is not <em>empty</em>.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is missing
	 */
	public JsonContentAssert hasJsonPathValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertHasValue(Object.class, "an object");
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path produces a non-null string
	 * result.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is missing or not a string
	 */
	public JsonContentAssert hasJsonPathStringValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertHasValue(String.class, "a string");
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path produces a non-null number
	 * result.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is missing or not a number
	 */
	public JsonContentAssert hasJsonPathNumberValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertHasValue(Number.class, "a number");
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path produces a non-null boolean
	 * result.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is missing or not a boolean
	 */
	public JsonContentAssert hasJsonPathBooleanValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertHasValue(Boolean.class, "a boolean");
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path produces a non-null array
	 * result.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is missing or not an array
	 */
	public JsonContentAssert hasJsonPathArrayValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertHasValue(List.class, "an array");
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path produces a non-null map result.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is missing or not a map
	 */
	public JsonContentAssert hasJsonPathMapValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertHasValue(Map.class, "a map");
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path produces an
	 * {@link ObjectUtils#isEmpty(Object) empty} result.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is not empty
	 */
	public JsonContentAssert hasEmptyJsonPathValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertHasEmptyValue();
		return this;
	}

	/**
	 * Verify that the JSON path is not present, even if it has a {@code null} value.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is not missing
	 * @since 2.2.0
	 * @see #doesNotHaveJsonPathValue(CharSequence, Object...)
	 */
	public JsonContentAssert doesNotHaveJsonPath(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertDoesNotHavePath();
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path produces no result. If the JSON
	 * path expression is not {@linkplain JsonPath#isDefinite() definite}, this method
	 * verifies that the value at the given path is <em>empty</em>.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is not missing
	 */
	public JsonContentAssert doesNotHaveJsonPathValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertDoesNotHaveValue();
		return this;
	}

	/**
	 * Verify that the actual value at the given JSON path does not produce an
	 * {@link ObjectUtils#isEmpty(Object) empty} result.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if the value at the given path is empty
	 */
	public JsonContentAssert doesNotHaveEmptyJsonPathValue(CharSequence expression, Object... args) {
		new JsonPathValue(expression, args).assertDoesNotHaveEmptyValue();
		return this;
	}

	/**
	 * Extract the value at the given JSON path for further object assertions.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the path is not valid
	 */
	public AbstractObjectAssert<?, Object> extractingJsonPathValue(CharSequence expression, Object... args) {
		return Assertions.assertThat(new JsonPathValue(expression, args).getValue(false));
	}

	/**
	 * Extract the string value at the given JSON path for further object assertions.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the path is not valid or does not result in a string
	 */
	public AbstractCharSequenceAssert<?, String> extractingJsonPathStringValue(CharSequence expression,
			Object... args) {
		return Assertions.assertThat(extractingJsonPathValue(expression, args, String.class, "a string"));
	}

	/**
	 * Extract the number value at the given JSON path for further object assertions.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the path is not valid or does not result in a number
	 */
	public AbstractObjectAssert<?, Number> extractingJsonPathNumberValue(CharSequence expression, Object... args) {
		return Assertions.assertThat(extractingJsonPathValue(expression, args, Number.class, "a number"));
	}

	/**
	 * Extract the boolean value at the given JSON path for further object assertions.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the path is not valid or does not result in a boolean
	 */
	public AbstractBooleanAssert<?> extractingJsonPathBooleanValue(CharSequence expression, Object... args) {
		return Assertions.assertThat(extractingJsonPathValue(expression, args, Boolean.class, "a boolean"));
	}

	/**
	 * Extract the array value at the given JSON path for further object assertions.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @param <E> element type
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the path is not valid or does not result in an array
	 */
	@SuppressWarnings("unchecked")
	public <E> ListAssert<E> extractingJsonPathArrayValue(CharSequence expression, Object... args) {
		return Assertions.assertThat(extractingJsonPathValue(expression, args, List.class, "an array"));
	}

	/**
	 * Extract the map value at the given JSON path for further object assertions.
	 * @param expression the {@link JsonPath} expression
	 * @param args arguments to parameterize the {@code JsonPath} expression with, using
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @param <K> key type
	 * @param <V> value type
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the path is not valid or does not result in a map
	 */
	@SuppressWarnings("unchecked")
	public <K, V> MapAssert<K, V> extractingJsonPathMapValue(CharSequence expression, Object... args) {
		return Assertions.assertThat(extractingJsonPathValue(expression, args, Map.class, "a map"));
	}

	@SuppressWarnings("unchecked")
	private <T> T extractingJsonPathValue(CharSequence expression, Object[] args, Class<T> type,
			String expectedDescription) {
		JsonPathValue value = new JsonPathValue(expression, args);
		if (value.getValue(false) != null) {
			value.assertHasValue(type, expectedDescription);
		}
		return (T) value.getValue(false);
	}

	private JSONCompareResult compare(CharSequence expectedJson, JSONCompareMode compareMode) {
		if (this.actual == null) {
			return compareForNull(expectedJson);
		}
		try {
			return JSONCompare.compareJSON((expectedJson != null) ? expectedJson.toString() : null,
					this.actual.toString(), compareMode);
		}
		catch (Exception ex) {
			if (ex instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException(ex);
		}
	}

	private JSONCompareResult compare(CharSequence expectedJson, JSONComparator comparator) {
		if (this.actual == null) {
			return compareForNull(expectedJson);
		}
		try {
			return JSONCompare.compareJSON((expectedJson != null) ? expectedJson.toString() : null,
					this.actual.toString(), comparator);
		}
		catch (Exception ex) {
			if (ex instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException(ex);
		}
	}

	private JSONCompareResult compareForNull(CharSequence expectedJson) {
		JSONCompareResult result = new JSONCompareResult();
		result.passed();
		if (expectedJson != null) {
			result.fail("Expected null JSON");
		}
		return result;
	}

	private JsonContentAssert assertNotFailed(JSONCompareResult result) {
		if (result.failed()) {
			failWithMessage("JSON Comparison failure: %s", result.getMessage());
		}
		return this;
	}

	private JsonContentAssert assertNotPassed(JSONCompareResult result) {
		if (result.passed()) {
			failWithMessage("JSON Comparison failure: %s", result.getMessage());
		}
		return this;
	}

	/**
	 * A {@link JsonPath} value.
	 */
	private class JsonPathValue {

		private final String expression;

		private final JsonPath jsonPath;

		JsonPathValue(CharSequence expression, Object... args) {
			org.springframework.util.Assert.hasText((expression != null) ? expression.toString() : null,
					"expression must not be null or empty");
			this.expression = String.format(expression.toString(), args);
			this.jsonPath = JsonPath.compile(this.expression);
		}

		void assertHasEmptyValue() {
			if (ObjectUtils.isEmpty(getValue(false)) || isIndefiniteAndEmpty()) {
				return;
			}
			failWithMessage(getExpectedValueMessage("an empty value"));
		}

		void assertDoesNotHaveEmptyValue() {
			if (!ObjectUtils.isEmpty(getValue(false))) {
				return;
			}
			failWithMessage(getExpectedValueMessage("a non-empty value"));

		}

		void assertHasPath() {
			try {
				read();
			}
			catch (PathNotFoundException ex) {
				failWithMessage("No JSON path \"%s\" found", this.expression);
			}
		}

		void assertDoesNotHavePath() {
			try {
				read();
				failWithMessage("Expecting no JSON path \"%s\"", this.expression);
			}
			catch (PathNotFoundException ex) {
			}
		}

		void assertHasValue(Class<?> type, String expectedDescription) {
			Object value = getValue(true);
			if (value == null || isIndefiniteAndEmpty()) {
				failWithNoValueMessage();
			}
			if (type != null && !type.isInstance(value)) {
				failWithMessage(getExpectedValueMessage(expectedDescription));
			}
		}

		void assertDoesNotHaveValue() {
			if (getValue(false) == null || isIndefiniteAndEmpty()) {
				return;
			}
			failWithMessage(getExpectedValueMessage("no value"));
		}

		private boolean isIndefiniteAndEmpty() {
			return !isDefinite() && isEmpty();
		}

		private boolean isDefinite() {
			return this.jsonPath.isDefinite();
		}

		private boolean isEmpty() {
			return ObjectUtils.isEmpty(getValue(false));
		}

		Object getValue(boolean required) {
			try {
				return read();
			}
			catch (Exception ex) {
				if (required) {
					failWithNoValueMessage();
				}
				return null;
			}
		}

		private void failWithNoValueMessage() {
			failWithMessage("No value at JSON path \"%s\"", this.expression);
		}

		private Object read() {
			CharSequence json = JsonContentAssert.this.actual;
			return this.jsonPath.read((json != null) ? json.toString() : null, JsonContentAssert.this.configuration);
		}

		private String getExpectedValueMessage(String expectedDescription) {
			return String.format("Expected %s at JSON path \"%s\" but found: %s", expectedDescription, this.expression,
					ObjectUtils.nullSafeToString(StringUtils.quoteIfString(getValue(false))));
		}

	}

}
