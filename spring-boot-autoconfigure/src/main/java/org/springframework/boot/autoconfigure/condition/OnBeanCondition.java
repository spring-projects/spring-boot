/*
On * Copyright 2012-2014 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.ResolvableType;
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
 * @author Jakub Kubrynski
 */
class OnBeanCondition extends SpringBootCondition implements ConfigurationCondition {

	/**
	 * Bean definition attribute name for factory beans to signal their product type (if
	 * known and it can't be deduced from the factory bean class).
	 */
	public static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

	private static final String[] NO_BEANS = {};

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {

		StringBuffer matchMessage = new StringBuffer();

		if (metadata.isAnnotated(ConditionalOnBean.class.getName())) {
			BeanSearchSpec spec = new BeanSearchSpec(context, metadata,
					ConditionalOnBean.class);
			List<String> matching = getMatchingBeans(context, spec);
			if (matching.isEmpty()) {
				return ConditionOutcome.noMatch("@ConditionalOnBean " + spec
						+ " found no beans");
			}
			matchMessage.append("@ConditionalOnBean " + spec + " found the following "
					+ matching);
		}

		if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
			BeanSearchSpec spec = new BeanSearchSpec(context, metadata,
					ConditionalOnMissingBean.class);
			List<String> matching = getMatchingBeans(context, spec);
			if (!matching.isEmpty()) {
				return ConditionOutcome.noMatch("@ConditionalOnMissingBean " + spec
						+ " found the following " + matching);
			}
			matchMessage.append(matchMessage.length() == 0 ? "" : " ");
			matchMessage.append("@ConditionalOnMissingBean " + spec + " found no beans");
		}

		return ConditionOutcome.match(matchMessage.toString());
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
			beanNames.addAll(getBeanNamesForType(beanFactory, type,
					context.getClassLoader(), considerHierarchy));
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

	private Collection<String> getBeanNamesForType(
			ConfigurableListableBeanFactory beanFactory, String type,
			ClassLoader classLoader, boolean considerHierarchy) throws LinkageError {
		try {
			Set<String> result = new LinkedHashSet<String>();
			collectBeanNamesForType(result, beanFactory,
					ClassUtils.forName(type, classLoader), considerHierarchy);
			return result;
		}
		catch (ClassNotFoundException ex) {
			return Collections.emptySet();
		}
	}

	private void collectBeanNamesForType(Set<String> result,
			ListableBeanFactory beanFactory, Class<?> type, boolean considerHierarchy) {
		// eagerInit set to false to prevent early instantiation
		result.addAll(Arrays.asList(beanFactory.getBeanNamesForType(type, true, false)));
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			collectBeanNamesForTypeFromFactoryBeans(result,
					(ConfigurableListableBeanFactory) beanFactory, type);
		}
		if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory) {
			BeanFactory parent = ((HierarchicalBeanFactory) beanFactory)
					.getParentBeanFactory();
			if (parent instanceof ListableBeanFactory) {
				collectBeanNamesForType(result, (ListableBeanFactory) parent, type,
						considerHierarchy);
			}
		}
	}

	/**
	 * Attempt to collect bean names for type by considering FactoryBean generics. Some
	 * factory beans will not be able to determine their object type at this stage, so
	 * those are not eligible for matching this condition.
	 */
	private void collectBeanNamesForTypeFromFactoryBeans(Set<String> result,
			ConfigurableListableBeanFactory beanFactory, Class<?> type) {
		String[] names = beanFactory.getBeanNamesForType(FactoryBean.class, true, false);
		for (String name : names) {
			name = BeanFactoryUtils.transformedBeanName(name);
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(name);
			Class<?> generic = getFactoryBeanGeneric(beanFactory, beanDefinition, name);
			if (generic != null && ClassUtils.isAssignable(type, generic)) {
				result.add(name);
			}
		}
	}

	private Class<?> getFactoryBeanGeneric(ConfigurableListableBeanFactory beanFactory,
			BeanDefinition definition, String name) {
		try {
			if (StringUtils.hasLength(definition.getFactoryBeanName())
					&& StringUtils.hasLength(definition.getFactoryMethodName())) {
				return getConfigurationClassFactoryBeanGeneric(beanFactory, definition,
						name);
			}
			if (StringUtils.hasLength(definition.getBeanClassName())) {
				return getDirectFactoryBeanGeneric(beanFactory, definition, name);
			}
		}
		catch (Exception ex) {
		}
		return null;
	}

	private Class<?> getConfigurationClassFactoryBeanGeneric(
			ConfigurableListableBeanFactory beanFactory, BeanDefinition definition,
			String name) throws Exception {
		BeanDefinition factoryDefinition = beanFactory.getBeanDefinition(definition
				.getFactoryBeanName());
		Class<?> factoryClass = ClassUtils.forName(factoryDefinition.getBeanClassName(),
				beanFactory.getBeanClassLoader());
		Method method = ReflectionUtils.findMethod(factoryClass,
				definition.getFactoryMethodName());
		Class<?> generic = ResolvableType.forMethodReturnType(method)
				.as(FactoryBean.class).resolveGeneric();
		if ((generic == null || generic.equals(Object.class))
				&& definition.hasAttribute(FACTORY_BEAN_OBJECT_TYPE)) {
			generic = (Class<?>) definition.getAttribute(FACTORY_BEAN_OBJECT_TYPE);
		}
		return generic;
	}

	private Class<?> getDirectFactoryBeanGeneric(
			ConfigurableListableBeanFactory beanFactory, BeanDefinition definition,
			String name) throws ClassNotFoundException, LinkageError {
		Class<?> factoryBeanClass = ClassUtils.forName(definition.getBeanClassName(),
				beanFactory.getBeanClassLoader());
		Class<?> generic = ResolvableType.forClass(factoryBeanClass)
				.as(FactoryBean.class).resolveGeneric();
		if ((generic == null || generic.equals(Object.class))
				&& definition.hasAttribute(FACTORY_BEAN_OBJECT_TYPE)) {
			generic = (Class<?>) definition.getAttribute(FACTORY_BEAN_OBJECT_TYPE);
		}
		return generic;
	}

	private String[] getBeanNamesForAnnotation(
			ConfigurableListableBeanFactory beanFactory, String type,
			ClassLoader classLoader, boolean considerHierarchy) throws LinkageError {
		String[] result = NO_BEANS;
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> typeClass = (Class<? extends Annotation>) ClassUtils
					.forName(type, classLoader);
			result = beanFactory.getBeanNamesForAnnotation(typeClass);
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

		private final List<String> names = new ArrayList<String>();

		private final List<String> types = new ArrayList<String>();

		private final List<String> annotations = new ArrayList<String>();

		private final SearchStrategy strategy;

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
			Assert.isTrue(hasAtLeastOne(this.types, this.names, this.annotations),
					annotationName(annotationType) + " annotations must "
							+ "specify at least one bean (type, name or annotation)");
			this.strategy = (SearchStrategy) metadata.getAnnotationAttributes(
					annotationType.getName()).get("search");
		}

		private boolean hasAtLeastOne(List<?>... lists) {
			for (List<?> list : lists) {
				if (!list.isEmpty()) {
					return true;
				}
			}
			return false;
		}

		private String annotationName(Class<?> annotationType) {
			return "@" + ClassUtils.getShortName(annotationType);
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
				catch (Throwable ex) {
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
			StringBuilder string = new StringBuilder();
			string.append("(");
			if (!this.names.isEmpty()) {
				string.append("names: ");
				string.append(StringUtils.collectionToCommaDelimitedString(this.names));
				if (!this.types.isEmpty()) {
					string.append("; ");
				}
			}
			if (!this.types.isEmpty()) {
				string.append("types: ");
				string.append(StringUtils.collectionToCommaDelimitedString(this.types));
			}
			string.append("; SearchStrategy: ");
			string.append(this.strategy.toString().toLowerCase());
			string.append(")");
			return string.toString();
		}

	}

}
