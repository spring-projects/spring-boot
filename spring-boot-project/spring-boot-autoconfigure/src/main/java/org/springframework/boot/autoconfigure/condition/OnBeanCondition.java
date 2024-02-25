/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks for the presence or absence of specific beans.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Jakub Kubrynski
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see ConditionalOnBean
 * @see ConditionalOnMissingBean
 * @see ConditionalOnSingleCandidate
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {

	/**
	 * Returns the configuration phase of this method.
	 * @return The configuration phase of this method.
	 */
	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	/**
	 * Retrieves the outcomes of the condition evaluation for each auto configuration
	 * class.
	 * @param autoConfigurationClasses the array of auto configuration classes
	 * @param autoConfigurationMetadata the auto configuration metadata
	 * @return the array of condition outcomes
	 */
	@Override
	protected final ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
		for (int i = 0; i < outcomes.length; i++) {
			String autoConfigurationClass = autoConfigurationClasses[i];
			if (autoConfigurationClass != null) {
				Set<String> onBeanTypes = autoConfigurationMetadata.getSet(autoConfigurationClass, "ConditionalOnBean");
				outcomes[i] = getOutcome(onBeanTypes, ConditionalOnBean.class);
				if (outcomes[i] == null) {
					Set<String> onSingleCandidateTypes = autoConfigurationMetadata.getSet(autoConfigurationClass,
							"ConditionalOnSingleCandidate");
					outcomes[i] = getOutcome(onSingleCandidateTypes, ConditionalOnSingleCandidate.class);
				}
			}
		}
		return outcomes;
	}

	/**
	 * Returns the outcome of the condition evaluation based on the required bean types
	 * and annotation.
	 * @param requiredBeanTypes the set of required bean types
	 * @param annotation the annotation class
	 * @return the condition outcome
	 */
	private ConditionOutcome getOutcome(Set<String> requiredBeanTypes, Class<? extends Annotation> annotation) {
		List<String> missing = filter(requiredBeanTypes, ClassNameFilter.MISSING, getBeanClassLoader());
		if (!missing.isEmpty()) {
			ConditionMessage message = ConditionMessage.forCondition(annotation)
				.didNotFind("required type", "required types")
				.items(Style.QUOTE, missing);
			return ConditionOutcome.noMatch(message);
		}
		return null;
	}

	/**
	 * Determines the outcome of the condition for a given context and metadata.
	 * @param context the condition context
	 * @param metadata the annotated type metadata
	 * @return the condition outcome
	 */
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage matchMessage = ConditionMessage.empty();
		MergedAnnotations annotations = metadata.getAnnotations();
		if (annotations.isPresent(ConditionalOnBean.class)) {
			Spec<ConditionalOnBean> spec = new Spec<>(context, metadata, annotations, ConditionalOnBean.class);
			MatchResult matchResult = getMatchingBeans(context, spec);
			if (!matchResult.isAllMatched()) {
				String reason = createOnBeanNoMatchReason(matchResult);
				return ConditionOutcome.noMatch(spec.message().because(reason));
			}
			matchMessage = spec.message(matchMessage)
				.found("bean", "beans")
				.items(Style.QUOTE, matchResult.getNamesOfAllMatches());
		}
		if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
			Spec<ConditionalOnSingleCandidate> spec = new SingleCandidateSpec(context, metadata, annotations);
			MatchResult matchResult = getMatchingBeans(context, spec);
			if (!matchResult.isAllMatched()) {
				return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
			}
			Set<String> allBeans = matchResult.getNamesOfAllMatches();
			if (allBeans.size() == 1) {
				matchMessage = spec.message(matchMessage).found("a single bean").items(Style.QUOTE, allBeans);
			}
			else {
				List<String> primaryBeans = getPrimaryBeans(context.getBeanFactory(), allBeans,
						spec.getStrategy() == SearchStrategy.ALL);
				if (primaryBeans.isEmpty()) {
					return ConditionOutcome
						.noMatch(spec.message().didNotFind("a primary bean from beans").items(Style.QUOTE, allBeans));
				}
				if (primaryBeans.size() > 1) {
					return ConditionOutcome
						.noMatch(spec.message().found("multiple primary beans").items(Style.QUOTE, primaryBeans));
				}
				matchMessage = spec.message(matchMessage)
					.found("a single primary bean '" + primaryBeans.get(0) + "' from beans")
					.items(Style.QUOTE, allBeans);
			}
		}
		if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
			Spec<ConditionalOnMissingBean> spec = new Spec<>(context, metadata, annotations,
					ConditionalOnMissingBean.class);
			MatchResult matchResult = getMatchingBeans(context, spec);
			if (matchResult.isAnyMatched()) {
				String reason = createOnMissingBeanNoMatchReason(matchResult);
				return ConditionOutcome.noMatch(spec.message().because(reason));
			}
			matchMessage = spec.message(matchMessage).didNotFind("any beans").atAll();
		}
		return ConditionOutcome.match(matchMessage);
	}

	/**
	 * Retrieves the matching beans based on the given condition context and
	 * specification.
	 * @param context The condition context.
	 * @param spec The specification for matching beans.
	 * @return The match result containing the matched beans.
	 */
	protected final MatchResult getMatchingBeans(ConditionContext context, Spec<?> spec) {
		ClassLoader classLoader = context.getClassLoader();
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		boolean considerHierarchy = spec.getStrategy() != SearchStrategy.CURRENT;
		Set<Class<?>> parameterizedContainers = spec.getParameterizedContainers();
		if (spec.getStrategy() == SearchStrategy.ANCESTORS) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			Assert.isInstanceOf(ConfigurableListableBeanFactory.class, parent,
					"Unable to use SearchStrategy.ANCESTORS");
			beanFactory = (ConfigurableListableBeanFactory) parent;
		}
		MatchResult result = new MatchResult();
		Set<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(classLoader, beanFactory, considerHierarchy,
				spec.getIgnoredTypes(), parameterizedContainers);
		for (String type : spec.getTypes()) {
			Collection<String> typeMatches = getBeanNamesForType(classLoader, considerHierarchy, beanFactory, type,
					parameterizedContainers);
			typeMatches
				.removeIf((match) -> beansIgnoredByType.contains(match) || ScopedProxyUtils.isScopedTarget(match));
			if (typeMatches.isEmpty()) {
				result.recordUnmatchedType(type);
			}
			else {
				result.recordMatchedType(type, typeMatches);
			}
		}
		for (String annotation : spec.getAnnotations()) {
			Set<String> annotationMatches = getBeanNamesForAnnotation(classLoader, beanFactory, annotation,
					considerHierarchy);
			annotationMatches.removeAll(beansIgnoredByType);
			if (annotationMatches.isEmpty()) {
				result.recordUnmatchedAnnotation(annotation);
			}
			else {
				result.recordMatchedAnnotation(annotation, annotationMatches);
			}
		}
		for (String beanName : spec.getNames()) {
			if (!beansIgnoredByType.contains(beanName) && containsBean(beanFactory, beanName, considerHierarchy)) {
				result.recordMatchedName(beanName);
			}
			else {
				result.recordUnmatchedName(beanName);
			}
		}
		return result;
	}

	/**
	 * Retrieves the names of beans that are ignored based on their type.
	 * @param classLoader the class loader to use for loading classes
	 * @param beanFactory the bean factory to retrieve bean names from
	 * @param considerHierarchy flag indicating whether to consider the hierarchy of bean
	 * types
	 * @param ignoredTypes the set of types to ignore
	 * @param parameterizedContainers the set of parameterized containers
	 * @return the set of names of beans ignored by type
	 */
	private Set<String> getNamesOfBeansIgnoredByType(ClassLoader classLoader, ListableBeanFactory beanFactory,
			boolean considerHierarchy, Set<String> ignoredTypes, Set<Class<?>> parameterizedContainers) {
		Set<String> result = null;
		for (String ignoredType : ignoredTypes) {
			Collection<String> ignoredNames = getBeanNamesForType(classLoader, considerHierarchy, beanFactory,
					ignoredType, parameterizedContainers);
			result = addAll(result, ignoredNames);
		}
		return (result != null) ? result : Collections.emptySet();
	}

	/**
	 * Retrieves the names of beans of a specified type from the given bean factory.
	 * @param classLoader the class loader to use for resolving the type
	 * @param considerHierarchy flag indicating whether to consider the type hierarchy
	 * @param beanFactory the bean factory to retrieve the bean names from
	 * @param type the fully qualified name of the type to retrieve bean names for
	 * @param parameterizedContainers a set of parameterized container classes
	 * @return a set of bean names matching the specified type, or an empty set if no
	 * beans are found
	 * @throws LinkageError if a linkage error occurs while resolving the type
	 */
	private Set<String> getBeanNamesForType(ClassLoader classLoader, boolean considerHierarchy,
			ListableBeanFactory beanFactory, String type, Set<Class<?>> parameterizedContainers) throws LinkageError {
		try {
			return getBeanNamesForType(beanFactory, considerHierarchy, resolve(type, classLoader),
					parameterizedContainers);
		}
		catch (ClassNotFoundException | NoClassDefFoundError ex) {
			return Collections.emptySet();
		}
	}

	/**
	 * Retrieves the names of beans of a specified type from the given bean factory.
	 * @param beanFactory the bean factory to retrieve the bean names from
	 * @param considerHierarchy flag indicating whether to consider the bean hierarchy
	 * @param type the type of beans to retrieve
	 * @param parameterizedContainers a set of parameterized container classes
	 * @return a set of bean names of the specified type, or an empty set if none found
	 */
	private Set<String> getBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type,
			Set<Class<?>> parameterizedContainers) {
		Set<String> result = collectBeanNamesForType(beanFactory, considerHierarchy, type, parameterizedContainers,
				null);
		return (result != null) ? result : Collections.emptySet();
	}

	/**
	 * Collects the bean names for a given type from the provided bean factory.
	 * @param beanFactory the bean factory to collect bean names from
	 * @param considerHierarchy flag indicating whether to consider the hierarchy of bean
	 * factories
	 * @param type the type of beans to collect names for
	 * @param parameterizedContainers set of parameterized container classes to consider
	 * @param result the set to store the collected bean names
	 * @return the set of collected bean names
	 */
	private Set<String> collectBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy,
			Class<?> type, Set<Class<?>> parameterizedContainers, Set<String> result) {
		result = addAll(result, beanFactory.getBeanNamesForType(type, true, false));
		for (Class<?> container : parameterizedContainers) {
			ResolvableType generic = ResolvableType.forClassWithGenerics(container, type);
			result = addAll(result, beanFactory.getBeanNamesForType(generic, true, false));
		}
		if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
			BeanFactory parent = hierarchicalBeanFactory.getParentBeanFactory();
			if (parent instanceof ListableBeanFactory listableBeanFactory) {
				result = collectBeanNamesForType(listableBeanFactory, considerHierarchy, type, parameterizedContainers,
						result);
			}
		}
		return result;
	}

	/**
	 * Retrieves a set of bean names that are annotated with a specific annotation type.
	 * @param classLoader the class loader to use for resolving the annotation type
	 * @param beanFactory the bean factory to search for annotated beans
	 * @param type the fully qualified name of the annotation type
	 * @param considerHierarchy flag indicating whether to consider the hierarchy of beans
	 * @return a set of bean names that are annotated with the specified annotation type,
	 * or an empty set if none found
	 * @throws LinkageError if there is a linkage error while resolving the annotation
	 * type
	 */
	private Set<String> getBeanNamesForAnnotation(ClassLoader classLoader, ConfigurableListableBeanFactory beanFactory,
			String type, boolean considerHierarchy) throws LinkageError {
		Set<String> result = null;
		try {
			result = collectBeanNamesForAnnotation(beanFactory, resolveAnnotationType(classLoader, type),
					considerHierarchy, result);
		}
		catch (ClassNotFoundException ex) {
			// Continue
		}
		return (result != null) ? result : Collections.emptySet();
	}

	/**
	 * Resolves the annotation type based on the given class loader and type.
	 * @param classLoader the class loader to use for resolving the annotation type
	 * @param type the fully qualified name of the annotation type
	 * @return the resolved annotation type
	 * @throws ClassNotFoundException if the annotation type cannot be found
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends Annotation> resolveAnnotationType(ClassLoader classLoader, String type)
			throws ClassNotFoundException {
		return (Class<? extends Annotation>) resolve(type, classLoader);
	}

	/**
	 * Collects the names of beans that are annotated with the specified annotation type.
	 * @param beanFactory the ListableBeanFactory to search for beans
	 * @param annotationType the type of annotation to search for
	 * @param considerHierarchy flag indicating whether to consider the parent bean
	 * factory hierarchy
	 * @param result the set to store the collected bean names
	 * @return the set of bean names annotated with the specified annotation type
	 */
	private Set<String> collectBeanNamesForAnnotation(ListableBeanFactory beanFactory,
			Class<? extends Annotation> annotationType, boolean considerHierarchy, Set<String> result) {
		result = addAll(result, getBeanNamesForAnnotation(beanFactory, annotationType));
		if (considerHierarchy) {
			BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
			if (parent instanceof ListableBeanFactory listableBeanFactory) {
				result = collectBeanNamesForAnnotation(listableBeanFactory, annotationType, considerHierarchy, result);
			}
		}
		return result;
	}

	/**
	 * Retrieves the names of beans that are annotated with the specified annotation type.
	 * @param beanFactory the ListableBeanFactory to search for beans
	 * @param annotationType the annotation type to search for
	 * @return an array of bean names that are annotated with the specified annotation
	 * type
	 */
	private String[] getBeanNamesForAnnotation(ListableBeanFactory beanFactory,
			Class<? extends Annotation> annotationType) {
		Set<String> foundBeanNames = new LinkedHashSet<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			if (beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory) {
				BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition(beanName);
				if (beanDefinition != null && beanDefinition.isAbstract()) {
					continue;
				}
			}
			if (beanFactory.findAnnotationOnBean(beanName, annotationType, false) != null) {
				foundBeanNames.add(beanName);
			}
		}
		if (beanFactory instanceof SingletonBeanRegistry singletonBeanRegistry) {
			for (String beanName : singletonBeanRegistry.getSingletonNames()) {
				if (beanFactory.findAnnotationOnBean(beanName, annotationType) != null) {
					foundBeanNames.add(beanName);
				}
			}
		}
		return foundBeanNames.toArray(String[]::new);
	}

	/**
	 * Checks if the specified bean is present in the given bean factory.
	 * @param beanFactory the bean factory to check
	 * @param beanName the name of the bean to check
	 * @param considerHierarchy flag indicating whether to consider the bean hierarchy
	 * @return {@code true} if the bean is present, {@code false} otherwise
	 */
	private boolean containsBean(ConfigurableListableBeanFactory beanFactory, String beanName,
			boolean considerHierarchy) {
		if (considerHierarchy) {
			return beanFactory.containsBean(beanName);
		}
		return beanFactory.containsLocalBean(beanName);
	}

	/**
	 * Creates a reason message for no matches found in the given {@link MatchResult}.
	 * @param matchResult the {@link MatchResult} containing the unmatched annotations,
	 * types, and names
	 * @return the reason message for no matches found
	 */
	private String createOnBeanNoMatchReason(MatchResult matchResult) {
		StringBuilder reason = new StringBuilder();
		appendMessageForNoMatches(reason, matchResult.getUnmatchedAnnotations(), "annotated with");
		appendMessageForNoMatches(reason, matchResult.getUnmatchedTypes(), "of type");
		appendMessageForNoMatches(reason, matchResult.getUnmatchedNames(), "named");
		return reason.toString();
	}

	/**
	 * Appends a message to the given StringBuilder if there are no matches found.
	 * @param reason the StringBuilder to append the message to
	 * @param unmatched the collection of unmatched beans
	 * @param description the description of the beans being searched for
	 */
	private void appendMessageForNoMatches(StringBuilder reason, Collection<String> unmatched, String description) {
		if (!unmatched.isEmpty()) {
			if (!reason.isEmpty()) {
				reason.append(" and ");
			}
			reason.append("did not find any beans ");
			reason.append(description);
			reason.append(" ");
			reason.append(StringUtils.collectionToDelimitedString(unmatched, ", "));
		}
	}

	/**
	 * Creates a reason message for when no bean match is found.
	 * @param matchResult the match result containing the matched annotations, types, and
	 * names
	 * @return the reason message for no bean match
	 */
	private String createOnMissingBeanNoMatchReason(MatchResult matchResult) {
		StringBuilder reason = new StringBuilder();
		appendMessageForMatches(reason, matchResult.getMatchedAnnotations(), "annotated with");
		appendMessageForMatches(reason, matchResult.getMatchedTypes(), "of type");
		if (!matchResult.getMatchedNames().isEmpty()) {
			if (!reason.isEmpty()) {
				reason.append(" and ");
			}
			reason.append("found beans named ");
			reason.append(StringUtils.collectionToDelimitedString(matchResult.getMatchedNames(), ", "));
		}
		return reason.toString();
	}

	/**
	 * Appends a message for the matches to the given StringBuilder.
	 * @param reason the StringBuilder to append the message to
	 * @param matches a Map containing the matches
	 * @param description the description of the beans
	 */
	private void appendMessageForMatches(StringBuilder reason, Map<String, Collection<String>> matches,
			String description) {
		if (!matches.isEmpty()) {
			matches.forEach((key, value) -> {
				if (!reason.isEmpty()) {
					reason.append(" and ");
				}
				reason.append("found beans ");
				reason.append(description);
				reason.append(" '");
				reason.append(key);
				reason.append("' ");
				reason.append(StringUtils.collectionToDelimitedString(value, ", "));
			});
		}
	}

	/**
	 * Retrieves the list of primary beans from the given bean factory based on the
	 * provided bean names.
	 * @param beanFactory The configurable listable bean factory to retrieve the beans
	 * from.
	 * @param beanNames The set of bean names to consider.
	 * @param considerHierarchy Flag indicating whether to consider the bean hierarchy.
	 * @return The list of primary beans.
	 */
	private List<String> getPrimaryBeans(ConfigurableListableBeanFactory beanFactory, Set<String> beanNames,
			boolean considerHierarchy) {
		List<String> primaryBeans = new ArrayList<>();
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = findBeanDefinition(beanFactory, beanName, considerHierarchy);
			if (beanDefinition != null && beanDefinition.isPrimary()) {
				primaryBeans.add(beanName);
			}
		}
		return primaryBeans;
	}

	/**
	 * Finds the bean definition for the given bean name in the specified bean factory.
	 * @param beanFactory the configurable listable bean factory to search for the bean
	 * definition
	 * @param beanName the name of the bean to find the definition for
	 * @param considerHierarchy flag indicating whether to consider the parent bean
	 * factory hierarchy
	 * @return the bean definition if found, null otherwise
	 */
	private BeanDefinition findBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName,
			boolean considerHierarchy) {
		if (beanFactory.containsBeanDefinition(beanName)) {
			return beanFactory.getBeanDefinition(beanName);
		}
		if (considerHierarchy
				&& beanFactory.getParentBeanFactory() instanceof ConfigurableListableBeanFactory listableBeanFactory) {
			return findBeanDefinition(listableBeanFactory, beanName, considerHierarchy);
		}
		return null;
	}

	/**
	 * Adds all elements from the additional collection to the result set.
	 * @param result the set to add elements to
	 * @param additional the collection containing additional elements to be added
	 * @return the updated result set with the additional elements added
	 */
	private static Set<String> addAll(Set<String> result, Collection<String> additional) {
		if (CollectionUtils.isEmpty(additional)) {
			return result;
		}
		result = (result != null) ? result : new LinkedHashSet<>();
		result.addAll(additional);
		return result;
	}

	/**
	 * Adds all the elements from the given array to the provided set.
	 * @param result the set to which the elements will be added
	 * @param additional the array of elements to be added to the set
	 * @return the updated set with the additional elements added
	 */
	private static Set<String> addAll(Set<String> result, String[] additional) {
		if (ObjectUtils.isEmpty(additional)) {
			return result;
		}
		result = (result != null) ? result : new LinkedHashSet<>();
		Collections.addAll(result, additional);
		return result;
	}

	/**
	 * A search specification extracted from the underlying annotation.
	 */
	private static class Spec<A extends Annotation> {

		private final ClassLoader classLoader;

		private final Class<? extends Annotation> annotationType;

		private final Set<String> names;

		private final Set<String> types;

		private final Set<String> annotations;

		private final Set<String> ignoredTypes;

		private final Set<Class<?>> parameterizedContainers;

		private final SearchStrategy strategy;

		/**
		 * Constructs a new instance of the {@code Spec} class.
		 * @param context the {@code ConditionContext} object representing the condition
		 * context
		 * @param metadata the {@code AnnotatedTypeMetadata} object representing the
		 * annotated type metadata
		 * @param annotations the {@code MergedAnnotations} object representing the merged
		 * annotations
		 * @param annotationType the {@code Class} object representing the annotation type
		 */
		Spec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations,
				Class<A> annotationType) {
			MultiValueMap<String, Object> attributes = annotations.stream(annotationType)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.collect(MergedAnnotationCollectors.toMultiValueMap(Adapt.CLASS_TO_STRING));
			MergedAnnotation<A> annotation = annotations.get(annotationType);
			this.classLoader = context.getClassLoader();
			this.annotationType = annotationType;
			this.names = extract(attributes, "name");
			this.annotations = extract(attributes, "annotation");
			this.ignoredTypes = extract(attributes, "ignored", "ignoredType");
			this.parameterizedContainers = resolveWhenPossible(extract(attributes, "parameterizedContainer"));
			this.strategy = annotation.getValue("search", SearchStrategy.class).orElse(null);
			Set<String> types = extractTypes(attributes);
			BeanTypeDeductionException deductionException = null;
			if (types.isEmpty() && this.names.isEmpty()) {
				try {
					types = deducedBeanType(context, metadata);
				}
				catch (BeanTypeDeductionException ex) {
					deductionException = ex;
				}
			}
			this.types = types;
			validate(deductionException);
		}

		/**
		 * Extracts the types from the given attributes using the specified keys.
		 * @param attributes the MultiValueMap containing the attributes
		 * @return a Set of Strings representing the extracted types
		 */
		protected Set<String> extractTypes(MultiValueMap<String, Object> attributes) {
			return extract(attributes, "value", "type");
		}

		/**
		 * Extracts a set of strings from the given attributes based on the specified
		 * attribute names.
		 * @param attributes the multi-value map of attributes
		 * @param attributeNames the names of the attributes to extract
		 * @return a set of strings extracted from the attributes
		 */
		private Set<String> extract(MultiValueMap<String, Object> attributes, String... attributeNames) {
			if (attributes.isEmpty()) {
				return Collections.emptySet();
			}
			Set<String> result = new LinkedHashSet<>();
			for (String attributeName : attributeNames) {
				List<Object> values = attributes.getOrDefault(attributeName, Collections.emptyList());
				for (Object value : values) {
					if (value instanceof String[] stringArray) {
						merge(result, stringArray);
					}
					else if (value instanceof String string) {
						merge(result, string);
					}
				}
			}
			return result.isEmpty() ? Collections.emptySet() : result;
		}

		/**
		 * Merges the given additional strings into the specified result set.
		 * @param result the set to merge the additional strings into
		 * @param additional the additional strings to be merged into the result set
		 */
		private void merge(Set<String> result, String... additional) {
			Collections.addAll(result, additional);
		}

		/**
		 * Resolves a set of class names into a set of Class objects when possible.
		 * @param classNames the set of class names to resolve
		 * @return a set of resolved Class objects
		 */
		private Set<Class<?>> resolveWhenPossible(Set<String> classNames) {
			if (classNames.isEmpty()) {
				return Collections.emptySet();
			}
			Set<Class<?>> resolved = new LinkedHashSet<>(classNames.size());
			for (String className : classNames) {
				try {
					resolved.add(resolve(className, this.classLoader));
				}
				catch (ClassNotFoundException | NoClassDefFoundError ex) {
					// Ignore
				}
			}
			return resolved;
		}

		/**
		 * Validates the bean type deduction exception.
		 * @param ex the bean type deduction exception
		 * @throws IllegalStateException if no element is specified using type, name, or
		 * annotation
		 * @throws IllegalStateException if the attempt to deduce the bean's type failed
		 */
		protected void validate(BeanTypeDeductionException ex) {
			if (!hasAtLeastOneElement(this.types, this.names, this.annotations)) {
				String message = getAnnotationName() + " did not specify a bean using type, name or annotation";
				if (ex == null) {
					throw new IllegalStateException(message);
				}
				throw new IllegalStateException(message + " and the attempt to deduce the bean's type failed", ex);
			}
		}

		/**
		 * Checks if at least one of the given sets has at least one element.
		 * @param sets the sets to check
		 * @return {@code true} if at least one set has at least one element,
		 * {@code false} otherwise
		 */
		private boolean hasAtLeastOneElement(Set<?>... sets) {
			for (Set<?> set : sets) {
				if (!set.isEmpty()) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Returns the name of the annotation.
		 * @return the name of the annotation
		 */
		protected final String getAnnotationName() {
			return "@" + ClassUtils.getShortName(this.annotationType);
		}

		/**
		 * Deduces the bean type based on the given context and metadata.
		 * @param context the condition context
		 * @param metadata the annotated type metadata
		 * @return a set of deduced bean types
		 */
		private Set<String> deducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName())) {
				return deducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata);
			}
			return Collections.emptySet();
		}

		/**
		 * Deduces the bean type for a bean method.
		 * @param context the condition context
		 * @param metadata the method metadata
		 * @return a set containing the deduced bean type
		 * @throws BeanTypeDeductionException if an error occurs during bean type
		 * deduction
		 */
		private Set<String> deducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata) {
			try {
				Class<?> returnType = getReturnType(context, metadata);
				return Collections.singleton(returnType.getName());
			}
			catch (Throwable ex) {
				throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
			}
		}

		/**
		 * Retrieves the return type of a method based on the provided context and
		 * metadata.
		 * @param context the condition context
		 * @param metadata the method metadata
		 * @return the return type of the method
		 * @throws ClassNotFoundException if the return type class cannot be found
		 * @throws LinkageError if there is a linkage error while resolving the return
		 * type
		 */
		private Class<?> getReturnType(ConditionContext context, MethodMetadata metadata)
				throws ClassNotFoundException, LinkageError {
			// Safe to load at this point since we are in the REGISTER_BEAN phase
			ClassLoader classLoader = context.getClassLoader();
			Class<?> returnType = resolve(metadata.getReturnTypeName(), classLoader);
			if (isParameterizedContainer(returnType)) {
				returnType = getReturnTypeGeneric(metadata, classLoader);
			}
			return returnType;
		}

		/**
		 * Checks if the given type is a parameterized container.
		 * @param type the type to check
		 * @return true if the type is a parameterized container, false otherwise
		 */
		private boolean isParameterizedContainer(Class<?> type) {
			for (Class<?> parameterizedContainer : this.parameterizedContainers) {
				if (parameterizedContainer.isAssignableFrom(type)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Retrieves the generic return type of a method based on the provided metadata
		 * and class loader.
		 * @param metadata the metadata of the method
		 * @param classLoader the class loader to use for resolving the declaring class
		 * @return the generic return type of the method
		 * @throws ClassNotFoundException if the declaring class cannot be found
		 * @throws LinkageError if there is a linkage error while resolving the declaring
		 * class
		 */
		private Class<?> getReturnTypeGeneric(MethodMetadata metadata, ClassLoader classLoader)
				throws ClassNotFoundException, LinkageError {
			Class<?> declaringClass = resolve(metadata.getDeclaringClassName(), classLoader);
			Method beanMethod = findBeanMethod(declaringClass, metadata.getMethodName());
			return ResolvableType.forMethodReturnType(beanMethod).resolveGeneric();
		}

		/**
		 * Finds a bean method in the specified declaring class with the given method
		 * name.
		 * @param declaringClass the class in which to search for the method
		 * @param methodName the name of the method to find
		 * @return the found bean method
		 * @throws IllegalStateException if the bean method cannot be found
		 */
		private Method findBeanMethod(Class<?> declaringClass, String methodName) {
			Method method = ReflectionUtils.findMethod(declaringClass, methodName);
			if (isBeanMethod(method)) {
				return method;
			}
			Method[] candidates = ReflectionUtils.getAllDeclaredMethods(declaringClass);
			for (Method candidate : candidates) {
				if (candidate.getName().equals(methodName) && isBeanMethod(candidate)) {
					return candidate;
				}
			}
			throw new IllegalStateException("Unable to find bean method " + methodName);
		}

		/**
		 * Checks if the given method is a bean method.
		 * @param method the method to check
		 * @return {@code true} if the method is a bean method, {@code false} otherwise
		 */
		private boolean isBeanMethod(Method method) {
			return method != null && MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
				.isPresent(Bean.class);
		}

		/**
		 * Returns the search strategy to be used.
		 * @return the search strategy
		 */
		private SearchStrategy getStrategy() {
			return (this.strategy != null) ? this.strategy : SearchStrategy.ALL;
		}

		/**
		 * Returns the set of names.
		 * @return the set of names
		 */
		Set<String> getNames() {
			return this.names;
		}

		/**
		 * Returns the set of types.
		 * @return the set of types
		 */
		Set<String> getTypes() {
			return this.types;
		}

		/**
		 * Returns the set of annotations associated with this object.
		 * @return the set of annotations
		 */
		Set<String> getAnnotations() {
			return this.annotations;
		}

		/**
		 * Returns the set of ignored types.
		 * @return the set of ignored types
		 */
		Set<String> getIgnoredTypes() {
			return this.ignoredTypes;
		}

		/**
		 * Returns the parameterized containers.
		 * @return the parameterized containers
		 */
		Set<Class<?>> getParameterizedContainers() {
			return this.parameterizedContainers;
		}

		/**
		 * Returns a new instance of {@link ConditionMessage.Builder} for creating a
		 * condition message.
		 * @return a new instance of {@link ConditionMessage.Builder}
		 */
		ConditionMessage.Builder message() {
			return ConditionMessage.forCondition(this.annotationType, this);
		}

		/**
		 * Creates a new ConditionMessage.Builder object by adding the current condition
		 * to the given ConditionMessage.
		 * @param message the ConditionMessage to which the current condition is added
		 * @return a new ConditionMessage.Builder object with the current condition added
		 */
		ConditionMessage.Builder message(ConditionMessage message) {
			return message.andCondition(this.annotationType, this);
		}

		/**
		 * Returns a string representation of the object.
		 * @return a string representation of the object
		 */
		@Override
		public String toString() {
			boolean hasNames = !this.names.isEmpty();
			boolean hasTypes = !this.types.isEmpty();
			boolean hasIgnoredTypes = !this.ignoredTypes.isEmpty();
			StringBuilder string = new StringBuilder();
			string.append("(");
			if (hasNames) {
				string.append("names: ");
				string.append(StringUtils.collectionToCommaDelimitedString(this.names));
				string.append(hasTypes ? " " : "; ");
			}
			if (hasTypes) {
				string.append("types: ");
				string.append(StringUtils.collectionToCommaDelimitedString(this.types));
				string.append(hasIgnoredTypes ? " " : "; ");
			}
			if (hasIgnoredTypes) {
				string.append("ignored: ");
				string.append(StringUtils.collectionToCommaDelimitedString(this.ignoredTypes));
				string.append("; ");
			}
			string.append("SearchStrategy: ");
			string.append(this.strategy.toString().toLowerCase(Locale.ENGLISH));
			string.append(")");
			return string.toString();
		}

	}

	/**
	 * Specialized {@link Spec specification} for
	 * {@link ConditionalOnSingleCandidate @ConditionalOnSingleCandidate}.
	 */
	private static class SingleCandidateSpec extends Spec<ConditionalOnSingleCandidate> {

		private static final Collection<String> FILTERED_TYPES = Arrays.asList("", Object.class.getName());

		/**
		 * Constructs a new SingleCandidateSpec with the specified ConditionContext,
		 * AnnotatedTypeMetadata, and MergedAnnotations.
		 * @param context the ConditionContext representing the current condition
		 * evaluation context
		 * @param metadata the AnnotatedTypeMetadata representing the metadata of the
		 * annotated type
		 * @param annotations the MergedAnnotations representing the merged annotations of
		 * the annotated type
		 */
		SingleCandidateSpec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations) {
			super(context, metadata, annotations, ConditionalOnSingleCandidate.class);
		}

		/**
		 * Extracts the types from the given attributes and removes the filtered types.
		 * @param attributes the attributes from which to extract the types
		 * @return the set of types extracted from the attributes, with the filtered types
		 * removed
		 */
		@Override
		protected Set<String> extractTypes(MultiValueMap<String, Object> attributes) {
			Set<String> types = super.extractTypes(attributes);
			types.removeAll(FILTERED_TYPES);
			return types;
		}

		/**
		 * Validates the given BeanTypeDeductionException.
		 * @param ex the BeanTypeDeductionException to be validated
		 * @throws IllegalArgumentException if the number of types specified in the
		 * annotation is not equal to 1
		 */
		@Override
		protected void validate(BeanTypeDeductionException ex) {
			Assert.isTrue(getTypes().size() == 1,
					() -> getAnnotationName() + " annotations must specify only one type (got "
							+ StringUtils.collectionToCommaDelimitedString(getTypes()) + ")");
		}

	}

	/**
	 * Results collected during the condition evaluation.
	 */
	private static final class MatchResult {

		private final Map<String, Collection<String>> matchedAnnotations = new HashMap<>();

		private final List<String> matchedNames = new ArrayList<>();

		private final Map<String, Collection<String>> matchedTypes = new HashMap<>();

		private final List<String> unmatchedAnnotations = new ArrayList<>();

		private final List<String> unmatchedNames = new ArrayList<>();

		private final List<String> unmatchedTypes = new ArrayList<>();

		private final Set<String> namesOfAllMatches = new HashSet<>();

		/**
		 * Records a matched name.
		 * @param name the name to be recorded
		 */
		private void recordMatchedName(String name) {
			this.matchedNames.add(name);
			this.namesOfAllMatches.add(name);
		}

		/**
		 * Records an unmatched name.
		 * @param name the name to be recorded
		 */
		private void recordUnmatchedName(String name) {
			this.unmatchedNames.add(name);
		}

		/**
		 * Records the matched annotation and the collection of matching names.
		 * @param annotation the annotation to be recorded
		 * @param matchingNames the collection of matching names
		 */
		private void recordMatchedAnnotation(String annotation, Collection<String> matchingNames) {
			this.matchedAnnotations.put(annotation, matchingNames);
			this.namesOfAllMatches.addAll(matchingNames);
		}

		/**
		 * Records an unmatched annotation.
		 * @param annotation the unmatched annotation to be recorded
		 */
		private void recordUnmatchedAnnotation(String annotation) {
			this.unmatchedAnnotations.add(annotation);
		}

		/**
		 * Records the matched type and the collection of matching names.
		 * @param type the type of the match
		 * @param matchingNames the collection of matching names
		 */
		private void recordMatchedType(String type, Collection<String> matchingNames) {
			this.matchedTypes.put(type, matchingNames);
			this.namesOfAllMatches.addAll(matchingNames);
		}

		/**
		 * Records an unmatched type.
		 * @param type the type to be recorded
		 */
		private void recordUnmatchedType(String type) {
			this.unmatchedTypes.add(type);
		}

		/**
		 * Checks if all the annotations, names, and types are matched.
		 * @return true if all the annotations, names, and types are matched, false
		 * otherwise.
		 */
		boolean isAllMatched() {
			return this.unmatchedAnnotations.isEmpty() && this.unmatchedNames.isEmpty()
					&& this.unmatchedTypes.isEmpty();
		}

		/**
		 * Checks if there are any matches found in the MatchResult object.
		 * @return true if there are any matches found, false otherwise.
		 */
		boolean isAnyMatched() {
			return (!this.matchedAnnotations.isEmpty()) || (!this.matchedNames.isEmpty())
					|| (!this.matchedTypes.isEmpty());
		}

		/**
		 * Returns a map of matched annotations.
		 * @return a map where the keys are annotation names and the values are
		 * collections of matched annotations
		 */
		Map<String, Collection<String>> getMatchedAnnotations() {
			return this.matchedAnnotations;
		}

		/**
		 * Returns the list of matched names.
		 * @return the list of matched names
		 */
		List<String> getMatchedNames() {
			return this.matchedNames;
		}

		/**
		 * Returns the matched types.
		 * @return a map containing the matched types, where the keys are strings and the
		 * values are collections of strings
		 */
		Map<String, Collection<String>> getMatchedTypes() {
			return this.matchedTypes;
		}

		/**
		 * Returns a list of unmatched annotations.
		 * @return the list of unmatched annotations
		 */
		List<String> getUnmatchedAnnotations() {
			return this.unmatchedAnnotations;
		}

		/**
		 * Returns a list of unmatched names.
		 * @return the list of unmatched names
		 */
		List<String> getUnmatchedNames() {
			return this.unmatchedNames;
		}

		/**
		 * Returns a list of unmatched types.
		 * @return the list of unmatched types
		 */
		List<String> getUnmatchedTypes() {
			return this.unmatchedTypes;
		}

		/**
		 * Returns a Set of Strings containing the names of all matches.
		 * @return a Set of Strings containing the names of all matches
		 */
		Set<String> getNamesOfAllMatches() {
			return this.namesOfAllMatches;
		}

	}

	/**
	 * Exception thrown when the bean type cannot be deduced.
	 */
	static final class BeanTypeDeductionException extends RuntimeException {

		/**
		 * Constructs a new BeanTypeDeductionException with the specified class name, bean
		 * method name, and cause.
		 * @param className the name of the class
		 * @param beanMethodName the name of the bean method
		 * @param cause the cause of the exception
		 */
		private BeanTypeDeductionException(String className, String beanMethodName, Throwable cause) {
			super("Failed to deduce bean type for " + className + "." + beanMethodName, cause);
		}

	}

}
