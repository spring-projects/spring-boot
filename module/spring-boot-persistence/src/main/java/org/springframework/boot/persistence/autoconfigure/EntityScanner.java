/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.persistence.autoconfigure;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * An entity scanner that searches the classpath from an {@link EntityScan @EntityScan}
 * specified packages.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class EntityScanner {

	private final ApplicationContext context;

	/**
	 * Create a new {@link EntityScanner} instance.
	 * @param context the source application context
	 */
	public EntityScanner(ApplicationContext context) {
		Assert.notNull(context, "'context' must not be null");
		this.context = context;
	}

	/**
	 * Scan for entities with the specified annotations.
	 * @param annotationTypes the annotation types used on the entities
	 * @return a set of entity classes
	 * @throws ClassNotFoundException if an entity class cannot be loaded
	 */
	@SafeVarargs
	public final Set<Class<?>> scan(Class<? extends Annotation>... annotationTypes) throws ClassNotFoundException {
		List<String> packages = getPackages();
		if (packages.isEmpty()) {
			return Collections.emptySet();
		}
		ClassPathScanningCandidateComponentProvider scanner = createClassPathScanningCandidateComponentProvider(
				this.context);
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
		}
		Set<Class<?>> entitySet = new HashSet<>();
		for (String basePackage : packages) {
			if (StringUtils.hasText(basePackage)) {
				for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
					String beanClassName = candidate.getBeanClassName();
					Assert.state(beanClassName != null, "'beanClassName' must not be null");
					entitySet.add(ClassUtils.forName(beanClassName, this.context.getClassLoader()));
				}
			}
		}
		return entitySet;
	}

	/**
	 * Create a {@link ClassPathScanningCandidateComponentProvider} to scan entities based
	 * on the specified {@link ApplicationContext}.
	 * @param context the {@link ApplicationContext} to use
	 * @return a {@link ClassPathScanningCandidateComponentProvider} suitable to scan
	 * entities
	 */
	protected ClassPathScanningCandidateComponentProvider createClassPathScanningCandidateComponentProvider(
			ApplicationContext context) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.setEnvironment(context.getEnvironment());
		scanner.setResourceLoader(context);
		return scanner;
	}

	private List<String> getPackages() {
		List<String> packages = EntityScanPackages.get(this.context).getPackageNames();
		if (packages.isEmpty() && AutoConfigurationPackages.has(this.context)) {
			packages = AutoConfigurationPackages.get(this.context);
		}
		return packages;
	}

}
