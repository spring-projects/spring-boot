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

package org.springframework.boot.context.properties;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConfigurationProperties} annotated beans. Covers
 * {@link EnableConfigurationProperties},
 * {@link ConfigurationPropertiesBindingPostProcessorRegistrar},
 * {@link ConfigurationPropertiesBindingPostProcessor} and
 * {@link ConfigurationPropertiesBinder}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class ConfigurationPropertiesTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCapture output = new OutputCapture();

	@After
	public void cleanup() {
		this.context.close();
		System.clearProperty("name");
		System.clearProperty("nested.name");
		System.clearProperty("nested_name");
	}

	@Test
	public void loadShouldBind() {
		load(BasicConfiguration.class, "name=foo");
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.containsBean(BasicProperties.class.getName())).isTrue();
		assertThat(this.context.getBean(BasicProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void loadShouldBindNested() {
		load(NestedConfiguration.class, "name=foo", "nested.name=bar");
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name)
				.isEqualTo("bar");
	}

	@Test
	public void loadWhenUsingSystemPropertiesShouldBind() {
		System.setProperty("name", "foo");
		load(BasicConfiguration.class);
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.getBean(BasicProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenUsingSystemPropertiesShouldBindNested() {
		System.setProperty("name", "foo");
		System.setProperty("nested.name", "bar");
		load(NestedConfiguration.class);
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name)
				.isEqualTo("bar");
	}

	@Test
	public void loadWhenHasIgnoreUnknownFieldsFalseAndNoUnknownFieldsShouldBind() {
		removeSystemProperties();
		load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo");
		IgnoreUnknownFieldsFalseProperties bean = this.context
				.getBean(IgnoreUnknownFieldsFalseProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenHasIgnoreUnknownFieldsFalseAndUnknownFieldsShouldFail() {
		removeSystemProperties();
		this.thrown.expectCause(Matchers.instanceOf(BindException.class));
		load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo", "bar=baz");
	}

	@Test
	public void loadWhenHasIgnoreInvalidFieldsTrueAndInvalidFieldsShouldBind() {
		load(IgnoreInvalidFieldsFalseProperties.class, "com.example.bar=spam");
		IgnoreInvalidFieldsFalseProperties bean = this.context
				.getBean(IgnoreInvalidFieldsFalseProperties.class);
		assertThat(bean.getBar()).isEqualTo(0);
	}

	@Test
	public void loadWhenHasPrefixShouldBind() {
		load(PrefixConfiguration.class, "spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenPropertiesHaveAnnotationOnBaseClassShouldBind() {
		load(AnnotationOnBaseClassConfiguration.class, "name=foo");
		AnnotationOnBaseClassProperties bean = this.context
				.getBean(AnnotationOnBaseClassProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenBindingArrayShouldBind() {
		load(BasicConfiguration.class, "name=foo", "array=1,2,3");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.array).containsExactly(1, 2, 3);
	}

	@Test
	public void loadWhenBindingArrayFromYamlArrayShouldBind() {
		load(BasicConfiguration.class, "name=foo", "list[0]=1", "list[1]=2", "list[2]=3");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.list).containsExactly(1, 2, 3);
	}

	@Test
	public void loadWhenBindingOver256ElementsShouldBind() {
		List<String> pairs = new ArrayList<>();
		pairs.add("name:foo");
		for (int i = 0; i < 1000; i++) {
			pairs.add("list[" + i + "]:" + i);
		}
		load(BasicConfiguration.class, StringUtils.toStringArray(pairs));
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.list).hasSize(1000);
	}

	@Test
	public void loadWhenBindingWithoutAndAnnotationShouldFail() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("No ConfigurationProperties annotation found");
		load(WithoutAndAnnotationConfiguration.class, "name:foo");
	}

	@Test
	public void loadWhenBindingWithoutAnnotationValueShouldBind() {
		load(WithoutAnnotationValueConfiguration.class, "name=foo");
		WithoutAnnotationValueProperties bean = this.context
				.getBean(WithoutAnnotationValueProperties.class);
		assertThat(bean.name).isEqualTo("foo");
	}

	@Test
	public void loadWhenBindingWithDefaultsInXmlShouldBind() {
		load(new Class<?>[] { BasicConfiguration.class,
				DefaultsInXmlConfiguration.class });
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("bar");
	}

	@Test
	public void loadWhenBindingWithDefaultsInJavaConfigurationShouldBind() {
		load(DefaultsInJavaConfiguration.class);
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("bar");
	}

	@Test
	public void loadWhenBindingTwoBeansShouldBind() {
		load(new Class<?>[] { WithoutAnnotationValueConfiguration.class,
				BasicConfiguration.class });
		assertThat(this.context.getBean(BasicProperties.class)).isNotNull();
		assertThat(this.context.getBean(WithoutAnnotationValueProperties.class))
				.isNotNull();
	}

	@Test
	public void loadWhenBindingWithParentContextShouldBind() {
		AnnotationConfigApplicationContext parent = load(BasicConfiguration.class,
				"name=parent");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		load(new Class[] { BasicConfiguration.class, BasicPropertiesConsumer.class },
				"name=child");
		assertThat(this.context.getBean(BasicProperties.class)).isNotNull();
		assertThat(parent.getBean(BasicProperties.class)).isNotNull();
		assertThat(this.context.getBean(BasicPropertiesConsumer.class).getName())
				.isEqualTo("parent");
		parent.close();
	}

	@Test
	public void loadWhenBindingOnlyParentContextShouldBind() {
		AnnotationConfigApplicationContext parent = load(BasicConfiguration.class,
				"name=foo");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		load(BasicPropertiesConsumer.class);
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).isEmpty();
		assertThat(parent.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.getBean(BasicPropertiesConsumer.class).getName())
				.isEqualTo("foo");
	}

	@Test
	public void loadWhenPrefixedPropertiesDeclaredAsBeanShouldBind() {
		load(PrefixPropertiesDeclaredAsBeanConfiguration.class, "spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenPrefixedPropertiesDeclaredAsAnnotationValueShouldBind() {
		load(PrefixPropertiesDeclaredAsAnnotationValueConfiguration.class,
				"spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(
				"spring.foo-" + PrefixProperties.class.getName(), PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenMultiplePrefixedPropertiesDeclaredAsAnnotationValueShouldBind() {
		load(MultiplePrefixPropertiesDeclaredAsAnnotationValueConfiguration.class,
				"spring.foo.name=foo", "spring.bar.name=bar");
		PrefixProperties bean1 = this.context.getBean(PrefixProperties.class);
		AnotherPrefixProperties bean2 = this.context
				.getBean(AnotherPrefixProperties.class);
		assertThat(((BasicProperties) bean1).name).isEqualTo("foo");
		assertThat(((BasicProperties) bean2).name).isEqualTo("bar");
	}

	@Test
	public void loadWhenBindingToMapKeyWithPeriodShouldBind() {
		load(MapProperties.class, "mymap.key1.key2:value12", "mymap.key3:value3");
		MapProperties bean = this.context.getBean(MapProperties.class);
		assertThat(bean.mymap).containsOnly(entry("key3", "value3"),
				entry("key1.key2", "value12"));
	}

	@Test
	public void loadWhenPrefixedPropertiesAreReplacedOnBeanMethodShouldBind() {
		load(PrefixedPropertiesReplacedOnBeanMethodConfiguration.class,
				"external.name=bar", "spam.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadShouldBindToJavaTimeDuration() {
		load(BasicConfiguration.class, "duration=PT1M");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.getDuration().getSeconds()).isEqualTo(60);
	}

	@Test
	public void loadWhenBindingToValidatedImplementationOfInterfaceShouldBind() {
		load(ValidatedImplementationConfiguration.class, "test.foo=bar");
		ValidatedImplementationProperties bean = this.context
				.getBean(ValidatedImplementationProperties.class);
		assertThat(bean.getFoo()).isEqualTo("bar");
	}

	@Test
	public void loadWithPropertyPlaceholderValueShouldBind() {
		load(WithPropertyPlaceholderValueConfiguration.class, "default.value=foo");
		WithPropertyPlaceholderValueProperties bean = this.context
				.getBean(WithPropertyPlaceholderValueProperties.class);
		assertThat(bean.getValue()).isEqualTo("foo");
	}

	@Test
	public void loadWhenHasPostConstructShouldTriggerPostConstructWithBoundBean() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("bar", "foo");
		this.context.setEnvironment(environment);
		this.context.register(WithPostConstructConfiguration.class);
		this.context.refresh();
		WithPostConstructConfiguration bean = this.context
				.getBean(WithPostConstructConfiguration.class);
		assertThat(bean.initialized).isTrue();
	}

	@Test
	public void loadShouldNotInitializeFactoryBeans() {
		WithFactoryBeanConfiguration.factoryBeanInitialized = false;
		this.context = new AnnotationConfigApplicationContext() {

			@Override
			protected void onRefresh() throws BeansException {
				assertThat(WithFactoryBeanConfiguration.factoryBeanInitialized)
						.as("Initialized too early").isFalse();
				super.onRefresh();
			}

		};
		this.context.register(WithFactoryBeanConfiguration.class);
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(FactoryBeanTester.class);
		beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		this.context.registerBeanDefinition("test", beanDefinition);
		this.context.refresh();
		assertThat(WithFactoryBeanConfiguration.factoryBeanInitialized)
				.as("Not Initialized").isTrue();
	}

	@Test
	public void loadWhenUsingRelaxedFormsShouldBindToEnum() {
		bindToEnum("test.theValue=FOO");
		bindToEnum("test.theValue=foo");
		bindToEnum("test.the-value=FoO");
		bindToEnum("test.THE_VALUE=FoO");
	}

	private void bindToEnum(String... inlinedProperties) {
		load(WithEnumProperties.class, inlinedProperties);
		WithEnumProperties bean = this.context.getBean(WithEnumProperties.class);
		assertThat(bean.getTheValue()).isEqualTo(FooEnum.FOO);
		resetContext();
	}

	@Test
	public void loadWhenUsingRelaxedFormsShouldBindToEnumSet() {
		bindToEnumSet("test.the-values=foo,bar", FooEnum.FOO, FooEnum.BAR);
		bindToEnumSet("test.the-values=foo", FooEnum.FOO);
	}

	private void bindToEnumSet(String inlinedProperty, FooEnum... expected) {
		load(WithEnumProperties.class, inlinedProperty);
		WithEnumProperties bean = this.context.getBean(WithEnumProperties.class);
		assertThat(bean.getTheValues()).contains(expected);
		resetContext();
	}

	@Test
	public void loadShouldBindToCharArray() {
		load(WithCharArrayProperties.class, "test.chars=word");
		WithCharArrayProperties bean = this.context
				.getBean(WithCharArrayProperties.class);
		assertThat(bean.getChars()).isEqualTo("word".toCharArray());
	}

	@Test
	public void loadWhenUsingRelaxedFormsAndOverrideShouldBind() {
		load(WithRelaxedNamesProperties.class, "test.FOO_BAR=test1", "test.FOO_BAR=test2",
				"test.BAR-B-A-Z=testa", "test.BAR-B-A-Z=testb");
		WithRelaxedNamesProperties bean = this.context
				.getBean(WithRelaxedNamesProperties.class);
		assertThat(bean.getFooBar()).isEqualTo("test2");
		assertThat(bean.getBarBAZ()).isEqualTo("testb");
	}

	@Test
	public void loadShouldBindToMap() {
		load(WithMapProperties.class, "test.map.foo=bar");
		WithMapProperties bean = this.context.getBean(WithMapProperties.class);
		assertThat(bean.getMap()).containsOnly(entry("foo", "bar"));
	}

	@Test
	public void loadShouldBindToMapWithNumericKey() {
		load(MapWithNumericKeyProperties.class, "sample.properties.1.name=One");
		MapWithNumericKeyProperties bean = this.context
				.getBean(MapWithNumericKeyProperties.class);
		assertThat(bean.getProperties().get("1").name).isEqualTo("One");
	}

	@Test
	public void loadWhenUsingSystemPropertiesShouldBindToMap() {
		this.context.getEnvironment().getPropertySources()
				.addLast(new SystemEnvironmentPropertySource(
						StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						Collections.singletonMap("TEST_MAP_FOO_BAR", "baz")));
		load(WithComplexMapProperties.class);
		WithComplexMapProperties bean = this.context
				.getBean(WithComplexMapProperties.class);
		assertThat(bean.getMap()).containsOnlyKeys("foo");
		assertThat(bean.getMap().get("foo")).containsOnly(entry("bar", "baz"));
	}

	@Test
	public void loadWhenOverridingPropertiesShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		sources.addFirst(new SystemEnvironmentPropertySource("system",
				Collections.singletonMap("SPRING_FOO_NAME", "Jane")));
		sources.addLast(new MapPropertySource("test",
				Collections.singletonMap("spring.foo.name", "John")));
		load(PrefixConfiguration.class);
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("Jane");
	}

	@Test
	public void loadWhenJsr303ConstraintDoesNotMatchShouldFail() {
		this.thrown.expectCause(Matchers.instanceOf(BindException.class));
		load(ValidatedJsr303Configuration.class, "description=");
	}

	@Test
	public void loadValidatedOnBeanMethodAndJsr303ConstraintDoesNotMatchShouldFail() {
		this.thrown.expectCause(Matchers.instanceOf(BindException.class));
		load(ValidatedOnBeanJsr303Configuration.class, "description=");
	}

	@Test
	public void loadWhenJsr303ConstraintDoesNotMatchOnNestedThatIsNotDirectlyAnnotatedShouldFail() {
		this.thrown.expectCause(Matchers.instanceOf(BindException.class));
		load(ValidatedNestedJsr303Properties.class, "properties.description=");
	}

	@Test
	public void loadWhenJsr303ConstraintDoesNotMatchOnNestedThatIsNotDirectlyAnnotatedButIsValidShouldFail() {
		this.thrown.expectCause(Matchers.instanceOf(BindException.class));
		load(ValidatedValidNestedJsr303Properties.class);
	}

	@Test
	public void loadWhenJsr303ConstraintMatchesShouldBind() {
		load(ValidatedJsr303Configuration.class, "description=foo");
		ValidatedJsr303Properties bean = this.context
				.getBean(ValidatedJsr303Properties.class);
		assertThat(bean.getDescription()).isEqualTo("foo");
	}

	@Test
	public void loadWhenJsr303ConstraintDoesNotMatchAndNotValidatedAnnotationShouldBind() {
		load(NonValidatedJsr303Configuration.class, "name=foo");
		NonValidatedJsr303Properties bean = this.context
				.getBean(NonValidatedJsr303Properties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenHasMultiplePropertySourcesPlaceholderConfigurerShouldLogWarning() {
		load(MultiplePropertySourcesPlaceholderConfigurerConfiguration.class);
		assertThat(this.output.toString()).contains(
				"Multiple PropertySourcesPlaceholderConfigurer beans registered");
	}

	@Test
	public void loadWhenOverridingPropertiesWithPlaceholderResolutionInEnvironmentShouldBindWithOverride() {
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		sources.addFirst(new SystemEnvironmentPropertySource("system",
				Collections.singletonMap("COM_EXAMPLE_BAR", "10")));
		Map<String, Object> source = new HashMap<>();
		source.put("com.example.bar", 5);
		source.put("com.example.foo", "${com.example.bar}");
		sources.addLast(new MapPropertySource("test", source));
		load(SimplePrefixedProperties.class);
		SimplePrefixedProperties bean = this.context
				.getBean(SimplePrefixedProperties.class);
		assertThat(bean.getFoo()).isEqualTo(10);
	}

	@Test
	public void loadWhenHasUnboundElementsFromSystemEnvironmentShouldNotThrowException() {
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		sources.addFirst(new MapPropertySource("test",
				Collections.singletonMap("com.example.foo", 5)));
		sources.addLast(new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("COM_EXAMPLE_OTHER", "10")));
		load(SimplePrefixedProperties.class);
		SimplePrefixedProperties bean = this.context
				.getBean(SimplePrefixedProperties.class);
		assertThat(bean.getFoo()).isEqualTo(5);
	}

	@Test
	public void loadShouldSupportRebindableConfigurationProperties() {
		// gh-9160
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("example.one", "foo");
		sources.addFirst(new MapPropertySource("test-source", source));
		this.context.register(PrototypePropertiesConfiguration.class);
		this.context.refresh();
		PrototypeBean first = this.context.getBean(PrototypeBean.class);
		assertThat(first.getOne()).isEqualTo("foo");
		source.put("example.one", "bar");
		sources.addFirst(new MapPropertySource("extra",
				Collections.singletonMap("example.two", "baz")));
		PrototypeBean second = this.context.getBean(PrototypeBean.class);
		assertThat(second.getOne()).isEqualTo("bar");
		assertThat(second.getTwo()).isEqualTo("baz");
	}

	@Test
	public void loadWhenHasPropertySourcesPlaceholderConfigurerShouldSupportRebindableConfigurationProperties() {
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("example.one", "foo");
		sources.addFirst(new MapPropertySource("test-source", source));
		this.context.register(PrototypePropertiesConfiguration.class);
		this.context.register(PropertySourcesPlaceholderConfigurer.class);
		this.context.refresh();
		PrototypeBean first = this.context.getBean(PrototypeBean.class);
		assertThat(first.getOne()).isEqualTo("foo");
		source.put("example.one", "bar");
		sources.addFirst(new MapPropertySource("extra",
				Collections.singletonMap("example.two", "baz")));
		PrototypeBean second = this.context.getBean(PrototypeBean.class);
		assertThat(second.getOne()).isEqualTo("bar");
		assertThat(second.getTwo()).isEqualTo("baz");
	}

	@Test
	public void customProtocolResolverIsInvoked() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.resource=application.properties");
		ProtocolResolver protocolResolver = mock(ProtocolResolver.class);
		given(protocolResolver.resolve(anyString(), any(ResourceLoader.class)))
				.willReturn(null);
		this.context.addProtocolResolver(protocolResolver);
		this.context.register(PropertiesWithResource.class);
		this.context.refresh();
		verify(protocolResolver).resolve(eq("application.properties"),
				any(ResourceLoader.class));
	}

	@Test
	public void customProtocolResolver() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.resource=test:/application.properties");
		this.context.addProtocolResolver(new TestProtocolResolver());
		this.context.register(PropertiesWithResource.class);
		this.context.refresh();
		Resource resource = this.context.getBean(PropertiesWithResource.class)
				.getResource();
		assertThat(resource).isNotNull();
		assertThat(resource).isInstanceOf(ClassPathResource.class);
		assertThat(resource.exists()).isTrue();
		assertThat(((ClassPathResource) resource).getPath())
				.isEqualTo("application.properties");
	}

	@Test
	public void loadShouldUseConfigurationConverter() {
		prepareConverterContext(ConverterConfiguration.class, PersonProperties.class);
		Person person = this.context.getBean(PersonProperties.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	public void loadWhenConfigurationConverterIsNotQualifiedShouldNotConvert() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectCause(instanceOf(BindException.class));
		prepareConverterContext(NonQualifiedConverterConfiguration.class,
				PersonProperties.class);
	}

	@Test
	public void loadShouldUseGenericConfigurationConverter() {
		prepareConverterContext(GenericConverterConfiguration.class,
				PersonProperties.class);
		Person person = this.context.getBean(PersonProperties.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	public void loadWhenGenericConfigurationConverterIsNotQualifiedShouldNotConvert() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectCause(instanceOf(BindException.class));
		prepareConverterContext(NonQualifiedGenericConverterConfiguration.class,
				PersonProperties.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void loadShouldBindToBeanWithGenerics() {
		load(GenericConfiguration.class, "foo.bar=hello");
		AGenericClass foo = this.context.getBean(AGenericClass.class);
		assertThat(foo.getBar()).isNotNull();
	}

	private void prepareConverterContext(Class<?>... config) {
		load(config, "test.person=John Smith");
	}

	@Test
	public void loadWhenHasConfigurationPropertiesValidatorShouldApplyValidator() {
		try {
			load(WithCustomValidatorConfiguration.class);
			fail("Did not throw");
		}
		catch (Exception ex) {
			assertThat(ex).hasCauseInstanceOf(BindException.class);
			assertThat(ex.getCause())
					.hasCauseExactlyInstanceOf(BindValidationException.class);
		}
	}

	@Test
	public void loadWhenHasUnsupportedConfigurationPropertiesValidatorShouldBind() {
		load(WithUnsupportedCustomValidatorConfiguration.class, "test.foo=bar");
		WithSetterThatThrowsValidationExceptionProperties bean = this.context
				.getBean(WithSetterThatThrowsValidationExceptionProperties.class);
		assertThat(bean.getFoo()).isEqualTo("bar");
	}

	@Test
	public void loadWhenConfigurationPropertiesIsAlsoValidatorShouldApplyValidator() {
		try {
			load(ValidatorProperties.class);
			fail("Did not throw");
		}
		catch (Exception ex) {
			assertThat(ex).hasCauseInstanceOf(BindException.class);
			assertThat(ex.getCause())
					.hasCauseExactlyInstanceOf(BindValidationException.class);
		}
	}

	@Test
	public void loadWhenSetterThrowsValidationExceptionShouldFail() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectCause(instanceOf(BindException.class));
		load(WithSetterThatThrowsValidationExceptionProperties.class, "test.foo=spam");
	}

	@Test
	public void loadWhenFailsShouldIncludeAnnotationDetails() {
		removeSystemProperties();
		this.thrown.expectMessage("Could not bind properties to "
				+ "'ConfigurationPropertiesTests.IgnoreUnknownFieldsFalseProperties' : "
				+ "prefix=, ignoreInvalidFields=false, ignoreUnknownFields=false;");
		load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo", "bar=baz");
	}

	@Test
	public void loadWhenHasCustomPropertyEditorShouldBind() {
		this.context.getBeanFactory().registerCustomEditor(Person.class,
				PersonPropertyEditor.class);
		load(PersonProperties.class, "test.person=boot,spring");
		PersonProperties bean = this.context.getBean(PersonProperties.class);
		assertThat(bean.getPerson().firstName).isEqualTo("spring");
		assertThat(bean.getPerson().lastName).isEqualTo("boot");
	}

	@Test
	public void loadWhenBindingToListOfGenericClassShouldBind() {
		// gh-12166
		load(ListOfGenericClassProperties.class, "test.list=java.lang.RuntimeException");
		ListOfGenericClassProperties bean = this.context
				.getBean(ListOfGenericClassProperties.class);
		assertThat(bean.getList()).containsExactly(RuntimeException.class);
	}

	@Test
	public void loadWhenBindingCurrentDirectoryToFileShouldBind() {
		load(FileProperties.class, "test.file=.");
		FileProperties bean = this.context.getBean(FileProperties.class);
		assertThat(bean.getFile()).isEqualTo(new File("."));
	}

	private AnnotationConfigApplicationContext load(Class<?> configuration,
			String... inlinedProperties) {
		return load(new Class<?>[] { configuration }, inlinedProperties);
	}

	private AnnotationConfigApplicationContext load(Class<?>[] configuration,
			String... inlinedProperties) {
		this.context.register(configuration);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				inlinedProperties);
		this.context.refresh();
		return this.context;
	}

	/**
	 * Strict tests need a known set of properties so we remove system items which may be
	 * environment specific.
	 */
	private void removeSystemProperties() {
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		sources.remove("systemProperties");
		sources.remove("systemEnvironment");
	}

	private void resetContext() {
		this.context.close();
		this.context = new AnnotationConfigApplicationContext();
	}

	@Configuration
	@EnableConfigurationProperties(BasicProperties.class)
	static class BasicConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(NestedProperties.class)
	static class NestedConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(IgnoreUnknownFieldsFalseProperties.class)
	static class IgnoreUnknownFieldsFalseConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(PrefixProperties.class)
	static class PrefixConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(ValidatedJsr303Properties.class)
	static class ValidatedJsr303Configuration {

	}

	@Configuration
	@EnableConfigurationProperties
	static class ValidatedOnBeanJsr303Configuration {

		@Bean
		@Validated
		public NonValidatedJsr303Properties properties() {
			return new NonValidatedJsr303Properties();
		}
	}

	@Configuration
	@EnableConfigurationProperties(NonValidatedJsr303Properties.class)
	static class NonValidatedJsr303Configuration {

	}

	@Configuration
	@EnableConfigurationProperties(AnnotationOnBaseClassProperties.class)
	static class AnnotationOnBaseClassConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(WithoutAndAnnotationConfiguration.class)
	static class WithoutAndAnnotationConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(WithoutAnnotationValueProperties.class)
	static class WithoutAnnotationValueConfiguration {

	}

	@Configuration
	@ImportResource("org/springframework/boot/context/properties/testProperties.xml")
	static class DefaultsInXmlConfiguration {

	}

	@Configuration
	static class DefaultsInJavaConfiguration {

		@Bean
		public BasicProperties basicProperties() {
			BasicProperties test = new BasicProperties();
			test.setName("bar");
			return test;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	static class PrefixPropertiesDeclaredAsBeanConfiguration {

		@Bean
		public PrefixProperties prefixProperties() {
			return new PrefixProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties(PrefixProperties.class)
	static class PrefixPropertiesDeclaredAsAnnotationValueConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties({ PrefixProperties.class,
			AnotherPrefixProperties.class })
	static class MultiplePrefixPropertiesDeclaredAsAnnotationValueConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties
	static class PrefixedPropertiesReplacedOnBeanMethodConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spam")
		public PrefixProperties prefixProperties() {
			return new PrefixProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	static class ValidatedImplementationConfiguration {

		@Bean
		public ValidatedImplementationProperties testProperties() {
			return new ValidatedImplementationProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties
	static class WithPostConstructConfiguration {

		private String bar;

		private boolean initialized;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

		@PostConstruct
		public void init() {
			assertThat(this.bar).isNotNull();
			this.initialized = true;
		}

	}

	@Configuration
	@EnableConfigurationProperties(WithPropertyPlaceholderValueProperties.class)
	static class WithPropertyPlaceholderValueConfiguration {

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	static class WithFactoryBeanConfiguration {

		public static boolean factoryBeanInitialized;

	}

	@Configuration
	@EnableConfigurationProperties
	static class MultiplePropertySourcesPlaceholderConfigurerConfiguration {

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer1() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer2() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	static class PrototypePropertiesConfiguration {

		@Bean
		@Scope("prototype")
		@ConfigurationProperties("example")
		public PrototypeBean prototypeBean() {
			return new PrototypeBean();
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	public static class PropertiesWithResource {

		private Resource resource;

		public Resource getResource() {
			return this.resource;
		}

		public void setResource(Resource resource) {
			this.resource = resource;
		}

	}

	private static class TestProtocolResolver implements ProtocolResolver {

		public static final String PREFIX = "test:/";

		@Override
		public Resource resolve(String location, ResourceLoader resourceLoader) {
			if (location.startsWith(PREFIX)) {
				String path = location.substring(PREFIX.length(), location.length());
				return new ClassPathResource(path);
			}
			return null;
		}

	}

	@Configuration
	static class ConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		public Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration
	static class NonQualifiedConverterConfiguration {

		@Bean
		public Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration
	static class GenericConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		public GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	@Configuration
	static class NonQualifiedGenericConverterConfiguration {

		@Bean
		public GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	static class GenericConfiguration {

		@Bean
		@ConfigurationProperties("foo")
		public AGenericClass<String> aBeanToBind() {
			return new AGenericClass<>();
		}

	}

	@Configuration
	@EnableConfigurationProperties(WithCustomValidatorProperties.class)
	static class WithCustomValidatorConfiguration {

		@Bean(name = ConfigurationPropertiesBindingPostProcessor.VALIDATOR_BEAN_NAME)
		public CustomPropertiesValidator validator() {
			return new CustomPropertiesValidator();
		}

	}

	@Configuration
	@EnableConfigurationProperties(WithSetterThatThrowsValidationExceptionProperties.class)
	static class WithUnsupportedCustomValidatorConfiguration {

		@Bean(name = ConfigurationPropertiesBindingPostProcessor.VALIDATOR_BEAN_NAME)
		public CustomPropertiesValidator validator() {
			return new CustomPropertiesValidator();
		}

	}

	static class AGenericClass<T> {

		private T bar;

		public T getBar() {
			return this.bar;
		}

		public void setBar(T bar) {
			this.bar = bar;
		}

	}

	static class PrototypeBean {

		private String one;

		private String two;

		public String getOne() {
			return this.one;
		}

		public void setOne(String one) {
			this.one = one;
		}

		public String getTwo() {
			return this.two;
		}

		public void setTwo(String two) {
			this.two = two;
		}

	}

	// Must be a raw type
	@SuppressWarnings("rawtypes")
	static class FactoryBeanTester implements FactoryBean, InitializingBean {

		@Override
		public Object getObject() {
			return Object.class;
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public void afterPropertiesSet() {
			WithFactoryBeanConfiguration.factoryBeanInitialized = true;
		}

	}

	@ConfigurationProperties
	static class BasicProperties {

		private String name;

		private int[] array;

		private List<Integer> list = new ArrayList<>();

		private Duration duration;

		// No getter - you should be able to bind to a write-only bean

		public void setName(String name) {
			this.name = name;
		}

		public void setArray(int... values) {
			this.array = values;
		}

		public int[] getArray() {
			return this.array;
		}

		public List<Integer> getList() {
			return this.list;
		}

		public void setList(List<Integer> list) {
			this.list = list;
		}

		public Duration getDuration() {
			return this.duration;
		}

		public void setDuration(Duration duration) {
			this.duration = duration;
		}

	}

	@ConfigurationProperties
	static class NestedProperties {

		private String name;

		private final Nested nested = new Nested();

		public void setName(String name) {
			this.name = name;
		}

		public Nested getNested() {
			return this.nested;
		}

		protected static class Nested {

			private String name;

			public void setName(String name) {
				this.name = name;
			}

		}

	}

	@ConfigurationProperties(ignoreUnknownFields = false)
	static class IgnoreUnknownFieldsFalseProperties extends BasicProperties {

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "com.example", ignoreInvalidFields = true)
	static class IgnoreInvalidFieldsFalseProperties {

		private long bar;

		public void setBar(long bar) {
			this.bar = bar;
		}

		public long getBar() {
			return this.bar;
		}

	}

	@ConfigurationProperties(prefix = "spring.foo")
	static class PrefixProperties extends BasicProperties {

	}

	@ConfigurationProperties(prefix = "spring.bar")
	static class AnotherPrefixProperties extends BasicProperties {

	}

	static class Jsr303Properties extends BasicProperties {

		@NotEmpty
		private String description;

		public String getDescription() {
			return this.description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

	}

	@ConfigurationProperties
	@Validated
	static class ValidatedJsr303Properties extends Jsr303Properties {

	}

	@ConfigurationProperties
	static class NonValidatedJsr303Properties extends Jsr303Properties {

	}

	@EnableConfigurationProperties
	@ConfigurationProperties
	@Validated
	static class ValidatedNestedJsr303Properties {

		private Jsr303Properties properties;

		public Jsr303Properties getProperties() {
			return this.properties;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties
	@Validated
	static class ValidatedValidNestedJsr303Properties {

		@Valid
		private List<Jsr303Properties> properties = Collections
				.singletonList(new Jsr303Properties());

		public List<Jsr303Properties> getProperties() {
			return this.properties;
		}

	}

	static class AnnotationOnBaseClassProperties extends BasicProperties {

	}

	@ConfigurationProperties
	static class WithoutAnnotationValueProperties {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		// No getter - you should be able to bind to a write-only bean

	}

	@EnableConfigurationProperties
	@ConfigurationProperties
	static class MapProperties {

		private Map<String, String> mymap;

		public void setMymap(Map<String, String> mymap) {
			this.mymap = mymap;
		}

		public Map<String, String> getMymap() {
			return this.mymap;
		}

	}

	@Component
	static class BasicPropertiesConsumer {

		@Autowired
		private BasicProperties properties;

		@PostConstruct
		public void init() {
			assertThat(this.properties).isNotNull();
		}

		public String getName() {
			return this.properties.name;
		}

	}

	interface InterfaceForValidatedImplementation {

		String getFoo();
	}

	@ConfigurationProperties("test")
	@Validated
	static class ValidatedImplementationProperties
			implements InterfaceForValidatedImplementation {

		@NotNull
		private String foo;

		@Override
		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@ConfigurationProperties(prefix = "test")
	@Validated
	static class WithPropertyPlaceholderValueProperties {

		@Value("${default.value}")
		private String value;

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class WithEnumProperties {

		private FooEnum theValue;

		private List<FooEnum> theValues;

		public void setTheValue(FooEnum value) {
			this.theValue = value;
		}

		public FooEnum getTheValue() {
			return this.theValue;
		}

		public List<FooEnum> getTheValues() {
			return this.theValues;
		}

		public void setTheValues(List<FooEnum> theValues) {
			this.theValues = theValues;
		}

	}

	enum FooEnum {

		FOO, BAZ, BAR

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test", ignoreUnknownFields = false)
	static class WithCharArrayProperties {

		private char[] chars;

		public char[] getChars() {
			return this.chars;
		}

		public void setChars(char[] chars) {
			this.chars = chars;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class WithRelaxedNamesProperties {

		private String fooBar;

		private String barBAZ;

		public String getFooBar() {
			return this.fooBar;
		}

		public void setFooBar(String fooBar) {
			this.fooBar = fooBar;
		}

		public String getBarBAZ() {
			return this.barBAZ;
		}

		public void setBarBAZ(String barBAZ) {
			this.barBAZ = barBAZ;
		}

	}

	@Validated
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class WithMapProperties {

		private Map<String, String> map;

		public Map<String, String> getMap() {
			return this.map;
		}

		public void setMap(Map<String, String> map) {
			this.map = map;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class WithComplexMapProperties {

		private Map<String, Map<String, String>> map;

		public Map<String, Map<String, String>> getMap() {
			return this.map;
		}

		public void setMap(Map<String, Map<String, String>> map) {
			this.map = map;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "com.example", ignoreUnknownFields = false)
	static class SimplePrefixedProperties {

		private int foo;

		private String bar;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

		public int getFoo() {
			return this.foo;
		}

		public void setFoo(int foo) {
			this.foo = foo;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class PersonProperties {

		private Person person;

		public Person getPerson() {
			return this.person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "sample")
	static class MapWithNumericKeyProperties {

		private Map<String, BasicProperties> properties = new LinkedHashMap<>();

		public Map<String, BasicProperties> getProperties() {
			return this.properties;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties
	static class ValidatorProperties implements Validator {

		private String foo;

		@Override
		public boolean supports(Class<?> type) {
			return type == ValidatorProperties.class;
		}

		@Override
		public void validate(Object target, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "foo", "TEST1");
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class WithSetterThatThrowsValidationExceptionProperties {

		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
			if (!foo.equals("bar")) {
				throw new IllegalArgumentException("Wrong value for foo");
			}
		}

	}

	@ConfigurationProperties(prefix = "custom")
	static class WithCustomValidatorProperties {

		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class ListOfGenericClassProperties {

		private List<Class<? extends Throwable>> list;

		public List<Class<? extends Throwable>> getList() {
			return this.list;
		}

		public void setList(List<Class<? extends Throwable>> list) {
			this.list = list;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class FileProperties {

		private File file;

		public File getFile() {
			return this.file;
		}

		public void setFile(File file) {
			this.file = file;
		}

	}

	static class CustomPropertiesValidator implements Validator {

		@Override
		public boolean supports(Class<?> type) {
			return type == WithCustomValidatorProperties.class;
		}

		@Override
		public void validate(Object target, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "foo", "TEST1");
		}

	}

	static class PersonConverter implements Converter<String, Person> {

		@Nullable
		@Override
		public Person convert(String source) {
			String[] content = StringUtils.split(source, " ");
			return new Person(content[0], content[1]);
		}
	}

	static class GenericPersonConverter implements GenericConverter {

		@Nullable
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, Person.class));
		}

		@Nullable
		@Override
		public Object convert(@Nullable Object source, TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			String[] content = StringUtils.split((String) source, " ");
			return new Person(content[0], content[1]);
		}
	}

	static class PersonPropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			String[] split = text.split(",");
			setValue(new Person(split[1], split[0]));
		}

	}

	static class Person {

		private final String firstName;

		private final String lastName;

		Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

	}

}
