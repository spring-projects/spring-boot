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

package org.springframework.boot.jackson;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.jackson.scan.a.RenameMixInClass;
import org.springframework.boot.jackson.scan.b.RenameMixInAbstractClass;
import org.springframework.boot.jackson.scan.c.RenameMixInInterface;
import org.springframework.boot.jackson.scan.d.EmptyMixInClass;
import org.springframework.boot.jackson.scan.f.EmptyMixIn;
import org.springframework.boot.jackson.types.Name;
import org.springframework.boot.jackson.types.NameAndAge;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JsonMixinModule}.
 *
 * @author Guirong Hu
 */
class JsonMixinModuleTests {

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void jsonWithModuleEmptyMixInWithEmptyTypesShouldFail() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> load(EmptyMixIn.class))
			.withMessageContaining("Error creating bean with name 'jsonMixinModule'")
			.withStackTraceContaining("@JsonMixin annotation on class "
					+ "'org.springframework.boot.jackson.scan.f.EmptyMixIn' does not specify any types");
	}

	@Test
	void jsonWithModuleWithRenameMixInClassShouldBeMixedIn() throws Exception {
		load(RenameMixInClass.class);
		JsonMixinModule module = getContext().getBean(JsonMixinModule.class);
		assertMixIn(module, new Name("spring"), "{\"username\":\"spring\"}");
		assertMixIn(module, NameAndAge.create("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
	}

	@Test
	void jsonWithModuleWithEmptyMixInClassShouldNotBeMixedIn() throws Exception {
		load(EmptyMixInClass.class);
		JsonMixinModule module = getContext().getBean(JsonMixinModule.class);
		assertMixIn(module, new Name("spring"), "{\"name\":\"spring\"}");
		assertMixIn(module, NameAndAge.create("spring", 100), "{\"age\":100,\"name\":\"spring\"}");
	}

	@Test
	void jsonWithModuleWithRenameMixInAbstractClassShouldBeMixedIn() throws Exception {
		load(RenameMixInAbstractClass.class);
		JsonMixinModule module = getContext().getBean(JsonMixinModule.class);
		assertMixIn(module, NameAndAge.create("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
	}

	@Test
	void jsonWithModuleWithRenameMixInInterfaceShouldBeMixedIn() throws Exception {
		load(RenameMixInInterface.class);
		JsonMixinModule module = getContext().getBean(JsonMixinModule.class);
		assertMixIn(module, NameAndAge.create("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
	}

	private AnnotationConfigApplicationContext getContext() {
		AnnotationConfigApplicationContext context = this.context;
		assertThat(context).isNotNull();
		return context;
	}

	private void load(Class<?>... basePackageClasses) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(JsonMixinModule.class, () -> createJsonMixinModule(context, basePackageClasses));
		context.refresh();
		this.context = context;
	}

	private JsonMixinModule createJsonMixinModule(AnnotationConfigApplicationContext context,
			Class<?>... basePackageClasses) {
		List<String> basePackages = Arrays.stream(basePackageClasses).map(ClassUtils::getPackageName).toList();
		JsonMixinModuleEntries entries = JsonMixinModuleEntries.scan(context, basePackages);
		JsonMixinModule jsonMixinModule = new JsonMixinModule();
		jsonMixinModule.registerEntries(entries, context.getClassLoader());
		return jsonMixinModule;
	}

	private void assertMixIn(JacksonModule module, Name value, String expectedJson) throws Exception {
		JsonMapper mapper = JsonMapper.builder().addModule(module).build();
		String json = mapper.writeValueAsString(value);
		assertThat(json).isEqualToIgnoringWhitespace(expectedJson);
	}

}
