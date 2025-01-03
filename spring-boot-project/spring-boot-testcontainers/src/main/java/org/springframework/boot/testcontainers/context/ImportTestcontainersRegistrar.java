/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
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
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link ImportTestcontainers @ImportTestcontainers}.
 *
 * @author Phillip Webb
 * @see ContainerFieldsImporter
 * @see DynamicPropertySourceMethodsImporter
 */
class ImportTestcontainersRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String DYNAMIC_PROPERTY_SOURCE_CLASS = "org.springframework.test.context.DynamicPropertySource";

	private final ContainerFieldsImporter containerFieldsImporter;

	private final DynamicPropertySourceMethodsImporter dynamicPropertySourceMethodsImporter;

	ImportTestcontainersRegistrar(Environment environment) {
		this.containerFieldsImporter = new ContainerFieldsImporter();
		this.dynamicPropertySourceMethodsImporter = (!ClassUtils.isPresent(DYNAMIC_PROPERTY_SOURCE_CLASS, null)) ? null
				: new DynamicPropertySourceMethodsImporter(environment);
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		MergedAnnotation<ImportTestcontainers> annotation = importingClassMetadata.getAnnotations()
			.get(ImportTestcontainers.class);
		Class<?>[] definitionClasses = annotation.getClassArray(MergedAnnotation.VALUE);
		Class<?> importingClass = ClassUtils.resolveClassName(importingClassMetadata.getClassName(), null);
		if (ObjectUtils.isEmpty(definitionClasses)) {
			definitionClasses = new Class<?>[] { importingClass };
		}
		registerMetadataBeanDefinition(registry, importingClass, definitionClasses);
		registerBeanDefinitions(registry, definitionClasses);
	}

	void registerBeanDefinitions(BeanDefinitionRegistry registry, Class<?>[] definitionClasses) {
		for (Class<?> definitionClass : definitionClasses) {
			this.containerFieldsImporter.registerBeanDefinitions(registry, definitionClass);
			if (this.dynamicPropertySourceMethodsImporter != null) {
				this.dynamicPropertySourceMethodsImporter.registerDynamicPropertySources(registry, definitionClass);
			}
		}
	}

	private void registerMetadataBeanDefinition(BeanDefinitionRegistry registry, Class<?> importingClass,
			Class<?>[] definitionClasses) {
		String beanName = "%s.%s.metadata".formatted(ImportTestcontainersMetadata.class, importingClass.getName());
		if (!registry.containsBeanDefinition(beanName)) {
			RootBeanDefinition bd = new RootBeanDefinition(ImportTestcontainersMetadata.class);
			bd.setInstanceSupplier(() -> new ImportTestcontainersMetadata(importingClass, definitionClasses));
			bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			bd.setAutowireCandidate(false);
			bd.setAttribute(ImportTestcontainersMetadata.class.getName(), true);
			registry.registerBeanDefinition(beanName, bd);
		}
	}

	private record ImportTestcontainersMetadata(Class<?> importingClass, Class<?>[] definitionClasses) {
	}

	static class ImportTestcontainersBeanRegistrationExcludeFilter implements BeanRegistrationExcludeFilter {

		@Override
		public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
			RootBeanDefinition bd = registeredBean.getMergedBeanDefinition();
			return bd.hasAttribute(TestcontainerFieldBeanDefinition.class.getName())
					|| bd.hasAttribute(ImportTestcontainersMetadata.class.getName());
		}

	}

	static class ImportTestcontainersBeanFactoryInitializationAotProcessor
			implements BeanFactoryInitializationAotProcessor {

		@Override
		public BeanFactoryInitializationAotContribution processAheadOfTime(
				ConfigurableListableBeanFactory beanFactory) {
			Map<String, ImportTestcontainersMetadata> metadata = beanFactory
				.getBeansOfType(ImportTestcontainersMetadata.class, false, false);
			if (metadata.isEmpty()) {
				return null;
			}
			return new AotContribution(new LinkedHashSet<>(metadata.values()));
		}

		private static final class AotContribution implements BeanFactoryInitializationAotContribution {

			private static final String BEAN_FACTORY_PARAM = "beanFactory";

			private static final String ENVIRONMENT_PARAM = "environment";

			private final Set<ImportTestcontainersMetadata> metadata;

			private AotContribution(Set<ImportTestcontainersMetadata> metadata) {
				this.metadata = metadata;
			}

			@Override
			public void applyTo(GenerationContext generationContext,
					BeanFactoryInitializationCode beanFactoryInitializationCode) {

				Set<Class<?>> definitionClasses = getDefinitionClasses();
				contributeHints(generationContext.getRuntimeHints(), definitionClasses);

				GeneratedClass generatedClass = generationContext.getGeneratedClasses()
					.addForFeatureComponent(ImportTestcontainers.class.getSimpleName(),
							ImportTestcontainersRegistrar.class, (code) -> code.addModifiers(Modifier.PUBLIC));

				GeneratedMethod initializeMethod = generatedClass.getMethods()
					.add("registerBeanDefinitions", (code) -> {
						code.addJavadoc("Register bean definitions for '$T'", ImportTestcontainers.class);
						code.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
						code.addParameter(ConfigurableEnvironment.class, ENVIRONMENT_PARAM);
						code.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_PARAM);
						code.addStatement("$T<$T<?>> definitionClasses = new $T<>()", Set.class, Class.class,
								LinkedHashSet.class);
						code.addStatement("$T classLoader = $L.getBeanClassLoader()", ClassLoader.class,
								BEAN_FACTORY_PARAM);
						definitionClasses.forEach((definitionClass) -> code.addStatement(
								"definitionClasses.add($T.resolveClassName($S, classLoader))", ClassUtils.class,
								definitionClass.getTypeName()));
						code.addStatement(
								"new $T($L).registerBeanDefinitions($L, definitionClasses.toArray(new $T<?>[0]))",
								ImportTestcontainersRegistrar.class, ENVIRONMENT_PARAM, BEAN_FACTORY_PARAM,
								Class.class);
					});
				beanFactoryInitializationCode.addInitializer(initializeMethod.toMethodReference());
			}

			private Set<Class<?>> getDefinitionClasses() {
				return this.metadata.stream()
					.map(ImportTestcontainersMetadata::definitionClasses)
					.flatMap(Stream::of)
					.collect(Collectors.toCollection(LinkedHashSet::new));
			}

			private void contributeHints(RuntimeHints runtimeHints, Set<Class<?>> definitionClasses) {
				definitionClasses.forEach((definitionClass) -> runtimeHints.reflection()
					.registerType(definitionClass, MemberCategory.DECLARED_FIELDS, MemberCategory.PUBLIC_FIELDS,
							MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS));
			}

		}

	}

}
