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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.style.ToStringCreator;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks for the presence or absence of specific beans.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
class OnBeanCondition extends SpringBootCondition implements ConfigurationCondition {

	private static final String[] NO_BEANS = {};

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	@Override
	public Outcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {

		if (metadata.isAnnotated(ConditionalOnBean.class.getName())) {
			BeanSearchSpec spec = new BeanSearchSpec(context, metadata,
					ConditionalOnBean.class);
			List<String> matching = getMatchingBeans(context, spec);
			if (matching.isEmpty()) {
				return Outcome.noMatch("@ConditionalOnBean " + spec + " found no beans");
			}
		}

		if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
			BeanSearchSpec spec = new BeanSearchSpec(context, metadata,
					ConditionalOnMissingBean.class);
			List<String> matching = getMatchingBeans(context, spec);
			if (!matching.isEmpty()) {
				return Outcome.noMatch("@ConditionalOnMissingBean " + spec
						+ " found the following " + matching);
			}
		}

		return Outcome.match();
	}

	private List<String> getMatchingBeans(ConditionContext context, BeanSearchSpec beans) {

		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beans.getStrategy() == SearchStrategy.PARENTS) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			Assert.isInstanceOf(ConfigurableListableBeanFactory.class, parent,
					"Unable to use SearchStrategy.PARENTS");
			beanFactory = (ConfigurableListableBeanFactory) parent;
		}

		List<String> beanNames = new ArrayList<String>();
		boolean considerHierarchy = beans.getStrategy() == SearchStrategy.ALL;

		for (String type : beans.getTypes()) {
			beanNames.addAll(Arrays.asList(getBeanNamesForType(beanFactory, type,
					context.getClassLoader(), considerHierarchy)));
		}

		for (String annotation : beans.getAnnotations()) {
			beanNames.addAll(Arrays.asList(getBeanNamesForAnnotation(beanFactory,
					annotation, context.getClassLoader(), considerHierarchy)));
		}

		for (String beanName : beans.getNames()) {
			if (containsBean(beanFactory, beanName, considerHierarchy)) {
				beanNames.add(beanName);
			}
		}

		return beanNames;
	}

	private boolean containsBean(ConfigurableListableBeanFactory beanFactory,
			String beanName, boolean considerHierarchy) {
		if (considerHierarchy) {
			return beanFactory.containsBean(beanName);
		}
		return beanFactory.containsLocalBean(beanName);
	}

	private String[] getBeanNamesForType(ConfigurableListableBeanFactory beanFactory,
			String type, ClassLoader classLoader, boolean considerHierarchy)
			throws LinkageError {
		// eagerInit set to false to prevent early instantiation (some
		// factory beans will not be able to determine their object type at this
		// stage, so those are not eligible for matching this condition)
		try {
			Class<?> typeClass = ClassUtils.forName(type, classLoader);
			if (considerHierarchy) {
				return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
						typeClass, false, false);
			}
			return beanFactory.getBeanNamesForType(typeClass, false, false);
		}
		catch (ClassNotFoundException ex) {
			return NO_BEANS;
		}
	}

	private String[] getBeanNamesForAnnotation(
			ConfigurableListableBeanFactory beanFactory, String type,
			ClassLoader classLoader, boolean considerHierarchy) throws LinkageError {
		String[] result = NO_BEANS;
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> typeClass = (Class<? extends Annotation>) ClassUtils
					.forName(type, classLoader);
			Map<String, Object> annotated = beanFactory.getBeansWithAnnotation(typeClass);
			result = annotated.keySet().toArray(new String[annotated.size()]);
			if (considerHierarchy) {
				if (beanFactory.getParentBeanFactory() instanceof ConfigurableListableBeanFactory) {
					String[] parentResult = getBeanNamesForAnnotation(
							(ConfigurableListableBeanFactory) beanFactory
									.getParentBeanFactory(),
							type, classLoader, true);
					List<String> resultList = new ArrayList<String>();
					resultList.addAll(Arrays.asList(result));
					for (String beanName : parentResult) {
						if (!resultList.contains(beanName)
								&& !beanFactory.containsLocalBean(beanName)) {
							resultList.add(beanName);
						}
					}
					result = StringUtils.toStringArray(resultList);
				}
			}
			return result;
		}
		catch (ClassNotFoundException ex) {
			return NO_BEANS;
		}
	}

	private static class BeanSearchSpec {

		private List<String> names = new ArrayList<String>();
		private List<String> types = new ArrayList<String>();
		private List<String> annotations = new ArrayList<String>();
		private SearchStrategy strategy;

		public BeanSearchSpec(ConditionContext context, AnnotatedTypeMetadata metadata,
				Class<?> annotationType) {
			MultiValueMap<String, Object> attributes = metadata
					.getAllAnnotationAttributes(annotationType.getName(), true);
			collect(attributes, "name", this.names);
			collect(attributes, "value", this.types);
			collect(attributes, "annotation", this.annotations);
			if (this.types.isEmpty() && this.names.isEmpty()) {
				addDeducedBeanType(context, metadata, this.types);
			}
			Assert.isTrue(
					!this.types.isEmpty() || !this.names.isEmpty()
							|| !this.annotations.isEmpty(),
					"@"
							+ ClassUtils.getShortName(annotationType)
							+ " annotations must specify at least one bean (type, name or annotation)");
			this.strategy = (SearchStrategy) metadata.getAnnotationAttributes(
					annotationType.getName()).get("search");
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void collect(MultiValueMap<String, Object> attributes, String key,
				List<String> destination) {
			List<String[]> valueList = (List) attributes.get(key);
			for (String[] valueArray : valueList) {
				for (String value : valueArray) {
					destination.add(value);
				}
			}
		}

		private void addDeducedBeanType(ConditionContext context,
				AnnotatedTypeMetadata metadata, final List<String> beanTypes) {
			if (metadata instanceof MethodMetadata
					&& metadata.isAnnotated(Bean.class.getName())) {
				try {
					final MethodMetadata methodMetadata = (MethodMetadata) metadata;
					// We should be safe to load at this point since we are in the
					// REGISTER_BEAN phase
					Class<?> configClass = ClassUtils.forName(
							methodMetadata.getDeclaringClassName(),
							context.getClassLoader());
					ReflectionUtils.doWithMethods(configClass, new MethodCallback() {
						@Override
						public void doWith(Method method)
								throws IllegalArgumentException, IllegalAccessException {
							if (methodMetadata.getMethodName().equals(method.getName())) {
								beanTypes.add(method.getReturnType().getName());
							}
						}
					});
				}
				catch (Exception ex) {
					// swallow exception and continue
				}
			}
		}

		public SearchStrategy getStrategy() {
			return (this.strategy != null ? this.strategy : SearchStrategy.ALL);
		}

		public List<String> getNames() {
			return this.names;
		}

		public List<String> getTypes() {
			return this.types;
		}

		public List<String> getAnnotations() {
			return this.annotations;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("names", this.names)
					.append("types", this.types).append("strategy", this.strategy)
					.toString();
		}
	}
}
