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

package org.springframework.boot.context.properties;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.text.ParseException;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationFormat;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.convert.PeriodFormat;
import org.springframework.boot.convert.PeriodStyle;
import org.springframework.boot.convert.PeriodUnit;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.format.Formatter;
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
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationProperties @ConfigurationProperties}-annotated beans.
 * Covers {@link EnableConfigurationProperties @EnableConfigurationProperties},
 * {@link ConfigurationPropertiesBindingPostProcessor} and
 * {@link ConfigurationPropertiesBinder}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Vladislav Kisel
 */
@ExtendWith(OutputCaptureExtension.class)
class ConfigurationPropertiesTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@AfterEach
	void cleanup() {
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
	void loadWhenIgnoreUnresolvablePlaceholdersSetsToFalseShouldFail() {
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
			.isThrownBy(() -> load(BasicConfiguration.class, "name=${FOO}",
					"spring.configurationproperties.ignore-unresolvable-placeholders=false"))
			.withCauseInstanceOf(BindException.class)
			.withStackTraceContaining("Could not resolve placeholder 'FOO'");
	}

	@Test
	void loadWhenIgnoreUnresolvablePlaceholdersSetsToTrueShouldNotFail() {
		load(BasicConfiguration.class, "name=${FOO}",
				"spring.configurationproperties.ignore-unresolvable-placeholders=true");
		BasicProperties properties = this.context.getBean(BasicProperties.class);
		assertThat(properties.name).isEqualTo("${FOO}");
	}

	@Test
	void loadWhenIgnoreUnresolvablePlaceholdersSetsToTrueByDefaultShouldNotFail() {
		load(BasicConfiguration.class, "name=${FOO}");
		BasicProperties properties = this.context.getBean(BasicProperties.class);
		assertThat(properties.name).isEqualTo("${FOO}");
	}

	@Test
	void loadWhenHasIgnoreUnknownFieldsFalseAndUnknownFieldsShouldFail() {
		removeSystemProperties();
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
			.isThrownBy(() -> load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo", "bar=baz"))
			.withCauseInstanceOf(BindException.class);
	}

	@Test
	void givenIgnoreUnknownFieldsFalseAndIgnoreInvalidFieldsTrueWhenThereAreUnknownFieldsThenBindingShouldFail() {
		removeSystemProperties();
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class).isThrownBy(
				() -> load(IgnoreUnknownFieldsFalseIgnoreInvalidFieldsTrueConfiguration.class, "name=foo", "bar=baz"))
			.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadWhenHasIgnoreInvalidFieldsTrueAndInvalidFieldsShouldBind() {
		load(IgnoreInvalidFieldsFalseProperties.class, "com.example.bar=spam");
		IgnoreInvalidFieldsFalseProperties bean = this.context.getBean(IgnoreInvalidFieldsFalseProperties.class);
		assertThat(bean.getBar()).isZero();
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
		assertThatIllegalStateException().isThrownBy(() -> load(WithoutAndAnnotationConfiguration.class, "name:foo"))
			.withMessageContaining("No ConfigurationProperties annotation found");
	}

	@Test
	void loadWhenBindingWithoutAnnotationValueShouldBind() {
		load(WithoutAnnotationValueConfiguration.class, "name=foo");
		WithoutAnnotationValueProperties bean = this.context.getBean(WithoutAnnotationValueProperties.class);
		assertThat(bean.name).isEqualTo("foo");
	}

	@Test
	@WithResource(name = "testProperties.xml",
			content = """
					<?xml version="1.0" encoding="UTF-8"?>
					<beans xmlns="http://www.springframework.org/schema/beans"
						xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xsi:schemaLocation="http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd">

						<bean
							id="org.springframework.boot.context.properties.ConfigurationPropertiesTests$BasicProperties"
							class="org.springframework.boot.context.properties.ConfigurationPropertiesTests$BasicProperties">
							<property name="name" value="bar"/>
						</bean>

					</beans>
					""")
	void loadWhenBindingWithDefaultsInXmlShouldBind() {
		removeSystemProperties();
		load(new Class<?>[] { DefaultsInXmlConfiguration.class });
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
		assertThat(parent.getEnvironment().getProperty("name")).isEqualTo("parent");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		removeSystemProperties();
		load(new Class<?>[] { BasicConfiguration.class, BasicPropertiesConsumer.class }, "name=child");
		assertThat(this.context.getEnvironment().getProperty("name")).isEqualTo("child");
		assertThat(this.context.getBean(BasicProperties.class)).isNotNull();
		assertThat(parent.getBean(BasicProperties.class)).isNotNull();
		assertThat(this.context.getBean(BasicPropertiesConsumer.class).getName()).isEqualTo("child");
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
			protected void onRefresh() {
				assertThat(WithFactoryBeanConfiguration.factoryBeanInitialized).as("Initialized too early").isFalse();
				super.onRefresh();
			}

		};
		this.context.register(WithFactoryBeanConfiguration.class);
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(FactoryBeanTester.class)
			.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
			.getBeanDefinition();
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
		this.context.getEnvironment()
			.getPropertySources()
			.addLast(new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
					Collections.singletonMap("TEST_MAP_FOO_BAR", "baz")));
		load(WithComplexMapProperties.class);
		WithComplexMapProperties bean = this.context.getBean(WithComplexMapProperties.class);
		assertThat(bean.getMap()).containsOnlyKeys("foo");
		assertThat(bean.getMap().get("foo")).containsOnly(entry("bar", "baz"));
	}

	@Test
	void loadWhenDotsInSystemEnvironmentPropertiesShouldBind() {
		this.context.getEnvironment()
			.getPropertySources()
			.addLast(new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
					Collections.singletonMap("com.example.bar", "baz")));
		load(SimplePrefixedProperties.class);
		SimplePrefixedProperties bean = this.context.getBean(SimplePrefixedProperties.class);
		assertThat(bean.getBar()).isEqualTo("baz");
	}

	@Test
	void loadWhenEnvironmentPrefixSetShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		sources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						Collections.singletonMap("MY_SPRING_FOO_NAME", "Jane")));
		SpringApplication application = new SpringApplication(PrefixConfiguration.class);
		application.setApplicationContextFactory((webApplicationType) -> ConfigurationPropertiesTests.this.context);
		application.setEnvironmentPrefix("my");
		application.setEnvironment(this.context.getEnvironment());
		application.run();
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("Jane");
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
	void loadWhenJsr303ConstraintDoesNotMatchOnNestedThatIsNotAnnotatedWithValidShouldNotFail() {
		assertThatNoException()
			.isThrownBy(() -> load(ValidatedNestedJsr303Properties.class, "properties.description="));
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
	void loadWhenHasMultiplePropertySourcesPlaceholderConfigurerShouldLogWarning(CapturedOutput output) {
		load(MultiplePropertySourcesPlaceholderConfigurerConfiguration.class);
		assertThat(output).contains("Multiple PropertySourcesPlaceholderConfigurer beans registered");
	}

	@Test
	void loadWhenOverridingPropertiesWithPlaceholderResolutionInEnvironmentShouldBindWithOverride() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		sources
			.addFirst(new SystemEnvironmentPropertySource("system", Collections.singletonMap("COM_EXAMPLE_BAR", "10")));
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
	void loadShouldSupportRebindableConfigurationPropertiesRegisteredAsBean() {
		testRebindableConfigurationProperties(PrototypePropertiesBeanConfiguration.class);
	}

	@Test
	void loadShouldSupportRebindableConfigurationPropertiesRegisteredUsingRegistrar() {
		testRebindableConfigurationProperties(PrototypePropertiesRegistrarConfiguration.class);
	}

	void testRebindableConfigurationProperties(Class<?> configurationClass) {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("example.one", "foo");
		sources.addFirst(new MapPropertySource("test-source", source));
		this.context.register(configurationClass);
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
	void loadWhenHasPropertySourcesPlaceholderConfigurerShouldSupportRebindableConfigurationPropertiesRegisteredAsBean() {
		testPropertySourcesPlaceholderConfigurerShouldSupportRebindableConfigurationProperties(
				PrototypePropertiesBeanConfiguration.class);
	}

	@Test
	void loadWhenHasPropertySourcesPlaceholderConfigurerShouldSupportRebindableConfigurationPropertiesRegisteredUsingRegistrar() {
		testPropertySourcesPlaceholderConfigurerShouldSupportRebindableConfigurationProperties(
				PrototypePropertiesRegistrarConfiguration.class);
	}

	void testPropertySourcesPlaceholderConfigurerShouldSupportRebindableConfigurationProperties(
			Class<?> configurationClass) {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("example.one", "foo");
		sources.addFirst(new MapPropertySource("test-source", source));
		this.context.register(configurationClass);
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
		then(protocolResolver).should().resolve(eq("application.properties"), any(ResourceLoader.class));
	}

	@Test
	@WithResource(name = "application.properties")
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
	void loadShouldUseConverterBean() {
		prepareConverterContext(PersonConverterConfiguration.class, PersonProperties.class);
		Person person = this.context.getBean(PersonProperties.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	void loadShouldUseStringConverterBeanWhenValueIsCharSequence() {
		this.context.register(PersonConverterConfiguration.class, PersonProperties.class);
		PropertySource<?> testProperties = new MapPropertySource("test", Map.of("test.person", new CharSequence() {

			private final String value = "John Smith";

			@Override
			public int length() {
				return this.value.length();
			}

			@Override
			public char charAt(int index) {
				return this.value.charAt(index);
			}

			@Override
			public CharSequence subSequence(int start, int end) {
				return this.value.subSequence(start, end);
			}

			@Override
			public String toString() {
				return this.value;
			}

		}));
		this.context.getEnvironment().getPropertySources().addLast(testProperties);
		this.context.refresh();
		Person person = this.context.getBean(PersonProperties.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	void loadWhenBeanFactoryConversionServiceAndConverterBeanCanUseBeanFactoryConverter() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new AlienConverter());
		this.context.getBeanFactory().setConversionService(conversionService);
		load(new Class<?>[] { PersonConverterConfiguration.class, PersonAndAlienProperties.class },
				"test.person=John Smith", "test.alien=Alf Tanner");
		PersonAndAlienProperties properties = this.context.getBean(PersonAndAlienProperties.class);
		assertThat(properties.getPerson().firstName).isEqualTo("John");
		assertThat(properties.getPerson().lastName).isEqualTo("Smith");
		assertThat(properties.getAlien().name).isEqualTo("rennaT flA");
	}

	@Test
	void loadWhenBeanFactoryConversionServiceAndConverterBeanCanUseConverterBean() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new PersonConverter());
		this.context.getBeanFactory().setConversionService(conversionService);
		load(new Class<?>[] { AlienConverterConfiguration.class, PersonAndAlienProperties.class },
				"test.person=John Smith", "test.alien=Alf Tanner");
		PersonAndAlienProperties properties = this.context.getBean(PersonAndAlienProperties.class);
		assertThat(properties.getPerson().firstName).isEqualTo("John");
		assertThat(properties.getPerson().lastName).isEqualTo("Smith");
		assertThat(properties.getAlien().name).isEqualTo("rennaT flA");
	}

	@Test // gh-38734
	void loadWhenBeanFactoryConversionServiceAndConverterBeanCanUseConverterBeanWithCollections() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new PersonConverter());
		this.context.getBeanFactory().setConversionService(conversionService);
		load(new Class<?>[] { AlienConverterConfiguration.class, PersonAndAliensProperties.class },
				"test.person=John Smith", "test.aliens=Alf Tanner,Gilbert");
		PersonAndAliensProperties properties = this.context.getBean(PersonAndAliensProperties.class);
		assertThat(properties.getPerson().firstName).isEqualTo("John");
		assertThat(properties.getPerson().lastName).isEqualTo("Smith");
		assertThat(properties.getAliens().get(0).name).isEqualTo("rennaT flA");
		assertThat(properties.getAliens().get(1).name).isEqualTo("trebliG");
	}

	@Test
	void loadWhenConfigurationConverterIsNotQualifiedShouldNotConvert() {
		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(() -> prepareConverterContext(NonQualifiedConverterConfiguration.class, PersonProperties.class))
			.withCauseInstanceOf(BindException.class);
	}

	@Test
	void loadShouldUseGenericConverterBean() {
		prepareConverterContext(GenericConverterConfiguration.class, PersonProperties.class);
		Person person = this.context.getBean(PersonProperties.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	void loadShouldUseFormatterBean() {
		prepareConverterContext(FormatterConfiguration.class, PersonProperties.class);
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
		assertThatException().isThrownBy(() -> load(WithCustomValidatorConfiguration.class)).satisfies((ex) -> {
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
		assertThatException().isThrownBy(() -> load(ValidatorProperties.class)).satisfies((ex) -> {
			assertThat(ex).hasCauseInstanceOf(BindException.class);
			assertThat(ex.getCause()).hasCauseExactlyInstanceOf(BindValidationException.class);
		});
	}

	@Test
	void loadWhenConstructorBoundConfigurationPropertiesIsAlsoValidatorShouldApplyValidator() {
		assertThatException().isThrownBy(() -> load(ValidatorConstructorBoundPropertiesConfiguration.class))
			.satisfies((ex) -> {
				assertThat(ex).hasCauseInstanceOf(BindException.class);
				assertThat(ex.getCause()).hasCauseExactlyInstanceOf(BindValidationException.class);
			});
	}

	@Test
	void loadWhenConfigurationPropertiesWithValidDefaultValuesShouldNotFail() {
		AnnotationConfigApplicationContext context = load(ValidatorPropertiesWithDefaultValues.class);
		ValidatorPropertiesWithDefaultValues bean = context.getBean(ValidatorPropertiesWithDefaultValues.class);
		assertThat(bean.getBar()).isEqualTo("a");
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
					+ "prefix=, ignoreInvalidFields=false, ignoreUnknownFields=false");
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
		assertThat(x.get(-1).getB()).isZero();
		assertThat(x.get(1).getA()).isEqualTo("bar");
		assertThat(x.get(1).getB()).isOne();
	}

	@Test
	void loadWhenConfigurationPropertiesInjectsAnotherBeanShouldNotFail() {
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class)
			.isThrownBy(() -> load(OtherInjectPropertiesConfiguration.class))
			.havingCause()
			.isInstanceOf(BindException.class)
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
	void loadWhenBindingToConstructorParametersWithCustomDataUnitShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.duration", "12");
		source.put("test.size", "13");
		source.put("test.period", "14");
		sources.addLast(new MapPropertySource("test", source));
		load(ConstructorParameterWithUnitConfiguration.class);
		ConstructorParameterWithUnitProperties bean = this.context
			.getBean(ConstructorParameterWithUnitProperties.class);
		assertThat(bean.getDuration()).isEqualTo(Duration.ofDays(12));
		assertThat(bean.getSize()).isEqualTo(DataSize.ofMegabytes(13));
		assertThat(bean.getPeriod()).isEqualTo(Period.ofYears(14));
	}

	@Test
	void loadWhenBindingToConstructorParametersWithDefaultValuesShouldBind() {
		load(ConstructorParameterConfiguration.class);
		ConstructorParameterProperties bean = this.context.getBean(ConstructorParameterProperties.class);
		assertThat(bean.getFoo()).isEqualTo("hello");
		assertThat(bean.getBar()).isZero();
	}

	@Test
	void loadWhenBindingToConstructorParametersWithEmptyDefaultValueShouldBind() {
		load(ConstructorParameterEmptyDefaultValueConfiguration.class);
		ConstructorParameterEmptyDefaultValueProperties bean = this.context
			.getBean(ConstructorParameterEmptyDefaultValueProperties.class);
		assertThat(bean.getSet()).isEmpty();
		assertThat(bean.getMap()).isEmpty();
		assertThat(bean.getArray()).isEmpty();
		assertThat(bean.getOptional()).isEmpty();
	}

	@Test
	void loadWhenBindingToConstructorParametersWithDefaultDataUnitShouldBind() {
		load(ConstructorParameterWithUnitConfiguration.class);
		ConstructorParameterWithUnitProperties bean = this.context
			.getBean(ConstructorParameterWithUnitProperties.class);
		assertThat(bean.getDuration()).isEqualTo(Duration.ofDays(2));
		assertThat(bean.getSize()).isEqualTo(DataSize.ofMegabytes(3));
		assertThat(bean.getPeriod()).isEqualTo(Period.ofYears(4));
	}

	@Test
	void loadWhenBindingToConstructorParametersWithCustomDataFormatShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.duration", "12d");
		source.put("test.period", "13y");
		sources.addLast(new MapPropertySource("test", source));
		load(ConstructorParameterWithFormatConfiguration.class);
		ConstructorParameterWithFormatProperties bean = this.context
			.getBean(ConstructorParameterWithFormatProperties.class);
		assertThat(bean.getDuration()).isEqualTo(Duration.ofDays(12));
		assertThat(bean.getPeriod()).isEqualTo(Period.ofYears(13));
	}

	@Test
	void loadWhenBindingToConstructorParametersWithNotMatchingCustomDurationFormatShouldFail() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.duration", "P12D");
		sources.addLast(new MapPropertySource("test", source));
		assertThatException().isThrownBy(() -> load(ConstructorParameterWithFormatConfiguration.class))
			.havingCause()
			.isInstanceOf(BindException.class);
	}

	@Test
	void loadWhenBindingToConstructorParametersWithNotMatchingCustomPeriodFormatShouldFail() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.period", "P12D");
		sources.addLast(new MapPropertySource("test", source));
		assertThatException().isThrownBy(() -> load(ConstructorParameterWithFormatConfiguration.class))
			.havingCause()
			.isInstanceOf(BindException.class);
	}

	@Test
	void loadWhenBindingToConstructorParametersWithDefaultDataFormatShouldBind() {
		load(ConstructorParameterWithFormatConfiguration.class);
		ConstructorParameterWithFormatProperties bean = this.context
			.getBean(ConstructorParameterWithFormatProperties.class);
		assertThat(bean.getDuration()).isEqualTo(Duration.ofDays(2));
		assertThat(bean.getPeriod()).isEqualTo(Period.ofYears(3));
	}

	@Test
	void loadWhenBindingToConstructorParametersShouldValidate() {
		assertThatException().isThrownBy(() -> load(ConstructorParameterValidationConfiguration.class))
			.satisfies((ex) -> {
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
		this.context.getBean(ConfigurationPropertiesBindingPostProcessor.class)
			.postProcessBeforeInitialization(bean, "does-not-exist");
		assertThat(bean.name).isEqualTo("test");
	}

	@Test
	void loadWhenBindingToNestedConstructorPropertiesShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.name", "spring");
		source.put("test.nested.age", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(NestedConstructorPropertiesConfiguration.class);
		NestedConstructorProperties bean = this.context.getBean(NestedConstructorProperties.class);
		assertThat(bean.getName()).isEqualTo("spring");
		assertThat(bean.getNested().getAge()).isEqualTo(5);
	}

	@Test // gh-18485
	void loadWhenBindingToMultiConstructorConfigurationProperties() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.nested[0].name", "spring");
		source.put("test.nested[0].age", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(MultiConstructorConfigurationPropertiesConfiguration.class);
		MultiConstructorConfigurationListProperties bean = this.context
			.getBean(MultiConstructorConfigurationListProperties.class);
		MultiConstructorConfigurationProperties nested = bean.getNested().get(0);
		assertThat(nested.getName()).isEqualTo("spring");
		assertThat(nested.getAge()).isEqualTo(5);
	}

	@Test // gh-18485
	void loadWhenBindingToMultiConstructorConfigurationPropertiesUsingShortcutSyntax() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.nested[0]", "spring");
		sources.addLast(new MapPropertySource("test", source));
		load(MultiConstructorConfigurationPropertiesConfiguration.class);
		MultiConstructorConfigurationListProperties bean = this.context
			.getBean(MultiConstructorConfigurationListProperties.class);
		MultiConstructorConfigurationProperties nested = bean.getNested().get(0);
		assertThat(nested.getName()).isEqualTo("spring");
		assertThat(nested.getAge()).isZero();
	}

	@Test // gh-18481
	void loadWhenBindingToNestedConstructorPropertiesWithDeducedNestedShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.name", "spring");
		source.put("test.nested.age", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(DeducedNestedConstructorPropertiesConfiguration.class);
		DeducedNestedConstructorProperties bean = this.context.getBean(DeducedNestedConstructorProperties.class);
		assertThat(bean.getName()).isEqualTo("spring");
		assertThat(bean.getNested().getAge()).isEqualTo(5);
	}

	@Test
	void loadWhenBindingToNestedPropertiesWithSyntheticConstructorShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.nested.age", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(SyntheticConstructorPropertiesConfiguration.class);
		SyntheticNestedConstructorProperties bean = this.context.getBean(SyntheticNestedConstructorProperties.class);
		assertThat(bean.getNested().getAge()).isEqualTo(5);
	}

	@Test
	void loadWhenBindingToJavaBeanWithNestedConstructorBindingShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.nested.age", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(JavaBeanNestedConstructorBindingPropertiesConfiguration.class);
		JavaBeanNestedConstructorBindingProperties bean = this.context
			.getBean(JavaBeanNestedConstructorBindingProperties.class);
		assertThat(bean.getNested().getAge()).isEqualTo(5);
	}

	@Test
	void loadWhenBindingToNestedWithMultipleConstructorsShouldBind() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.nested.age", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(NestedMultipleConstructorsConfiguration.class);
		NestedMultipleConstructorProperties bean = this.context.getBean(NestedMultipleConstructorProperties.class);
		assertThat(bean.getNested().getAge()).isEqualTo(5);
	}

	@Test
	void loadWhenBindingToJavaBeanWithoutExplicitConstructorBindingOnNestedShouldUseSetterBasedBinding() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.nested.age", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(JavaBeanNonDefaultConstructorPropertiesConfiguration.class);
		JavaBeanNonDefaultConstructorProperties bean = this.context
			.getBean(JavaBeanNonDefaultConstructorProperties.class);
		assertThat(bean.getNested().getAge()).isEqualTo(10);
	}

	@Test // gh-18652
	void loadWhenBeanFactoryContainsSingletonForConstructorBindingTypeShouldNotFail() {
		ConfigurableListableBeanFactory beanFactory = this.context.getBeanFactory();
		((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("test",
				new RootBeanDefinition(ConstructorParameterProperties.class));
		beanFactory.registerSingleton("test", new ConstructorParameterProperties("bar", 5));
		load(TestConfiguration.class);
	}

	@Test
	void loadWhenConstructorBindingWithOuterClassDeducedConstructorBound() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.nested.outer.age", "5");
		sources.addLast(new MapPropertySource("test", source));
		load(ConstructorBindingWithOuterClassConstructorBoundConfiguration.class);
		ConstructorBindingWithOuterClassConstructorBoundProperties bean = this.context
			.getBean(ConstructorBindingWithOuterClassConstructorBoundProperties.class);
		assertThat(bean.getNested().getOuter().getAge()).isEqualTo(5);
	}

	@Test
	void loadWhenConstructorBindingWithOuterClassAndNestedAutowiredShouldThrowException() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test.nested.age", "5");
		sources.addLast(new MapPropertySource("test", source));
		assertThatExceptionOfType(ConfigurationPropertiesBindException.class).isThrownBy(
				() -> load(ConstructorBindingWithOuterClassConstructorBoundAndNestedAutowiredConfiguration.class));
	}

	@Test
	void loadWhenConfigurationPropertiesPrefixMatchesPropertyInEnvironment() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("test", "bar");
		source.put("test.a", "baz");
		sources.addLast(new MapPropertySource("test", source));
		load(WithPublicStringConstructorPropertiesConfiguration.class);
		WithPublicStringConstructorProperties bean = this.context.getBean(WithPublicStringConstructorProperties.class);
		assertThat(bean.getA()).isEqualTo("baz");
	}

	@Test // gh-26201
	void loadWhenBoundToRandomPropertyPlaceholder() {
		MutablePropertySources sources = this.context.getEnvironment().getPropertySources();
		sources.addFirst(new RandomValuePropertySource());
		Map<String, Object> source = new HashMap<>();
		source.put("com.example.bar", "${random.int[100,200]}");
		sources.addLast(new MapPropertySource("test", source));
		load(SimplePrefixedProperties.class);
		SimplePrefixedProperties bean = this.context.getBean(SimplePrefixedProperties.class);
		assertThat(bean.getBar()).isNotNull().containsOnlyDigits();
	}

	@Test
	void boundPropertiesShouldBeRecorded() {
		load(NestedConfiguration.class, "name=foo", "nested.name=bar");
		BoundConfigurationProperties bound = BoundConfigurationProperties.get(this.context);
		Set<ConfigurationPropertyName> keys = bound.getAll().keySet();
		assertThat(keys.stream().map(ConfigurationPropertyName::toString)).contains("name", "nested.name");
	}

	@Test // gh-28592
	void loadWhenBindingWithCustomConverterAndObjectToObjectMethod() {
		this.context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
		load(WithCustomConverterAndObjectToObjectMethodConfiguration.class, "test.item=foo");
		WithCustomConverterAndObjectToObjectMethodProperties bean = this.context
			.getBean(WithCustomConverterAndObjectToObjectMethodProperties.class);
		assertThat(bean.getItem().getValue()).isEqualTo("foo");
	}

	@Test // gh-33710
	void loadWhenConstructorUsedInBeanMethodAndNotAsConstructorBinding() {
		load(ConstructorUsedInBeanMethodConfiguration.class, "test.two=bound-2");
		ConstructorUsedDirectly bean = this.context.getBean(ConstructorUsedDirectly.class);
		assertThat(bean.getOne()).isEqualTo("bean-method-1");
		assertThat(bean.getTwo()).isEqualTo("bound-2");
	}

	@Test // gh-33409
	void loadWhenConstructorUsedInNestedPropertyAndNotAsConstructorBinding() {
		load(ConstructorUsedInNestedPropertyConfiguration.class, "test.nested.two=bound-2");
		ConstructorUsedInNestedProperty bean = this.context.getBean(ConstructorUsedInNestedProperty.class);
		assertThat(bean.getNested().getOne()).isEqualTo("nested-1");
		assertThat(bean.getNested().getTwo()).isEqualTo("bound-2");
	}

	@Test // gh-34407
	void loadWhenNestedRecordWithExistingInstance() {
		load(NestedRecordInstancePropertiesConfiguration.class, "test.nested.name=spring");
		NestedRecordInstanceProperties bean = this.context.getBean(NestedRecordInstanceProperties.class);
		assertThat(bean.getNested().name()).isEqualTo("spring");
	}

	@Test
	void loadWhenPotentiallyConstructorBoundPropertiesAreImportedUsesJavaBeanBinding() {
		load(PotentiallyConstructorBoundPropertiesImporter.class, "test.prop=alpha");
		PotentiallyConstructorBoundProperties properties = this.context
			.getBean(PotentiallyConstructorBoundProperties.class);
		assertThat(properties.getProp()).isEqualTo("alpha");
	}

	@Test
	void loadWhenBindingClasspathPatternToResourceArrayShouldBindMultipleValues() {
		load(ResourceArrayPropertiesConfiguration.class,
				"test.resources=classpath*:org/springframework/boot/context/properties/*.class");
		ResourceArrayProperties properties = this.context.getBean(ResourceArrayProperties.class);
		assertThat(properties.getResources()).hasSizeGreaterThan(1);
	}

	@Test
	void loadWhenBindingClasspathPatternToResourceCollectionShouldBindMultipleValues() {
		load(ResourceCollectionPropertiesConfiguration.class,
				"test.resources=classpath*:org/springframework/boot/context/properties/*.class");
		ResourceCollectionProperties properties = this.context.getBean(ResourceCollectionProperties.class);
		assertThat(properties.getResources()).hasSizeGreaterThan(1);
	}

	@Test
	void loadWhenBindingToConstructorParametersWithConversionToCustomListImplementation() {
		load(ConstructorBoundCustomListPropertiesConfiguration.class, "test.values=a,b");
		assertThat(this.context.getBean(ConstructorBoundCustomListProperties.class).getValues()).containsExactly("a",
				"b");
	}

	@Test
	void loadWhenBindingToJavaBeanWithConversionToCustomListImplementation() {
		load(SetterBoundCustomListPropertiesConfiguration.class, "test.values=a,b");
		assertThat(this.context.getBean(SetterBoundCustomListProperties.class).getValues()).containsExactly("a", "b");
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
	@EnableConfigurationProperties
	static class TestConfiguration {

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
	@EnableConfigurationProperties(IgnoreUnknownFieldsFalseIgnoreInvalidFieldsTrueProperties.class)
	static class IgnoreUnknownFieldsFalseIgnoreInvalidFieldsTrueConfiguration {

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
		NonValidatedJsr303Properties properties() {
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
	@EnableConfigurationProperties
	@ImportResource("testProperties.xml")
	static class DefaultsInXmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class DefaultsInJavaConfiguration {

		@Bean
		BasicProperties basicProperties() {
			BasicProperties test = new BasicProperties();
			test.setName("bar");
			return test;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class PrefixPropertiesDeclaredAsBeanConfiguration {

		@Bean
		PrefixProperties prefixProperties() {
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
		@ConfigurationProperties("spam")
		PrefixProperties prefixProperties() {
			return new PrefixProperties();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class ValidatedImplementationConfiguration {

		@Bean
		ValidatedImplementationProperties testProperties() {
			return new ValidatedImplementationProperties();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@ConfigurationProperties
	static class WithPostConstructConfiguration {

		private String bar;

		private boolean initialized;

		void setBar(String bar) {
			this.bar = bar;
		}

		String getBar() {
			return this.bar;
		}

		@PostConstruct
		void init() {
			assertThat(this.bar).isNotNull();
			this.initialized = true;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithPropertyPlaceholderValueProperties.class)
	static class WithPropertyPlaceholderValueConfiguration {

		@Bean
		static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(SimplePrefixedProperties.class)
	static class WithPropertyPlaceholderWithLocalPropertiesValueConfiguration {

		@Bean
		static PropertySourcesPlaceholderConfigurer configurer() {
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

		static boolean factoryBeanInitialized;

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class MultiplePropertySourcesPlaceholderConfigurerConfiguration {

		@Bean
		static PropertySourcesPlaceholderConfigurer configurer1() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		static PropertySourcesPlaceholderConfigurer configurer2() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class PrototypePropertiesBeanConfiguration {

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		@ConfigurationProperties("example")
		PrototypeBean prototypeBean() {
			return new PrototypeBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(PrototypeBeanProperties.class)
	static class PrototypePropertiesRegistrarConfiguration {

	}

	@ConfigurationProperties("example")
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	static class PrototypeBeanProperties extends PrototypeBean {

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class PropertiesWithResource {

		private Resource resource;

		Resource getResource() {
			return this.resource;
		}

		void setResource(Resource resource) {
			this.resource = resource;
		}

	}

	static class TestProtocolResolver implements ProtocolResolver {

		static final String PREFIX = "test:/";

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
	static class PersonConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		static Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AlienConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		static Converter<String, Alien> alienConverter() {
			return new AlienConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonQualifiedConverterConfiguration {

		@Bean
		Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		static GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FormatterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		static Formatter<Person> personFormatter() {
			return new PersonFormatter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonQualifiedGenericConverterConfiguration {

		@Bean
		GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class GenericConfiguration {

		@Bean
		@ConfigurationProperties("foo")
		AGenericClass<String> aBeanToBind() {
			return new AGenericClass<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithCustomValidatorProperties.class)
	static class WithCustomValidatorConfiguration {

		@Bean(name = EnableConfigurationProperties.VALIDATOR_BEAN_NAME)
		CustomPropertiesValidator validator() {
			return new CustomPropertiesValidator();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithSetterThatThrowsValidationExceptionProperties.class)
	static class WithUnsupportedCustomValidatorConfiguration {

		@Bean(name = EnableConfigurationProperties.VALIDATOR_BEAN_NAME)
		CustomPropertiesValidator validator() {
			return new CustomPropertiesValidator();
		}

	}

	static class AGenericClass<T> {

		private T bar;

		T getBar() {
			return this.bar;
		}

		void setBar(T bar) {
			this.bar = bar;
		}

	}

	static class PrototypeBean {

		private String one;

		private String two;

		String getOne() {
			return this.one;
		}

		void setOne(String one) {
			this.one = one;
		}

		String getTwo() {
			return this.two;
		}

		void setTwo(String two) {
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
	public static class BasicProperties {

		private String name;

		private int[] array;

		private List<Integer> list = new ArrayList<>();

		private Duration duration;

		// No getter - you should be able to bind to a write-only bean

		public void setName(String name) {
			// Must be public for XML
			this.name = name;
		}

		void setArray(int... values) {
			this.array = values;
		}

		int[] getArray() {
			return this.array;
		}

		List<Integer> getList() {
			return this.list;
		}

		void setList(List<Integer> list) {
			this.list = list;
		}

		Duration getDuration() {
			return this.duration;
		}

		void setDuration(Duration duration) {
			this.duration = duration;
		}

	}

	@ConfigurationProperties
	static class NestedProperties {

		private String name;

		private final Nested nested = new Nested();

		void setName(String name) {
			this.name = name;
		}

		Nested getNested() {
			return this.nested;
		}

		static class Nested {

			private String name;

			void setName(String name) {
				this.name = name;
			}

		}

	}

	@ConfigurationProperties(ignoreUnknownFields = false)
	static class IgnoreUnknownFieldsFalseProperties extends BasicProperties {

	}

	@ConfigurationProperties(ignoreUnknownFields = false, ignoreInvalidFields = true)
	static class IgnoreUnknownFieldsFalseIgnoreInvalidFieldsTrueProperties extends BasicProperties {

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "com.example", ignoreInvalidFields = true)
	static class IgnoreInvalidFieldsFalseProperties {

		private long bar;

		void setBar(long bar) {
			this.bar = bar;
		}

		long getBar() {
			return this.bar;
		}

	}

	@ConfigurationProperties("spring.foo")
	static class PrefixProperties extends BasicProperties {

	}

	@ConfigurationProperties("spring.bar")
	static class AnotherPrefixProperties extends BasicProperties {

	}

	static class Jsr303Properties extends BasicProperties {

		@NotEmpty
		private String description;

		String getDescription() {
			return this.description;
		}

		void setDescription(String description) {
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

		private final Jsr303Properties properties = new Jsr303Properties();

		Jsr303Properties getProperties() {
			return this.properties;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties
	@Validated
	static class ValidatedValidNestedJsr303Properties {

		@Valid
		private final Jsr303Properties properties = new Jsr303Properties();

		Jsr303Properties getProperties() {
			return this.properties;
		}

	}

	static class AnnotationOnBaseClassProperties extends BasicProperties {

	}

	@ConfigurationProperties
	static class WithoutAnnotationValueProperties {

		private String name;

		void setName(String name) {
			this.name = name;
		}

		// No getter - you should be able to bind to a write-only bean

	}

	@EnableConfigurationProperties
	@ConfigurationProperties
	static class MapProperties {

		private Map<String, String> mymap;

		void setMymap(Map<String, String> mymap) {
			this.mymap = mymap;
		}

		Map<String, String> getMymap() {
			return this.mymap;
		}

	}

	@Component
	static class BasicPropertiesConsumer {

		@Autowired
		private BasicProperties properties;

		@PostConstruct
		void init() {
			assertThat(this.properties).isNotNull();
		}

		String getName() {
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

		void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@ConfigurationProperties("test")
	@Validated
	static class WithPropertyPlaceholderValueProperties {

		@Value("${default.value}")
		private String value;

		void setValue(String value) {
			this.value = value;
		}

		String getValue() {
			return this.value;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class WithEnumProperties {

		private FooEnum theValue;

		private List<FooEnum> theValues;

		void setTheValue(FooEnum value) {
			this.theValue = value;
		}

		FooEnum getTheValue() {
			return this.theValue;
		}

		List<FooEnum> getTheValues() {
			return this.theValues;
		}

		void setTheValues(List<FooEnum> theValues) {
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

		char[] getChars() {
			return this.chars;
		}

		void setChars(char[] chars) {
			this.chars = chars;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class WithRelaxedNamesProperties {

		private String fooBar;

		private String barBAZ;

		String getFooBar() {
			return this.fooBar;
		}

		void setFooBar(String fooBar) {
			this.fooBar = fooBar;
		}

		String getBarBAZ() {
			return this.barBAZ;
		}

		void setBarBAZ(String barBAZ) {
			this.barBAZ = barBAZ;
		}

	}

	@Validated
	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class WithMapProperties {

		private Map<String, String> map;

		Map<String, String> getMap() {
			return this.map;
		}

		void setMap(Map<String, String> map) {
			this.map = map;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class WithComplexMapProperties {

		private Map<String, Map<String, String>> map;

		Map<String, Map<String, String>> getMap() {
			return this.map;
		}

		void setMap(Map<String, Map<String, String>> map) {
			this.map = map;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class WithIntegerMapProperties {

		private Map<String, Map<Integer, Foo>> map;

		Map<String, Map<Integer, Foo>> getMap() {
			return this.map;
		}

		void setMap(Map<String, Map<Integer, Foo>> map) {
			this.map = map;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "com.example", ignoreUnknownFields = false)
	static class SimplePrefixedProperties {

		private int foo;

		private String bar;

		String getBar() {
			return this.bar;
		}

		void setBar(String bar) {
			this.bar = bar;
		}

		int getFoo() {
			return this.foo;
		}

		void setFoo(int foo) {
			this.foo = foo;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class PersonProperties {

		private Person person;

		Person getPerson() {
			return this.person;
		}

		void setPerson(Person person) {
			this.person = person;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class PersonAndAlienProperties {

		private Person person;

		private Alien alien;

		Person getPerson() {
			return this.person;
		}

		void setPerson(Person person) {
			this.person = person;
		}

		Alien getAlien() {
			return this.alien;
		}

		void setAlien(Alien alien) {
			this.alien = alien;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class PersonAndAliensProperties {

		private Person person;

		private List<Alien> aliens;

		Person getPerson() {
			return this.person;
		}

		void setPerson(Person person) {
			this.person = person;
		}

		List<Alien> getAliens() {
			return this.aliens;
		}

		void setAliens(List<Alien> aliens) {
			this.aliens = aliens;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("sample")
	static class MapWithNumericKeyProperties {

		private final Map<String, BasicProperties> properties = new LinkedHashMap<>();

		Map<String, BasicProperties> getProperties() {
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

		String getFoo() {
			return this.foo;
		}

		void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@EnableConfigurationProperties(ValidatorConstructorBoundProperties.class)
	static class ValidatorConstructorBoundPropertiesConfiguration {

	}

	@ConfigurationProperties
	static class ValidatorConstructorBoundProperties implements Validator {

		private final String foo;

		ValidatorConstructorBoundProperties(String foo) {
			this.foo = foo;
		}

		@Override
		public boolean supports(Class<?> type) {
			return type == ValidatorConstructorBoundProperties.class;
		}

		@Override
		public void validate(Object target, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "foo", "TEST1");
		}

		String getFoo() {
			return this.foo;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class WithSetterThatThrowsValidationExceptionProperties {

		private String foo;

		String getFoo() {
			return this.foo;
		}

		void setFoo(String foo) {
			this.foo = foo;
			if (!foo.equals("bar")) {
				throw new IllegalArgumentException("Wrong value for foo");
			}
		}

	}

	@ConfigurationProperties("custom")
	static class WithCustomValidatorProperties {

		private String foo;

		String getFoo() {
			return this.foo;
		}

		void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class ListOfGenericClassProperties {

		private List<Class<? extends Throwable>> list;

		List<Class<? extends Throwable>> getList() {
			return this.list;
		}

		void setList(List<Class<? extends Throwable>> list) {
			this.list = list;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class FileProperties {

		private File file;

		File getFile() {
			return this.file;
		}

		void setFile(File file) {
			this.file = file;
		}

	}

	@EnableConfigurationProperties
	@ConfigurationProperties("test")
	static class DataSizeProperties {

		private DataSize size;

		@DataSizeUnit(DataUnit.KILOBYTES)
		private DataSize anotherSize;

		DataSize getSize() {
			return this.size;
		}

		void setSize(DataSize size) {
			this.size = size;
		}

		DataSize getAnotherSize() {
			return this.anotherSize;
		}

		void setAnotherSize(DataSize anotherSize) {
			this.anotherSize = anotherSize;
		}

	}

	@ConfigurationProperties("test")
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

	@ConfigurationProperties("test")
	@Validated
	static class ConstructorParameterProperties {

		@NotEmpty
		private final String foo;

		private final int bar;

		ConstructorParameterProperties(@DefaultValue("hello") String foo, int bar) {
			this.foo = foo;
			this.bar = bar;
		}

		String getFoo() {
			return this.foo;
		}

		int getBar() {
			return this.bar;
		}

	}

	@ConfigurationProperties("test")
	static class ConstructorParameterEmptyDefaultValueProperties {

		private final Set<String> set;

		private final Map<String, String> map;

		private final int[] array;

		private final Optional<String> optional;

		ConstructorParameterEmptyDefaultValueProperties(@DefaultValue Set<String> set,
				@DefaultValue Map<String, String> map, @DefaultValue int[] array,
				@DefaultValue Optional<String> optional) {
			this.set = set;
			this.map = map;
			this.array = array;
			this.optional = optional;
		}

		Set<String> getSet() {
			return this.set;
		}

		Map<String, String> getMap() {
			return this.map;
		}

		int[] getArray() {
			return this.array;
		}

		Optional<String> getOptional() {
			return this.optional;
		}

	}

	@ConfigurationProperties("test")
	static class ConstructorParameterWithUnitProperties {

		private final Duration duration;

		private final DataSize size;

		private final Period period;

		@ConstructorBinding
		ConstructorParameterWithUnitProperties(@DefaultValue("2") @DurationUnit(ChronoUnit.DAYS) Duration duration,
				@DefaultValue("3") @DataSizeUnit(DataUnit.MEGABYTES) DataSize size,
				@DefaultValue("4") @PeriodUnit(ChronoUnit.YEARS) Period period) {
			this.size = size;
			this.duration = duration;
			this.period = period;
		}

		Duration getDuration() {
			return this.duration;
		}

		DataSize getSize() {
			return this.size;
		}

		Period getPeriod() {
			return this.period;
		}

	}

	@ConfigurationProperties("test")
	static class ConstructorParameterWithFormatProperties {

		private final Duration duration;

		private final Period period;

		ConstructorParameterWithFormatProperties(
				@DefaultValue("2d") @DurationFormat(DurationStyle.SIMPLE) Duration duration,
				@DefaultValue("3y") @PeriodFormat(PeriodStyle.SIMPLE) Period period) {
			this.duration = duration;
			this.period = period;
		}

		Duration getDuration() {
			return this.duration;
		}

		Period getPeriod() {
			return this.period;
		}

	}

	@ConfigurationProperties("test")
	@Validated
	static class ConstructorParameterValidatedProperties {

		@NotEmpty
		private final String foo;

		ConstructorParameterValidatedProperties(String foo) {
			this.foo = foo;
		}

		String getFoo() {
			return this.foo;
		}

	}

	@EnableConfigurationProperties(ConstructorParameterProperties.class)
	static class ConstructorParameterConfiguration {

	}

	@EnableConfigurationProperties(ConstructorParameterEmptyDefaultValueProperties.class)
	static class ConstructorParameterEmptyDefaultValueConfiguration {

	}

	@EnableConfigurationProperties(ConstructorParameterWithUnitProperties.class)
	static class ConstructorParameterWithUnitConfiguration {

	}

	@EnableConfigurationProperties(ConstructorParameterWithFormatProperties.class)
	static class ConstructorParameterWithFormatConfiguration {

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

	static class AlienConverter implements Converter<String, Alien> {

		@Override
		public Alien convert(String source) {
			return new Alien(new StringBuilder(source).reverse().toString());
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

	static class PersonFormatter implements Formatter<Person> {

		@Override
		public String print(Person person, Locale locale) {
			return person.getFirstName() + " " + person.getLastName();
		}

		@Override
		public Person parse(String text, Locale locale) throws ParseException {
			String[] content = text.split(" ");
			return new Person(content[0], content[1]);
		}

	}

	static class PersonPropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			String[] content = text.split(",");
			setValue(new Person(content[1], content[0]));
		}

	}

	static class Person {

		private final String firstName;

		private final String lastName;

		Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		String getFirstName() {
			return this.firstName;
		}

		String getLastName() {
			return this.lastName;
		}

	}

	static class Alien {

		private final String name;

		Alien(String name) {
			this.name = name;
		}

		String getName() {
			return this.name;
		}

	}

	static class Foo {

		private String a;

		private int b;

		String getA() {
			return this.a;
		}

		void setA(String a) {
			this.a = a;
		}

		int getB() {
			return this.b;
		}

		void setB(int b) {
			this.b = b;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(NestedConstructorProperties.class)
	static class NestedConstructorPropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class NestedConstructorProperties {

		private final String name;

		private final Nested nested;

		NestedConstructorProperties(String name, Nested nested) {
			this.name = name;
			this.nested = nested;
		}

		String getName() {
			return this.name;
		}

		Nested getNested() {
			return this.nested;
		}

		static class Nested {

			private final int age;

			@ConstructorBinding
			Nested(int age) {
				this.age = age;
			}

			int getAge() {
				return this.age;
			}

		}

	}

	@ConfigurationProperties("test")
	static class NestedMultipleConstructorProperties {

		private final String name;

		private final Nested nested;

		NestedMultipleConstructorProperties(String name, Nested nested) {
			this.name = name;
			this.nested = nested;
		}

		String getName() {
			return this.name;
		}

		Nested getNested() {
			return this.nested;
		}

		static class Nested {

			private int age;

			Nested(String property) {

			}

			@ConstructorBinding
			Nested(int age) {
				this.age = age;
			}

			int getAge() {
				return this.age;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(NestedMultipleConstructorProperties.class)
	static class NestedMultipleConstructorsConfiguration {

	}

	@ConfigurationProperties("test")
	static class ConstructorBindingWithOuterClassConstructorBoundProperties {

		private final Nested nested;

		ConstructorBindingWithOuterClassConstructorBoundProperties(Nested nested) {
			this.nested = nested;
		}

		Nested getNested() {
			return this.nested;
		}

		static class Nested {

			private Outer outer;

			Outer getOuter() {
				return this.outer;
			}

			void setOuter(Outer nested) {
				this.outer = nested;
			}

		}

	}

	@ConfigurationProperties("test")
	static class ConstructorBindingWithOuterClassConstructorBoundAndNestedAutowired {

		private final Nested nested;

		ConstructorBindingWithOuterClassConstructorBoundAndNestedAutowired(Nested nested) {
			this.nested = nested;
		}

		Nested getNested() {
			return this.nested;
		}

		static class Nested {

			private final int age;

			@Autowired
			Nested(int age) {
				this.age = age;
			}

			int getAge() {
				return this.age;
			}

		}

	}

	static class Outer {

		private final int age;

		Outer(int age) {
			this.age = age;
		}

		int getAge() {
			return this.age;
		}

	}

	@EnableConfigurationProperties(ConstructorBindingWithOuterClassConstructorBoundProperties.class)
	static class ConstructorBindingWithOuterClassConstructorBoundConfiguration {

	}

	@EnableConfigurationProperties(ConstructorBindingWithOuterClassConstructorBoundAndNestedAutowired.class)
	static class ConstructorBindingWithOuterClassConstructorBoundAndNestedAutowiredConfiguration {

	}

	@ConfigurationProperties("test")
	static class MultiConstructorConfigurationListProperties {

		private final List<MultiConstructorConfigurationProperties> nested = new ArrayList<>();

		List<MultiConstructorConfigurationProperties> getNested() {
			return this.nested;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MultiConstructorConfigurationListProperties.class)
	static class MultiConstructorConfigurationPropertiesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JavaBeanNestedConstructorBindingProperties.class)
	static class JavaBeanNestedConstructorBindingPropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class JavaBeanNestedConstructorBindingProperties {

		private Nested nested;

		Nested getNested() {
			return this.nested;
		}

		void setNested(Nested nested) {
			this.nested = nested;
		}

		static final class Nested {

			private final int age;

			@ConstructorBinding
			private Nested(int age) {
				this.age = age;
			}

			int getAge() {
				return this.age;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JavaBeanNonDefaultConstructorProperties.class)
	static class JavaBeanNonDefaultConstructorPropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class JavaBeanNonDefaultConstructorProperties {

		private Nested nested;

		Nested getNested() {
			return this.nested;
		}

		void setNested(Nested nested) {
			this.nested = nested;
		}

		static final class Nested {

			private int age;

			private Nested() {

			}

			private Nested(int age) {
				this.age = age;
			}

			int getAge() {
				return this.age;
			}

			void setAge(int age) {
				this.age = age + 5;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(SyntheticNestedConstructorProperties.class)
	static class SyntheticConstructorPropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class SyntheticNestedConstructorProperties {

		private final Nested nested;

		SyntheticNestedConstructorProperties(Nested nested) {
			this.nested = nested;
		}

		Nested getNested() {
			return this.nested;
		}

		static final class Nested {

			private int age;

			private Nested() {

			}

			int getAge() {
				return this.age;
			}

			void setAge(int age) {
				this.age = age;
			}

			static class AnotherNested {

				private final Nested nested;

				AnotherNested(String name) {
					this.nested = new Nested();
				}

				Nested getNested() {
					return this.nested;
				}

			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(DeducedNestedConstructorProperties.class)
	static class DeducedNestedConstructorPropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class DeducedNestedConstructorProperties {

		private final String name;

		private final Nested nested;

		DeducedNestedConstructorProperties(String name, Nested nested) {
			this.name = name;
			this.nested = nested;
		}

		String getName() {
			return this.name;
		}

		Nested getNested() {
			return this.nested;
		}

		static class Nested {

			private final int age;

			Nested(int age) {
				this.age = age;
			}

			int getAge() {
				return this.age;
			}

		}

	}

	@Configuration
	@EnableConfigurationProperties(WithPublicStringConstructorProperties.class)
	static class WithPublicStringConstructorPropertiesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WithCustomConverterAndObjectToObjectMethodProperties.class)
	static class WithCustomConverterAndObjectToObjectMethodConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		static WithObjectToObjectMethodConverter withObjectToObjectMethodConverter() {
			return new WithObjectToObjectMethodConverter();
		}

	}

	@ConfigurationProperties("test")
	static class WithCustomConverterAndObjectToObjectMethodProperties {

		private WithPublicObjectToObjectMethod item;

		WithPublicObjectToObjectMethod getItem() {
			return this.item;
		}

		void setItem(WithPublicObjectToObjectMethod item) {
			this.item = item;
		}

	}

	static class WithObjectToObjectMethodConverter implements Converter<String, WithPublicObjectToObjectMethod> {

		@Override
		public WithPublicObjectToObjectMethod convert(String source) {
			return new WithPublicObjectToObjectMethod(source);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class ConstructorUsedInBeanMethodConfiguration {

		@Bean
		@ConfigurationProperties("test")
		ConstructorUsedDirectly constructorUsedInBeanMethod() {
			return new ConstructorUsedDirectly("bean-method-1", "bean-method-2");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ConstructorUsedInNestedProperty.class)
	static class ConstructorUsedInNestedPropertyConfiguration {

	}

	@ConfigurationProperties("test")
	static class ConstructorUsedInNestedProperty {

		@NestedConfigurationProperty
		private ConstructorUsedDirectly nested = new ConstructorUsedDirectly("nested-1", "nested-2");

		ConstructorUsedDirectly getNested() {
			return this.nested;
		}

		void setNested(ConstructorUsedDirectly nested) {
			this.nested = nested;
		}

	}

	static class ConstructorUsedDirectly {

		private String one;

		private String two;

		ConstructorUsedDirectly(String one, String two) {
			this.one = one;
			this.two = two;
		}

		String getOne() {
			return this.one;
		}

		void setOne(String one) {
			this.one = one;
		}

		String getTwo() {
			return this.two;
		}

		void setTwo(String two) {
			this.two = two;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(NestedRecordInstanceProperties.class)
	static class NestedRecordInstancePropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class NestedRecordInstanceProperties {

		@NestedConfigurationProperty
		private NestedRecord nested = new NestedRecord("unnamed");

		NestedRecord getNested() {
			return this.nested;
		}

		void setNested(NestedRecord nestedRecord) {
			this.nested = nestedRecord;
		}

	}

	record NestedRecord(String name) {
	}

	@EnableConfigurationProperties
	@Import(PotentiallyConstructorBoundProperties.class)
	static class PotentiallyConstructorBoundPropertiesImporter {

		@Bean
		String notAProperty() {
			return "notAProperty";
		}

	}

	@ConfigurationProperties("test")
	static class PotentiallyConstructorBoundProperties {

		private String prop;

		PotentiallyConstructorBoundProperties(String notAProperty) {

		}

		String getProp() {
			return this.prop;
		}

		void setProp(String prop) {
			this.prop = prop;
		}

	}

	@EnableConfigurationProperties(ResourceArrayProperties.class)
	static class ResourceArrayPropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class ResourceArrayProperties {

		private Resource[] resources;

		Resource[] getResources() {
			return this.resources;
		}

		void setResources(Resource[] resources) {
			this.resources = resources;
		}

	}

	@EnableConfigurationProperties(ResourceCollectionProperties.class)
	static class ResourceCollectionPropertiesConfiguration {

	}

	@ConfigurationProperties("test")
	static class ResourceCollectionProperties {

		private Collection<Resource> resources;

		Collection<Resource> getResources() {
			return this.resources;
		}

		void setResources(Collection<Resource> resources) {
			this.resources = resources;
		}

	}

	@EnableConfigurationProperties(ConstructorBoundCustomListProperties.class)
	static class ConstructorBoundCustomListPropertiesConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		static Converter<ArrayList<?>, CustomList<?>> arrayListToCustomList() {
			return new Converter<>() {

				@Override
				public CustomList<?> convert(ArrayList<?> source) {
					return new CustomList<>(source);
				}

			};

		}

	}

	@ConfigurationProperties("test")
	static class ConstructorBoundCustomListProperties {

		private final CustomList<String> values;

		ConstructorBoundCustomListProperties(CustomList<String> values) {
			this.values = values;
		}

		CustomList<String> getValues() {
			return this.values;
		}

	}

	@EnableConfigurationProperties(SetterBoundCustomListProperties.class)
	static class SetterBoundCustomListPropertiesConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		static Converter<ArrayList<?>, CustomList<?>> arrayListToCustomList() {
			return new Converter<>() {

				@Override
				public CustomList<?> convert(ArrayList<?> source) {
					return new CustomList<>(source);
				}

			};

		}

	}

	@ConfigurationProperties("test")
	static class SetterBoundCustomListProperties {

		private CustomList<String> values;

		CustomList<String> getValues() {
			return this.values;
		}

		void setValues(CustomList<String> values) {
			this.values = values;
		}

	}

	static final class CustomList<E> extends ArrayList<E> {

		CustomList(List<E> delegate) {
			super(delegate);
		}

	}

}
