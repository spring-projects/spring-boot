/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Loads bean definitions from underlying sources, including XML and JavaConfig. Acts as a
 * simple facade over {@link AnnotatedBeanDefinitionReader},
 * {@link XmlBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}. See
 * {@link SpringApplication} for the types of sources that are supported.
 * 
 * @author Phillip Webb
 * @see #setBeanNameGenerator(BeanNameGenerator)
 */
class BeanDefinitionLoader {

	private static final ResourceLoader DEFAULT_RESOURCE_LOADER = new DefaultResourceLoader();

	private Object[] sources;

	private AnnotatedBeanDefinitionReader annotatedReader;

	private XmlBeanDefinitionReader xmlReader;

	private ClassPathBeanDefinitionScanner scanner;

	private ResourceLoader resourceLoader;

	/**
	 * Create a new {@link BeanDefinitionLoader} that will load beans into the specified
	 * {@link BeanDefinitionRegistry}.
	 * @param registry the bean definition registry that will contain the loaded beans
	 * @param sources the bean sources
	 */
	public BeanDefinitionLoader(BeanDefinitionRegistry registry, Object... sources) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notEmpty(sources, "Sources must not be empty");
		this.sources = sources;
		this.annotatedReader = new AnnotatedBeanDefinitionReader(registry);
		this.xmlReader = new XmlBeanDefinitionReader(registry);
		this.scanner = new ClassPathBeanDefinitionScanner(registry);
		this.scanner.addExcludeFilter(new ClassExcludeFilter(sources));
	}

	/**
	 * Set the bean name generator to be used by the underlying readers and scanner.
	 * @param beanNameGenerator the bean name generator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.annotatedReader.setBeanNameGenerator(beanNameGenerator);
		this.xmlReader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
	}

	/**
	 * Set the resource loader to be used by the underlying readers and scanner.
	 * @param resourceLoader the resource loader
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
		this.xmlReader.setResourceLoader(resourceLoader);
		this.scanner.setResourceLoader(resourceLoader);
	}

	/**
	 * Set the environment to be used by the underlying readers and scanner.
	 * @param environment
	 */
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.annotatedReader.setEnvironment(environment);
		this.xmlReader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	/**
	 * Load the sources into the reader.
	 * @return the number of loaded beans
	 */
	public int load() {
		int count = 0;
		for (Object source : this.sources) {
			count += load(source);
		}
		return count;
	}

	private int load(Object source) {
		Assert.notNull(source, "Source must not be null");
		if (source instanceof Class<?>) {
			return load((Class<?>) source);
		}
		if (source instanceof Resource) {
			return load((Resource) source);
		}
		if (source instanceof Package) {
			return load((Package) source);
		}
		if (source instanceof CharSequence) {
			return load((CharSequence) source);
		}
		throw new IllegalArgumentException("Invalid source type " + source.getClass());
	}

	private int load(Class<?> source) {
		if (isComponent(source)) {
			this.annotatedReader.register(source);
			return 1;
		}
		return 0;
	}

	private int load(Resource source) {
		return this.xmlReader.loadBeanDefinitions(source);
	}

	private int load(Package source) {
		// FIXME register the scanned package for data to pick up
		return this.scanner.scan(source.getName());
	}

	private int load(CharSequence source) {
		try {
			return load(Class.forName(source.toString()));
		}
		catch (ClassNotFoundException ex) {
			// swallow exception and continue
		}

		Resource loadedResource = (this.resourceLoader != null ? this.resourceLoader
				: DEFAULT_RESOURCE_LOADER).getResource(source.toString());
		if (loadedResource != null && loadedResource.exists()) {
			return load(loadedResource);
		}
		Package packageResource = findPackage(source);
		if (packageResource != null) {
			return load(packageResource);
		}
		throw new IllegalArgumentException("Invalid source '" + source + "'");
	}

	private Package findPackage(CharSequence source) {
		Package pkg = Package.getPackage(source.toString());
		if (pkg != null) {
			return pkg;
		}
		try {
			// Attempt to find a class in this package
			ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
					getClass().getClassLoader());
			Resource[] resources = resolver.getResources(ClassUtils
					.convertClassNameToResourcePath(source.toString()) + "/*.class");
			for (Resource resource : resources) {
				String className = StringUtils.stripFilenameExtension(resource
						.getFilename());
				load(Class.forName(source.toString() + "." + className));
				break;
			}
		}
		catch (Exception ex) {
			// swallow exception and continue
		}
		return Package.getPackage(source.toString());
	}

	private boolean isComponent(Class<?> type) {
		// This has to be a bit of a guess. The only way to be sure that this type is
		// eligible is to make a bean definition out of it and try to instantiate it.
		if (AnnotationUtils.findAnnotation(type, Component.class) != null) {
			return true;
		}
		// Nested anonymous classes are not eligible for registration, nor are groovy
		// closures
		if (type.isAnonymousClass() || type.getName().matches(".*\\$_.*closure.*")
				|| type.getConstructors() == null || type.getConstructors().length == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Simple {@link TypeFilter} used to ensure that specified {@link Class} sources are
	 * not accidentally re-added during scanning.
	 */
	private static class ClassExcludeFilter extends AbstractTypeHierarchyTraversingFilter {

		private Set<String> classNames = new HashSet<String>();

		public ClassExcludeFilter(Object... sources) {
			super(false, false);
			for (Object source : sources) {
				if (source instanceof Class<?>) {
					this.classNames.add(((Class<?>) source).getName());
				}
			}
		}

		@Override
		protected boolean matchClassName(String className) {
			return this.classNames.contains(className);
		}
	}

}
