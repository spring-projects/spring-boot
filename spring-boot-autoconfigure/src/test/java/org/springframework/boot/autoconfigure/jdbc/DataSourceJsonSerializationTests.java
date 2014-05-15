/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test that a {@link DataSource} can be exposed as JSON for actuator endpoints.
 * 
 * @author Dave Syer
 */
public class DataSourceJsonSerializationTests {

	@Test
	public void serializerFactory() throws Exception {
		DataSource dataSource = new DataSource();
		SerializerFactory factory = BeanSerializerFactory.instance
				.withSerializerModifier(new GenericSerializerModifier());
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializerFactory(factory);
		String value = mapper.writeValueAsString(dataSource);
		assertTrue(value.contains("\"url\":"));
	}

	@Test
	public void serializerWithMixin() throws Exception {
		DataSource dataSource = new DataSource();
		ObjectMapper mapper = new ObjectMapper();
		mapper.addMixInAnnotations(DataSource.class, DataSourceJson.class);
		String value = mapper.writeValueAsString(dataSource);
		assertTrue(value.contains("\"url\":"));
		assertEquals(1, StringUtils.countOccurrencesOf(value, "\"url\""));
	}

	@JsonSerialize(using = TomcatDataSourceSerializer.class)
	protected static interface DataSourceJson {
	}

	protected static class TomcatDataSourceSerializer extends JsonSerializer<DataSource> {

		private ConversionService conversionService = new DefaultConversionService();

		@Override
		public void serialize(DataSource value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			for (PropertyDescriptor property : BeanUtils
					.getPropertyDescriptors(DataSource.class)) {
				Method reader = property.getReadMethod();
				if (reader != null
						&& property.getWriteMethod() != null
						&& this.conversionService.canConvert(String.class,
								property.getPropertyType())) {
					jgen.writeObjectField(property.getName(),
							ReflectionUtils.invokeMethod(reader, value));
				}
			}
			jgen.writeEndObject();
		}

	}

	protected static class GenericSerializerModifier extends BeanSerializerModifier {

		private ConversionService conversionService = new DefaultConversionService();

		@Override
		public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
				BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
			List<BeanPropertyWriter> result = new ArrayList<BeanPropertyWriter>();
			for (BeanPropertyWriter writer : beanProperties) {
				AnnotatedMethod setter = beanDesc.findMethod(
						"set" + StringUtils.capitalize(writer.getName()),
						new Class<?>[] { writer.getPropertyType() });
				if (setter != null
						&& this.conversionService.canConvert(String.class,
								writer.getPropertyType())) {
					result.add(writer);
				}
			}
			return result;
		}

	}

}
