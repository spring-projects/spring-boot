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

package org.springframework.boot.test.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.AbstractJsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingException;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * AssertJ based JSON tester backed by Jackson. Usually instantiated via
 * {@link #initFields(Object, JsonMapper)}, for example: <pre class="code">
 * public class ExampleObjectJsonTests {
 *
 *     private JacksonTester&lt;ExampleObject&gt; json;
 *
 *     &#064;Before
 *     public void setup() {
 *         JsonMapper jsonMapper = new JsonMapper();
 *         JacksonTester.initFields(this, jsonMapper);
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
 * @author Madhura Bhave
 * @author Diego Berrueta
 * @since 1.4.0
 */
public class JacksonTester<T> extends AbstractJsonMarshalTester<T> {

	private final JsonMapper jsonMapper;

	private @Nullable Class<?> view;

	/**
	 * Create a new {@link JacksonTester} instance.
	 * @param jsonMapper the Jackson JSON mapper
	 * @since 4.0.0
	 */
	protected JacksonTester(JsonMapper jsonMapper) {
		Assert.notNull(jsonMapper, "'jsonMapper' must not be null");
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Create a new {@link JacksonTester} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test
	 * @param jsonMapper the Jackson JSON mapper
	 * @since 4.0.0
	 */
	public JacksonTester(Class<?> resourceLoadClass, ResolvableType type, JsonMapper jsonMapper) {
		this(resourceLoadClass, type, jsonMapper, null);
	}

	/**
	 * Create a new {@link JacksonTester} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test
	 * @param jsonMapper the Jackson JSON mapper
	 * @param view the JSON view
	 * @since 4.0.0
	 */
	public JacksonTester(Class<?> resourceLoadClass, ResolvableType type, JsonMapper jsonMapper,
			@Nullable Class<?> view) {
		super(resourceLoadClass, type);
		Assert.notNull(jsonMapper, "'jsonMapper' must not be null");
		this.jsonMapper = jsonMapper;
		this.view = view;
	}

	@Override
	protected JsonContent<T> getJsonContent(String json) {
		Configuration configuration = Configuration.builder()
			.jsonProvider(new JacksonJsonProvider(this.jsonMapper))
			.mappingProvider(new JacksonMappingProvider(this.jsonMapper))
			.build();
		Class<?> resourceLoadClass = getResourceLoadClass();
		Assert.state(resourceLoadClass != null, "'resourceLoadClass' must not be null");
		return new JsonContent<>(resourceLoadClass, getType(), json, configuration);
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
		ObjectReader objectReader = this.jsonMapper.readerFor(getType(type));
		if (this.view != null) {
			return objectReader.withView(this.view);
		}
		return objectReader;
	}

	@Override
	protected String writeObject(T value, ResolvableType type) throws IOException {
		return getObjectWriter(type).writeValueAsString(value);
	}

	private ObjectWriter getObjectWriter(ResolvableType type) {
		ObjectWriter objectWriter = this.jsonMapper.writerFor(getType(type));
		if (this.view != null) {
			return objectWriter.withView(this.view);
		}
		return objectWriter;
	}

	private JavaType getType(ResolvableType type) {
		return this.jsonMapper.constructType(type.getType());
	}

	/**
	 * Utility method to initialize {@link JacksonTester} fields. See {@link JacksonTester
	 * class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param jsonMapper the JSON mapper
	 * @since 4.0.0
	 * @see #initFields(Object, JsonMapper)
	 */
	public static void initFields(Object testInstance, JsonMapper jsonMapper) {
		new JacksonFieldInitializer().initFields(testInstance, jsonMapper);
	}

	/**
	 * Utility method to initialize {@link JacksonTester} fields. See {@link JacksonTester
	 * class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param jsonMapperFactory a factory to create the JSON mapper
	 * @since 4.0.0
	 * @see #initFields(Object, JsonMapper)
	 */
	public static void initFields(Object testInstance, ObjectFactory<JsonMapper> jsonMapperFactory) {
		new JacksonFieldInitializer().initFields(testInstance, jsonMapperFactory);
	}

	/**
	 * Returns a new instance of {@link JacksonTester} with the view that should be used
	 * for json serialization/deserialization.
	 * @param view the view class
	 * @return the new instance
	 */
	public JacksonTester<T> forView(Class<?> view) {
		Class<?> resourceLoadClass = getResourceLoadClass();
		ResolvableType type = getType();
		Assert.state(resourceLoadClass != null, "'resourceLoadClass' must not be null");
		Assert.state(type != null, "'type' must not be null");
		return new JacksonTester<>(resourceLoadClass, type, this.jsonMapper, view);
	}

	/**
	 * {@link FieldInitializer} for Jackson.
	 */
	private static class JacksonFieldInitializer extends FieldInitializer<JsonMapper> {

		protected JacksonFieldInitializer() {
			super(JacksonTester.class);
		}

		@Override
		protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type,
				JsonMapper marshaller) {
			return new JacksonTester<>(resourceLoadClass, type, marshaller);
		}

	}

	private static final class JacksonJsonProvider extends AbstractJsonProvider {

		private final JsonMapper jsonMapper;

		private final ObjectReader objectReader;

		private JacksonJsonProvider(JsonMapper jsonMapper) {
			this.jsonMapper = jsonMapper;
			this.objectReader = jsonMapper.reader().forType(Object.class);
		}

		@Override
		public Object parse(String json) throws InvalidJsonException {
			try {
				return this.objectReader.readValue(json);
			}
			catch (JacksonException ex) {
				throw new InvalidJsonException(ex, json);
			}
		}

		@Override
		public Object parse(byte[] json) throws InvalidJsonException {
			try {
				return this.objectReader.readValue(json);
			}
			catch (JacksonException ex) {
				throw new InvalidJsonException(ex, new String(json, StandardCharsets.UTF_8));
			}
		}

		@Override
		public Object parse(InputStream jsonStream, String charset) throws InvalidJsonException {
			try {
				return this.objectReader.readValue(new InputStreamReader(jsonStream, charset));
			}
			catch (UnsupportedEncodingException | JacksonException ex) {
				throw new InvalidJsonException(ex);
			}
		}

		@Override
		public String toJson(Object obj) {
			StringWriter writer = new StringWriter();
			try (JsonGenerator generator = this.jsonMapper.createGenerator(writer)) {
				this.jsonMapper.writeValue(generator, obj);
			}
			catch (JacksonException ex) {
				throw new InvalidJsonException(ex);
			}
			return writer.toString();
		}

		@Override
		public List<Object> createArray() {
			return new LinkedList<>();
		}

		@Override
		public Object createMap() {
			return new LinkedHashMap<String, Object>();
		}

	}

	private static final class JacksonMappingProvider implements MappingProvider {

		private final JsonMapper jsonMapper;

		private JacksonMappingProvider(JsonMapper jsonMapper) {
			this.jsonMapper = jsonMapper;
		}

		@Override
		public <T> @Nullable T map(Object source, Class<T> targetType, Configuration configuration) {
			if (source == null) {
				return null;
			}
			try {
				return this.jsonMapper.convertValue(source, targetType);
			}
			catch (Exception ex) {
				throw new MappingException(ex);
			}

		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> @Nullable T map(Object source, final TypeRef<T> targetType, Configuration configuration) {
			if (source == null) {
				return null;
			}
			JavaType type = this.jsonMapper.getTypeFactory().constructType(targetType.getType());
			try {
				return (T) this.jsonMapper.convertValue(source, type);
			}
			catch (Exception ex) {
				throw new MappingException(ex);
			}

		}

	}

}
