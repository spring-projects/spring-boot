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

package org.springframework.boot.jackson2;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.jackson2.scan.a.RenameMixInClass;
import org.springframework.boot.jackson2.scan.b.RenameMixInAbstractClass;
import org.springframework.boot.jackson2.scan.c.RenameMixInInterface;
import org.springframework.boot.jackson2.scan.d.EmptyMixInClass;
import org.springframework.boot.jackson2.scan.f.EmptyMixIn;
import org.springframework.boot.jackson2.types.Name;
import org.springframework.boot.jackson2.types.NameAndAge;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JsonMixinModule}.
 *
 * @author Guirong Hu
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
class JsonMixinModuleTests {

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			getContext().close();
		}
	}

	@Test
	void jsonWithModuleEmptyMixInWithEmptyTypesShouldFail() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> load(EmptyMixIn.class))
			.withMessageContaining("Error creating bean with name 'jsonMixinModule'")
			.withStackTraceContaining("@JsonMixin annotation on class "
					+ "'org.springframework.boot.jackson2.scan.f.EmptyMixIn' does not specify any types");
	}

	@Test
	void jsonWithModuleWithRenameMixInClassShouldBeMixedIn() throws Exception {
		load(RenameMixInClass.class);
		JsonMixinModule module = getContext().getBean(JsonMixinModule.class);
		assertMixIn(module, new Name("spring"), "{\"username\":\"spring\"}");
		assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
	}

	@Test
	void jsonWithModuleWithEmptyMixInClassShouldNotBeMixedIn() throws Exception {
		load(EmptyMixInClass.class);
		JsonMixinModule module = getContext().getBean(JsonMixinModule.class);
		assertMixIn(module, new Name("spring"), "{\"name\":\"spring\"}");
		assertMixIn(module, new NameAndAge("spring", 100), "{\"name\":\"spring\",\"age\":100}");
	}

	@Test
	void jsonWithModuleWithRenameMixInAbstractClassShouldBeMixedIn() throws Exception {
		load(RenameMixInAbstractClass.class);
		JsonMixinModule module = getContext().getBean(JsonMixinModule.class);
		assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
	}

	@Test
	void jsonWithModuleWithRenameMixInInterfaceShouldBeMixedIn() throws Exception {
		load(RenameMixInInterface.class);
		JsonMixinModule module = getContext().getBean(JsonMixinModule.class);
		assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
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

	private void assertMixIn(Module module, Name value, String expectedJson) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		String json = mapper.writeValueAsString(value);
		assertThat(json).isEqualToIgnoringWhitespace(expectedJson);
	}

	private AnnotationConfigApplicationContext getContext() {
		AnnotationConfigApplicationContext context = this.context;
		assertThat(context).isNotNull();
		return context;
	}

}
