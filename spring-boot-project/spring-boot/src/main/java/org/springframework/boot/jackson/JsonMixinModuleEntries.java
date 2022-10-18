/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.jackson;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Provide the mapping of json mixin class to consider.
 *
 * @author Stephane Nicoll
 * @since 3.0.0
 */
public final class JsonMixinModuleEntries {

	private final Map<Object, Object> entries;

	private JsonMixinModuleEntries(Builder builder) {
		this.entries = new LinkedHashMap<>(builder.entries);
	}

	/**
	 * Create an instance using the specified {@link Builder}.
	 * @param mixins a consumer of the builder
	 * @return an instance with the state of the customized builder.
	 */
	public static JsonMixinModuleEntries create(Consumer<Builder> mixins) {
		Builder builder = new Builder();
		mixins.accept(builder);
		return builder.build();
	}

	/**
	 * Scan the classpath for {@link JsonMixin @JsonMixin} in the specified
	 * {@code basePackages}.
	 * @param context the application context to use
	 * @param basePackages the base packages to consider
	 * @return an instance with the result of the scanning
	 */
	public static JsonMixinModuleEntries scan(ApplicationContext context, Collection<String> basePackages) {
		return JsonMixinModuleEntries.create((builder) -> {
			if (ObjectUtils.isEmpty(basePackages)) {
				return;
			}
			JsonMixinComponentScanner scanner = new JsonMixinComponentScanner();
			scanner.setEnvironment(context.getEnvironment());
			scanner.setResourceLoader(context);
			for (String basePackage : basePackages) {
				if (StringUtils.hasText(basePackage)) {
					for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
						Class<?> mixinClass = ClassUtils.resolveClassName(candidate.getBeanClassName(),
								context.getClassLoader());
						registerMixinClass(builder, mixinClass);
					}
				}
			}
		});
	}

	private static void registerMixinClass(Builder builder, Class<?> mixinClass) {
		MergedAnnotation<JsonMixin> annotation = MergedAnnotations
				.from(mixinClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(JsonMixin.class);
		for (Class<?> targetType : annotation.getClassArray("type")) {
			builder.and(targetType, mixinClass);
		}

	}

	/**
	 * Perform an action on each entry defined by this instance. If a class needs to be
	 * resolved from its class name, the specified {@link ClassLoader} is used.
	 * @param classLoader the classloader to use to resolve class name if necessary
	 * @param action the action to invoke on each type to mixin class entry
	 */
	public void doWithEntry(ClassLoader classLoader, BiConsumer<Class<?>, Class<?>> action) {
		this.entries.forEach((type, mixin) -> action.accept(resolveClassNameIfNecessary(type, classLoader),
				resolveClassNameIfNecessary(mixin, classLoader)));
	}

	private Class<?> resolveClassNameIfNecessary(Object type, ClassLoader classLoader) {
		return (type instanceof Class<?> clazz) ? clazz : ClassUtils.resolveClassName((String) type, classLoader);
	}

	/**
	 * Builder for {@link JsonMixinModuleEntries}.
	 */
	public static class Builder {

		private final Map<Object, Object> entries;

		Builder() {
			this.entries = new LinkedHashMap<>();
		}

		/**
		 * Add a mapping for the specified class names.
		 * @param typeClassName the type class name
		 * @param mixinClassName the mixin class name
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder and(String typeClassName, String mixinClassName) {
			this.entries.put(typeClassName, mixinClassName);
			return this;
		}

		/**
		 * Add a mapping for the specified classes.
		 * @param type the type class
		 * @param mixinClass the mixin class
		 * @return {@code this}, to facilitate method chaining
		 */
		public Builder and(Class<?> type, Class<?> mixinClass) {
			this.entries.put(type, mixinClass);
			return this;
		}

		JsonMixinModuleEntries build() {
			return new JsonMixinModuleEntries(this);
		}

	}

	static class JsonMixinComponentScanner extends ClassPathScanningCandidateComponentProvider {

		JsonMixinComponentScanner() {
			addIncludeFilter(new AnnotationTypeFilter(JsonMixin.class));
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			return true;
		}

	}

}
