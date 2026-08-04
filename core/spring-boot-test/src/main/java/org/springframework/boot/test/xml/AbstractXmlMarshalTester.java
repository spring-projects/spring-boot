/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.boot.test.json.ObjectContentAssert;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Base class for AssertJ based XML marshal testers. Exposes specific Asserts following a
 * {@code read}, {@code write} or {@code parse} of XML content. Typically used in
 * combination with an AssertJ {@link org.assertj.core.api.Assertions#assertThat(Object)
 * assertThat} call. For example:
 *
 * <pre class="code">
 * public class ExampleObjectXmlTests {
 *
 *     private AbstractXmlMarshalTester&lt;ExampleObject&gt; xml = //...
 *
 *     &#064;Test
 *     public void testWriteXml() {
 *         ExampleObject object = //...
 *         assertThat(xml.write(object)).isEqualToXml("expected.xml");
 *         assertThat(xml.read("expected.xml")).isEqualTo(object);
 *     }
 *
 * }
 * </pre>
 *
 * For a complete list of supported assertions see {@link XmlContentAssert} and
 * {@link ObjectContentAssert}.
 * <p>
 * To use this library XMLUnit must be on the test classpath.
 *
 * @param <T> the type under test
 * @author Tiziano Basile
 * @since 4.2.0
 * @see XmlContentAssert
 * @see ObjectContentAssert
 */
public abstract class AbstractXmlMarshalTester<T> {

	private @Nullable Class<?> resourceLoadClass;

	private @Nullable ResolvableType type;

	/**
	 * Create a new uninitialized {@link AbstractXmlMarshalTester} instance.
	 */
	protected AbstractXmlMarshalTester() {
	}

	/**
	 * Create a new {@link AbstractXmlMarshalTester} instance.
	 * @param resourceLoadClass the source class used when loading relative classpath
	 * resources
	 * @param type the type under test
	 */
	public AbstractXmlMarshalTester(Class<?> resourceLoadClass, ResolvableType type) {
		Assert.notNull(resourceLoadClass, "'resourceLoadClass' must not be null");
		Assert.notNull(type, "'type' must not be null");
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
	protected final @Nullable ResolvableType getType() {
		return this.type;
	}

	private ResolvableType getTypeNotNull() {
		ResolvableType type = getType();
		Assert.state(type != null, "Instance has not been initialized");
		return type;
	}

	/**
	 * Return class used to load relative resources.
	 * @return the resource load class
	 */
	protected final @Nullable Class<?> getResourceLoadClass() {
		return this.resourceLoadClass;
	}

	private Class<?> getResourceLoadClassNotNull() {
		Class<?> resourceLoadClass = getResourceLoadClass();
		Assert.state(resourceLoadClass != null, "Instance has not been initialized");
		return resourceLoadClass;
	}

	/**
	 * Return {@link XmlContent} from writing the specific value.
	 * @param value the value to write
	 * @return the {@link XmlContent}
	 * @throws IOException on write error
	 */
	public XmlContent<T> write(T value) throws IOException {
		verify();
		Assert.notNull(value, "'value' must not be null");
		String xml = writeObject(value, getTypeNotNull());
		return getXmlContent(xml);
	}

	/**
	 * Factory method used to get an {@link XmlContent} instance from a source XML string.
	 * @param xml the source XML
	 * @return a new {@link XmlContent} instance
	 */
	protected XmlContent<T> getXmlContent(String xml) {
		return new XmlContent<>(getResourceLoadClassNotNull(), getType(), xml);
	}

	/**
	 * Return the object created from parsing the specific XML bytes.
	 * @param xmlBytes the source XML bytes
	 * @return the resulting object
	 * @throws IOException on parse error
	 */
	public T parseObject(byte[] xmlBytes) throws IOException {
		verify();
		return parse(xmlBytes).getObject();
	}

	/**
	 * Return {@link ObjectContent} from parsing the specific XML bytes.
	 * @param xmlBytes the source XML bytes
	 * @return the {@link ObjectContent}
	 * @throws IOException on parse error
	 */
	public ObjectContent<T> parse(byte[] xmlBytes) throws IOException {
		verify();
		Assert.notNull(xmlBytes, "'xmlBytes' must not be null");
		return read(new ByteArrayResource(xmlBytes));
	}

	/**
	 * Return the object created from parsing the specific XML String.
	 * @param xmlString the source XML string
	 * @return the resulting object
	 * @throws IOException on parse error
	 */
	public T parseObject(String xmlString) throws IOException {
		verify();
		return parse(xmlString).getObject();
	}

	/**
	 * Return {@link ObjectContent} from parsing the specific XML String.
	 * @param xmlString the source XML string
	 * @return the {@link ObjectContent}
	 * @throws IOException on parse error
	 */
	public ObjectContent<T> parse(String xmlString) throws IOException {
		verify();
		Assert.notNull(xmlString, "'xmlString' must not be null");
		return read(new StringReader(xmlString));
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
		Assert.notNull(resourcePath, "'resourcePath' must not be null");
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
		Assert.notNull(file, "'file' must not be null");
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
		Assert.notNull(inputStream, "'inputStream' must not be null");
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
		Assert.notNull(resource, "'resource' must not be null");
		try (InputStream inputStream = resource.getInputStream()) {
			T object = readObject(inputStream, getTypeNotNull());
			return new ObjectContent<>(this.type, object);
		}
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
		Assert.notNull(reader, "'reader' must not be null");
		try (Reader source = reader) {
			T object = readObject(source, getTypeNotNull());
			return new ObjectContent<>(this.type, object);
		}
	}

	private void verify() {
		Assert.state(this.resourceLoadClass != null, "Uninitialized XmlMarshalTester (ResourceLoadClass is null)");
		Assert.state(this.type != null, "Uninitialized XmlMarshalTester (Type is null)");
	}

	/**
	 * Write the specified object to an XML string.
	 * @param value the source value (never {@code null})
	 * @param type the resulting type (never {@code null})
	 * @return the XML string
	 * @throws IOException on write error
	 */
	protected abstract String writeObject(T value, ResolvableType type) throws IOException;

	/**
	 * Read from the specified input stream to create an object of the specified type.
	 * Implementations must pass the bytes to the XML parser so that any encoding declared
	 * by the XML prolog is honored, rather than decoding them with a charset of their own
	 * choosing.
	 * @param inputStream the source input stream (never {@code null})
	 * @param type the resulting type (never {@code null})
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	protected abstract T readObject(InputStream inputStream, ResolvableType type) throws IOException;

	/**
	 * Read from the specified reader to create an object of the specified type.
	 * @param reader the source reader (never {@code null})
	 * @param type the resulting type (never {@code null})
	 * @return the resulting object
	 * @throws IOException on read error
	 */
	protected abstract T readObject(Reader reader, ResolvableType type) throws IOException;

	/**
	 * Utility class used to support field initialization. Used by subclasses to support
	 * {@code initFields}.
	 *
	 * @param <M> the marshaller type
	 */
	protected abstract static class FieldInitializer<M> {

		private final Class<?> testerClass;

		@SuppressWarnings("rawtypes")
		protected FieldInitializer(Class<? extends AbstractXmlMarshalTester> testerClass) {
			Assert.notNull(testerClass, "'testerClass' must not be null");
			this.testerClass = testerClass;
		}

		public void initFields(Object testInstance, M marshaller) {
			Assert.notNull(testInstance, "'testInstance' must not be null");
			Assert.notNull(marshaller, "'marshaller' must not be null");
			initFields(testInstance, () -> marshaller);
		}

		public void initFields(Object testInstance, final ObjectFactory<M> marshaller) {
			Assert.notNull(testInstance, "'testInstance' must not be null");
			Assert.notNull(marshaller, "'marshaller' must not be null");
			ReflectionUtils.doWithFields(testInstance.getClass(),
					(field) -> doWithField(field, testInstance, marshaller));
		}

		protected void doWithField(Field field, Object test, ObjectFactory<M> marshaller) {
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
			Assert.state(type.resolve() != null,
					() -> "Unable to determine the type under test for field '" + field.getName() + "' of "
							+ field.getDeclaringClass().getName() + ". Declare the field with an explicit generic "
							+ "type, for example '" + field.getType().getSimpleName() + "<MyType> " + field.getName()
							+ ";'.");
			ReflectionUtils.setField(field, test, createTester(test.getClass(), type, marshaller.getObject()));
		}

		protected abstract AbstractXmlMarshalTester<Object> createTester(Class<?> resourceLoadClass,
				ResolvableType type, M marshaller);

	}

}
