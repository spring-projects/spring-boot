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

import com.google.gson.Gson;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * AssertJ based JSON tester backed by Gson. Usually instantiated via
 * {@link #initFields(Object, Gson)}, for example: <pre class="code">
 * public class ExampleObjectJsonTests {
 *
 *     private GsonTester&lt;ExampleObject&gt; json;
 *
 *     &#064;Before
 *     public void setup() {
 *         Gson gson = new GsonBuilder().create();
 *         GsonTester.initFields(this, gson);
 *     }
 *
 *     &#064;Test
 *     public void testWriteJson() throws IOException {
 *         ExampleObject object = //...
 *         assertThat(json.write(object)).isEqualToJson("expected.json");
 *     }
 *
 * }
 * </pre>
 *
 * See {@link AbstractJsonMarshalTester} for more details.
 *
 * @param <T> the type under test
 * @author Phillip Webb
 * @since 1.4.0
 */
public class GsonTester<T> extends AbstractJsonMarshalTester<T> {

	private final Gson gson;

	/**
	 * Create a new uninitialized {@link GsonTester} instance.
	 * @param gson the Gson instance
	 */
	protected GsonTester(Gson gson) {
		Assert.notNull(gson, "Gson must not be null");
		this.gson = gson;
	}

	/**
	 * Create a new {@link GsonTester} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test
	 * @param gson the Gson instance
	 * @see #initFields(Object, Gson)
	 */
	public GsonTester(Class<?> resourceLoadClass, ResolvableType type, Gson gson) {
		super(resourceLoadClass, type);
		Assert.notNull(gson, "Gson must not be null");
		this.gson = gson;
	}

	/**
     * Writes the given object to JSON using the Gson library.
     * 
     * @param value the object to be written to JSON
     * @param type the ResolvableType representing the type of the object
     * @return a JSON string representation of the object
     * @throws IOException if an I/O error occurs while writing the object to JSON
     */
    @Override
	protected String writeObject(T value, ResolvableType type) throws IOException {
		return this.gson.toJson(value, type.getType());
	}

	/**
     * Reads an object from a reader using Gson library.
     * 
     * @param reader the reader to read the object from
     * @param type the type of the object to be read
     * @return the object read from the reader
     * @throws IOException if an I/O error occurs while reading from the reader
     */
    @Override
	protected T readObject(Reader reader, ResolvableType type) throws IOException {
		return this.gson.fromJson(reader, type.getType());
	}

	/**
	 * Utility method to initialize {@link GsonTester} fields. See {@link GsonTester
	 * class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param gson the Gson instance
	 */
	public static void initFields(Object testInstance, Gson gson) {
		new GsonFieldInitializer().initFields(testInstance, gson);
	}

	/**
	 * Utility method to initialize {@link GsonTester} fields. See {@link GsonTester
	 * class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param gson an object factory to create the Gson instance
	 */
	public static void initFields(Object testInstance, ObjectFactory<Gson> gson) {
		new GsonFieldInitializer().initFields(testInstance, gson);
	}

	/**
	 * {@link FieldInitializer} for Gson.
	 */
	private static class GsonFieldInitializer extends FieldInitializer<Gson> {

		/**
         * Creates a new instance of the GsonFieldInitializer class.
         * This constructor calls the constructor of the superclass, passing the GsonTester class as a parameter.
         */
        protected GsonFieldInitializer() {
			super(GsonTester.class);
		}

		/**
         * Creates a tester for JSON marshalling using Gson.
         * 
         * @param resourceLoadClass the class used for resource loading
         * @param type the ResolvableType representing the type to be marshalled
         * @param marshaller the Gson marshaller
         * @return the created GsonTester
         */
        @Override
		protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type,
				Gson marshaller) {
			return new GsonTester<>(resourceLoadClass, type, marshaller);
		}

	}

}
