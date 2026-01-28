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

package org.springframework.boot.actuate.context.properties;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * {@link BeanSerializer} backed by Jackson 2.
 *
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
class Jackson2BeanSerializer implements BeanSerializer {

	private static final String CONFIGURATION_PROPERTIES_FILTER_ID = "configurationPropertiesFilter";

	private final ObjectMapper mapper;

	Jackson2BeanSerializer() {
		JsonMapper.Builder builder = JsonMapper.builder();
		configureMapper(builder);
		this.mapper = builder.build();
	}

	private void configureMapper(JsonMapper.Builder builder) {
		builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		builder.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		builder.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
		builder.configure(MapperFeature.USE_STD_BEAN_NAMING, true);
		builder.serializationInclusion(Include.NON_NULL);
		applyConfigurationPropertiesFilter(builder);
		applySerializationModifier(builder);
		if (ClassUtils.isPresent("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
				builder.getClass().getClassLoader())) {
			builder.addModule(new JavaTimeModule());
		}
		builder.addModule(new ConfigurationPropertiesModule());
	}

	private void applyConfigurationPropertiesFilter(JsonMapper.Builder builder) {
		builder.annotationIntrospector(new ConfigurationPropertiesAnnotationIntrospector());
		ConfigurationPropertiesPropertyFilter filter = new ConfigurationPropertiesPropertyFilter();
		builder.filterProvider(new SimpleFilterProvider().setDefaultFilter(filter));
	}

	/**
	 * Ensure only bindable and non-cyclic bean properties are reported.
	 * @param builder the JsonMapper builder
	 */
	private void applySerializationModifier(JsonMapper.Builder builder) {
		SerializerFactory factory = BeanSerializerFactory.instance
			.withSerializerModifier(new GenericSerializerModifier());
		builder.serializerFactory(factory);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, @Nullable Object> serialize(@Nullable Object bean) {
		return this.mapper.convertValue(bean, Map.class);
	}

	/**
	 * Extension to {@link JacksonAnnotationIntrospector} to suppress CGLIB generated bean
	 * properties.
	 */
	private static final class ConfigurationPropertiesAnnotationIntrospector extends JacksonAnnotationIntrospector {

		@Override
		public Object findFilterId(Annotated a) {
			Object id = super.findFilterId(a);
			return (id != null) ? id : CONFIGURATION_PROPERTIES_FILTER_ID;
		}

	}

	/**
	 * {@link SimpleBeanPropertyFilter} for serialization of
	 * {@link ConfigurationProperties @ConfigurationProperties} beans. The filter hides:
	 *
	 * <ul>
	 * <li>Properties that have a name starting with '$$'.
	 * <li>Properties that are self-referential.
	 * <li>Properties that throw an exception when retrieving their value.
	 * </ul>
	 */
	private static final class ConfigurationPropertiesPropertyFilter extends SimpleBeanPropertyFilter {

		private static final Log logger = LogFactory.getLog(ConfigurationPropertiesPropertyFilter.class);

		@Override
		protected boolean include(BeanPropertyWriter writer) {
			return include(writer.getFullName().getSimpleName());
		}

		@Override
		protected boolean include(PropertyWriter writer) {
			return include(writer.getFullName().getSimpleName());
		}

		private boolean include(String name) {
			return !name.startsWith("$$");
		}

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
				PropertyWriter writer) throws Exception {
			if (writer instanceof BeanPropertyWriter beanPropertyWriter) {
				try {
					if (pojo == beanPropertyWriter.get(pojo)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Skipping '" + writer.getFullName() + "' on '" + pojo.getClass().getName()
									+ "' as it is self-referential");
						}
						return;
					}
				}
				catch (Exception ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping '" + writer.getFullName() + "' on '" + pojo.getClass().getName()
								+ "' as an exception was thrown when retrieving its value", ex);
					}
					return;
				}
			}
			super.serializeAsField(pojo, jgen, provider, writer);
		}

	}

	/**
	 * {@link SimpleModule} for configuring the serializer.
	 */
	private static final class ConfigurationPropertiesModule extends SimpleModule {

		private ConfigurationPropertiesModule() {
			addSerializer(DataSize.class, ToStringSerializer.instance);
		}

	}

	/**
	 * {@link BeanSerializerModifier} to return only relevant configuration properties.
	 */
	protected static class GenericSerializerModifier extends BeanSerializerModifier {

		private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

		@Override
		public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
				List<BeanPropertyWriter> beanProperties) {
			List<BeanPropertyWriter> result = new ArrayList<>();
			Class<?> beanClass = beanDesc.getType().getRawClass();
			Bindable<?> bindable = Bindable.of(ClassUtils.getUserClass(beanClass));
			Constructor<?> bindConstructor = BindConstructorProvider.DEFAULT.getBindConstructor(bindable, false);
			for (BeanPropertyWriter writer : beanProperties) {
				if (isCandidate(beanDesc, writer, bindConstructor)) {
					result.add(writer);
				}
			}
			return result;
		}

		private boolean isCandidate(BeanDescription beanDesc, BeanPropertyWriter writer,
				@Nullable Constructor<?> constructor) {
			if (constructor != null) {
				Parameter[] parameters = constructor.getParameters();
				@Nullable String @Nullable [] names = parameterNameDiscoverer.getParameterNames(constructor);
				if (names == null) {
					names = new String[parameters.length];
				}
				for (int i = 0; i < parameters.length; i++) {
					String name = MergedAnnotations.from(parameters[i])
						.get(Name.class)
						.getValue(MergedAnnotation.VALUE, String.class)
						.orElse((names[i] != null) ? names[i] : parameters[i].getName());
					if (name.equals(writer.getName())) {
						return true;
					}
				}
			}
			return isReadable(beanDesc, writer);
		}

		private boolean isReadable(BeanDescription beanDesc, BeanPropertyWriter writer) {
			Class<?> parentType = beanDesc.getType().getRawClass();
			Class<?> type = writer.getType().getRawClass();
			AnnotatedMethod setter = findSetter(beanDesc, writer);
			// If there's a setter, we assume it's OK to report on the value,
			// similarly, if there's no setter but the package names match, we assume
			// that it is a nested class used solely for binding to config props, so it
			// should be kosher. Lists and Maps are also auto-detected by default since
			// that's what the metadata generator does. This filter is not used if there
			// is JSON metadata for the property, so it's mainly for user-defined beans.
			return (setter != null) || ClassUtils.getPackageName(parentType).equals(ClassUtils.getPackageName(type))
					|| Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
		}

		private @Nullable AnnotatedMethod findSetter(BeanDescription beanDesc, BeanPropertyWriter writer) {
			String name = "set" + determineAccessorSuffix(writer.getName());
			Class<?> type = writer.getType().getRawClass();
			AnnotatedMethod setter = beanDesc.findMethod(name, new Class<?>[] { type });
			// The enabled property of endpoints returns a boolean primitive but is set
			// using a Boolean class
			if (setter == null && type.equals(Boolean.TYPE)) {
				setter = beanDesc.findMethod(name, new Class<?>[] { Boolean.class });
			}
			return setter;
		}

		/**
		 * Determine the accessor suffix of the specified {@code propertyName}, see
		 * section 8.8 "Capitalization of inferred names" of the JavaBean specs for more
		 * details.
		 * @param propertyName the property name to turn into an accessor suffix
		 * @return the accessor suffix for {@code propertyName}
		 */
		private String determineAccessorSuffix(String propertyName) {
			if (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(1))) {
				return propertyName;
			}
			return StringUtils.capitalize(propertyName);
		}

	}

}
