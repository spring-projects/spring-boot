/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testcontainers.context;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.properties.TestcontainersPropertySource;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import
 * {@link DynamicPropertySource @DynamicPropertySource} methods.
 *
 * @author Phillip Webb
 */
class DynamicPropertySourceMethodsImporter {

	private final Environment environment;

	DynamicPropertySourceMethodsImporter(Environment environment) {
		this.environment = environment;
	}

	void registerDynamicPropertySources(BeanDefinitionRegistry beanDefinitionRegistry, Class<?> definitionClass) {
		Set<Method> methods = MethodIntrospector.selectMethods(definitionClass, this::isAnnotated);
		if (methods.isEmpty()) {
			return;
		}
		DynamicPropertyRegistry dynamicPropertyRegistry = TestcontainersPropertySource.attach(this.environment,
				beanDefinitionRegistry);
		methods.forEach((method) -> {
			assertValid(method);
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, null, dynamicPropertyRegistry);
		});
		String beanName = "%s.%s".formatted(DynamicPropertySourceMetadata.class.getName(), definitionClass);
		if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {
			RootBeanDefinition bd = new RootBeanDefinition(DynamicPropertySourceMetadata.class);
			bd.setInstanceSupplier(() -> new DynamicPropertySourceMetadata(definitionClass, methods));
			bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			bd.setAutowireCandidate(false);
			bd.setAttribute(DynamicPropertySourceMetadata.class.getName(), true);
			beanDefinitionRegistry.registerBeanDefinition(beanName, bd);
		}
	}

	private boolean isAnnotated(Method method) {
		return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
	}

	private void assertValid(Method method) {
		Assert.state(Modifier.isStatic(method.getModifiers()),
				() -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
		Class<?>[] types = method.getParameterTypes();
		Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
				() -> "@DynamicPropertySource method '" + method.getName()
						+ "' must accept a single DynamicPropertyRegistry argument");
	}

	private record DynamicPropertySourceMetadata(Class<?> definitionClass, Set<Method> methods) {
	}

	/**
	 * {@link BeanRegistrationExcludeFilter} to exclude
	 * {@link DynamicPropertySourceMetadata} from AOT bean registrations.
	 */
	static class DynamicPropertySourceMetadataBeanRegistrationExcludeFilter implements BeanRegistrationExcludeFilter {

		@Override
		public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
			return registeredBean.getMergedBeanDefinition().hasAttribute(DynamicPropertySourceMetadata.class.getName());
		}

	}

	/**
	 * The {@link BeanFactoryInitializationAotProcessor} generates methods for each
	 * {@code @DynamicPropertySource-annotated} method.
	 *
	 */
	static class DynamicPropertySourceBeanFactoryInitializationAotProcessor
			implements BeanFactoryInitializationAotProcessor {

		private static final String DYNAMIC_PROPERTY_REGISTRY = "dynamicPropertyRegistry";

		@Override
		public BeanFactoryInitializationAotContribution processAheadOfTime(
				ConfigurableListableBeanFactory beanFactory) {
			Map<String, DynamicPropertySourceMetadata> metadata = beanFactory
				.getBeansOfType(DynamicPropertySourceMetadata.class, false, false);
			if (metadata.isEmpty()) {
				return null;
			}
			return new AotContribution(metadata);
		}

		private static final class AotContribution implements BeanFactoryInitializationAotContribution {

			private final Map<String, DynamicPropertySourceMetadata> metadata;

			private AotContribution(Map<String, DynamicPropertySourceMetadata> metadata) {
				this.metadata = metadata;
			}

			@Override
			public void applyTo(GenerationContext generationContext,
					BeanFactoryInitializationCode beanFactoryInitializationCode) {
				GeneratedMethod initializerMethod = beanFactoryInitializationCode.getMethods()
					.add("registerDynamicPropertySources", (code) -> {
						code.addJavadoc("Registers {@code @DynamicPropertySource} properties");
						code.addParameter(ConfigurableEnvironment.class, "environment");
						code.addParameter(DefaultListableBeanFactory.class, "beanFactory");
						code.addModifiers(javax.lang.model.element.Modifier.PRIVATE,
								javax.lang.model.element.Modifier.STATIC);
						code.addStatement("$T dynamicPropertyRegistry = $T.attach(environment, beanFactory)",
								DynamicPropertyRegistry.class, TestcontainersPropertySource.class);
						this.metadata.forEach((name, metadata) -> {
							GeneratedMethod dynamicPropertySourceMethod = generateMethods(generationContext, metadata);
							code.addStatement(dynamicPropertySourceMethod.toMethodReference()
								.toInvokeCodeBlock(ArgumentCodeGenerator.of(DynamicPropertyRegistry.class,
										DYNAMIC_PROPERTY_REGISTRY)));
						});
					});
				beanFactoryInitializationCode.addInitializer(initializerMethod.toMethodReference());
			}

			// Generates a new class in definition class package and invokes
			// all @DynamicPropertySource methods.
			private GeneratedMethod generateMethods(GenerationContext generationContext,
					DynamicPropertySourceMetadata metadata) {
				Class<?> definitionClass = metadata.definitionClass();
				GeneratedClass generatedClass = generationContext.getGeneratedClasses()
					.addForFeatureComponent(DynamicPropertySource.class.getSimpleName(), definitionClass,
							(code) -> code.addModifiers(javax.lang.model.element.Modifier.PUBLIC));
				return generatedClass.getMethods().add("registerDynamicPropertySource", (code) -> {
					code.addJavadoc("Registers {@code @DynamicPropertySource} properties for class '$T'",
							definitionClass);
					code.addParameter(DynamicPropertyRegistry.class, DYNAMIC_PROPERTY_REGISTRY);
					code.addModifiers(javax.lang.model.element.Modifier.PUBLIC,
							javax.lang.model.element.Modifier.STATIC);
					metadata.methods().forEach((method) -> {
						GeneratedMethod generateMethod = generateMethod(generationContext, generatedClass, method);
						code.addStatement(generateMethod.toMethodReference()
							.toInvokeCodeBlock(ArgumentCodeGenerator.of(DynamicPropertyRegistry.class,
									DYNAMIC_PROPERTY_REGISTRY)));
					});
				});
			}

			// If the method is inaccessible, the reflection will be used; otherwise,
			// direct call to the method will be used.
			private static GeneratedMethod generateMethod(GenerationContext generationContext,
					GeneratedClass generatedClass, Method method) {
				return generatedClass.getMethods().add(method.getName(), (code) -> {
					code.addJavadoc("Register {@code @DynamicPropertySource} for method '$T.$L'",
							method.getDeclaringClass(), method.getName());
					code.addModifiers(javax.lang.model.element.Modifier.PRIVATE,
							javax.lang.model.element.Modifier.STATIC);
					code.addParameter(DynamicPropertyRegistry.class, DYNAMIC_PROPERTY_REGISTRY);
					if (isMethodAccessible(generatedClass, method)) {
						code.addStatement(CodeBlock.of("$T.$L($L)", method.getDeclaringClass(), method.getName(),
								DYNAMIC_PROPERTY_REGISTRY));
					}
					else {
						generationContext.getRuntimeHints().reflection().registerMethod(method, ExecutableMode.INVOKE);
						code.addStatement("$T<?> clazz = $T.resolveClassName($S, $T.class.getClassLoader())",
								Class.class, ClassUtils.class, method.getDeclaringClass().getTypeName(),
								generatedClass.getName());
						// ReflectionTestUtils can be used here because
						// @DynamicPropertyRegistry in a test module.
						code.addStatement("$T.invokeMethod(clazz, $S, $L)", ReflectionTestUtils.class, method.getName(),
								DYNAMIC_PROPERTY_REGISTRY);
					}
				});

			}

			private static boolean isMethodAccessible(GeneratedClass generatedClass, Method method) {
				ClassName className = generatedClass.getName();
				return AccessControl.forClass(method.getDeclaringClass()).isAccessibleFrom(className)
						&& AccessControl.forMember(method).isAccessibleFrom(className);
			}

		}

	}

}
