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

import java.io.IOException;
import java.io.Reader;

import javax.json.bind.Jsonb;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * AssertJ based JSON tester backed by Jsonb. Usually instantiated via
 * {@link #initFields(Object, Jsonb)}, for example: <pre class="code">
 * public class ExampleObjectJsonTests {
 *
 * 	private JsonbTester&lt;ExampleObject&gt; json;
 *
 * 	&#064;Before
 * 	public void setup() {
 * 		Jsonb jsonb = JsonbBuilder.create();
 * 		JsonbTester.initFields(this, jsonb);
 * 	}
 *
 * 	&#064;Test
 * 	public void testWriteJson() throws IOException {
 * 		ExampleObject object = // ...
 * 		assertThat(json.write(object)).isEqualToJson(&quot;expected.json&quot;);
 * 	}
 *
 * }
 * </pre>
 *
 * See {@link AbstractJsonMarshalTester} for more details.
 *
 * @param <T> the type under test
 * @author Eddú Meléndez
 * @since 2.0.0
 */
public class JsonbTester<T> extends AbstractJsonMarshalTester<T> {

	private final Jsonb jsonb;

	/**
	 * Create a new uninitialized {@link JsonbTester} instance.
	 * @param jsonb the Jsonb instance
	 */
	protected JsonbTester(Jsonb jsonb) {
		Assert.notNull(jsonb, "Jsonb must not be null");
		this.jsonb = jsonb;
	}

	/**
	 * Create a new {@link JsonbTester} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test
	 * @param jsonb the Jsonb instance
	 * @see #initFields(Object, Jsonb)
	 */
	public JsonbTester(Class<?> resourceLoadClass, ResolvableType type, Jsonb jsonb) {
		super(resourceLoadClass, type);
		Assert.notNull(jsonb, "Jsonb must not be null");
		this.jsonb = jsonb;
	}

	@Override
	protected String writeObject(T value, ResolvableType type) throws IOException {
		return this.jsonb.toJson(value, type.getType());
	}

	@Override
	protected T readObject(Reader reader, ResolvableType type) throws IOException {
		return this.jsonb.fromJson(reader, type.getType());
	}

	/**
	 * Utility method to initialize {@link JsonbTester} fields. See {@link JsonbTester
	 * class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param jsonb the Jsonb instance
	 */
	public static void initFields(Object testInstance, Jsonb jsonb) {
		new JsonbFieldInitializer().initFields(testInstance, jsonb);
	}

	/**
	 * Utility method to initialize {@link JsonbTester} fields. See {@link JsonbTester
	 * class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param jsonb an object factory to create the Jsonb instance
	 */
	public static void initFields(Object testInstance, ObjectFactory<Jsonb> jsonb) {
		new JsonbTester.JsonbFieldInitializer().initFields(testInstance, jsonb);
	}

	/**
	 * {@link FieldInitializer} for Jsonb.
	 */
	private static class JsonbFieldInitializer extends FieldInitializer<Jsonb> {

		protected JsonbFieldInitializer() {
			super(JsonbTester.class);
		}

		@Override
		protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type,
				Jsonb marshaller) {
			return new JsonbTester<>(resourceLoadClass, type, marshaller);
		}

	}

}
