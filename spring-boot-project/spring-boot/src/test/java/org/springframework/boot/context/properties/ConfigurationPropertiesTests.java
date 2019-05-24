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
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
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
import org.springframework.mock.env.MockEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
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
@ExtendWith(OutputCaptureExtension.class)
class ConfigurationPropertiesTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@AfterEach
	public void cleanup() {
		this.context.close();
		System.clearProperty("name");
		System.clearProperty("nested.name");
		System.clearProperty("nested_name");
	}

	@Test
	void loadShouldBind() {
		load(BasicConfiguration.class, "name=foo");
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.containsBean(BasicProperties.class.getName())).isTrue();
		assertThat(this.context.getBean(BasicProperties.class).name).isEqualTo("foo");
	}

	@Test
	void loadShouldBindNested() {
		load(NestedConfiguration.class, "name=foo", "nested.name=bar");
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name).isEqualTo("bar");
	}

	@Test
	void loadWhenUsingSystemPropertiesShouldBind() {
		System.setProperty("name", "foo");
		load(BasicConfiguration.class);
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.getBean(BasicProperties.class).name).isEqualTo("foo");
	}

	@Test
	void loadWhenUsingSystemPropertiesShouldBindNested() {
		System.setProperty("name", "foo");
		System.setProperty("nested.name", "bar");
		load(NestedConfiguration.class);
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name).isEqualTo("bar");
	}

	@Test
	void loadWhenHasIgnoreUnknownFieldsFalseAndNoUnknownFieldsShouldBind() {
		removeSystemProperties();
		load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo");
		IgnoreUnknownFieldsFalseProperties bean = this.context.getBean(IgnoreUnknownFieldsFalseProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	void loadWhenHasIgnoreUnknownFieldsFalseAndUnknownFieldsShouldFail() {
		removeSystemProperties();
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
				.isThrownBy(() -> load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo", "bar=baz"))
				.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadWhenHasIgnoreInvalidFieldsTrueAndInvalidFieldsShouldBind() {
		load(IgnoreInvalidFieldsFalseProperties.class, "com.example.bar=spam");
		IgnoreInvalidFieldsFalseProperties bean = this.context.getBean(IgnoreInvalidFieldsFalseProperties.class);
		assertThat(bean.getBar()).isEqualTo(0);
	}

	@Test
	void loadWhenHasPrefixShouldBind() {
		load(PrefixConfiguration.class, "spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	void loadWhenPropertiesHaveAnnotationOnBaseClassShouldBind() {
		load(AnnotationOnBaseClassConfiguration.class, "name=foo");
		AnnotationOnBaseClassProperties bean = this.context.getBean(AnnotationOnBaseClassProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	void loadWhenBindingArrayShouldBind() {
		load(BasicConfiguration.class, "name=foo", "array=1,2,3");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.array).containsExactly(1, 2, 3);
	}

	@Test
	void loadWhenBindingArrayFromYamlArrayShouldBind() {
		load(BasicConfiguration.class, "name=foo", "list[0]=1", "list[1]=2", "list[2]=3");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.list).containsExactly(1, 2, 3);
	}

	@Test
	void loadWhenBindingOver256ElementsShouldBind() {
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
	void loadWhenBindingWithoutAndAnnotationShouldFail() {
		assertThatIllegalArgumentException().isThrownBy(() -> load(WithoutAndAnnotationConfiguration.class, "name:foo"))
				.withMessageContaining("No ConfigurationProperties annotation found");
	}

	@Test
	void loadWhenBindingWithoutAnnotationValueShouldBind() {
		load(WithoutAnnotationValueConfiguration.class, "name=foo");
		WithoutAnnotationValueProperties bean = this.context.getBean(WithoutAnnotationValueProperties.class);
		assertThat(bean.name).isEqualTo("foo");
	}

	@Test
	void loadWhenBindingWithDefaultsInXmlShouldBind() {
		load(new Class<?>[] { BasicConfiguration.class, DefaultsInXmlConfiguration.class });
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("bar");
	}

	@Test
	void loadWhenBindingWithDefaultsInJavaConfigurationShouldBind() {
		load(DefaultsInJavaConfiguration.class);
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("bar");
	}

	@Test
	void loadWhenBindingTwoBeansShouldBind() {
		load(new Class<?>[] { WithoutAnnotationValueConfiguration.class, BasicConfiguration.class });
		assertThat(this.context.getBean(BasicProperties.class)).isNotNull();
		assertThat(this.context.getBean(WithoutAnnotationValueProperties.class)).isNotNull();
	}

	@Test
	void loadWhenBindingWithParentContextShouldBind() {
		AnnotationConfigApplicationContext parent = load(BasicConfiguration.class, "name=parent");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		load(new Class[] { BasicConfiguration.class, BasicPropertiesConsumer.class }, "name=child");
		assertThat(this.context.getBean(BasicProperties.class)).isNotNull();
		assertThat(parent.getBean(BasicProperties.class)).isNotNull();
		assertThat(this.context.getBean(BasicPropertiesConsumer.class).getName()).isEqualTo("parent");
		parent.close();
	}

	@Test
	void loadWhenBindingOnlyParentContextShouldBind() {
		AnnotationConfigApplicationContext parent = load(BasicConfiguration.class, "name=foo");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		load(BasicPropertiesConsumer.class);
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).isEmpty();
		assertThat(parent.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.getBean(BasicPropertiesConsumer.class).getName()).isEqualTo("foo");
	}

	@Test
	void loadWhenPrefixedPropertiesDeclaredAsBeanShouldBind() {
		load(PrefixPropertiesDeclaredAsBeanConfiguration.class, "spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	void loadWhenPrefixedPropertiesDeclaredAsAnnotationValueShouldBind() {
		load(PrefixPropertiesDeclaredAsAnnotationValueConfiguration.class, "spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean("spring.foo-" + PrefixProperties.class.getName(),
				PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	void loadWhenMultiplePrefixedPropertiesDeclaredAsAnnotationValueShouldBind() {
		load(MultiplePrefixPropertiesDeclaredAsAnnotationValueConfiguration.class, "spring.foo.name=foo",
				"spring.bar.name=bar");
		PrefixProperties bean1 = this.context.getBean(PrefixProperties.class);
		AnotherPrefixProperties bean2 = this.context.getBean(AnotherPrefixProperties.class);
		assertThat(((BasicProperties) bean1).name).isEqualTo("foo");
		assertThat(((BasicProperties) bean2).name).isEqualTo("bar");
	}

	@Test
	void loadWhenBindingToMapKeyWithPeriodShouldBind() {
		load(MapProperties.class, "mymap.key1.key2:value12", "mymap.key3:value3");
		MapProperties bean = this.context.getBean(MapProperties.class);
		assertThat(bean.mymap).containsOnly(entry("key3", "value3"), entry("key1.key2", "value12"));
	}

	@Test
	void loadWhenPrefixedPropertiesAreReplacedOnBeanMethodShouldBind() {
		load(PrefixedPropertiesReplacedOnBeanMethodConfiguration.class, "external.name=bar", "spam.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	void loadShouldBindToJavaTimeDuration() {
		load(BasicConfiguration.class, "duration=PT1M");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.getDuration().getSeconds()).isEqualTo(60);
	}

	@Test
	void loadWhenBindingToValidatedImplementationOfInterfaceShouldBind() {
		load(ValidatedImplementationConfiguration.class, "test.foo=bar");
		ValidatedImplementationProperties bean = this.context.getBean(ValidatedImplementationProperties.class);
		assertThat(bean.getFoo()).isEqualTo("bar");
	}

	@Test
	void loadWithPropertyPlaceholderValueShouldBind() {
		load(WithPropertyPlaceholderValueConfiguration.class, "default.value=foo");
		WithPropertyPlaceholderValueProperties bean = this.context
				.getBean(WithPropertyPlaceholderValueProperties.class);
		assertThat(bean.getValue()).isEqualTo("foo");
	}

	@Test
	void loadWithPropertyPlaceholderShouldNotAlterPropertySourceOrder() {
		load(WithPropertyPlaceholderWithLocalPropertiesValueConfiguration.class, "com.example.bar=a");
		SimplePrefixedProperties bean = this.context.getBean(SimplePrefixedProperties.class);
		assertThat(bean.getBar()).isEqualTo("a");
	}

	@Test
	void loadWhenHasPostConstructShouldTriggerPostConstructWithBoundBean() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("bar", "foo");
		this.context.setEnvironment(environment);
		this.context.register(WithPostConstructConfiguration.class);
		this.context.refresh();
		WithPostConstructConfiguration bean = this.context.getBean(WithPostConstructConfiguration.class);
		assertThat(bean.initialized).isTrue();
	}

	@Test
	void loadShouldNotInitializeFactoryBeans() {
		WithFactoryBeanConfiguration.factoryBeanInitialized = false;
		this.context = new AnnotationConfigApplicationContext() {

			@Override
			protected void onRefresh() throws BeansException {
				assertThat(WithFactoryBeanConfiguration.factoryBeanInitialized).as("Initialized too early").isFalse();
				super.onRefresh();
			}

		};
		this.context.register(WithFactoryBeanConfiguration.class);
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(FactoryBeanTester.class);
		beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		this.context.registerBeanDefinition("test", beanDefinition);
		this.context.refresh();
		assertThat(WithFactoryBeanConfiguration.factoryBeanInitialized).as("Not Initialized").isTrue();
	}

	@Test
	void loadWhenUsingRelaxedFormsShouldBindToEnum() {
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
	void loadWhenUsingRelaxedFormsShouldBindToEnumSet() {
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
	void loadShouldBindToCharArray() {
		load(WithCharArrayProperties.class, "test.chars=word");
		WithCharArrayProperties bean = this.context.getBean(WithCharArrayProperties.class);
		assertThat(bean.getChars()).isEqualTo("word".toCharArray());
	}

	@Test
	void loadWhenUsingRelaxedFormsAndOverrideShouldBind() {
		load(WithRelaxedNamesProperties.class, "test.FOO_BAR=test1", "test.FOO_BAR=test2", "test.BAR-B-A-Z=testa",
				"test.BAR-B-A-Z=testb");
		WithRelaxedNamesProperties bean = this.context.getBean(WithRelaxedNamesProperties.class);
		assertThat(bean.getFooBar()).isEqualTo("test2");
		assertThat(bean.getBarBAZ()).isEqualTo("testb");
	}

	@Test
	void loadShouldBindToMap() {
		load(WithMapProperties.class, "test.map.foo=bar");
		WithMapProperties bean = this.context.getBean(WithMapProperties.class);
		assertThat(bean.getMap()).containsOnly(entry("foo", "bar"));
	}

	@Test
	void loadShouldBindToMapWithNumericKey() {
		load(MapWithNumericKeyProperties.class, "sample.properties.1.name=One");
		MapWithNumericKeyProperties bean = this.context.getBean(MapWithNumericKeyProperties.class);
		assertThat(bean.getProperties().get("1").name).isEqualTo("One");
	}

	@Test
	void loadWhenUsingSystemPropertiesShouldBindToMap() {
		this.context.getEnvironment().getPropertySources().addLast(
				new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						Collections.singletonMap("TEST_MAP_FOO_BAR", "baz")));
		load(WithComplexMapProperties.class);
		WithComplexMapProperties bean = this.context.getBean(WithComplexMapProperties.class);
		assertThat(bean.getMap()).containsOnlyKeys("foo");
		assertThat(bean.getMap().get("foo")).containsOnly(entry("bar", "baz"));
	}

	@Test
	void loadWhenDotsInSystemEnvironmentPropertiesShouldBind() {
		this.context.getEnvironment().getPropertySources().addLast(
				new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						Collections.singletonMap("com.example.bar", "baz")));
		load(SimplePrefixedProperties.class);
		SimplePrefixedProperties bean = this.context.getBean(SimplePrefixedProperties.class);
		assertThat(bean.getBar()).isEqualTo("baz");
	}

	@Test
	void loadWhenOverridingPropertiesShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		sources.addFirst(
				new SystemEnvironmentPropertySource("system", Collections.singletonMap("SPRING_FOO_NAME", "Jane")));
		sources.addLast(new MapPropertySource("test", Collections.singletonMap("spring.foo.name", "John")));
		load(PrefixConfiguration.class);
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("Jane");
	}

	@Test
	void loadWhenJsr303ConstraintDoesNotMatchShouldFail() {
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
				.isThrownBy(() -> load(ValidatedJsr303Configuration.class, "description="))
				.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadValidatedOnBeanMethodAndJsr303ConstraintDoesNotMatchShouldFail() {
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
				.isThrownBy(() -> load(ValidatedOnBeanJsr303Configuration.class, "description="))
				.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadWhenJsr303ConstraintDoesNotMatchOnNestedThatIsNotDirectlyAnnotatedShouldFail() {
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
				.isThrownBy(() -> load(ValidatedNestedJsr303Properties.class, "properties.description="))
				.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadWhenJsr303ConstraintDoesNotMatchOnNestedThatIsNotDirectlyAnnotatedButIsValidShouldFail() {
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
				.isThrownBy(() -> load(ValidatedValidNestedJsr303Properties.class))
				.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadWhenJsr303ConstraintMatchesShouldBind() {
		load(ValidatedJsr303Configuration.class, "description=foo");
		ValidatedJsr303Properties bean = this.context.getBean(ValidatedJsr303Properties.class);
		assertThat(bean.getDescription()).isEqualTo("foo");
	}

	@Test
	void loadWhenJsr303ConstraintDoesNotMatchAndNotValidatedAnnotationShouldBind() {
		load(NonValidatedJsr303Configuration.class, "name=foo");
		NonValidatedJsr303Properties bean = this.context.getBean(NonValidatedJsr303Properties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	void loadWhenHasMultiplePropertySourcesPlaceholderConfigurerShouldLogWarning(CapturedOutput capturedOutput) {
		load(MultiplePropertySourcesPlaceholderConfigurerConfiguration.class);
		assertThat(capturedOutput).contains("Multiple PropertySourcesPlaceholderConfigurer beans registered");
	}

	@Test
	void loadWhenOverridingPropertiesWithPlaceholderResolutionInEnvironmentShouldBindWithOverride() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		sources.addFirst(
				new SystemEnvironmentPropertySource("system", Collections.singletonMap("COM_EXAMPLE_BAR", "10")));
		Map<String, Object> source = new HashMap<>();
		source.put("com.example.bar", 5);
		source.put("com.example.foo", "${com.example.bar}");
		sources.addLast(new MapPropertySource("test", source));
		load(SimplePrefixedProperties.class);
		SimplePrefixedProperties bean = this.context.getBean(SimplePrefixedProperties.class);
		assertThat(bean.getFoo()).isEqualTo(10);
	}

	@Test
	void loadWhenHasUnboundElementsFromSystemEnvironmentShouldNotThrowException() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		sources.addFirst(new MapPropertySource("test", Collections.singletonMap("com.example.foo", 5)));
		sources.addLast(new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("COM_EXAMPLE_OTHER", "10")));
		load(SimplePrefixedProperties.class);
		SimplePrefixedProperties bean = this.context.getBean(SimplePrefixedProperties.class);
		assertThat(bean.getFoo()).isEqualTo(5);
	}

	@Test
	void loadShouldSupportRebindableConfigurationProperties() {
		// gh-9160
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("example.one", "foo");
		sources.addFirst(new MapPropertySource("test-source", source));
		this.context.register(PrototypePropertiesConfiguration.class);
		this.context.refresh();
		PrototypeBean first = this.context.getBean(PrototypeBean.class);
		assertThat(first.getOne()).isEqualTo("foo");
		source.put("example.one", "bar");
		sources.addFirst(new MapPropertySource("extra", Collections.singletonMap("example.two", "baz")));
		PrototypeBean second = this.context.getBean(PrototypeBean.class);
		assertThat(second.getOne()).isEqualTo("bar");
		assertThat(second.getTwo()).isEqualTo("baz");
	}

	@Test
	void loadWhenHasPropertySourcesPlaceholderConfigurerShouldSupportRebindableConfigurationProperties() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("example.one", "foo");
		sources.addFirst(new MapPropertySource("test-source", source));
		this.context.register(PrototypePropertiesConfiguration.class);
		this.context.register(PropertySourcesPlaceholderConfigurer.class);
		this.context.refresh();
		PrototypeBean first = this.context.getBean(PrototypeBean.class);
		assertThat(first.getOne()).isEqualTo("foo");
		source.put("example.one", "bar");
		sources.addFirst(new MapPropertySource("extra", Collections.singletonMap("example.two", "baz")));
		PrototypeBean second = this.context.getBean(PrototypeBean.class);
		assertThat(second.getOne()).isEqualTo("bar");
		assertThat(second.getTwo()).isEqualTo("baz");
	}

	@Test
	void customProtocolResolverIsInvoked() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "test.resource=application.properties");
		ProtocolResolver protocolResolver = mock(ProtocolResolver.class);
		given(protocolResolver.resolve(anyString(), any(ResourceLoader.class))).willReturn(null);
		this.context.addProtocolResolver(protocolResolver);
		this.context.register(PropertiesWithResource.class);
		this.context.refresh();
		verify(protocolResolver).resolve(eq("application.properties"), any(ResourceLoader.class));
	}

	@Test
	void customProtocolResolver() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.resource=test:/application.properties");
		this.context.addProtocolResolver(new TestProtocolResolver());
		this.context.register(PropertiesWithResource.class);
		this.context.refresh();
		Resource resource = this.context.getBean(PropertiesWithResource.class).getResource();
		assertThat(resource).isNotNull();
		assertThat(resource).isInstanceOf(ClassPathResource.class);
		assertThat(resource.exists()).isTrue();
		assertThat(((ClassPathResource) resource).getPath()).isEqualTo("application.properties");
	}

	@Test
	void loadShouldUseConfigurationConverter() {
		prepareConverterContext(ConverterConfiguration.class, PersonProperties.class);
		Person person = this.context.getBean(PersonProperties.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	void loadWhenConfigurationConverterIsNotQualifiedShouldNotConvert() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(
						() -> prepareConverterContext(NonQualifiedConverterConfiguration.class, PersonProperties.class))
				.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadShouldUseGenericConfigurationConverter() {
		prepareConverterContext(GenericConverterConfiguration.class, PersonProperties.class);
		Person person = this.context.getBean(PersonProperties.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	void loadWhenGenericConfigurationConverterIsNotQualifiedShouldNotConvert() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				() -> prepareConverterContext(NonQualifiedGenericConverterConfiguration.class, PersonProperties.class))
				.withCauseInstanceOf(BindException.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void loadShouldBindToBeanWithGenerics() {
		load(GenericConfiguration.class, "foo.bar=hello");
		AGenericClass foo = this.context.getBean(AGenericClass.class);
		assertThat(foo.getBar()).isNotNull();
	}

	private void prepareConverterContext(Class<?>... config) {
		load(config, "test.person=John Smith");
	}

	@Test
	void loadWhenHasConfigurationPropertiesValidatorShouldApplyValidator() {
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> load(WithCustomValidatorConfiguration.class))
				.satisfies((ex) -> {
					assertThat(ex).hasCauseInstanceOf(BindException.class);
					assertThat(ex.getCause()).hasCauseExactlyInstanceOf(BindValidationException.class);
				});
	}

	@Test
	void loadWhenHasUnsupportedConfigurationPropertiesValidatorShouldBind() {
		load(WithUnsupportedCustomValidatorConfiguration.class, "test.foo=bar");
		WithSetterThatThrowsValidationExceptionProperties bean = this.context
				.getBean(WithSetterThatThrowsValidationExceptionProperties.class);
		assertThat(bean.getFoo()).isEqualTo("bar");
	}

	@Test
	void loadWhenConfigurationPropertiesIsAlsoValidatorShouldApplyValidator() {
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> load(ValidatorProperties.class)).satisfies((ex) -> {
			assertThat(ex).hasCauseInstanceOf(BindException.class);
			assertThat(ex.getCause()).hasCauseExactlyInstanceOf(BindValidationException.class);
		});
	}

	@Test
	void loadWhenSetterThrowsValidationExceptionShouldFail() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> load(WithSetterThatThrowsValidationExceptionProperties.class, "test.foo=spam"))
				.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadWhenFailsShouldIncludeAnnotationDetails() {
		removeSystemProperties();
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
				.isThrownBy(() -> load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo", "bar=baz"))
				.withMessageContaining("Could not bind properties to "
						+ "'ConfigurationPropertiesTests.IgnoreUnknownFieldsFalseProperties' : "
						+ "prefix=, ignoreInvalidFields=false, ignoreUnknownFields=false;");
	}

	@Test
	void loadWhenHasCustomPropertyEditorShouldBind() {
		this.context.getBeanFactory().registerCustomEditor(Person.class, PersonPropertyEditor.class);
		load(PersonProperties.class, "test.person=boot,spring");
		PersonProperties bean = this.context.getBean(PersonProperties.class);
		assertThat(bean.getPerson().firstName).isEqualTo("spring");
		assertThat(bean.getPerson().lastName).isEqualTo("boot");
	}

	@Test
	void loadWhenBindingToListOfGenericClassShouldBind() {
		// gh-12166
		load(ListOfGenericClassProperties.class, "test.list=java.lang.RuntimeException");
		ListOfGenericClassProperties bean = this.context.getBean(ListOfGenericClassProperties.class);
		assertThat(bean.getList()).containsExactly(RuntimeException.class);
	}

	@Test
	void loadWhenBindingCurrentDirectoryToFileShouldBind() {
		load(FileProperties.class, "test.file=.");
		FileProperties bean = this.context.getBean(FileProperties.class);
		assertThat(bean.getFile()).isEqualTo(new File("."));
	}

	@Test
	void loadWhenBindingToDataSizeShouldBind() {
		load(DataSizeProperties.class, "test.size=10GB", "test.another-size=5");
		DataSizeProperties bean = this.context.getBean(DataSizeProperties.class);
		assertThat(bean.getSize()).isEqualTo(DataSize.ofGigabytes(10));
		assertThat(bean.getAnotherSize()).isEqualTo(DataSize.ofKilobytes(5));
	}

	@Test
	void loadWhenTopLevelConverterNotFoundExceptionShouldNotFail() {
		load(PersonProperties.class, "test=boot");
	}

	@Test
	void loadWhenConfigurationPropertiesContainsMapWithPositiveAndNegativeIntegerKeys() {
		// gh-14136
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.map.x.[-1].a", "baz");
		source.put("test.map.x.1.a", "bar");
		source.put("test.map.x.1.b", 1);
		sources.addLast(new MapPropertySource("test", source));
		load(WithIntegerMapProperties.class);
		WithIntegerMapProperties bean = this.context.getBean(WithIntegerMapProperties.class);
		Map<Integer, Foo> x = bean.getMap().get("x");
		assertThat(x.get(-1).getA()).isEqualTo("baz");
		assertThat(x.get(-1).getB()).isEqualTo(0);
		assertThat(x.get(1).getA()).isEqualTo("bar");
		assertThat(x.get(1).getB()).isEqualTo(1);
	}

	@Test
	void loadWhenConfigurationPropertiesInjectsAnotherBeanShouldNotFail() {
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
				.isThrownBy(() -> load(OtherInjectPropertiesConfiguration.class))
				.withMessageContaining(OtherInjectedProperties.class.getName())
				.withMessageContaining("Failed to bind properties under 'test'");
	}

	@Test
	void loadWhenBindingToConstructorParametersShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.foo", "baz");
		source.put("test.bar", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(ConstructorParameterConfiguration.class);
		ConstructorParameterProperties bean = this.context.getBean(ConstructorParameterProperties.class);
		assertThat(bean.getFoo()).isEqualTo("baz");
		assertThat(bean.getBar()).isEqualTo(5);
	}

	@Test
	void loadWhenBindingToConstructorParametersWithDefaultValuesShouldBind() {
		load(ConstructorParameterConfiguration.class);
		ConstructorParameterProperties bean = this.context.getBean(ConstructorParameterProperties.class);
		assertThat(bean.getFoo()).isEqualTo("hello");
		assertThat(bean.getBar()).isEqualTo(0);
	}

	@Test
	void loadWhenBindingToConstructorParametersShouldValidate() {
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> load(ConstructorParameterValidationConfiguration.class)).satisfies((ex) -> {
					assertThat(ex).hasCauseInstanceOf(BindException.class);
					assertThat(ex.getCause()).hasCauseExactlyInstanceOf(BindValidationException.class);
				});
	}

	@Test
	void loadWhenBindingOnBeanWithoutBeanDefinitionShouldBind() {
		load(BasicConfiguration.class, "name=test");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("test");
		bean.name = "override";
		this.context.getBean(ConfigurationPropertiesBindingPostProcessor.class).postProcessBeforeInitialization(bean,
				"does-not-exist");
		assertThat(bean.name).isEqualTo("test");
	}

	private AnnotationConfigApplicationContext load(Class<?> configuration, String... inlinedProperties) {
		return load(new Class<?>[] { configuration }, inlinedProperties);
	}

	private AnnotationConfigApplicationContext load(Class<?>[] configuration, String... inlinedProperties) {
		this.context.register(configuration);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, inlinedProperties);
		this.context.refresh();
		return this.context;
	}

	/**
	 * Strict tests need a known set of properties so we remove system items which may be
	 * environment specific.
	 */
	private void removeSystemProperties() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		sources.remove("systemProperties");
		sources.remove("systemEnvironment");
	}

	private void resetContext() {
		this.context.close();
		this.context = new AnnotationConfigApplicationContext();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(BasicProperties.class)
	static class BasicConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(NestedProperties.class)
	static class NestedConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(IgnoreUnknownFieldsFalseProperties.class)
	static class IgnoreUnknownFieldsFalseConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(PrefixProperties.class)
	static class PrefixConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ValidatedJsr303Properties.class)
	static class ValidatedJsr303Configuration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class ValidatedOnBeanJsr303Configuration {

		@Bean
		@Validated
		public NonValidatedJsr303Properties properties() {
			return new NonValidatedJsr303Properties();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(NonValidatedJsr303Properties.class)
	static class NonValidatedJsr303Configuration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AnnotationOnBaseClassProperties.class)
	static class AnnotationOnBaseClassConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithoutAndAnnotationConfiguration.class)
	static class WithoutAndAnnotationConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithoutAnnotationValueProperties.class)
	static class WithoutAnnotationValueConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("org/springframework/boot/context/properties/testProperties.xml")
	static class DefaultsInXmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class DefaultsInJavaConfiguration {

		@Bean
		public BasicProperties basicProperties() {
			BasicProperties test = new BasicProperties();
			test.setName("bar");
			return test;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class PrefixPropertiesDeclaredAsBeanConfiguration {

		@Bean
		public PrefixProperties prefixProperties() {
			return new PrefixProperties();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(PrefixProperties.class)
	static class PrefixPropertiesDeclaredAsAnnotationValueConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ PrefixProperties.class, AnotherPrefixProperties.class })
	static class MultiplePrefixPropertiesDeclaredAsAnnotationValueConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class PrefixedPropertiesReplacedOnBeanMethodConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spam")
		public PrefixProperties prefixProperties() {
			return new PrefixProperties();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class ValidatedImplementationConfiguration {

		@Bean
		public ValidatedImplementationProperties testProperties() {
			return new ValidatedImplementationProperties();
		}

	}

	@Configuration(proxyBeanMethods = false)
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

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithPropertyPlaceholderValueProperties.class)
	static class WithPropertyPlaceholderValueConfiguration {

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(SimplePrefixedProperties.class)
	static class WithPropertyPlaceholderWithLocalPropertiesValueConfiguration {

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer() {
			PropertySourcesPlaceholderConfigurer placeholderConfigurer = new PropertySourcesPlaceholderConfigurer();
			Properties properties = new Properties();
			properties.put("com.example.bar", "b");
			placeholderConfigurer.setProperties(properties);
			return placeholderConfigurer;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class WithFactoryBeanConfiguration {

		public static boolean factoryBeanInitialized;

	}

	@Configuration(proxyBeanMethods = false)
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

	@Configuration(proxyBeanMethods = false)
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
				String path = location.substring(PREFIX.length());
				return new ClassPathResource(path);
			}
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		public Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonQualifiedConverterConfiguration {

		@Bean
		public Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		public GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonQualifiedGenericConverterConfiguration {

		@Bean
		public GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class GenericConfiguration {

		@Bean
		@ConfigurationProperties("foo")
		public AGenericClass<String> aBeanToBind() {
			return new AGenericClass<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithCustomValidatorProperties.class)
	static class WithCustomValidatorConfiguration {

		@Bean(name = ConfigurationPropertiesBindingPostProcessorRegistrar.VALIDATOR_BEAN_NAME)
		public CustomPropertiesValidator validator() {
			return new CustomPropertiesValidator();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithSetterThatThrowsValidationExceptionProperties.class)
	static class WithUnsupportedCustomValidatorConfiguration {

		@Bean(name = ConfigurationPropertiesBindingPostProcessorRegistrar.VALIDATOR_BEAN_NAME)
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
		private List<Jsr303Properties> properties = Collections.singletonList(new Jsr303Properties());

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
	static class ValidatedImplementationProperties implements InterfaceForValidatedImplementation {

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
	@ConfigurationProperties(prefix = "test")
	static class WithIntegerMapProperties {

		private Map<String, Map<Integer, Foo>> map;

		public Map<String, Map<Integer, Foo>> getMap() {
			return this.map;
		}

		public void setMap(Map<String, Map<Integer, Foo>> map) {
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

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	static class DataSizeProperties {

		private DataSize size;

		@DataSizeUnit(DataUnit.KILOBYTES)
		private DataSize anotherSize;

		public DataSize getSize() {
			return this.size;
		}

		public void setSize(DataSize size) {
			this.size = size;
		}

		public DataSize getAnotherSize() {
			return this.anotherSize;
		}

		public void setAnotherSize(DataSize anotherSize) {
			this.anotherSize = anotherSize;
		}

	}

	@ConfigurationProperties(prefix = "test")
	static class OtherInjectedProperties {

		final DataSizeProperties dataSizeProperties;

		OtherInjectedProperties(ObjectProvider<DataSizeProperties> dataSizeProperties) {
			this.dataSizeProperties = dataSizeProperties.getIfUnique();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OtherInjectedProperties.class)
	static class OtherInjectPropertiesConfiguration {

	}

	@ConfigurationProperties(prefix = "test")
	@Validated
	static class ConstructorParameterProperties {

		@NotEmpty
		private final String foo;

		private final int bar;

		ConstructorParameterProperties(@DefaultValue("hello") String foo, int bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public String getFoo() {
			return this.foo;
		}

		public int getBar() {
			return this.bar;
		}

	}

	@ConfigurationProperties(prefix = "test")
	@Validated
	static class ConstructorParameterValidatedProperties {

		@NotEmpty
		private final String foo;

		ConstructorParameterValidatedProperties(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return this.foo;
		}

	}

	@EnableConfigurationProperties(ConstructorParameterProperties.class)
	static class ConstructorParameterConfiguration {

	}

	@EnableConfigurationProperties(ConstructorParameterValidatedProperties.class)
	static class ConstructorParameterValidationConfiguration {

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

		@Override
		public Person convert(String source) {
			String[] content = StringUtils.split(source, " ");
			return new Person(content[0], content[1]);
		}

	}

	static class GenericPersonConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, Person.class));
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
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

	static class Foo {

		private String a;

		private int b;

		public String getA() {
			return this.a;
		}

		public void setA(String a) {
			this.a = a;
		}

		public int getB() {
			return this.b;
		}

		public void setB(int b) {
			this.b = b;
		}

	}

}
