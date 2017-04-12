/*
 * Copyright 2012-2016 the original author or authors.
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

import com.google.gson.*;
import com.google.gson.annotations.Since;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GsonAutoConfiguration}.
 *
 * @author David Liu
 * @author Ivan Golovko
 */
public class GsonAutoConfigurationTests {

    AnnotationConfigApplicationContext context;

    @Before
    public void setUp() {
        this.context = new AnnotationConfigApplicationContext();
    }

    @After
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void gsonRegistration() {
        this.context.register(GsonAutoConfiguration.class);
        this.context.refresh();
        Gson gson = this.context.getBean(Gson.class);
        assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
    }

    @Test
    public void generateNonExecutableJson() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.generate-non-executable-json:true");
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);
        assertThat(gson.toJson(new DataObject())).isNotEqualTo("{\"data\":1}");
        assertThat(gson.toJson(new DataObject())).endsWith("{\"data\":1}");
    }


    @Test
    public void excludeFieldsWithoutExposeAnnotation() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.exclude-fields-without-expose-annotation:true");
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);
        assertThat(gson.toJson(new DataObject())).isEqualTo("{}");
    }


    @Test
    public void serializeNulls() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.serialize-nulls:true");
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);
        assertThat(gson.serializeNulls()).isTrue();
    }


    @Test
    public void enableComplexMapKeySerialization() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.enable-complex-map-key-serialization:true");
        this.context.refresh();


        Gson gson = this.context.getBean(Gson.class);
        Map<DataObject, String> original = new LinkedHashMap<>();
        original.put(new DataObject(), "a");
        assertThat(gson.toJson(original)).isEqualTo("[[{\"data\":1},\"a\"]]");
    }


    @Test
    public void notDisableInnerClassSerialization() {
        this.context.register(GsonAutoConfiguration.class);
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);

        WrapperObject wrapperObject = new WrapperObject();
        assertThat(gson.toJson(wrapperObject.new NestedObject())).isEqualTo("{\"data\":\"nested\"}");
    }

    @Test
    public void disableInnerClassSerialization() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.disable-inner-class-serialization:true");
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);
        WrapperObject wrapperObject = new WrapperObject();
        assertThat(gson.toJson(wrapperObject.new NestedObject())).isEqualTo("null");
    }


    @Test
    public void withLongSerializationPolicy() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.long-serialization-policy:"
                        + LongSerializationPolicy.STRING);
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);

        assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":\"1\"}");
    }


    @Test
    public void withFieldNamingPolicy() {
        this.context.register(GsonAutoConfiguration.class);
        FieldNamingPolicy fieldNamingPolicy = FieldNamingPolicy.UPPER_CAMEL_CASE;
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.field-naming-policy:" + fieldNamingPolicy);
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);

        assertThat(gson.fieldNamingStrategy()).isEqualTo(fieldNamingPolicy);
    }

    @Test
    public void additionalGsonBuilderCustomization() {
        this.context.register(GsonAutoConfiguration.class, GsonBuilderCustomConfig.class);
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);
        assertThat(gson.toJson(new DataObject())).isEqualTo("{}");
    }

    @Test
    public void withPrettyPrinting() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.pretty-printing:true");
        this.context.refresh();
        Gson gson = this.context.getBean(Gson.class);
        assertThat(gson.toJson(new DataObject())).isEqualTo("{\n  \"data\": 1\n}");
    }

    @Test
    public void withoutLenient() throws Exception {
        this.context.register(GsonAutoConfiguration.class);
        this.context.refresh();
        Gson gson = this.context.getBean(Gson.class);

        /**
         * It seems, that lenient setting not work in version 2.8.0
         * We get access to it via reflection
         */
        Field lenientField = gson.getClass().getDeclaredField("lenient");
        lenientField.setAccessible(true);
        boolean lenient = lenientField.getBoolean(gson);

        assertThat(lenient).isFalse();
    }

    @Test
    public void withLenient() throws Exception {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.lenient:true");
        this.context.refresh();
        Gson gson = this.context.getBean(Gson.class);

        /**
         * It seems, that lenient setting not work in version 2.8.0 of gson
         * We get access to it via reflection
         */
        Field lenientField = gson.getClass().getDeclaredField("lenient");
        lenientField.setAccessible(true);
        boolean lenient = lenientField.getBoolean(gson);

        assertThat(lenient).isTrue();
    }


    @Test
    public void withHtmlEscaping() {
        this.context.register(GsonAutoConfiguration.class);
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);
        assertThat(gson.htmlSafe()).isTrue();
    }


    @Test
    public void withoutHtmlEscaping() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.disable-html-escaping:true");
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);
        assertThat(gson.htmlSafe()).isFalse();
    }

    @Test
    public void customDateFormat() {
        this.context.register(GsonAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.gson.date-format:H");
        this.context.refresh();

        Gson gson = this.context.getBean(Gson.class);
        DateTime dateTime = new DateTime(1988, 6, 25, 20, 30);
        Date date = dateTime.toDate();
        assertThat(gson.toJson(date)).isEqualTo("\"20\"");
    }

    protected static class GsonBuilderCustomConfig {

        @Bean
        public GsonBuilderCustomizer customSerializationExclusionStrategy() {
            return new GsonBuilderCustomizer() {
                @Override
                public void customize(GsonBuilder gsonBuilder) {
                    gsonBuilder.addSerializationExclusionStrategy(new ExclusionStrategy() {
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
            };
        }

    }


    public class DataObject {

        @SuppressWarnings("unused")
        public static final String STATIC_DATA = "bye";

        @SuppressWarnings("unused")
        private Long data = 1L;

        public void setData(Long data) {
            this.data = data;
        }
    }

    public class WrapperObject {

        @SuppressWarnings("unused")
        class NestedObject {
            @SuppressWarnings("unused")
            private String data = "nested";
        }
    }

}
