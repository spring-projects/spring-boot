/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.jackson.scan.a.RenameMixInClass;
import org.springframework.boot.jackson.scan.b.RenameMixInAbstractClass;
import org.springframework.boot.jackson.scan.c.RenameMixInInterface;
import org.springframework.boot.jackson.scan.d.EmptyMixInClass;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JsonMixinModule}.
 *
 * @author Guirong Hu
 */
class JsonMixinModuleTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void createWhenContextIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JsonMixinModule(null, Collections.emptyList()))
				.withMessageContaining("Context must not be null");
	}

	@Test
	void jsonWithModuleWithRenameMixInClassShouldBeMixedIn() throws Exception {
		load(RenameMixInClass.class);
		JsonMixinModule module = this.context.getBean(JsonMixinModule.class);
		assertMixIn(module, new Name("spring"), "{\"username\":\"spring\"}");
		assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
	}

	@Test
	void jsonWithModuleWithEmptyMixInClassShouldNotBeMixedIn() throws Exception {
		load(EmptyMixInClass.class);
		JsonMixinModule module = this.context.getBean(JsonMixinModule.class);
		assertMixIn(module, new Name("spring"), "{\"name\":\"spring\"}");
		assertMixIn(module, new NameAndAge("spring", 100), "{\"name\":\"spring\",\"age\":100}");
	}

	@Test
	void jsonWithModuleWithRenameMixInAbstractClassShouldBeMixedIn() throws Exception {
		load(RenameMixInAbstractClass.class);
		JsonMixinModule module = this.context.getBean(JsonMixinModule.class);
		assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
	}

	@Test
	void jsonWithModuleWithRenameMixInInterfaceShouldBeMixedIn() throws Exception {
		load(RenameMixInInterface.class);
		JsonMixinModule module = this.context.getBean(JsonMixinModule.class);
		assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
	}

	private void load(Class<?>... basePackageClasses) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		List<String> basePackages = Arrays.stream(basePackageClasses).map(ClassUtils::getPackageName)
				.collect(Collectors.toList());
		context.registerBean(JsonMixinModule.class, () -> new JsonMixinModule(context, basePackages));
		context.refresh();
		this.context = context;
	}

	private void assertMixIn(Module module, Name value, String expectedJson) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		String json = mapper.writeValueAsString(value);
		assertThat(json).isEqualToIgnoringWhitespace(expectedJson);
	}

}
