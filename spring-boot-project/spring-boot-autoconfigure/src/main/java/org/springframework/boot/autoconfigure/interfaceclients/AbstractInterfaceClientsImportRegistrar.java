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

package org.springframework.boot.autoconfigure.interfaceclients;

import java.lang.annotation.Annotation;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.CaseUtils;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Registers bean definitions for annotated Interface Clients in order to automatically
 * instantiate client beans based on those interfaces.
 *
 * @author Josh Long
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
// TODO: Handle AOT
public abstract class AbstractInterfaceClientsImportRegistrar
		implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

	private static final String INTERFACE_CLIENT_SUFFIX = "InterfaceClient";

	private static final String BEAN_NAME_ATTRIBUTE_NAME = "beanName";

	private static final Log logger = LogFactory.getLog(AbstractInterfaceClientsImportRegistrar.class);

	private Environment environment;

	private ResourceLoader resourceLoader;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {
		Assert.isInstanceOf(ListableBeanFactory.class, registry,
				"Registry must be an instance of " + ListableBeanFactory.class.getSimpleName());
		ListableBeanFactory beanFactory = (ListableBeanFactory) registry;
		Set<BeanDefinition> candidateComponents = discoverCandidateComponents(beanFactory);
		for (BeanDefinition candidateComponent : candidateComponents) {
			if (candidateComponent instanceof AnnotatedBeanDefinition beanDefinition) {
				registerInterfaceClient(registry, beanFactory, beanDefinition);
			}
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	protected abstract Class<? extends Annotation> getAnnotation();

	protected Set<BeanDefinition> discoverCandidateComponents(ListableBeanFactory beanFactory) {
		Set<BeanDefinition> candidateComponents = new HashSet<>();
		ClassPathScanningCandidateComponentProvider scanner = getScanner();
		scanner.setResourceLoader(this.resourceLoader);
		scanner.addIncludeFilter(new AnnotationTypeFilter(getAnnotation()));
		List<String> basePackages = AutoConfigurationPackages.get(beanFactory);
		for (String basePackage : basePackages) {
			candidateComponents.addAll(scanner.findCandidateComponents(basePackage));
		}
		return candidateComponents;
	}

	private ClassPathScanningCandidateComponentProvider getScanner() {
		return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				boolean isCandidate = false;
				if (beanDefinition.getMetadata().isIndependent()) {
					if (!beanDefinition.getMetadata().isAnnotation()) {
						isCandidate = true;
					}
				}
				return isCandidate;
			}
		};
	}

	private void registerInterfaceClient(BeanDefinitionRegistry registry, ListableBeanFactory beanFactory,
			AnnotatedBeanDefinition beanDefinition) {
		AnnotationMetadata annotatedBeanMetadata = beanDefinition.getMetadata();
		Assert.isTrue(annotatedBeanMetadata.isInterface(),
				getAnnotation().getSimpleName() + "can only be placed on an interface.");
		MergedAnnotation<? extends Annotation> annotation = annotatedBeanMetadata.getAnnotations().get(getAnnotation());
		String beanClassName = annotatedBeanMetadata.getClassName();
		// The value of the annotation is the qualifier to look for related beans
		// while the default beanName corresponds to the simple class name suffixed with
		// `InterfaceClient`
		String clientId = annotation.getString(MergedAnnotation.VALUE);
		String beanName = !ObjectUtils.isEmpty(annotation.getString(BEAN_NAME_ATTRIBUTE_NAME))
				? annotation.getString(BEAN_NAME_ATTRIBUTE_NAME) : buildBeanName(clientId);
		InterfaceClientsAdapter adapter = beanFactory.getBean(InterfaceClientsAdapter.class);
		Class<?> beanClass = toClass(beanClassName);
		BeanDefinition definition = BeanDefinitionBuilder
			.rootBeanDefinition(ResolvableType.forClass(beanClass),
					() -> adapter.createClient(beanFactory, clientId, beanClass))
			.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
			.getBeanDefinition();
		BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, beanName, new String[] { clientId });
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	private String buildBeanName(String clientId) {
		String normalised = Normalizer.normalize(clientId, Normalizer.Form.NFD);
		String camelCased = CaseUtils.toCamelCase(normalised, false, '-', '_');
		return camelCased + INTERFACE_CLIENT_SUFFIX;
	}

	private static Class<?> toClass(String beanClassName) {
		Class<?> beanClass;
		try {
			beanClass = Class.forName(beanClassName);
		}
		catch (ClassNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Class not found for interface client " + beanClassName + ": " + ex.getMessage());
			}
			throw new RuntimeException(ex);
		}
		return beanClass;
	}

}
