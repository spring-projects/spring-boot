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

package org.springframework.boot;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import groovy.lang.Closure;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Loads bean definitions from underlying sources, including XML and JavaConfig. Acts as a
 * simple facade over {@link AnnotatedBeanDefinitionReader},
 * {@link XmlBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}. See
 * {@link SpringApplication} for the types of sources that are supported.
 *
 * @author Phillip Webb
 * @author Vladislav Kisel
 * @author Sebastien Deleuze
 * @see #setBeanNameGenerator(BeanNameGenerator)
 */
class BeanDefinitionLoader {

	// Static final field to facilitate code removal by Graal
	private static final boolean XML_ENABLED = !SpringProperties.getFlag("spring.xml.ignore");

	private final Object[] sources;

	private final AnnotatedBeanDefinitionReader annotatedReader;

	private final AbstractBeanDefinitionReader xmlReader;

	private final BeanDefinitionReader groovyReader;

	private final ClassPathBeanDefinitionScanner scanner;

	private ResourceLoader resourceLoader;

	/**
	 * Create a new {@link BeanDefinitionLoader} that will load beans into the specified
	 * {@link BeanDefinitionRegistry}.
	 * @param registry the bean definition registry that will contain the loaded beans
	 * @param sources the bean sources
	 */
	BeanDefinitionLoader(BeanDefinitionRegistry registry, Object... sources) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notEmpty(sources, "Sources must not be empty");
		this.sources = sources;
		this.annotatedReader = new AnnotatedBeanDefinitionReader(registry);
		this.xmlReader = (XML_ENABLED ? new XmlBeanDefinitionReader(registry) : null);
		this.groovyReader = (isGroovyPresent() ? new GroovyBeanDefinitionReader(registry) : null);
		this.scanner = new ClassPathBeanDefinitionScanner(registry);
		this.scanner.addExcludeFilter(new ClassExcludeFilter(sources));
	}

	/**
	 * Set the bean name generator to be used by the underlying readers and scanner.
	 * @param beanNameGenerator the bean name generator
	 */
	void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.annotatedReader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
		if (this.xmlReader != null) {
			this.xmlReader.setBeanNameGenerator(beanNameGenerator);
		}
	}

	/**
	 * Set the resource loader to be used by the underlying readers and scanner.
	 * @param resourceLoader the resource loader
	 */
	void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
		this.scanner.setResourceLoader(resourceLoader);
		if (this.xmlReader != null) {
			this.xmlReader.setResourceLoader(resourceLoader);
		}
	}

	/**
	 * Set the environment to be used by the underlying readers and scanner.
	 * @param environment the environment
	 */
	void setEnvironment(ConfigurableEnvironment environment) {
		this.annotatedReader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
		if (this.xmlReader != null) {
			this.xmlReader.setEnvironment(environment);
		}
	}

	/**
	 * Load the sources into the reader.
	 */
	void load() {
		for (Object source : this.sources) {
			load(source);
		}
	}

	private void load(Object source) {
		Assert.notNull(source, "Source must not be null");
		if (source instanceof Class<?>) {
			load((Class<?>) source);
			return;
		}
		if (source instanceof Resource) {
			load((Resource) source);
			return;
		}
		if (source instanceof Package) {
			load((Package) source);
			return;
		}
		if (source instanceof CharSequence) {
			load((CharSequence) source);
			return;
		}
		throw new IllegalArgumentException("Invalid source type " + source.getClass());
	}

	private void load(Class<?> source) {
		if (isGroovyPresent() && GroovyBeanDefinitionSource.class.isAssignableFrom(source)) {
			// Any GroovyLoaders added in beans{} DSL can contribute beans here
			GroovyBeanDefinitionSource loader = BeanUtils.instantiateClass(source, GroovyBeanDefinitionSource.class);
			((GroovyBeanDefinitionReader) this.groovyReader).beans(loader.getBeans());
		}
		if (isEligible(source)) {
			this.annotatedReader.register(source);
		}
	}

	private void load(Resource source) {
		if (source.getFilename().endsWith(".groovy")) {
			if (this.groovyReader == null) {
				throw new BeanDefinitionStoreException("Cannot load Groovy beans without Groovy on classpath");
			}
			this.groovyReader.loadBeanDefinitions(source);
		}
		else {
			if (this.xmlReader == null) {
				throw new BeanDefinitionStoreException("Cannot load XML bean definitions when XML support is disabled");
			}
			this.xmlReader.loadBeanDefinitions(source);
		}
	}

	private void load(Package source) {
		this.scanner.scan(source.getName());
	}

	private void load(CharSequence source) {
		String resolvedSource = this.scanner.getEnvironment().resolvePlaceholders(source.toString());
		// Attempt as a Class
		try {
			load(ClassUtils.forName(resolvedSource, null));
			return;
		}
		catch (IllegalArgumentException | ClassNotFoundException ex) {
			// swallow exception and continue
		}
		// Attempt as Resources
		if (loadAsResources(resolvedSource)) {
			return;
		}
		// Attempt as package
		Package packageResource = findPackage(resolvedSource);
		if (packageResource != null) {
			load(packageResource);
			return;
		}
		throw new IllegalArgumentException("Invalid source '" + resolvedSource + "'");
	}

	private boolean loadAsResources(String resolvedSource) {
		boolean foundCandidate = false;
		Resource[] resources = findResources(resolvedSource);
		for (Resource resource : resources) {
			if (isLoadCandidate(resource)) {
				foundCandidate = true;
				load(resource);
			}
		}
		return foundCandidate;
	}

	private boolean isGroovyPresent() {
		return ClassUtils.isPresent("groovy.lang.MetaClass", null);
	}

	private Resource[] findResources(String source) {
		ResourceLoader loader = (this.resourceLoader != null) ? this.resourceLoader
				: new PathMatchingResourcePatternResolver();
		try {
			if (loader instanceof ResourcePatternResolver) {
				return ((ResourcePatternResolver) loader).getResources(source);
			}
			return new Resource[] { loader.getResource(source) };
		}
		catch (IOException ex) {
			throw new IllegalStateException("Error reading source '" + source + "'");
		}
	}

	private boolean isLoadCandidate(Resource resource) {
		if (resource == null || !resource.exists()) {
			return false;
		}
		if (resource instanceof ClassPathResource) {
			// A simple package without a '.' may accidentally get loaded as an XML
			// document if we're not careful. The result of getInputStream() will be
			// a file list of the package content. We double check here that it's not
			// actually a package.
			String path = ((ClassPathResource) resource).getPath();
			if (path.indexOf('.') == -1) {
				try {
					return Package.getPackage(path) == null;
				}
				catch (Exception ex) {
					// Ignore
				}
			}
		}
		return true;
	}

	private Package findPackage(CharSequence source) {
		Package pkg = Package.getPackage(source.toString());
		if (pkg != null) {
			return pkg;
		}
		try {
			// Attempt to find a class in this package
			ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
			Resource[] resources = resolver
					.getResources(ClassUtils.convertClassNameToResourcePath(source.toString()) + "/*.class");
			for (Resource resource : resources) {
				String className = StringUtils.stripFilenameExtension(resource.getFilename());
				load(Class.forName(source.toString() + "." + className));
				break;
			}
		}
		catch (Exception ex) {
			// swallow exception and continue
		}
		return Package.getPackage(source.toString());
	}

	/**
	 * Check whether the bean is eligible for registration.
	 * @param type candidate bean type
	 * @return true if the given bean type is eligible for registration, i.e. not a groovy
	 * closure nor an anonymous class
	 */
	private boolean isEligible(Class<?> type) {
		return !(type.isAnonymousClass() || isGroovyClosure(type) || hasNoConstructors(type));
	}

	private boolean isGroovyClosure(Class<?> type) {
		return type.getName().matches(".*\\$_.*closure.*");
	}

	private boolean hasNoConstructors(Class<?> type) {
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		return ObjectUtils.isEmpty(constructors);
	}

	/**
	 * Simple {@link TypeFilter} used to ensure that specified {@link Class} sources are
	 * not accidentally re-added during scanning.
	 */
	private static class ClassExcludeFilter extends AbstractTypeHierarchyTraversingFilter {

		private final Set<String> classNames = new HashSet<>();

		ClassExcludeFilter(Object... sources) {
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

	/**
	 * Source for Bean definitions defined in Groovy.
	 */
	@FunctionalInterface
	protected interface GroovyBeanDefinitionSource {

		Closure<?> getBeans();

	}

}
