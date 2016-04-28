/*
 * Copyright 2012-2016 the original author or authors.
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

import java.io.File;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * AssertJ based JSON tester that works with basic JSON strings. Allows testing of JSON
 * payloads created from any source, for example:<pre class="code">
 * public class ExampleObjectJsonTests {
 *
 *     private BasicJsonTester json = new BasicJsonTester(getClass());
 *
 *     &#064;Test
 *     public void testWriteJson() throws IOException {
 *         assertThat(json.from("example.json")).extractingJsonPathStringValue("@.name")
				.isEqualTo("Spring");
 *     }
 *
 * }
 * </pre>
 *
 * See {@link AbstractJsonMarshalTester} for more details.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class BasicJsonTester {

	private final JsonLoader loader;

	/**
	 * Create a new {@link BasicJsonTester} instance.
	 * @param resourceLoadClass the source class used to load resources
	 */
	public BasicJsonTester(Class<?> resourceLoadClass) {
		Assert.notNull(resourceLoadClass, "ResourceLoadClass must not be null");
		this.loader = new JsonLoader(resourceLoadClass);
	}

	/**
	 * Create JSON content from the specified String source. The source can contain the
	 * JSON itself or, if it ends with {@code .json}, the name of a resource to be loaded
	 * using {@code resourceLoadClass}.
	 * @param source JSON content or a {@code .json} resource name
	 * @return the JSON content
	 */
	public JsonContent<Object> from(CharSequence source) {
		return getJsonContent(this.loader.getJson(source));
	}

	/**
	 * Create JSON content from the specified resource path.
	 * @param path the path of the resource to load
	 * @param resourceLoadClass the source class used to load the resource
	 * @return the JSON content
	 */
	public JsonContent<Object> from(String path, Class<?> resourceLoadClass) {
		return getJsonContent(this.loader.getJson(path, resourceLoadClass));
	}

	/**
	 * Create JSON content from the specified JSON bytes.
	 * @param source the bytes of JSON
	 * @return the JSON content
	 */
	public JsonContent<Object> from(byte[] source) {
		return getJsonContent(this.loader.getJson(source));
	}

	/**
	 * Create JSON content from the specified JSON file.
	 * @param source the file containing JSON
	 * @return the JSON content
	 */
	public JsonContent<Object> from(File source) {
		return getJsonContent(this.loader.getJson(source));
	}

	/**
	 * Create JSON content from the specified JSON input stream.
	 * @param source the input stream containing JSON
	 * @return the JSON content
	 */
	public JsonContent<Object> from(InputStream source) {
		return getJsonContent(this.loader.getJson(source));
	}

	/**
	 * Create JSON content from the specified JSON resource.
	 * @param source the resource containing JSON
	 * @return the JSON content
	 */
	public JsonContent<Object> from(Resource source) {
		return getJsonContent(this.loader.getJson(source));
	}

	private JsonContent<Object> getJsonContent(String json) {
		return new JsonContent<Object>(this.loader.getResourceLoadClass(), null, json);
	}

}
