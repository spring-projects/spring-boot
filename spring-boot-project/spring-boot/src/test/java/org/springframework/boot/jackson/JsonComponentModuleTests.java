/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JsonComponentModule}.
 *
 * @author Phillip Webb
 * @author Vladimir Tsanev
 * @author Paul Aly
 */
class JsonComponentModuleTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void moduleShouldRegisterSerializers() throws Exception {
		load(OnlySerializer.class);
		JsonComponentModule module = this.context.getBean(JsonComponentModule.class);
		assertSerialize(module);
	}

	@Test
	void moduleShouldRegisterDeserializers() throws Exception {
		load(OnlyDeserializer.class);
		JsonComponentModule module = this.context.getBean(JsonComponentModule.class);
		assertDeserialize(module);
	}

	@Test
	void moduleShouldRegisterInnerClasses() throws Exception {
		load(NameAndAgeJsonComponent.class);
		JsonComponentModule module = this.context.getBean(JsonComponentModule.class);
		assertSerialize(module);
		assertDeserialize(module);
	}

	@Test
	void moduleShouldAllowInnerAbstractClasses() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JsonComponentModule.class,
				ComponentWithInnerAbstractClass.class);
		JsonComponentModule module = context.getBean(JsonComponentModule.class);
		assertSerialize(module);
		context.close();
	}

	@Test
	void moduleShouldRegisterKeySerializers() throws Exception {
		load(OnlyKeySerializer.class);
		JsonComponentModule module = this.context.getBean(JsonComponentModule.class);
		assertKeySerialize(module);
	}

	@Test
	void moduleShouldRegisterKeyDeserializers() throws Exception {
		load(OnlyKeyDeserializer.class);
		JsonComponentModule module = this.context.getBean(JsonComponentModule.class);
		assertKeyDeserialize(module);
	}

	@Test
	void moduleShouldRegisterInnerClassesForKeyHandlers() throws Exception {
		load(NameAndAgeJsonKeyComponent.class);
		JsonComponentModule module = this.context.getBean(JsonComponentModule.class);
		assertKeySerialize(module);
		assertKeyDeserialize(module);
	}

	@Test
	void moduleShouldRegisterOnlyForSpecifiedClasses() throws Exception {
		load(NameAndCareerJsonComponent.class);
		JsonComponentModule module = this.context.getBean(JsonComponentModule.class);
		assertSerialize(module, new NameAndCareer("spring", "developer"), "{\"name\":\"spring\"}");
		assertSerialize(module);
		assertDeserializeForSpecifiedClasses(module);
	}

	private void load(Class<?>... configs) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(configs);
		context.register(JsonComponentModule.class);
		context.refresh();
		this.context = context;
	}

	private void assertSerialize(Module module, Name value, String expectedJson) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		String json = mapper.writeValueAsString(value);
		assertThat(json).isEqualToIgnoringWhitespace(expectedJson);
	}

	private void assertSerialize(Module module) throws Exception {
		assertSerialize(module, new NameAndAge("spring", 100), "{\"name\":\"spring\",\"age\":100}");
	}

	private void assertDeserialize(Module module) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		NameAndAge nameAndAge = mapper.readValue("{\"name\":\"spring\",\"age\":100}", NameAndAge.class);
		assertThat(nameAndAge.getName()).isEqualTo("spring");
		assertThat(nameAndAge.getAge()).isEqualTo(100);
	}

	private void assertDeserializeForSpecifiedClasses(JsonComponentModule module) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		assertThatExceptionOfType(JsonMappingException.class)
				.isThrownBy(() -> mapper.readValue("{\"name\":\"spring\",\"age\":100}", NameAndAge.class));
		NameAndCareer nameAndCareer = mapper.readValue("{\"name\":\"spring\",\"career\":\"developer\"}",
				NameAndCareer.class);
		assertThat(nameAndCareer.getName()).isEqualTo("spring");
		assertThat(nameAndCareer.getCareer()).isEqualTo("developer");
	}

	private void assertKeySerialize(Module module) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		Map<NameAndAge, Boolean> map = new HashMap<>();
		map.put(new NameAndAge("spring", 100), true);
		String json = mapper.writeValueAsString(map);
		assertThat(json).isEqualToIgnoringWhitespace("{\"spring is 100\":  true}");
	}

	private void assertKeyDeserialize(Module module) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		TypeReference<Map<NameAndAge, Boolean>> typeRef = new TypeReference<Map<NameAndAge, Boolean>>() {
		};
		Map<NameAndAge, Boolean> map = mapper.readValue("{\"spring is 100\":  true}", typeRef);
		assertThat(map).containsEntry(new NameAndAge("spring", 100), true);
	}

	@JsonComponent
	static class OnlySerializer extends NameAndAgeJsonComponent.Serializer {

	}

	@JsonComponent
	static class OnlyDeserializer extends NameAndAgeJsonComponent.Deserializer {

	}

	@JsonComponent
	static class ComponentWithInnerAbstractClass {

		private abstract static class AbstractSerializer extends NameAndAgeJsonComponent.Serializer {

		}

		static class ConcreteSerializer extends AbstractSerializer {

		}

	}

	@JsonComponent(scope = JsonComponent.Scope.KEYS)
	static class OnlyKeySerializer extends NameAndAgeJsonKeyComponent.Serializer {

	}

	@JsonComponent(scope = JsonComponent.Scope.KEYS, type = NameAndAge.class)
	static class OnlyKeyDeserializer extends NameAndAgeJsonKeyComponent.Deserializer {

	}

}
