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

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Spring Bean and Jackson {@link Module} to find and
 * {@link SimpleModule#setMixInAnnotation(Class, Class) register}
 * {@link JsonMixin @JsonMixin}-annotated classes.
 *
 * @author Guirong Hu
 * @since 2.7.0
 * @see JsonMixin
 */
public class JsonMixinModule extends SimpleModule implements InitializingBean {

	private final ApplicationContext context;

	private final Collection<String> basePackages;

	/**
	 * Create a new {@link JsonMixinModule} instance.
	 * @param context the source application context
	 * @param basePackages the packages to check for annotated classes
	 */
	public JsonMixinModule(ApplicationContext context, Collection<String> basePackages) {
		Assert.notNull(context, "Context must not be null");
		this.context = context;
		this.basePackages = basePackages;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (ObjectUtils.isEmpty(this.basePackages)) {
			return;
		}
		JsonMixinComponentScanner scanner = new JsonMixinComponentScanner();
		scanner.setEnvironment(this.context.getEnvironment());
		scanner.setResourceLoader(this.context);
		for (String basePackage : this.basePackages) {
			if (StringUtils.hasText(basePackage)) {
				for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
					addJsonMixin(ClassUtils.forName(candidate.getBeanClassName(), this.context.getClassLoader()));
				}
			}
		}
	}

	private void addJsonMixin(Class<?> mixinClass) {
		MergedAnnotation<JsonMixin> annotation = MergedAnnotations
				.from(mixinClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(JsonMixin.class);
		for (Class<?> targetType : annotation.getClassArray("type")) {
			setMixInAnnotation(targetType, mixinClass);
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
