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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.jackson2.types.Name;
import org.springframework.boot.jackson2.types.NameAndAge;
import org.springframework.boot.jackson2.types.NameAndCareer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JsonComponentModule}.
 *
 * @author Phillip Webb
 * @author Vladimir Tsanev
 * @author Paul Aly
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
class JsonComponentModuleTests {

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			getContext().close();
		}
	}

	@Test
	void moduleShouldRegisterSerializers() throws Exception {
		load(OnlySerializer.class);
		JsonComponentModule module = getContext().getBean(JsonComponentModule.class);
		assertSerialize(module);
	}

	@Test
	void moduleShouldRegisterDeserializers() throws Exception {
		load(OnlyDeserializer.class);
		JsonComponentModule module = getContext().getBean(JsonComponentModule.class);
		assertDeserialize(module);
	}

	@Test
	void moduleShouldRegisterInnerClasses() throws Exception {
		load(NameAndAgeJsonComponent.class);
		JsonComponentModule module = getContext().getBean(JsonComponentModule.class);
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
		JsonComponentModule module = getContext().getBean(JsonComponentModule.class);
		assertKeySerialize(module);
	}

	@Test
	void moduleShouldRegisterKeyDeserializers() throws Exception {
		load(OnlyKeyDeserializer.class);
		JsonComponentModule module = getContext().getBean(JsonComponentModule.class);
		assertKeyDeserialize(module);
	}

	@Test
	void moduleShouldRegisterInnerClassesForKeyHandlers() throws Exception {
		load(NameAndAgeJsonKeyComponent.class);
		JsonComponentModule module = getContext().getBean(JsonComponentModule.class);
		assertKeySerialize(module);
		assertKeyDeserialize(module);
	}

	@Test
	void moduleShouldRegisterOnlyForSpecifiedClasses() throws Exception {
		load(NameAndCareerJsonComponent.class);
		JsonComponentModule module = getContext().getBean(JsonComponentModule.class);
		assertSerialize(module, new NameAndCareer("spring", "developer"), "{\"name\":\"spring\"}");
		assertSerialize(module);
		assertDeserializeForSpecifiedClasses(module);
	}

	@Test
	void aotContributionRegistersReflectionHintsForSuitableInnerClasses() {
		load(ComponentWithInnerAbstractClass.class);
		ConfigurableListableBeanFactory beanFactory = getContext().getBeanFactory();
		BeanFactoryInitializationAotContribution contribution = new org.springframework.boot.jackson2.JsonComponentModule.JsonComponentBeanFactoryInitializationAotProcessor()
			.processAheadOfTime(beanFactory);
		TestGenerationContext generationContext = new TestGenerationContext();
		assertThat(contribution).isNotNull();
		contribution.applyTo(generationContext, mock(BeanFactoryInitializationCode.class));
		RuntimeHints runtimeHints = generationContext.getRuntimeHints();
		assertThat(RuntimeHintsPredicates.reflection().onType(ComponentWithInnerAbstractClass.class))
			.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(JsonComponentModuleTests.ComponentWithInnerAbstractClass.ConcreteSerializer.class)
			.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(JsonComponentModuleTests.ComponentWithInnerAbstractClass.AbstractSerializer.class)
			.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
			.negate()).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(JsonComponentModuleTests.ComponentWithInnerAbstractClass.NotSuitable.class)
			.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
			.negate()).accepts(runtimeHints);
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
		TypeReference<Map<NameAndAge, Boolean>> typeRef = new TypeReference<>() {
		};
		Map<NameAndAge, Boolean> map = mapper.readValue("{\"spring is 100\":  true}", typeRef);
		assertThat(map).containsEntry(new NameAndAge("spring", 100), true);
	}

	private AnnotationConfigApplicationContext getContext() {
		AnnotationConfigApplicationContext context = this.context;
		assertThat(context).isNotNull();
		return context;
	}

	@JsonComponent
	static class OnlySerializer extends NameAndAgeJsonComponent.Serializer {

	}

	@JsonComponent
	static class OnlyDeserializer extends NameAndAgeJsonComponent.Deserializer {

	}

	@JsonComponent
	static class ComponentWithInnerAbstractClass {

		abstract static class AbstractSerializer extends NameAndAgeJsonComponent.Serializer {

		}

		static class ConcreteSerializer extends AbstractSerializer {

		}

		static class NotSuitable {

		}

	}

	@JsonComponent(scope = JsonComponent.Scope.KEYS)
	static class OnlyKeySerializer extends NameAndAgeJsonKeyComponent.Serializer {

	}

	@JsonComponent(scope = JsonComponent.Scope.KEYS, type = NameAndAge.class)
	static class OnlyKeyDeserializer extends NameAndAgeJsonKeyComponent.Deserializer {

	}

}
