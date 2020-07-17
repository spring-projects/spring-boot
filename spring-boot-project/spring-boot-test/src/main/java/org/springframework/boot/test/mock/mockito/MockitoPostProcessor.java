/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanFactoryPostProcessor} used to register and inject
 * {@link MockBean @MockBeans} with the {@link ApplicationContext}. An initial set of
 * definitions can be passed to the processor with additional definitions being
 * automatically created from {@code @Configuration} classes that use
 * {@link MockBean @MockBean}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Andreas Neiser
 * @since 1.4.0
 */
public class MockitoPostProcessor implements InstantiationAwareBeanPostProcessor, BeanClassLoaderAware,
		BeanFactoryAware, BeanFactoryPostProcessor, Ordered {

	private static final String BEAN_NAME = MockitoPostProcessor.class.getName();

	private static final String CONFIGURATION_CLASS_ATTRIBUTE = Conventions
			.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

	private static final BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();

	private final Set<Definition> definitions;

	private ClassLoader classLoader;

	private BeanFactory beanFactory;

	private final MockitoBeans mockitoBeans = new MockitoBeans();

	private Map<Definition, String> beanNameRegistry = new HashMap<>();

	private Map<Field, String> fieldRegistry = new HashMap<>();

	private Map<String, SpyDefinition> spies = new HashMap<>();

	/**
	 * Create a new {@link MockitoPostProcessor} instance with the given initial
	 * definitions.
	 * @param definitions the initial definitions
	 */
	public MockitoPostProcessor(Set<Definition> definitions) {
		this.definitions = definitions;
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
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
				"@MockBean can only be used on bean factories that implement BeanDefinitionRegistry");
		postProcessBeanFactory(beanFactory, (BeanDefinitionRegistry) beanFactory);
	}

	private void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
		beanFactory.registerSingleton(MockitoBeans.class.getName(), this.mockitoBeans);
		DefinitionsParser parser = new DefinitionsParser(this.definitions);
		for (Class<?> configurationClass : getConfigurationClasses(beanFactory)) {
			parser.parse(configurationClass);
		}
		Set<Definition> definitions = parser.getDefinitions();
		for (Definition definition : definitions) {
			Field field = parser.getField(definition);
			register(beanFactory, registry, definition, field);
		}
	}

	private Set<Class<?>> getConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Set<Class<?>> configurationClasses = new LinkedHashSet<>();
		for (BeanDefinition beanDefinition : getConfigurationBeanDefinitions(beanFactory).values()) {
			configurationClasses.add(ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), this.classLoader));
		}
		return configurationClasses;
	}

	private Map<String, BeanDefinition> getConfigurationBeanDefinitions(ConfigurableListableBeanFactory beanFactory) {
		Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
			if (definition.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE) != null) {
				definitions.put(beanName, definition);
			}
		}
		return definitions;
	}

	private void register(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			Definition definition, Field field) {
		if (definition instanceof MockDefinition) {
			registerMock(beanFactory, registry, (MockDefinition) definition, field);
		}
		else if (definition instanceof SpyDefinition) {
			registerSpy(beanFactory, registry, (SpyDefinition) definition, field);
		}
	}

	private void registerMock(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			MockDefinition definition, Field field) {
		RootBeanDefinition beanDefinition = createBeanDefinition(definition);
		String beanName = getBeanName(beanFactory, registry, definition, beanDefinition);
		String transformedBeanName = BeanFactoryUtils.transformedBeanName(beanName);
		if (registry.containsBeanDefinition(transformedBeanName)) {
			BeanDefinition existing = registry.getBeanDefinition(transformedBeanName);
			copyBeanDefinitionDetails(existing, beanDefinition);
			registry.removeBeanDefinition(transformedBeanName);
		}
		registry.registerBeanDefinition(transformedBeanName, beanDefinition);
		Object mock = definition.createMock(beanName + " bean");
		beanFactory.registerSingleton(transformedBeanName, mock);
		this.mockitoBeans.add(mock);
		this.beanNameRegistry.put(definition, beanName);
		if (field != null) {
			this.fieldRegistry.put(field, beanName);
		}
	}

	private RootBeanDefinition createBeanDefinition(MockDefinition mockDefinition) {
		RootBeanDefinition definition = new RootBeanDefinition(mockDefinition.getTypeToMock().resolve());
		definition.setTargetType(mockDefinition.getTypeToMock());
		if (mockDefinition.getQualifier() != null) {
			mockDefinition.getQualifier().applyTo(definition);
		}
		return definition;
	}

	private String getBeanName(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			MockDefinition mockDefinition, RootBeanDefinition beanDefinition) {
		if (StringUtils.hasLength(mockDefinition.getName())) {
			return mockDefinition.getName();
		}
		Set<String> existingBeans = getExistingBeans(beanFactory, mockDefinition.getTypeToMock(),
				mockDefinition.getQualifier());
		if (existingBeans.isEmpty()) {
			return MockitoPostProcessor.beanNameGenerator.generateBeanName(beanDefinition, registry);
		}
		if (existingBeans.size() == 1) {
			return existingBeans.iterator().next();
		}
		String primaryCandidate = determinePrimaryCandidate(registry, existingBeans, mockDefinition.getTypeToMock());
		if (primaryCandidate != null) {
			return primaryCandidate;
		}
		throw new IllegalStateException("Unable to register mock bean " + mockDefinition.getTypeToMock()
				+ " expected a single matching bean to replace but found " + existingBeans);
	}

	private void copyBeanDefinitionDetails(BeanDefinition from, RootBeanDefinition to) {
		to.setPrimary(from.isPrimary());
	}

	private void registerSpy(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			SpyDefinition spyDefinition, Field field) {
		Set<String> existingBeans = getExistingBeans(beanFactory, spyDefinition.getTypeToSpy(),
				spyDefinition.getQualifier());
		if (ObjectUtils.isEmpty(existingBeans)) {
			createSpy(registry, spyDefinition, field);
		}
		else {
			registerSpies(registry, spyDefinition, field, existingBeans);
		}
	}

	private Set<String> getExistingBeans(ConfigurableListableBeanFactory beanFactory, ResolvableType type,
			QualifierDefinition qualifier) {
		Set<String> candidates = new TreeSet<>();
		for (String candidate : getExistingBeans(beanFactory, type)) {
			if (qualifier == null || qualifier.matches(beanFactory, candidate)) {
				candidates.add(candidate);
			}
		}
		return candidates;
	}

	private Set<String> getExistingBeans(ConfigurableListableBeanFactory beanFactory, ResolvableType type) {
		Set<String> beans = new LinkedHashSet<>(Arrays.asList(beanFactory.getBeanNamesForType(type, true, false)));
		String typeName = type.resolve(Object.class).getName();
		for (String beanName : beanFactory.getBeanNamesForType(FactoryBean.class, true, false)) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (typeName.equals(beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE))) {
				beans.add(beanName);
			}
		}
		beans.removeIf(this::isScopedTarget);
		return beans;
	}

	private boolean isScopedTarget(String beanName) {
		try {
			return ScopedProxyUtils.isScopedTarget(beanName);
		}
		catch (Throwable ex) {
			return false;
		}
	}

	private void createSpy(BeanDefinitionRegistry registry, SpyDefinition spyDefinition, Field field) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(spyDefinition.getTypeToSpy().resolve());
		String beanName = MockitoPostProcessor.beanNameGenerator.generateBeanName(beanDefinition, registry);
		registry.registerBeanDefinition(beanName, beanDefinition);
		registerSpy(spyDefinition, field, beanName);
	}

	private void registerSpies(BeanDefinitionRegistry registry, SpyDefinition spyDefinition, Field field,
			Collection<String> existingBeans) {
		try {
			String beanName = determineBeanName(existingBeans, spyDefinition, registry);
			registerSpy(spyDefinition, field, beanName);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Unable to register spy bean " + spyDefinition.getTypeToSpy(), ex);
		}
	}

	private String determineBeanName(Collection<String> existingBeans, SpyDefinition definition,
			BeanDefinitionRegistry registry) {
		if (StringUtils.hasText(definition.getName())) {
			return definition.getName();
		}
		if (existingBeans.size() == 1) {
			return existingBeans.iterator().next();
		}
		return determinePrimaryCandidate(registry, existingBeans, definition.getTypeToSpy());
	}

	private String determinePrimaryCandidate(BeanDefinitionRegistry registry, Collection<String> candidateBeanNames,
			ResolvableType type) {
		String primaryBeanName = null;
		for (String candidateBeanName : candidateBeanNames) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(candidateBeanName);
			if (beanDefinition.isPrimary()) {
				if (primaryBeanName != null) {
					throw new NoUniqueBeanDefinitionException(type.resolve(), candidateBeanNames.size(),
							"more than one 'primary' bean found among candidates: "
									+ Collections.singletonList(candidateBeanNames));
				}
				primaryBeanName = candidateBeanName;
			}
		}
		return primaryBeanName;
	}

	private void registerSpy(SpyDefinition definition, Field field, String beanName) {
		this.spies.put(beanName, definition);
		this.beanNameRegistry.put(definition, beanName);
		if (field != null) {
			this.fieldRegistry.put(field, beanName);
		}
	}

	protected final Object createSpyIfNecessary(Object bean, String beanName) throws BeansException {
		SpyDefinition definition = this.spies.get(beanName);
		if (definition != null) {
			bean = definition.createSpy(beanName, bean);
		}
		return bean;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), (field) -> postProcessField(bean, field));
		return pvs;
	}

	private void postProcessField(Object bean, Field field) {
		String beanName = this.fieldRegistry.get(field);
		if (StringUtils.hasText(beanName)) {
			inject(field, bean, beanName);
		}
	}

	void inject(Field field, Object target, Definition definition) {
		String beanName = this.beanNameRegistry.get(definition);
		Assert.state(StringUtils.hasLength(beanName), () -> "No bean found for definition " + definition);
		inject(field, target, beanName);
	}

	private void inject(Field field, Object target, String beanName) {
		try {
			field.setAccessible(true);
			Assert.state(ReflectionUtils.getField(field, target) == null,
					() -> "The field " + field + " cannot have an existing value");
			Object bean = this.beanFactory.getBean(beanName, field.getType());
			ReflectionUtils.setField(field, target, bean);
		}
		catch (Throwable ex) {
			throw new BeanCreationException("Could not inject field: " + field, ex);
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
	 * @param definitions the initial mock/spy definitions
	 */
	public static void register(BeanDefinitionRegistry registry, Set<Definition> definitions) {
		register(registry, MockitoPostProcessor.class, definitions);
	}

	/**
	 * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
	 * using the {@link SpringRunner} as registration is automatic.
	 * @param registry the bean definition registry
	 * @param postProcessor the post processor class to register
	 * @param definitions the initial mock/spy definitions
	 */
	@SuppressWarnings("unchecked")
	public static void register(BeanDefinitionRegistry registry, Class<? extends MockitoPostProcessor> postProcessor,
			Set<Definition> definitions) {
		SpyPostProcessor.register(registry);
		BeanDefinition definition = getOrAddBeanDefinition(registry, postProcessor);
		ValueHolder constructorArg = definition.getConstructorArgumentValues().getIndexedArgumentValue(0, Set.class);
		Set<Definition> existing = (Set<Definition>) constructorArg.getValue();
		if (definitions != null) {
			existing.addAll(definitions);
		}
	}

	private static BeanDefinition getOrAddBeanDefinition(BeanDefinitionRegistry registry,
			Class<? extends MockitoPostProcessor> postProcessor) {
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			RootBeanDefinition definition = new RootBeanDefinition(postProcessor);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
			constructorArguments.addIndexedArgumentValue(0, new LinkedHashSet<MockDefinition>());
			registry.registerBeanDefinition(BEAN_NAME, definition);
			return definition;
		}
		return registry.getBeanDefinition(BEAN_NAME);
	}

	/**
	 * {@link BeanPostProcessor} to handle {@link SpyBean} definitions. Registered as a
	 * separate processor so that it can be ordered above AOP post processors.
	 */
	static class SpyPostProcessor implements SmartInstantiationAwareBeanPostProcessor, PriorityOrdered {

		private static final String BEAN_NAME = SpyPostProcessor.class.getName();

		private final MockitoPostProcessor mockitoPostProcessor;

		SpyPostProcessor(MockitoPostProcessor mockitoPostProcessor) {
			this.mockitoPostProcessor = mockitoPostProcessor;
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
			return this.mockitoPostProcessor.createSpyIfNecessary(bean, beanName);
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof FactoryBean) {
				return bean;
			}
			return this.mockitoPostProcessor.createSpyIfNecessary(bean, beanName);
		}

		static void register(BeanDefinitionRegistry registry) {
			if (!registry.containsBeanDefinition(BEAN_NAME)) {
				RootBeanDefinition definition = new RootBeanDefinition(SpyPostProcessor.class);
				definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
				constructorArguments.addIndexedArgumentValue(0,
						new RuntimeBeanReference(MockitoPostProcessor.BEAN_NAME));
				registry.registerBeanDefinition(BEAN_NAME, definition);
			}
		}

	}

}
