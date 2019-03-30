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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;

import org.assertj.core.api.Assertions;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Base class for AssertJ based JSON marshal testers. Exposes specific Asserts following a
 * {@code read}, {@code write} or {@code parse} of JSON content. Typically used in
 * combination with an AssertJ {@link Assertions#assertThat(Object) assertThat} call. For
 * example: <pre class="code">
 * public class ExampleObjectJsonTests {
 *
 *     private AbstractJsonTester&lt;ExampleObject&gt; json = //...
 *
 *     &#064;Test
 *     public void testWriteJson() {
 *         ExampleObject object = //...
 *         assertThat(json.write(object)).isEqualToJson("expected.json");
 *         assertThat(json.read("expected.json")).isEqualTo(object);
 *     }
 *
 * }
 * </pre> For a complete list of supported assertions see {@link JsonContentAssert} and
 * {@link ObjectContentAssert}.
 * <p>
 * To use this library JSONAssert must be on the test classpath.
 *
 * @param <T> the type under test
 * @author Phillip Webb
 * @since 1.4.0
 * @see JsonContentAssert
 * @see ObjectContentAssert
 */
public abstract class AbstractJsonMarshalTester<T> {

	private Class<?> resourceLoadClass;

	private ResolvableType type;

	/**
	 * Create a new uninitialized {@link AbstractJsonMarshalTester} instance.
	 */
	protected AbstractJsonMarshalTester() {
	}

	/**
	 * Create a new {@link AbstractJsonMarshalTester} instance.
	 * @param resourceLoadClass the source class used when loading relative classpath
	 * resources
	 * @param type the type under test
	 */
	public AbstractJsonMarshalTester(Class<?> resourceLoadClass, ResolvableType type) {
		Assert.notNull(resourceLoadClass, "ResourceLoadClass must not be null");
		Assert.notNull(type, "Type must not be null");
		initialize(resourceLoadClass, type);
	}

	/**
	 * Initialize the marshal tester for use.
	 * @param resourceLoadClass the source class used when loading relative classpath
	 * resources
	 * @param type the type under test
	 */
	protected final void initialize(Class<?> resourceLoadClass, ResolvableType type) {
		if (this.resourceLoadClass == null && this.type == null) {
			this.resourceLoadClass = resourceLoadClass;
			this.type = type;
		}
	}

	/**
	 * Return the type under test.
	 * @return the type under test
	 */
	protected final ResolvableType getType() {
		return this.type;
	}

	/**
	 * Return class used to load relative resources.
	 * @return the resource load class
	 */
	protected final Class<?> getResourceLoadClass() {
		return this.resourceLoadClass;
	}

	/**
	 * Return {@link JsonContent} from writing the specific value.
	 * @param value the value to write
	 * @return the {@link JsonContent}
	 * @throws IOException on write error
	 */
	public JsonContent<T> write(T value) throws IOException {
		verify();
		Assert.notNull(value, "Value must not be null");
		String json = writeObject(value, this.type);
		return new JsonContent<>(this.resourceLoadClass, this.type, json);
	}

	/**
	 * Return the object created from parsing the specific JSON bytes.
	 * @param jsonBytes the source JSON bytes
	 * @return the resulting object
	 * @throws IOException on parse error
	 */
	public T parseObject(byte[] jsonBytes) throws IOException {
		verify();
		return parse(jsonBytes).getObject();
	}

	/**
	 * Return {@link ObjectContent} from parsing the specific JSON bytes.
	 * @param jsonBytes the source JSON bytes
	 * @return the {@link ObjectContent}
	 * @throws IOException on parse error
	 */
	public ObjectContent<T> parse(byte[] jsonBytes) throws IOException {
		verify();
		Assert.notNull(jsonBytes, "JsonBytes must not be null");
		return read(new ByteArrayResource(jsonBytes));
	}

	/**
	 * Return the object created from parsing the specific JSON String.
	 * @param jsonString the source JSON string
	 * @return the resulting object
	 * @throws IOException on parse error
	 */
	public T parseObject(String jsonString) throws IOException {
		verify();
		return parse(jsonString).getObject();
	}

	/**
	 * Return {@link ObjectContent} from parsing the specific JSON String.
	 * @param jsonString the source JSON string
	 * @return the {@link ObjectContent}
	 * @throws IOException on parse error
	 */
	public ObjectContent<T> parse(String jsonString) throws IOException {
		verify();
		Assert.notNull(jsonString, "JsonString must not be null");
		return read(new StringReader(jsonString));
	}

	/**
	 * Return the object created from reading from the specified classpath resource.
	 * @param resourcePath the source resource path. May be a full path or a path relative
	 * to the {@code resourceLoadClass} passed to the constructor
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	public T readObject(String resourcePath) throws IOException {
		verify();
		return read(resourcePath).getObject();
	}

	/**
	 * Return {@link ObjectContent} from reading from the specified classpath resource.
	 * @param resourcePath the source resource path. May be a full path or a path relative
	 * to the {@code resourceLoadClass} passed to the constructor
	 * @return the {@link ObjectContent}
	 * @throws IOException on read error
	 */
	public ObjectContent<T> read(String resourcePath) throws IOException {
		verify();
		Assert.notNull(resourcePath, "ResourcePath must not be null");
		return read(new ClassPathResource(resourcePath, this.resourceLoadClass));
	}

	/**
	 * Return the object created from reading from the specified file.
	 * @param file the source file
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	public T readObject(File file) throws IOException {
		verify();
		return read(file).getObject();
	}

	/**
	 * Return {@link ObjectContent} from reading from the specified file.
	 * @param file the source file
	 * @return the {@link ObjectContent}
	 * @throws IOException on read error
	 */
	public ObjectContent<T> read(File file) throws IOException {
		verify();
		Assert.notNull(file, "File must not be null");
		return read(new FileSystemResource(file));
	}

	/**
	 * Return the object created from reading from the specified input stream.
	 * @param inputStream the source input stream
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	public T readObject(InputStream inputStream) throws IOException {
		verify();
		return read(inputStream).getObject();
	}

	/**
	 * Return {@link ObjectContent} from reading from the specified input stream.
	 * @param inputStream the source input stream
	 * @return the {@link ObjectContent}
	 * @throws IOException on read error
	 */
	public ObjectContent<T> read(InputStream inputStream) throws IOException {
		verify();
		Assert.notNull(inputStream, "InputStream must not be null");
		return read(new InputStreamResource(inputStream));
	}

	/**
	 * Return the object created from reading from the specified resource.
	 * @param resource the source resource
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	public T readObject(Resource resource) throws IOException {
		verify();
		return read(resource).getObject();
	}

	/**
	 * Return {@link ObjectContent} from reading from the specified resource.
	 * @param resource the source resource
	 * @return the {@link ObjectContent}
	 * @throws IOException on read error
	 */
	public ObjectContent<T> read(Resource resource) throws IOException {
		verify();
		Assert.notNull(resource, "Resource must not be null");
		InputStream inputStream = resource.getInputStream();
		T object = readObject(inputStream, this.type);
		closeQuietly(inputStream);
		return new ObjectContent<>(this.type, object);
	}

	/**
	 * Return the object created from reading from the specified reader.
	 * @param reader the source reader
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	public T readObject(Reader reader) throws IOException {
		verify();
		return read(reader).getObject();
	}

	/**
	 * Return {@link ObjectContent} from reading from the specified reader.
	 * @param reader the source reader
	 * @return the {@link ObjectContent}
	 * @throws IOException on read error
	 */
	public ObjectContent<T> read(Reader reader) throws IOException {
		verify();
		Assert.notNull(reader, "Reader must not be null");
		T object = readObject(reader, this.type);
		closeQuietly(reader);
		return new ObjectContent<>(this.type, object);
	}

	private void closeQuietly(Closeable closeable) {
		try {
			closeable.close();
		}
		catch (IOException ex) {
		}
	}

	private void verify() {
		Assert.state(this.resourceLoadClass != null,
				"Uninitialized JsonMarshalTester (ResourceLoadClass is null)");
		Assert.state(this.type != null, "Uninitialized JsonMarshalTester (Type is null)");
	}

	/**
	 * Write the specified object to a JSON string.
	 * @param value the source value (never {@code null})
	 * @param type the resulting type (never {@code null})
	 * @return the JSON string
	 * @throws IOException on write error
	 */
	protected abstract String writeObject(T value, ResolvableType type)
			throws IOException;

	/**
	 * Read from the specified input stream to create an object of the specified type. The
	 * default implementation delegates to {@link #readObject(Reader, ResolvableType)}.
	 * @param inputStream the source input stream (never {@code null})
	 * @param type the resulting type (never {@code null})
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	protected T readObject(InputStream inputStream, ResolvableType type)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		return readObject(reader, type);
	}

	/**
	 * Read from the specified reader to create an object of the specified type.
	 * @param reader the source reader (never {@code null})
	 * @param type the resulting type (never {@code null})
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	protected abstract T readObject(Reader reader, ResolvableType type)
			throws IOException;

	/**
	 * Utility class used to support field initialization. Used by subclasses to support
	 * {@code initFields}.
	 *
	 * @param <M> the marshaller type
	 */
	protected abstract static class FieldInitializer<M> {

		private final Class<?> testerClass;

		@SuppressWarnings("rawtypes")
		protected FieldInitializer(
				Class<? extends AbstractJsonMarshalTester> testerClass) {
			Assert.notNull(testerClass, "TesterClass must not be null");
			this.testerClass = testerClass;
		}

		public void initFields(Object testInstance, M marshaller) {
			Assert.notNull(testInstance, "TestInstance must not be null");
			Assert.notNull(marshaller, "Marshaller must not be null");
			initFields(testInstance, () -> marshaller);
		}

		public void initFields(Object testInstance, final ObjectFactory<M> marshaller) {
			Assert.notNull(testInstance, "TestInstance must not be null");
			Assert.notNull(marshaller, "Marshaller must not be null");
			ReflectionUtils.doWithFields(testInstance.getClass(),
					(field) -> doWithField(field, testInstance, marshaller));
		}

		protected void doWithField(Field field, Object test,
				ObjectFactory<M> marshaller) {
			if (this.testerClass.isAssignableFrom(field.getType())) {
				ReflectionUtils.makeAccessible(field);
				Object existingValue = ReflectionUtils.getField(field, test);
				if (existingValue == null) {
					setupField(field, test, marshaller);
				}
			}
		}

		private void setupField(Field field, Object test, ObjectFactory<M> marshaller) {
			ResolvableType type = ResolvableType.forField(field).getGeneric();
			ReflectionUtils.setField(field, test,
					createTester(test.getClass(), type, marshaller.getObject()));
		}

		protected abstract AbstractJsonMarshalTester<Object> createTester(
				Class<?> resourceLoadClass, ResolvableType type, M marshaller);

	}

}
