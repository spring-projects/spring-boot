/*
 * Copyright 2012-2017 the original author or authors.
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
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.RuntimeBeanReference;
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
 * @since 1.4.0
 */
public class MockitoPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
		implements BeanClassLoaderAware, BeanFactoryAware, BeanFactoryPostProcessor,
		Ordered {

	private static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

	private static final String BEAN_NAME = MockitoPostProcessor.class.getName();

	private static final String CONFIGURATION_CLASS_ATTRIBUTE = Conventions
			.getQualifiedAttributeName(ConfigurationClassPostProcessor.class,
					"configurationClass");

	private final Set<Definition> definitions;

	private ClassLoader classLoader;

	private BeanFactory beanFactory;

	private final BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();

	private final MockitoBeans mockitoBeans = new MockitoBeans();

	private Map<Definition, String> beanNameRegistry = new HashMap<>();

	private Map<Field, RegisteredField> fieldRegistry = new HashMap<>();

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
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
				"@MockBean can only be used on bean factories that "
						+ "implement BeanDefinitionRegistry");
		postProcessBeanFactory(beanFactory, (BeanDefinitionRegistry) beanFactory);
	}

	private void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry) {
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

	private Set<Class<?>> getConfigurationClasses(
			ConfigurableListableBeanFactory beanFactory) {
		Set<Class<?>> configurationClasses = new LinkedHashSet<>();
		for (BeanDefinition beanDefinition : getConfigurationBeanDefinitions(beanFactory)
				.values()) {
			configurationClasses.add(ClassUtils.resolveClassName(
					beanDefinition.getBeanClassName(), this.classLoader));
		}
		return configurationClasses;
	}

	private Map<String, BeanDefinition> getConfigurationBeanDefinitions(
			ConfigurableListableBeanFactory beanFactory) {
		Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
			if (definition.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE) != null) {
				definitions.put(beanName, definition);
			}
		}
		return definitions;
	}

	private void register(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry, Definition definition, Field field) {
		if (definition instanceof MockDefinition) {
			registerMock(beanFactory, registry, (MockDefinition) definition, field);
		}
		else if (definition instanceof SpyDefinition) {
			registerSpy(beanFactory, registry, (SpyDefinition) definition, field);
		}
	}

	private void registerMock(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry, MockDefinition definition, Field field) {
		RootBeanDefinition beanDefinition = createBeanDefinition(definition);
		String beanName = getBeanName(beanFactory, registry, definition, beanDefinition);
		String transformedBeanName = BeanFactoryUtils.transformedBeanName(beanName);
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1,
				beanName);
		if (registry.containsBeanDefinition(transformedBeanName)) {
			registry.removeBeanDefinition(transformedBeanName);
		}
		registry.registerBeanDefinition(transformedBeanName, beanDefinition);
		Object mock = createMock(definition, beanName);
		beanFactory.registerSingleton(transformedBeanName, mock);
		this.mockitoBeans.add(mock);
		this.beanNameRegistry.put(definition, beanName);
		if (field != null) {
			this.fieldRegistry.put(field, new RegisteredField(definition, beanName));
		}
	}

	private RootBeanDefinition createBeanDefinition(MockDefinition mockDefinition) {
		RootBeanDefinition definition = new RootBeanDefinition(
				mockDefinition.getTypeToMock().resolve());
		definition.setTargetType(mockDefinition.getTypeToMock());
		definition.setFactoryBeanName(BEAN_NAME);
		definition.setFactoryMethodName("createMock");
		definition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				mockDefinition);
		if (mockDefinition.getQualifier() != null) {
			mockDefinition.getQualifier().applyTo(definition);
		}
		return definition;
	}

	/**
	 * Factory method used by defined beans to actually create the mock.
	 * @param mockDefinition the mock definition
	 * @param name the bean name
	 * @return the mock instance
	 */
	protected final Object createMock(MockDefinition mockDefinition, String name) {
		return mockDefinition.createMock(name + " bean");
	}

	private String getBeanName(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry, MockDefinition mockDefinition,
			RootBeanDefinition beanDefinition) {
		if (StringUtils.hasLength(mockDefinition.getName())) {
			return mockDefinition.getName();
		}
		Set<String> existingBeans = findCandidateBeans(beanFactory, mockDefinition);
		if (existingBeans.isEmpty()) {
			return this.beanNameGenerator.generateBeanName(beanDefinition, registry);
		}
		if (existingBeans.size() == 1) {
			return existingBeans.iterator().next();
		}
		throw new IllegalStateException(
				"Unable to register mock bean " + mockDefinition.getTypeToMock()
						+ " expected a single matching bean to replace but found "
						+ existingBeans);
	}

	private void registerSpy(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry, SpyDefinition definition, Field field) {
		String[] existingBeans = getExistingBeans(beanFactory, definition.getTypeToSpy());
		if (ObjectUtils.isEmpty(existingBeans)) {
			createSpy(registry, definition, field);
		}
		else {
			registerSpies(registry, definition, field, existingBeans);
		}
	}

	private Set<String> findCandidateBeans(ConfigurableListableBeanFactory beanFactory,
			MockDefinition mockDefinition) {
		QualifierDefinition qualifier = mockDefinition.getQualifier();
		Set<String> candidates = new TreeSet<>();
		for (String candidate : getExistingBeans(beanFactory,
				mockDefinition.getTypeToMock())) {
			if (qualifier == null || qualifier.matches(beanFactory, candidate)) {
				candidates.add(candidate);
			}
		}
		return candidates;
	}

	private String[] getExistingBeans(ConfigurableListableBeanFactory beanFactory,
			ResolvableType type) {
		Set<String> beans = new LinkedHashSet<>(
				Arrays.asList(beanFactory.getBeanNamesForType(type)));
		String resolvedTypeName = type.resolve(Object.class).getName();
		for (String beanName : beanFactory.getBeanNamesForType(FactoryBean.class)) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (resolvedTypeName
					.equals(beanDefinition.getAttribute(FACTORY_BEAN_OBJECT_TYPE))) {
				beans.add(beanName);
			}
		}
		beans.removeIf(this::isScopedTarget);
		return beans.toArray(new String[beans.size()]);
	}

	private boolean isScopedTarget(String beanName) {
		try {
			return ScopedProxyUtils.isScopedTarget(beanName);
		}
		catch (Throwable ex) {
			return false;
		}
	}

	private void createSpy(BeanDefinitionRegistry registry, SpyDefinition definition,
			Field field) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				definition.getTypeToSpy().resolve());
		String beanName = this.beanNameGenerator.generateBeanName(beanDefinition,
				registry);
		registry.registerBeanDefinition(beanName, beanDefinition);
		registerSpy(definition, field, beanName);
	}

	private void registerSpies(BeanDefinitionRegistry registry, SpyDefinition definition,
			Field field, String[] existingBeans) {
		try {
			registerSpy(definition, field,
					determineBeanName(existingBeans, definition, registry));
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException(
					"Unable to register spy bean " + definition.getTypeToSpy(), ex);
		}
	}

	private String determineBeanName(String[] existingBeans, SpyDefinition definition,
			BeanDefinitionRegistry registry) {
		if (StringUtils.hasText(definition.getName())) {
			return definition.getName();
		}
		if (existingBeans.length == 1) {
			return existingBeans[0];
		}
		return determinePrimaryCandidate(registry, existingBeans,
				definition.getTypeToSpy());
	}

	private String determinePrimaryCandidate(BeanDefinitionRegistry registry,
			String[] candidateBeanNames, ResolvableType type) {
		String primaryBeanName = null;
		for (String candidateBeanName : candidateBeanNames) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(candidateBeanName);
			if (beanDefinition.isPrimary()) {
				if (primaryBeanName != null) {
					throw new NoUniqueBeanDefinitionException(type.resolve(),
							candidateBeanNames.length,
							"more than one 'primary' bean found among candidates: "
									+ Arrays.asList(candidateBeanNames));
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
			this.fieldRegistry.put(field, new RegisteredField(definition, beanName));
		}
	}

	protected Object createSpyIfNecessary(Object bean, String beanName)
			throws BeansException {
		SpyDefinition definition = this.spies.get(beanName);
		if (definition != null) {
			bean = definition.createSpy(beanName, bean);
		}
		return bean;
	}

	@Override
	public PropertyValues postProcessPropertyValues(PropertyValues pvs,
			PropertyDescriptor[] pds, final Object bean, String beanName)
					throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(),
				(field) -> postProcessField(bean, field));
		return pvs;
	}

	private void postProcessField(Object bean, Field field) {
		RegisteredField registered = this.fieldRegistry.get(field);
		if (registered != null && StringUtils.hasLength(registered.getBeanName())) {
			inject(field, bean, registered.getBeanName(), registered.getDefinition());
		}
	}

	void inject(Field field, Object target, Definition definition) {
		String beanName = this.beanNameRegistry.get(definition);
		Assert.state(StringUtils.hasLength(beanName),
				() -> "No bean found for definition " + definition);
		inject(field, target, beanName, definition);
	}

	private void inject(Field field, Object target, String beanName,
			Definition definition) {
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
	public static void register(BeanDefinitionRegistry registry,
			Set<Definition> definitions) {
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
	public static void register(BeanDefinitionRegistry registry,
			Class<? extends MockitoPostProcessor> postProcessor,
			Set<Definition> definitions) {
		SpyPostProcessor.register(registry);
		BeanDefinition definition = getOrAddBeanDefinition(registry, postProcessor);
		ValueHolder constructorArg = definition.getConstructorArgumentValues()
				.getIndexedArgumentValue(0, Set.class);
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
			ConstructorArgumentValues constructorArguments = definition
					.getConstructorArgumentValues();
			constructorArguments.addIndexedArgumentValue(0,
					new LinkedHashSet<MockDefinition>());
			registry.registerBeanDefinition(BEAN_NAME, definition);
			return definition;
		}
		return registry.getBeanDefinition(BEAN_NAME);
	}

	/**
	 * {@link BeanPostProcessor} to handle {@link SpyBean} definitions. Registered as a
	 * separate processor so that it can be ordered above AOP post processors.
	 */
	static class SpyPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
			implements PriorityOrdered {

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
		public Object getEarlyBeanReference(Object bean, String beanName)
				throws BeansException {
			return createSpyIfNecessary(bean, beanName);
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof FactoryBean) {
				return bean;
			}
			return createSpyIfNecessary(bean, beanName);
		}

		private Object createSpyIfNecessary(Object bean, String beanName) {
			return this.mockitoPostProcessor.createSpyIfNecessary(bean, beanName);
		}

		public static void register(BeanDefinitionRegistry registry) {
			if (!registry.containsBeanDefinition(BEAN_NAME)) {
				RootBeanDefinition definition = new RootBeanDefinition(
						SpyPostProcessor.class);
				definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				ConstructorArgumentValues constructorArguments = definition
						.getConstructorArgumentValues();
				constructorArguments.addIndexedArgumentValue(0,
						new RuntimeBeanReference(MockitoPostProcessor.BEAN_NAME));
				registry.registerBeanDefinition(BEAN_NAME, definition);
			}
		}

	}

	/**
	 * A registered field item.
	 */
	private static class RegisteredField {

		private final Definition definition;

		private final String beanName;

		RegisteredField(Definition definition, String beanName) {
			this.definition = definition;
			this.beanName = beanName;
		}

		public Definition getDefinition() {
			return this.definition;
		}

		public String getBeanName() {
			return this.beanName;
		}

	}

}
