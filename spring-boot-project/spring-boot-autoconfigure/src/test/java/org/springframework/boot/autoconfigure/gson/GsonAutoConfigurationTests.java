/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.gson;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import org.joda.time.DateTime;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GsonAutoConfiguration}.
 *
 * @author David Liu
 * @author Ivan Golovko
 * @author Stephane Nicoll
 */
public class GsonAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GsonAutoConfiguration.class));

	@Test
	public void gsonRegistration() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
		});
	}

	@Test
	public void generateNonExecutableJson() {
		this.contextRunner
				.withPropertyValues("spring.gson.generate-non-executable-json:true")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.toJson(new DataObject()))
							.isNotEqualTo("{\"data\":1}");
					assertThat(gson.toJson(new DataObject())).endsWith("{\"data\":1}");
				});
	}

	@Test
	public void excludeFieldsWithoutExposeAnnotation() {
		this.contextRunner
				.withPropertyValues(
						"spring.gson.exclude-fields-without-expose-annotation:true")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.toJson(new DataObject())).isEqualTo("{}");
				});
	}

	@Test
	public void serializeNulls() {
		this.contextRunner.withPropertyValues("spring.gson.serialize-nulls:true")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.serializeNulls()).isTrue();
				});
	}

	@Test
	public void enableComplexMapKeySerialization() {
		this.contextRunner
				.withPropertyValues(
						"spring.gson.enable-complex-map-key-serialization:true")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					Map<DataObject, String> original = new LinkedHashMap<>();
					original.put(new DataObject(), "a");
					assertThat(gson.toJson(original)).isEqualTo("[[{\"data\":1},\"a\"]]");
				});
	}

	@Test
	public void notDisableInnerClassSerialization() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			WrapperObject wrapperObject = new WrapperObject();
			assertThat(gson.toJson(wrapperObject.new NestedObject()))
					.isEqualTo("{\"data\":\"nested\"}");
		});
	}

	@Test
	public void disableInnerClassSerialization() {
		this.contextRunner
				.withPropertyValues("spring.gson.disable-inner-class-serialization:true")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					WrapperObject wrapperObject = new WrapperObject();
					assertThat(gson.toJson(wrapperObject.new NestedObject()))
							.isEqualTo("null");
				});
	}

	@Test
	public void withLongSerializationPolicy() {
		this.contextRunner.withPropertyValues(
				"spring.gson.long-serialization-policy:" + LongSerializationPolicy.STRING)
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.toJson(new DataObject()))
							.isEqualTo("{\"data\":\"1\"}");
				});
	}

	@Test
	public void withFieldNamingPolicy() {
		FieldNamingPolicy fieldNamingPolicy = FieldNamingPolicy.UPPER_CAMEL_CASE;
		this.contextRunner
				.withPropertyValues(
						"spring.gson.field-naming-policy:" + fieldNamingPolicy)
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.fieldNamingStrategy()).isEqualTo(fieldNamingPolicy);
				});
	}

	@Test
	public void additionalGsonBuilderCustomization() {
		this.contextRunner.withUserConfiguration(GsonBuilderCustomizerConfig.class)
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.toJson(new DataObject())).isEqualTo("{}");
				});
	}

	@Test
	public void customGsonBuilder() {
		this.contextRunner.withUserConfiguration(GsonBuilderConfig.class)
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.toJson(new DataObject()))
							.isEqualTo("{\"data\":1,\"owner\":null}");
				});
	}

	@Test
	public void withPrettyPrinting() {
		this.contextRunner.withPropertyValues("spring.gson.pretty-printing:true")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.toJson(new DataObject()))
							.isEqualTo("{\n  \"data\": 1\n}");
				});
	}

	@Test
	public void withoutLenient() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson).hasFieldOrPropertyWithValue("lenient", false);
		});
	}

	@Test
	public void withLenient() {
		this.contextRunner.withPropertyValues("spring.gson.lenient:true")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson).hasFieldOrPropertyWithValue("lenient", true);
				});
	}

	@Test
	public void withHtmlEscaping() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.htmlSafe()).isTrue();
		});
	}

	@Test
	public void withoutHtmlEscaping() {
		this.contextRunner.withPropertyValues("spring.gson.disable-html-escaping:true")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					assertThat(gson.htmlSafe()).isFalse();
				});

	}

	@Test
	public void customDateFormat() {
		this.contextRunner.withPropertyValues("spring.gson.date-format:H")
				.run((context) -> {
					Gson gson = context.getBean(Gson.class);
					DateTime dateTime = new DateTime(1988, 6, 25, 20, 30);
					Date date = dateTime.toDate();
					assertThat(gson.toJson(date)).isEqualTo("\"20\"");
				});
	}

	@Configuration
	static class GsonBuilderCustomizerConfig {

		@Bean
		public GsonBuilderCustomizer customSerializationExclusionStrategy() {
			return (gsonBuilder) -> gsonBuilder
					.addSerializationExclusionStrategy(new ExclusionStrategy() {
						@Override
						public boolean shouldSkipField(FieldAttributes fieldAttributes) {
							return "data".equals(fieldAttributes.getName());
						}

						@Override
						public boolean shouldSkipClass(Class<?> aClass) {
							return false;
						}
					});
		}

	}

	@Configuration
	static class GsonBuilderConfig {

		@Bean
		public GsonBuilder customGsonBuilder() {
			return new GsonBuilder().serializeNulls();
		}

	}

	public class DataObject {

		@SuppressWarnings("unused")
		private Long data = 1L;

		@SuppressWarnings("unused")
		private String owner = null;

		public void setData(Long data) {
			this.data = data;
		}

	}

	public class WrapperObject {

		@SuppressWarnings("unused")
		class NestedObject {

			private String data = "nested";

		}

	}

}
