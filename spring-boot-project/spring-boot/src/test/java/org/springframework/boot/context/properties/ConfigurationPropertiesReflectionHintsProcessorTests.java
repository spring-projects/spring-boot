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

package org.springframework.boot.context.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for {@link ConfigurationPropertiesReflectionHintsProcessor}.
 *
 * @author Moritz Halbritter
 */
class ConfigurationPropertiesReflectionHintsProcessorTests {

	@Test
	void shouldRegisterNoArgConstructor() throws NoSuchMethodException {
		RuntimeHints hints = runProcessor(NoArgsCtorProperties.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onConstructor(ReflectionUtils.accessibleConstructor(NoArgsCtorProperties.class))).accepts(hints);
	}

	@Test
	void shouldRegisterMemberCategories() {
		RuntimeHints hints = runProcessor(NoArgsCtorProperties.class);
		hasReflectionHintsForType(hints, NoArgsCtorProperties.class);
	}

	@Test
	void shouldNotRegisterJavaTypes() {
		RuntimeHints hints = runProcessor(NoArgsCtorProperties.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).rejects(hints);
	}

	@Test
	void shouldNotRegisterJavaTypesInLists() {
		RuntimeHints hints = runProcessor(StringListProperties.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).rejects(hints);
	}

	@Test
	void shouldRegisterConstructorBinding() throws NoSuchMethodException {
		RuntimeHints hints = runProcessor(ConstructorBindingProperties.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onConstructor(ReflectionUtils.accessibleConstructor(ConstructorBindingProperties.class, String.class)))
						.accepts(hints);
	}

	@Test
	void shouldRegisterConstructorBindingWithRecords() throws NoSuchMethodException {
		RuntimeHints hints = runProcessor(RecordProperties.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onConstructor(ReflectionUtils.accessibleConstructor(RecordProperties.class, String.class)))
						.accepts(hints);
	}

	@Test
	void shouldRegisterNestedTypes() {
		RuntimeHints hints = runProcessor(NestedProperties.class);
		hasReflectionHintsForType(hints, Nested.class);
	}

	@Test
	void shouldRegisterNestedTypesWhenNoSetterIsPresent() {
		RuntimeHints hints = runProcessor(NestedNoSetterProperties.class);
		hasReflectionHintsForType(hints, Nested.class);
	}

	@Test
	void shouldRegisterNestedTypesInLists() {
		RuntimeHints hints = runProcessor(NestedListProperties.class);
		hasReflectionHintsForType(hints, Nested.class);
	}

	@Test
	void shouldRegisterNestedTypesInMaps() {
		RuntimeHints hints = runProcessor(NestedMapProperties.class);
		hasReflectionHintsForType(hints, Nested.class);
	}

	@Test
	void shouldRegisterNestedTypesInArrays() {
		RuntimeHints hints = runProcessor(NestedArrayProperties.class);
		hasReflectionHintsForType(hints, Nested.class);
	}

	@Test
	void shouldRegisterNestedWithRecords() {
		RuntimeHints hints = runProcessor(NestedRecordProperties.class);
		hasReflectionHintsForType(hints, Nested.class);
	}

	@Test
	void shouldRegisterMultipleLevelsOfNested() {
		RuntimeHints hints = runProcessor(NestedMultipleLevelsProperties.class);
		hasReflectionHintsForType(hints, NestedLevel1.class);
		hasReflectionHintsForType(hints, NestedLevel2.class);
		hasReflectionHintsForType(hints, NestedLevel3.class);
	}

	@Test
	void shouldNotCrashOnCycles() {
		assertThatCode(() -> runProcessor(CycleProperties.class)).doesNotThrowAnyException();
	}

	@Test
	void shouldNotRegisterInterfaces() {
		RuntimeHints hints = runProcessor(InterfaceProperties.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(SomeInterface.class)).rejects(hints);
	}

	@Test
	void shouldNotRegisterKnownTypes() {
		RuntimeHints hints = runProcessor(AwareProperties.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(Environment.class)).rejects(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(ApplicationContext.class)).rejects(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(BeanFactory.class)).rejects(hints);
	}

	private RuntimeHints runProcessor(Class<?> type) {
		RuntimeHints hints = new RuntimeHints();
		ConfigurationPropertiesReflectionHintsProcessor.processConfigurationProperties(type, hints.reflection());
		return hints;
	}

	private void hasReflectionHintsForType(RuntimeHints hints, Class<?> type) {
		assertThat(RuntimeHintsPredicates.reflection().onType(type)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS))
						.accepts(hints);
	}

	@ConfigurationProperties
	public static class NoArgsCtorProperties {

		private String field;

		NoArgsCtorProperties() {
		}

		NoArgsCtorProperties(String field) {
			this.field = field;
		}

		public String getField() {
			return this.field;
		}

		public void setField(String field) {
			this.field = field;
		}

	}

	@ConfigurationProperties
	public static class StringListProperties {

		private final List<String> stringList = new ArrayList<>();

		public List<String> getStringList() {
			return this.stringList;
		}

	}

	@ConfigurationProperties
	public static class ConstructorBindingProperties {

		private final String field;

		ConstructorBindingProperties(String field) {
			this.field = field;
		}

		public String getField() {
			return this.field;
		}

	}

	@ConfigurationProperties
	public record RecordProperties(String field) {
	}

	@ConfigurationProperties
	public static class NestedProperties {

		private Nested nested;

		public Nested getNested() {
			return this.nested;
		}

		public void setNested(Nested nested) {
			this.nested = nested;
		}

	}

	@ConfigurationProperties
	public static class NestedNoSetterProperties {

		private final Nested nested = new Nested();

		public Nested getNested() {
			return this.nested;
		}

	}

	@ConfigurationProperties
	public static class NestedListProperties {

		private List<Nested> nestedList;

		public List<Nested> getNestedList() {
			return this.nestedList;
		}

		public void setNestedList(List<Nested> nestedList) {
			this.nestedList = nestedList;
		}

	}

	@ConfigurationProperties
	public static class NestedMapProperties {

		private Map<String, Nested> nestedMap = new HashMap<>();

		public Map<String, Nested> getNestedMap() {
			return this.nestedMap;
		}

	}

	@ConfigurationProperties
	public static class NestedArrayProperties {

		private Nested[] nestedArray;

		public Nested[] getNestedArray() {
			return this.nestedArray;
		}

		public void setNestedArray(Nested[] nestedArray) {
			this.nestedArray = nestedArray;
		}

	}

	@ConfigurationProperties
	public record NestedRecordProperties(Nested nested) {

	}

	@ConfigurationProperties
	public static class CycleProperties {

		private CycleProperties cycleProperties;

		public CycleProperties getCycleProperties() {
			return this.cycleProperties;
		}

		public void setCycleProperties(CycleProperties cycleProperties) {
			this.cycleProperties = cycleProperties;
		}

	}

	@ConfigurationProperties
	public static class NestedMultipleLevelsProperties {

		private NestedLevel1 nestedLevel1;

		public NestedLevel1 getNestedLevel1() {
			return this.nestedLevel1;
		}

		public void setNestedLevel1(NestedLevel1 nestedLevel1) {
			this.nestedLevel1 = nestedLevel1;
		}

	}

	@ConfigurationProperties
	public static class InterfaceProperties {

		private SomeInterface someInterface;

		public SomeInterface getSomeInterface() {
			return this.someInterface;
		}

		public void setSomeInterface(SomeInterface someInterface) {
			this.someInterface = someInterface;
		}

	}

	@ConfigurationProperties
	public static class AwareProperties implements ApplicationContextAware, BeanFactoryAware, EnvironmentAware {

		private String field;

		private BeanFactory beanFactory;

		private ApplicationContext applicationContext;

		private Environment environment;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		public String getField() {
			return this.field;
		}

		public BeanFactory getBeanFactory() {
			return this.beanFactory;
		}

		public ApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

		public Environment getEnvironment() {
			return this.environment;
		}

	}

	interface SomeInterface {

	}

	public static class Nested {

		private String field;

		public String getField() {
			return this.field;
		}

		public void setField(String field) {
			this.field = field;
		}

	}

	public static class NestedLevel1 {

		private NestedLevel2 nestedLevel2;

		public NestedLevel2 getNestedLevel2() {
			return this.nestedLevel2;
		}

		public void setNestedLevel2(NestedLevel2 nestedLevel2) {
			this.nestedLevel2 = nestedLevel2;
		}

	}

	public static class NestedLevel2 {

		private NestedLevel3 nestedLevel3;

		public NestedLevel3 getNestedLevel3() {
			return this.nestedLevel3;
		}

		public void setNestedLevel3(NestedLevel3 nestedLevel3) {
			this.nestedLevel3 = nestedLevel3;
		}

	}

	public class NestedLevel3 {

		private String field;

		public String getField() {
			return this.field;
		}

		public void setField(String field) {
			this.field = field;
		}

	}

}
