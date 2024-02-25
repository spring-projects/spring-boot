/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.concurrent.ConcurrentHashMap;

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

	private final Map<Definition, String> beanNameRegistry = new HashMap<>();

	private final Map<Field, String> fieldRegistry = new HashMap<>();

	private final Map<String, SpyDefinition> spies = new HashMap<>();

	/**
	 * Create a new {@link MockitoPostProcessor} instance with the given initial
	 * definitions.
	 * @param definitions the initial definitions
	 */
	public MockitoPostProcessor(Set<Definition> definitions) {
		this.definitions = definitions;
	}

	/**
     * Sets the bean class loader.
     * 
     * @param classLoader the class loader to be set
     */
    @Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
     * Set the bean factory for this MockitoPostProcessor.
     * 
     * @param beanFactory the bean factory to set
     * @throws BeansException if the bean factory is not an instance of ConfigurableListableBeanFactory
     */
    @Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"Mock beans can only be used with a ConfigurableListableBeanFactory");
		this.beanFactory = beanFactory;
	}

	/**
     * Post-processes the bean factory by checking if it is an instance of BeanDefinitionRegistry and then
     * calling the overloaded postProcessBeanFactory method with the appropriate arguments.
     *
     * @param beanFactory the bean factory to be post-processed
     * @throws BeansException if an error occurs during the post-processing of the bean factory
     * @see BeanDefinitionRegistry
     * @see #postProcessBeanFactory(ConfigurableListableBeanFactory, BeanDefinitionRegistry)
     */
    @Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
				"@MockBean can only be used on bean factories that implement BeanDefinitionRegistry");
		postProcessBeanFactory(beanFactory, (BeanDefinitionRegistry) beanFactory);
	}

	/**
     * Post-processes the bean factory by registering the MockitoBeans singleton and parsing the definitions
     * from the configuration classes.
     * 
     * @param beanFactory the configurable listable bean factory
     * @param registry the bean definition registry
     */
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

	/**
     * Retrieves the set of configuration classes from the given bean factory.
     * 
     * @param beanFactory the configurable listable bean factory
     * @return the set of configuration classes
     */
    private Set<Class<?>> getConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Set<Class<?>> configurationClasses = new LinkedHashSet<>();
		for (BeanDefinition beanDefinition : getConfigurationBeanDefinitions(beanFactory).values()) {
			configurationClasses.add(ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), this.classLoader));
		}
		return configurationClasses;
	}

	/**
     * Retrieves the bean definitions for configuration beans from the given bean factory.
     * 
     * @param beanFactory the configurable listable bean factory
     * @return a map of bean names to their corresponding bean definitions
     */
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

	/**
     * Registers a mock or spy bean in the bean factory and registry based on the given definition and field.
     * 
     * @param beanFactory the configurable listable bean factory
     * @param registry the bean definition registry
     * @param definition the definition of the mock or spy
     * @param field the field annotated with @Mock or @Spy
     */
    private void register(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			Definition definition, Field field) {
		if (definition instanceof MockDefinition mockDefinition) {
			registerMock(beanFactory, registry, mockDefinition, field);
		}
		else if (definition instanceof SpyDefinition spyDefinition) {
			registerSpy(beanFactory, registry, spyDefinition, field);
		}
	}

	/**
     * Registers a mock bean in the bean factory and bean definition registry.
     * 
     * @param beanFactory the configurable listable bean factory
     * @param registry the bean definition registry
     * @param definition the mock definition
     * @param field the field associated with the mock bean (optional)
     */
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

	/**
     * Creates a bean definition for a mock object based on the provided mock definition.
     * 
     * @param mockDefinition the mock definition containing the type to mock and any qualifiers
     * @return the created bean definition
     */
    private RootBeanDefinition createBeanDefinition(MockDefinition mockDefinition) {
		RootBeanDefinition definition = new RootBeanDefinition(mockDefinition.getTypeToMock().resolve());
		definition.setTargetType(mockDefinition.getTypeToMock());
		if (mockDefinition.getQualifier() != null) {
			mockDefinition.getQualifier().applyTo(definition);
		}
		return definition;
	}

	/**
     * Returns the name of the bean to be registered as a mock.
     * If the mock definition has a specified name, that name is returned.
     * If there are no existing beans of the same type and qualifier, a new bean name is generated using the bean name generator.
     * If there is only one existing bean of the same type and qualifier, its name is returned.
     * If there are multiple existing beans of the same type and qualifier, the primary candidate is determined and its name is returned.
     * If no primary candidate is found, an IllegalStateException is thrown.
     *
     * @param beanFactory      the bean factory to check for existing beans
     * @param registry         the bean definition registry
     * @param mockDefinition   the mock definition containing the type and qualifier of the mock
     * @param beanDefinition   the root bean definition of the mock
     * @return the name of the bean to be registered as a mock
     * @throws IllegalStateException if no primary candidate is found among the existing beans
     */
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

	/**
     * Copies the bean definition details from the given source bean definition to the target root bean definition.
     * 
     * @param from the source bean definition to copy from
     * @param to the target root bean definition to copy to
     */
    private void copyBeanDefinitionDetails(BeanDefinition from, RootBeanDefinition to) {
		to.setPrimary(from.isPrimary());
	}

	/**
     * Registers a spy bean in the bean factory or registry.
     * 
     * @param beanFactory the bean factory to register the spy bean in
     * @param registry the bean definition registry to register the spy bean in
     * @param spyDefinition the definition of the spy bean
     * @param field the field annotated with @Spy
     */
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

	/**
     * Retrieves the existing beans from the given bean factory that match the specified type and qualifier.
     * 
     * @param beanFactory the configurable listable bean factory
     * @param type the resolvable type to match the beans against
     * @param qualifier the qualifier definition to match the beans against (can be null)
     * @return a set of existing beans that match the specified type and qualifier
     */
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

	/**
     * Retrieves the set of existing beans of a given type from the bean factory.
     * 
     * @param beanFactory the configurable listable bean factory
     * @param resolvableType the resolvable type of the beans to retrieve
     * @return the set of existing beans of the given type
     */
    private Set<String> getExistingBeans(ConfigurableListableBeanFactory beanFactory, ResolvableType resolvableType) {
		Set<String> beans = new LinkedHashSet<>(
				Arrays.asList(beanFactory.getBeanNamesForType(resolvableType, true, false)));
		Class<?> type = resolvableType.resolve(Object.class);
		for (String beanName : beanFactory.getBeanNamesForType(FactoryBean.class, true, false)) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			Object attribute = beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
			if (resolvableType.equals(attribute) || type.equals(attribute)) {
				beans.add(beanName);
			}
		}
		beans.removeIf(this::isScopedTarget);
		return beans;
	}

	/**
     * Checks if the given bean name is a scoped target.
     * 
     * @param beanName the name of the bean to check
     * @return {@code true} if the bean is a scoped target, {@code false} otherwise
     * @throws Throwable if an error occurs while checking the bean
     */
    private boolean isScopedTarget(String beanName) {
		try {
			return ScopedProxyUtils.isScopedTarget(beanName);
		}
		catch (Throwable ex) {
			return false;
		}
	}

	/**
     * Creates a spy bean definition and registers it in the given bean definition registry.
     * 
     * @param registry the bean definition registry to register the spy bean definition
     * @param spyDefinition the spy definition containing the type to spy
     * @param field the field in which the spy will be injected
     */
    private void createSpy(BeanDefinitionRegistry registry, SpyDefinition spyDefinition, Field field) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(spyDefinition.getTypeToSpy().resolve());
		String beanName = MockitoPostProcessor.beanNameGenerator.generateBeanName(beanDefinition, registry);
		registry.registerBeanDefinition(beanName, beanDefinition);
		registerSpy(spyDefinition, field, beanName);
	}

	/**
     * Registers a spy bean in the given BeanDefinitionRegistry.
     * 
     * @param registry         the BeanDefinitionRegistry to register the spy bean in
     * @param spyDefinition    the SpyDefinition containing the details of the spy bean to register
     * @param field            the Field object representing the field in which the spy bean is being injected
     * @param existingBeans    a collection of existing bean names in the registry
     * @throws IllegalStateException if unable to register the spy bean
     */
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

	/**
     * Determines the name of the bean to be created based on the given parameters.
     * 
     * @param existingBeans the collection of existing bean names
     * @param definition the spy definition
     * @param registry the bean definition registry
     * @return the name of the bean to be created
     */
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

	/**
     * Determines the primary candidate bean name from the given collection of candidate bean names.
     * 
     * @param registry the bean definition registry
     * @param candidateBeanNames the collection of candidate bean names
     * @param type the resolvable type
     * @return the primary candidate bean name, or null if no primary candidate is found
     * @throws NoUniqueBeanDefinitionException if more than one 'primary' bean is found among the candidates
     */
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

	/**
     * Registers a spy with the given definition, field, and bean name.
     * 
     * @param definition the definition of the spy
     * @param field the field associated with the spy (can be null)
     * @param beanName the name of the bean associated with the spy
     */
    private void registerSpy(SpyDefinition definition, Field field, String beanName) {
		this.spies.put(beanName, definition);
		this.beanNameRegistry.put(definition, beanName);
		if (field != null) {
			this.fieldRegistry.put(field, beanName);
		}
	}

	/**
     * Creates a spy object if necessary for the given bean.
     * 
     * @param bean the bean object
     * @param beanName the name of the bean
     * @return the spy object if created, otherwise the original bean object
     * @throws BeansException if an error occurs during spy creation
     */
    protected final Object createSpyIfNecessary(Object bean, String beanName) throws BeansException {
		SpyDefinition definition = this.spies.get(beanName);
		if (definition != null) {
			bean = definition.createSpy(beanName, bean);
		}
		return bean;
	}

	/**
     * Post-processes the properties of a bean.
     * 
     * This method is called after the properties of a bean have been set, allowing for further processing or modification of the properties.
     * 
     * @param pvs the PropertyValues object containing the properties of the bean
     * @param bean the bean object being processed
     * @param beanName the name of the bean being processed
     * @return the modified PropertyValues object
     * @throws BeansException if any error occurs during the post-processing of the properties
     */
    @Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), (field) -> postProcessField(bean, field));
		return pvs;
	}

	/**
     * Post-processes a field of an object by injecting a bean if the field is annotated with a bean name.
     * 
     * @param bean the object to post-process
     * @param field the field to be post-processed
     */
    private void postProcessField(Object bean, Field field) {
		String beanName = this.fieldRegistry.get(field);
		if (StringUtils.hasText(beanName)) {
			inject(field, bean, beanName);
		}
	}

	/**
     * Injects a bean into a field of a target object based on a given definition.
     * 
     * @param field the field to inject the bean into
     * @param target the target object to inject the bean into
     * @param definition the definition of the bean to inject
     * @throws IllegalStateException if no bean is found for the given definition
     */
    void inject(Field field, Object target, Definition definition) {
		String beanName = this.beanNameRegistry.get(definition);
		Assert.state(StringUtils.hasLength(beanName), () -> "No bean found for definition " + definition);
		inject(field, target, beanName);
	}

	/**
     * Injects a bean into a field of an object using reflection.
     * 
     * @param field     the field to inject the bean into
     * @param target    the object containing the field
     * @param beanName  the name of the bean to inject
     * @throws BeanCreationException if an error occurs during the injection process
     */
    private void inject(Field field, Object target, String beanName) {
		try {
			field.setAccessible(true);
			Object existingValue = ReflectionUtils.getField(field, target);
			Object bean = this.beanFactory.getBean(beanName, field.getType());
			if (existingValue == bean) {
				return;
			}
			Assert.state(existingValue == null, () -> "The existing value '" + existingValue + "' of field '" + field
					+ "' is not the same as the new value '" + bean + "'");
			ReflectionUtils.setField(field, target, bean);
		}
		catch (Throwable ex) {
			throw new BeanCreationException("Could not inject field: " + field, ex);
		}
	}

	/**
     * Returns the order of the method.
     * 
     * @return the order of the method
     */
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

	/**
     * Retrieves or adds a bean definition for the specified post processor class in the given bean definition registry.
     * If the bean definition registry does not contain the bean definition, a new one is created and registered.
     * The bean definition is then returned.
     *
     * @param registry the bean definition registry to retrieve or add the bean definition to
     * @param postProcessor the post processor class to create the bean definition for
     * @return the retrieved or added bean definition
     */
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

		private final Map<String, Object> earlySpyReferences = new ConcurrentHashMap<>(16);

		private final MockitoPostProcessor mockitoPostProcessor;

		/**
         * Constructs a new SpyPostProcessor with the specified MockitoPostProcessor.
         * 
         * @param mockitoPostProcessor the MockitoPostProcessor to be used by the SpyPostProcessor
         */
        SpyPostProcessor(MockitoPostProcessor mockitoPostProcessor) {
			this.mockitoPostProcessor = mockitoPostProcessor;
		}

		/**
         * Returns the order of this SpyPostProcessor.
         * The order is set to the highest precedence.
         *
         * @return the order of this SpyPostProcessor
         */
        @Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		/**
         * Retrieves an early bean reference for the given bean and bean name.
         * If the bean is an instance of FactoryBean, it is returned as is.
         * Otherwise, the bean is stored in the earlySpyReferences map using its cache key.
         * If necessary, a spy object is created using the MockitoPostProcessor.
         *
         * @param bean     the bean instance
         * @param beanName the name of the bean
         * @return the early bean reference
         * @throws BeansException if an error occurs while retrieving the early bean reference
         */
        @Override
		public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
			if (bean instanceof FactoryBean) {
				return bean;
			}
			this.earlySpyReferences.put(getCacheKey(bean, beanName), bean);
			return this.mockitoPostProcessor.createSpyIfNecessary(bean, beanName);
		}

		/**
         * This method is called after the initialization of a bean. It checks if the bean is an instance of FactoryBean.
         * If it is, the method returns the bean as is. Otherwise, it checks if the bean is a spy created by Mockito.
         * If it is, the method returns the bean as is. If the bean is not a spy, it creates a spy using the MockitoPostProcessor
         * and returns the spy. The method also removes the bean from the earlySpyReferences map if it exists in the map.
         * 
         * @param bean The bean object being processed.
         * @param beanName The name of the bean being processed.
         * @return The processed bean object.
         * @throws BeansException If an error occurs during the bean processing.
         */
        @Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof FactoryBean) {
				return bean;
			}
			if (this.earlySpyReferences.remove(getCacheKey(bean, beanName)) != bean) {
				return this.mockitoPostProcessor.createSpyIfNecessary(bean, beanName);
			}
			return bean;
		}

		/**
         * Returns the cache key for the given bean.
         * If the bean name is not empty, the bean name is returned as the cache key.
         * Otherwise, the fully qualified class name of the bean is returned as the cache key.
         *
         * @param bean     the bean object
         * @param beanName the name of the bean
         * @return the cache key for the given bean
         */
        private String getCacheKey(Object bean, String beanName) {
			return StringUtils.hasLength(beanName) ? beanName : bean.getClass().getName();
		}

		/**
         * Registers the SpyPostProcessor bean definition in the given BeanDefinitionRegistry if it does not already exist.
         * The SpyPostProcessor bean definition is created with the role set to BeanDefinition.ROLE_INFRASTRUCTURE.
         * It also adds a constructor argument value with index 0, which is a reference to the MockitoPostProcessor bean.
         * 
         * @param registry the BeanDefinitionRegistry to register the SpyPostProcessor bean definition in
         */
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
