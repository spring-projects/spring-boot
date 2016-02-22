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

package org.springframework.boot.test.mock.mockito;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanFactoryPostProcessor} used to register and inject
 * {@link MockBean @MockBeans} with the {@link ApplicationContext}. An initial set of
 * definitions can be passed to the processor with additional definitions being
 * automatically created from {@code @Configuration} classes that use
 * {@link MockBean @MockBean}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class MockitoPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
		implements BeanClassLoaderAware, BeanFactoryAware, BeanFactoryPostProcessor,
		Ordered {

	private static final String BEAN_NAME = MockitoPostProcessor.class.getName();

	private static final String CONFIGURATION_CLASS_ATTRIBUTE = Conventions
			.getQualifiedAttributeName(ConfigurationClassPostProcessor.class,
					"configurationClass");

	private final Set<MockDefinition> mockDefinitions;

	private ClassLoader classLoader;

	private BeanFactory beanFactory;

	private final BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();

	private Map<MockDefinition, String> beanNameRegistry = new HashMap<MockDefinition, String>();

	private Map<Field, String> fieldRegistry = new HashMap<Field, String>();

	/**
	 * Create a new {@link MockitoPostProcessor} instance with the given initial
	 * definitions.
	 * @param mockDefinitions the initial definitions
	 */
	public MockitoPostProcessor(Set<MockDefinition> mockDefinitions) {
		this.mockDefinitions = mockDefinitions;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"Mock beans can only be used with a ConfigurableListableBeanFactory");
		this.beanFactory = beanFactory;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
				"@RegisterMocks can only be used on bean factories that "
						+ "implement BeanDefinitionRegistry");
		postProcessBeanFactory(beanFactory, (BeanDefinitionRegistry) beanFactory);
	}

	private void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry) {
		MockDefinitionsParser parser = new MockDefinitionsParser(this.mockDefinitions);
		for (Class<?> configurationClass : getConfigurationClasses(beanFactory)) {
			parser.parse(configurationClass);
		}
		Set<MockDefinition> definitions = parser.getDefinitions();
		for (MockDefinition definition : definitions) {
			Field field = parser.getField(definition);
			registerMock(beanFactory, registry, definition, field);
		}
	}

	private Set<Class<?>> getConfigurationClasses(
			ConfigurableListableBeanFactory beanFactory) {
		Set<Class<?>> configurationClasses = new LinkedHashSet<Class<?>>();
		for (BeanDefinition beanDefinition : getConfigurationBeanDefinitions(beanFactory)
				.values()) {
			configurationClasses.add(ClassUtils.resolveClassName(
					beanDefinition.getBeanClassName(), this.classLoader));
		}
		return configurationClasses;
	}

	private Map<String, BeanDefinition> getConfigurationBeanDefinitions(
			ConfigurableListableBeanFactory beanFactory) {
		Map<String, BeanDefinition> definitions = new LinkedHashMap<String, BeanDefinition>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
			if (definition.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE) != null) {
				definitions.put(beanName, definition);
			}
		}
		return definitions;
	}

	void inject(Field field, Object target, MockDefinition definition) {
		String beanName = this.beanNameRegistry.get(definition);
		Assert.state(StringUtils.hasLength(beanName),
				"No mock found for definition " + definition);
		injectMock(field, target, beanName);
	}

	private void registerMock(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry, MockDefinition mockDefinition, Field field) {
		RootBeanDefinition beanDefinition = createBeanDefinition(mockDefinition);
		String name = getBeanName(beanFactory, registry, mockDefinition, beanDefinition);
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, name);
		registry.registerBeanDefinition(name, beanDefinition);
		this.beanNameRegistry.put(mockDefinition, name);
		if (field != null) {
			this.fieldRegistry.put(field, name);
		}
	}

	private RootBeanDefinition createBeanDefinition(MockDefinition mockDefinition) {
		RootBeanDefinition definition = new RootBeanDefinition(
				mockDefinition.getClassToMock());
		definition.setTargetType(mockDefinition.getClassToMock());
		definition.setFactoryBeanName(BEAN_NAME);
		definition.setFactoryMethodName("createMock");
		definition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				mockDefinition);
		return definition;
	}

	/**
	 * Factory method used by defined beans to actually create the mock.
	 * @param definition the mock definition
	 * @param name the bean name
	 * @return the mock instance
	 */
	protected final Object createMock(MockDefinition definition, String name) {
		return definition.createMock(name + " bean");
	}

	private String getBeanName(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry, MockDefinition mockDefinition,
			RootBeanDefinition beanDefinition) {
		if (StringUtils.hasLength(mockDefinition.getName())) {
			return mockDefinition.getName();
		}
		String[] existingBeans = beanFactory
				.getBeanNamesForType(mockDefinition.getClassToMock());
		if (ObjectUtils.isEmpty(existingBeans)) {
			return this.beanNameGenerator.generateBeanName(beanDefinition, registry);
		}
		if (existingBeans.length == 1) {
			return existingBeans[0];
		}
		throw new IllegalStateException("Unable to register mock bean "
				+ mockDefinition.getClassToMock().getName()
				+ " expected a single existing bean to replace but found "
				+ new TreeSet<String>(Arrays.asList(existingBeans)));
	}

	@Override
	public PropertyValues postProcessPropertyValues(PropertyValues pvs,
			PropertyDescriptor[] pds, final Object bean, String beanName)
					throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {

			@Override
			public void doWith(Field field)
					throws IllegalArgumentException, IllegalAccessException {
				postProcessField(bean, field);
			}

		});
		return pvs;
	}

	private void postProcessField(Object bean, Field field) {
		String beanName = this.fieldRegistry.get(field);
		if (StringUtils.hasLength(beanName)) {
			injectMock(field, bean, beanName);
		}
	}

	private void injectMock(Field field, Object target, String beanName) {
		try {
			field.setAccessible(true);
			Object mockBean = this.beanFactory.getBean(beanName, field.getType());
			ReflectionUtils.setField(field, target, mockBean);
		}
		catch (Throwable ex) {
			throw new BeanCreationException("Could not inject mock field: " + field, ex);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}

	/**
	 * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
	 * using the {@link SpringRunner} as registration is automatic.
	 * @param registry the bean definition registry
	 */
	public static void register(BeanDefinitionRegistry registry) {
		register(registry, null);
	}

	/**
	 * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
	 * using the {@link SpringRunner} as registration is automatic.
	 * @param registry the bean definition registry
	 * @param mockDefinitions the initial mock definitions
	 */
	public static void register(BeanDefinitionRegistry registry,
			Set<MockDefinition> mockDefinitions) {
		register(registry, MockitoPostProcessor.class, mockDefinitions);
	}

	/**
	 * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
	 * using the {@link SpringRunner} as registration is automatic.
	 * @param registry the bean definition registry
	 * @param postProcessor the post processor class to register
	 * @param mockDefinitions the initial mock definitions
	 */
	@SuppressWarnings("unchecked")
	public static void register(BeanDefinitionRegistry registry,
			Class<? extends MockitoPostProcessor> postProcessor,
			Set<MockDefinition> mockDefinitions) {
		BeanDefinition definition = getOrAddBeanDefinition(registry, postProcessor);
		ValueHolder constructorArg = definition.getConstructorArgumentValues()
				.getIndexedArgumentValue(0, Set.class);
		Set<MockDefinition> existing = (Set<MockDefinition>) constructorArg.getValue();
		if (mockDefinitions != null) {
			existing.addAll(mockDefinitions);
		}
	}

	private static BeanDefinition getOrAddBeanDefinition(BeanDefinitionRegistry registry,
			Class<? extends MockitoPostProcessor> postProcessor) {
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			RootBeanDefinition definition = new RootBeanDefinition(postProcessor);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			ConstructorArgumentValues constructorArguments = definition
					.getConstructorArgumentValues();
			constructorArguments.addIndexedArgumentValue(0,
					new LinkedHashSet<MockDefinition>());
			registry.registerBeanDefinition(BEAN_NAME, definition);
			return definition;
		}
		return registry.getBeanDefinition(BEAN_NAME);
	}

}
