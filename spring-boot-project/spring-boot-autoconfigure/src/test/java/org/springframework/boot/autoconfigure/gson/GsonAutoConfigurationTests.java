/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.gson;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.Strictness;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

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
class GsonAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GsonAutoConfiguration.class));

	@Test
	void gsonRegistration() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
		});
	}

	@Test
	void generateNonExecutableJsonTrue() {
		this.contextRunner.withPropertyValues("spring.gson.generate-non-executable-json:true").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.toJson(new DataObject())).isNotEqualTo("{\"data\":1}");
			assertThat(gson.toJson(new DataObject())).endsWith("{\"data\":1}");
		});
	}

	@Test
	void generateNonExecutableJsonFalse() {
		this.contextRunner.withPropertyValues("spring.gson.generate-non-executable-json:false").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
		});
	}

	@Test
	void excludeFieldsWithoutExposeAnnotationTrue() {
		this.contextRunner.withPropertyValues("spring.gson.exclude-fields-without-expose-annotation:true")
			.run((context) -> {
				Gson gson = context.getBean(Gson.class);
				assertThat(gson.toJson(new DataObject())).isEqualTo("{}");
			});
	}

	@Test
	void excludeFieldsWithoutExposeAnnotationFalse() {
		this.contextRunner.withPropertyValues("spring.gson.exclude-fields-without-expose-annotation:false")
			.run((context) -> {
				Gson gson = context.getBean(Gson.class);
				assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
			});
	}

	@Test
	void serializeNullsTrue() {
		this.contextRunner.withPropertyValues("spring.gson.serialize-nulls:true").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.serializeNulls()).isTrue();
		});
	}

	@Test
	void serializeNullsFalse() {
		this.contextRunner.withPropertyValues("spring.gson.serialize-nulls:false").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.serializeNulls()).isFalse();
		});
	}

	@Test
	void enableComplexMapKeySerializationTrue() {
		this.contextRunner.withPropertyValues("spring.gson.enable-complex-map-key-serialization:true")
			.run((context) -> {
				Gson gson = context.getBean(Gson.class);
				Map<DataObject, String> original = new LinkedHashMap<>();
				original.put(new DataObject(), "a");
				assertThat(gson.toJson(original)).isEqualTo("[[{\"data\":1},\"a\"]]");
			});
	}

	@Test
	void enableComplexMapKeySerializationFalse() {
		this.contextRunner.withPropertyValues("spring.gson.enable-complex-map-key-serialization:false")
			.run((context) -> {
				Gson gson = context.getBean(Gson.class);
				Map<DataObject, String> original = new LinkedHashMap<>();
				original.put(new DataObject(), "a");
				assertThat(gson.toJson(original)).contains(DataObject.class.getName()).doesNotContain("\"data\":");
			});
	}

	@Test
	void notDisableInnerClassSerialization() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			WrapperObject wrapperObject = new WrapperObject();
			assertThat(gson.toJson(wrapperObject.new NestedObject())).isEqualTo("{\"data\":\"nested\"}");
		});
	}

	@Test
	void disableInnerClassSerializationTrue() {
		this.contextRunner.withPropertyValues("spring.gson.disable-inner-class-serialization:true").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			WrapperObject wrapperObject = new WrapperObject();
			assertThat(gson.toJson(wrapperObject.new NestedObject())).isEqualTo("null");
		});
	}

	@Test
	void disableInnerClassSerializationFalse() {
		this.contextRunner.withPropertyValues("spring.gson.disable-inner-class-serialization:false").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			WrapperObject wrapperObject = new WrapperObject();
			assertThat(gson.toJson(wrapperObject.new NestedObject())).isEqualTo("{\"data\":\"nested\"}");
		});
	}

	@Test
	void withLongSerializationPolicy() {
		this.contextRunner.withPropertyValues("spring.gson.long-serialization-policy:" + LongSerializationPolicy.STRING)
			.run((context) -> {
				Gson gson = context.getBean(Gson.class);
				assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":\"1\"}");
			});
	}

	@Test
	void withFieldNamingPolicy() {
		FieldNamingPolicy fieldNamingPolicy = FieldNamingPolicy.UPPER_CAMEL_CASE;
		this.contextRunner.withPropertyValues("spring.gson.field-naming-policy:" + fieldNamingPolicy).run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.fieldNamingStrategy()).isEqualTo(fieldNamingPolicy);
		});
	}

	@Test
	void additionalGsonBuilderCustomization() {
		this.contextRunner.withUserConfiguration(GsonBuilderCustomizerConfig.class).run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.toJson(new DataObject())).isEqualTo("{}");
		});
	}

	@Test
	void customGsonBuilder() {
		this.contextRunner.withUserConfiguration(GsonBuilderConfig.class).run((context) -> {
			Gson gson = context.getBean(Gson.class);
			JSONAssert.assertEquals("{\"data\":1,\"owner\":null}", gson.toJson(new DataObject()), true);
		});
	}

	@Test
	void withPrettyPrintingTrue() {
		this.contextRunner.withPropertyValues("spring.gson.pretty-printing:true").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.toJson(new DataObject())).isEqualTo("{\n  \"data\": 1\n}");
		});
	}

	@Test
	void withPrettyPrintingFalse() {
		this.contextRunner.withPropertyValues("spring.gson.pretty-printing:false").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
		});
	}

	@Test
	@Deprecated(since = "3.4.0", forRemoval = true)
	void withoutLenient() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson).hasFieldOrPropertyWithValue("strictness", null);
		});
	}

	@Test
	@Deprecated(since = "3.4.0", forRemoval = true)
	void withLenientTrue() {
		this.contextRunner.withPropertyValues("spring.gson.lenient:true").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson).hasFieldOrPropertyWithValue("strictness", Strictness.LENIENT);
		});
	}

	@Test
	@Deprecated(since = "3.4.0", forRemoval = true)
	void withLenientFalse() {
		this.contextRunner.withPropertyValues("spring.gson.lenient:false").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson).hasFieldOrPropertyWithValue("strictness", Strictness.STRICT);
		});
	}

	@Test
	void withoutStrictness() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson).hasFieldOrPropertyWithValue("strictness", null);
		});
	}

	@Test
	void withStrictnessStrict() {
		this.contextRunner.withPropertyValues("spring.gson.strictness:strict").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson).hasFieldOrPropertyWithValue("strictness", Strictness.STRICT);
		});
	}

	@Test
	void withStrictnessLegacyStrict() {
		this.contextRunner.withPropertyValues("spring.gson.strictness:legacy-strict").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson).hasFieldOrPropertyWithValue("strictness", Strictness.LEGACY_STRICT);
		});
	}

	@Test
	void withStrictnessLenient() {
		this.contextRunner.withPropertyValues("spring.gson.strictness:lenient").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson).hasFieldOrPropertyWithValue("strictness", Strictness.LENIENT);
		});
	}

	@Test
	void withoutDisableHtmlEscaping() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.htmlSafe()).isTrue();
		});
	}

	@Test
	void withDisableHtmlEscapingTrue() {
		this.contextRunner.withPropertyValues("spring.gson.disable-html-escaping:true").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.htmlSafe()).isFalse();
		});
	}

	@Test
	void withDisableHtmlEscapingFalse() {
		this.contextRunner.withPropertyValues("spring.gson.disable-html-escaping:false").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.htmlSafe()).isTrue();
		});
	}

	@Test
	void customDateFormat() {
		this.contextRunner.withPropertyValues("spring.gson.date-format:H").run((context) -> {
			Gson gson = context.getBean(Gson.class);
			ZonedDateTime dateTime = ZonedDateTime.of(1988, 6, 25, 20, 30, 0, 0, ZoneId.systemDefault());
			assertThat(gson.toJson(Date.from(dateTime.toInstant()))).isEqualTo("\"20\"");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class GsonBuilderCustomizerConfig {

		@Bean
		GsonBuilderCustomizer customSerializationExclusionStrategy() {
			return (gsonBuilder) -> gsonBuilder.addSerializationExclusionStrategy(new ExclusionStrategy() {
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

	@Configuration(proxyBeanMethods = false)
	static class GsonBuilderConfig {

		@Bean
		GsonBuilder customGsonBuilder() {
			return new GsonBuilder().serializeNulls();
		}

	}

	public class DataObject {

		@SuppressWarnings("unused")
		private Long data = 1L;

		@SuppressWarnings("unused")
		private final String owner = null;

		public void setData(Long data) {
			this.data = data;
		}

	}

	public class WrapperObject {

		@SuppressWarnings("unused")
		class NestedObject {

			private final String data = "nested";

		}

	}

}
