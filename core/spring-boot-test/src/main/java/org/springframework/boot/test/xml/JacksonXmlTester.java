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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * AssertJ based XML tester backed by Jackson. Usually instantiated via
 * {@link #initFields(Object, XmlMapper)}, for example: <pre class="code">
 * public class ExampleObjectXmlTests {
 *
 *     private JacksonXmlTester&lt;ExampleObject&gt; xml;
 *
 *     &#064;BeforeEach
 *     public void setup() {
 *         XmlMapper xmlMapper = new XmlMapper();
 *         JacksonXmlTester.initFields(this, xmlMapper);
 *     }
 *
 *     &#064;Test
 *     public void testWriteXml() throws IOException {
 *         ExampleObject object = //...
 *         assertThat(xml.write(object)).isEqualToXml("expected.xml");
 *     }
 *
 * }
 * </pre>
 *
 * See {@link AbstractXmlMarshalTester} for more details.
 *
 * @param <T> the type under test
 * @author Tiziano Basile
 * @since 4.2.0
 */
public class JacksonXmlTester<T> extends AbstractXmlMarshalTester<T> {

	private final XmlMapper xmlMapper;

	/**
	 * Create a new uninitialized {@link JacksonXmlTester} instance.
	 * @param xmlMapper the Jackson XML mapper
	 */
	protected JacksonXmlTester(XmlMapper xmlMapper) {
		Assert.notNull(xmlMapper, "'xmlMapper' must not be null");
		this.xmlMapper = xmlMapper;
	}

	/**
	 * Create a new {@link JacksonXmlTester} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test
	 * @param xmlMapper the Jackson XML mapper
	 */
	public JacksonXmlTester(Class<?> resourceLoadClass, ResolvableType type, XmlMapper xmlMapper) {
		super(resourceLoadClass, type);
		Assert.notNull(xmlMapper, "'xmlMapper' must not be null");
		this.xmlMapper = xmlMapper;
	}

	@Override
	protected T readObject(InputStream inputStream, ResolvableType type) throws IOException {
		return getObjectReader(type).readValue(inputStream);
	}

	@Override
	protected T readObject(Reader reader, ResolvableType type) throws IOException {
		return getObjectReader(type).readValue(reader);
	}

	private ObjectReader getObjectReader(ResolvableType type) {
		return this.xmlMapper.readerFor(getType(type));
	}

	@Override
	protected String writeObject(T value, ResolvableType type) throws IOException {
		return getObjectWriter(type).writeValueAsString(value);
	}

	private ObjectWriter getObjectWriter(ResolvableType type) {
		return this.xmlMapper.writerFor(getType(type));
	}

	private JavaType getType(ResolvableType type) {
		return this.xmlMapper.constructType(type.getType());
	}

	/**
	 * Utility method to initialize {@link JacksonXmlTester} fields. See
	 * {@link JacksonXmlTester class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param xmlMapper the XML mapper
	 * @see #initFields(Object, ObjectFactory)
	 */
	public static void initFields(Object testInstance, XmlMapper xmlMapper) {
		new JacksonXmlFieldInitializer().initFields(testInstance, xmlMapper);
	}

	/**
	 * Utility method to initialize {@link JacksonXmlTester} fields. See
	 * {@link JacksonXmlTester class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param xmlMapperFactory a factory to create the XML mapper
	 * @see #initFields(Object, XmlMapper)
	 */
	public static void initFields(Object testInstance, ObjectFactory<XmlMapper> xmlMapperFactory) {
		new JacksonXmlFieldInitializer().initFields(testInstance, xmlMapperFactory);
	}

	/**
	 * {@link FieldInitializer} for Jackson XML.
	 */
	private static class JacksonXmlFieldInitializer extends FieldInitializer<XmlMapper> {

		protected JacksonXmlFieldInitializer() {
			super(JacksonXmlTester.class);
		}

		@Override
		protected AbstractXmlMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type,
				XmlMapper marshaller) {
			return new JacksonXmlTester<>(resourceLoadClass, type, marshaller);
		}

	}

}
