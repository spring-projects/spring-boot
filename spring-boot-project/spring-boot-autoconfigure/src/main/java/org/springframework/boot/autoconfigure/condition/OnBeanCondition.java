/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
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
 * @author Uladzislau Seuruk
 * @see ConditionalOnBean
 * @see ConditionalOnMissingBean
 * @see ConditionalOnSingleCandidate
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

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

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionOutcome matchOutcome = ConditionOutcome.match();
		MergedAnnotations annotations = metadata.getAnnotations();
		if (annotations.isPresent(ConditionalOnBean.class)) {
			Spec<ConditionalOnBean> spec = new Spec<>(context, metadata, annotations, ConditionalOnBean.class);
			matchOutcome = evaluateConditionalOnBean(spec, matchOutcome.getConditionMessage());
			if (!matchOutcome.isMatch()) {
				return matchOutcome;
			}
		}
		if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
			Spec<ConditionalOnSingleCandidate> spec = new SingleCandidateSpec(context, metadata,
					metadata.getAnnotations());
			matchOutcome = evaluateConditionalOnSingleCandidate(spec, matchOutcome.getConditionMessage());
			if (!matchOutcome.isMatch()) {
				return matchOutcome;
			}
		}
		if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
			Spec<ConditionalOnMissingBean> spec = new Spec<>(context, metadata, annotations,
					ConditionalOnMissingBean.class);
			matchOutcome = evaluateConditionalOnMissingBean(spec, matchOutcome.getConditionMessage());
			if (!matchOutcome.isMatch()) {
				return matchOutcome;
			}
		}
		return matchOutcome;
	}

	private ConditionOutcome evaluateConditionalOnBean(Spec<ConditionalOnBean> spec, ConditionMessage matchMessage) {
		MatchResult matchResult = getMatchingBeans(spec);
		if (!matchResult.isAllMatched()) {
			String reason = createOnBeanNoMatchReason(matchResult);
			return ConditionOutcome.noMatch(spec.message().because(reason));
		}
		return ConditionOutcome.match(spec.message(matchMessage)
			.found("bean", "beans")
			.items(Style.QUOTE, matchResult.getNamesOfAllMatches()));
	}

	private ConditionOutcome evaluateConditionalOnSingleCandidate(Spec<ConditionalOnSingleCandidate> spec,
			ConditionMessage matchMessage) {
		MatchResult matchResult = getMatchingBeans(spec);
		if (!matchResult.isAllMatched()) {
			return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
		}
		Set<String> allBeans = matchResult.getNamesOfAllMatches();
		if (allBeans.size() == 1) {
			return ConditionOutcome
				.match(spec.message(matchMessage).found("a single bean").items(Style.QUOTE, allBeans));
		}
		Map<String, BeanDefinition> beanDefinitions = getBeanDefinitions(spec.context.getBeanFactory(), allBeans,
				spec.getStrategy() == SearchStrategy.ALL);
		List<String> primaryBeans = getPrimaryBeans(beanDefinitions);
		if (primaryBeans.size() == 1) {
			return ConditionOutcome.match(spec.message(matchMessage)
				.found("a single primary bean '" + primaryBeans.get(0) + "' from beans")
				.items(Style.QUOTE, allBeans));
		}
		if (primaryBeans.size() > 1) {
			return ConditionOutcome
				.noMatch(spec.message().found("multiple primary beans").items(Style.QUOTE, primaryBeans));
		}
		List<String> nonFallbackBeans = getNonFallbackBeans(beanDefinitions);
		if (nonFallbackBeans.size() == 1) {
			return ConditionOutcome.match(spec.message(matchMessage)
				.found("a single non-fallback bean '" + nonFallbackBeans.get(0) + "' from beans")
				.items(Style.QUOTE, allBeans));
		}
		return ConditionOutcome.noMatch(spec.message().found("multiple beans").items(Style.QUOTE, allBeans));
	}

	private ConditionOutcome evaluateConditionalOnMissingBean(Spec<ConditionalOnMissingBean> spec,
			ConditionMessage matchMessage) {
		MatchResult matchResult = getMatchingBeans(spec);
		if (matchResult.isAnyMatched()) {
			String reason = createOnMissingBeanNoMatchReason(matchResult);
			return ConditionOutcome.noMatch(spec.message().because(reason));
		}
		return ConditionOutcome.match(spec.message(matchMessage).didNotFind("any beans").atAll());
	}

	protected final MatchResult getMatchingBeans(Spec<?> spec) {
		ConfigurableListableBeanFactory beanFactory = getSearchBeanFactory(spec);
		ClassLoader classLoader = spec.getContext().getClassLoader();
		boolean considerHierarchy = spec.getStrategy() != SearchStrategy.CURRENT;
		Set<ResolvableType> parameterizedContainers = spec.getParameterizedContainers();
		MatchResult result = new MatchResult();
		Set<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(beanFactory, considerHierarchy,
				spec.getIgnoredTypes(), parameterizedContainers);
		for (ResolvableType type : spec.getTypes()) {
			Map<String, BeanDefinition> typeMatchedDefinitions = getBeanDefinitionsForType(beanFactory,
					considerHierarchy, type, parameterizedContainers);
			Set<String> typeMatchedNames = matchedNamesFrom(typeMatchedDefinitions,
					(name, definition) -> !ScopedProxyUtils.isScopedTarget(name)
							&& isCandidate(beanFactory, name, definition, beansIgnoredByType));
			if (typeMatchedNames.isEmpty()) {
				result.recordUnmatchedType(type);
			}
			else {
				result.recordMatchedType(type, typeMatchedNames);
			}
		}
		for (String annotation : spec.getAnnotations()) {
			Map<String, BeanDefinition> annotationMatchedDefinitions = getBeanDefinitionsForAnnotation(classLoader,
					beanFactory, annotation, considerHierarchy);
			Set<String> annotationMatchedNames = matchedNamesFrom(annotationMatchedDefinitions,
					(name, definition) -> isCandidate(beanFactory, name, definition, beansIgnoredByType));
			if (annotationMatchedNames.isEmpty()) {
				result.recordUnmatchedAnnotation(annotation);
			}
			else {
				result.recordMatchedAnnotation(annotation, annotationMatchedNames);

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

	private ConfigurableListableBeanFactory getSearchBeanFactory(Spec<?> spec) {
		ConfigurableListableBeanFactory beanFactory = spec.getContext().getBeanFactory();
		if (spec.getStrategy() == SearchStrategy.ANCESTORS) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			Assert.state(parent instanceof ConfigurableListableBeanFactory,
					"Unable to use SearchStrategy.ANCESTORS without ConfigurableListableBeanFactory");
			beanFactory = (ConfigurableListableBeanFactory) parent;
		}
		return beanFactory;
	}

	private Set<String> matchedNamesFrom(Map<String, BeanDefinition> namedDefinitions,
			BiPredicate<String, BeanDefinition> filter) {
		Set<String> matchedNames = new LinkedHashSet<>(namedDefinitions.size());
		for (Entry<String, BeanDefinition> namedDefinition : namedDefinitions.entrySet()) {
			if (filter.test(namedDefinition.getKey(), namedDefinition.getValue())) {
				matchedNames.add(namedDefinition.getKey());
			}
		}
		return matchedNames;
	}

	private boolean isCandidate(ConfigurableListableBeanFactory beanFactory, String name, BeanDefinition definition,
			Set<String> ignoredBeans) {
		return (!ignoredBeans.contains(name)) && (definition == null
				|| isAutowireCandidate(beanFactory, name, definition) && isDefaultCandidate(definition));
	}

	private boolean isAutowireCandidate(ConfigurableListableBeanFactory beanFactory, String name,
			BeanDefinition definition) {
		return definition.isAutowireCandidate() || isScopeTargetAutowireCandidate(beanFactory, name);
	}

	private boolean isScopeTargetAutowireCandidate(ConfigurableListableBeanFactory beanFactory, String name) {
		try {
			return ScopedProxyUtils.isScopedTarget(name)
					&& beanFactory.getBeanDefinition(ScopedProxyUtils.getOriginalBeanName(name)).isAutowireCandidate();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	private boolean isDefaultCandidate(BeanDefinition definition) {
		if (definition instanceof AbstractBeanDefinition abstractBeanDefinition) {
			return abstractBeanDefinition.isDefaultCandidate();
		}
		return true;
	}

	private Set<String> getNamesOfBeansIgnoredByType(ListableBeanFactory beanFactory, boolean considerHierarchy,
			Set<ResolvableType> ignoredTypes, Set<ResolvableType> parameterizedContainers) {
		Set<String> result = null;
		for (ResolvableType ignoredType : ignoredTypes) {
			Collection<String> ignoredNames = getBeanDefinitionsForType(beanFactory, considerHierarchy, ignoredType,
					parameterizedContainers)
				.keySet();
			result = addAll(result, ignoredNames);
		}
		return (result != null) ? result : Collections.emptySet();
	}

	private Map<String, BeanDefinition> getBeanDefinitionsForType(ListableBeanFactory beanFactory,
			boolean considerHierarchy, ResolvableType type, Set<ResolvableType> parameterizedContainers) {
		Map<String, BeanDefinition> result = collectBeanDefinitionsForType(beanFactory, considerHierarchy, type,
				parameterizedContainers, null);
		return (result != null) ? result : Collections.emptyMap();
	}

	private Map<String, BeanDefinition> collectBeanDefinitionsForType(ListableBeanFactory beanFactory,
			boolean considerHierarchy, ResolvableType type, Set<ResolvableType> parameterizedContainers,
			Map<String, BeanDefinition> result) {
		result = putAll(result, beanFactory.getBeanNamesForType(type, true, false), beanFactory);
		for (ResolvableType parameterizedContainer : parameterizedContainers) {
			ResolvableType generic = ResolvableType.forClassWithGenerics(parameterizedContainer.resolve(), type);
			result = putAll(result, beanFactory.getBeanNamesForType(generic, true, false), beanFactory);
		}
		if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
			BeanFactory parent = hierarchicalBeanFactory.getParentBeanFactory();
			if (parent instanceof ListableBeanFactory listableBeanFactory) {
				result = collectBeanDefinitionsForType(listableBeanFactory, considerHierarchy, type,
						parameterizedContainers, result);
			}
		}
		return result;
	}

	private Map<String, BeanDefinition> getBeanDefinitionsForAnnotation(ClassLoader classLoader,
			ConfigurableListableBeanFactory beanFactory, String type, boolean considerHierarchy) throws LinkageError {
		Map<String, BeanDefinition> result = null;
		try {
			result = collectBeanDefinitionsForAnnotation(beanFactory, resolveAnnotationType(classLoader, type),
					considerHierarchy, result);
		}
		catch (ClassNotFoundException ex) {
			// Continue
		}
		return (result != null) ? result : Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Annotation> resolveAnnotationType(ClassLoader classLoader, String type)
			throws ClassNotFoundException {
		return (Class<? extends Annotation>) resolve(type, classLoader);
	}

	private Map<String, BeanDefinition> collectBeanDefinitionsForAnnotation(ListableBeanFactory beanFactory,
			Class<? extends Annotation> annotationType, boolean considerHierarchy, Map<String, BeanDefinition> result) {
		result = putAll(result, getBeanNamesForAnnotation(beanFactory, annotationType), beanFactory);
		if (considerHierarchy) {
			BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
			if (parent instanceof ListableBeanFactory listableBeanFactory) {
				result = collectBeanDefinitionsForAnnotation(listableBeanFactory, annotationType, considerHierarchy,
						result);
			}
		}
		return result;
	}

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

	private boolean containsBean(ConfigurableListableBeanFactory beanFactory, String beanName,
			boolean considerHierarchy) {
		if (considerHierarchy) {
			return beanFactory.containsBean(beanName);
		}
		return beanFactory.containsLocalBean(beanName);
	}

	private String createOnBeanNoMatchReason(MatchResult matchResult) {
		StringBuilder reason = new StringBuilder();
		appendMessageForNoMatches(reason, matchResult.getUnmatchedAnnotations(), "annotated with");
		appendMessageForNoMatches(reason, matchResult.getUnmatchedTypes(), "of type");
		appendMessageForNoMatches(reason, matchResult.getUnmatchedNames(), "named");
		return reason.toString();
	}

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

	private Map<String, BeanDefinition> getBeanDefinitions(ConfigurableListableBeanFactory beanFactory,
			Set<String> beanNames, boolean considerHierarchy) {
		Map<String, BeanDefinition> definitions = new HashMap<>(beanNames.size());
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = findBeanDefinition(beanFactory, beanName, considerHierarchy);
			definitions.put(beanName, beanDefinition);
		}
		return definitions;
	}

	private List<String> getPrimaryBeans(Map<String, BeanDefinition> beanDefinitions) {
		return getMatchingBeans(beanDefinitions, BeanDefinition::isPrimary);
	}

	private List<String> getNonFallbackBeans(Map<String, BeanDefinition> beanDefinitions) {
		return getMatchingBeans(beanDefinitions, Predicate.not(BeanDefinition::isFallback));
	}

	private List<String> getMatchingBeans(Map<String, BeanDefinition> beanDefinitions, Predicate<BeanDefinition> test) {
		List<String> matches = new ArrayList<>();
		for (Entry<String, BeanDefinition> namedBeanDefinition : beanDefinitions.entrySet()) {
			if (test.test(namedBeanDefinition.getValue())) {
				matches.add(namedBeanDefinition.getKey());
			}
		}
		return matches;
	}

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

	private static Set<String> addAll(Set<String> result, Collection<String> additional) {
		if (CollectionUtils.isEmpty(additional)) {
			return result;
		}
		result = (result != null) ? result : new LinkedHashSet<>();
		result.addAll(additional);
		return result;
	}

	private static Map<String, BeanDefinition> putAll(Map<String, BeanDefinition> result, String[] beanNames,
			ListableBeanFactory beanFactory) {
		if (ObjectUtils.isEmpty(beanNames)) {
			return result;
		}
		if (result == null) {
			result = new LinkedHashMap<>();
		}
		for (String beanName : beanNames) {
			if (beanFactory instanceof ConfigurableListableBeanFactory clbf) {
				result.put(beanName, getBeanDefinition(beanName, clbf));
			}
			else {
				result.put(beanName, null);
			}
		}
		return result;
	}

	private static BeanDefinition getBeanDefinition(String beanName, ConfigurableListableBeanFactory beanFactory) {
		try {
			return beanFactory.getBeanDefinition(beanName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (BeanFactoryUtils.isFactoryDereference(beanName)) {
				return getBeanDefinition(BeanFactoryUtils.transformedBeanName(beanName), beanFactory);
			}
		}
		return null;
	}

	/**
	 * A search specification extracted from the underlying annotation.
	 */
	private static class Spec<A extends Annotation> {

		private final ConditionContext context;

		private final Class<? extends Annotation> annotationType;

		private final Set<String> names;

		private final Set<ResolvableType> types;

		private final Set<String> annotations;

		private final Set<ResolvableType> ignoredTypes;

		private final Set<ResolvableType> parameterizedContainers;

		private final SearchStrategy strategy;

		Spec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations,
				Class<A> annotationType) {
			MultiValueMap<String, Object> attributes = annotations.stream(annotationType)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.collect(MergedAnnotationCollectors.toMultiValueMap(Adapt.CLASS_TO_STRING));
			MergedAnnotation<A> annotation = annotations.get(annotationType);
			this.context = context;
			this.annotationType = annotationType;
			this.names = extract(attributes, "name");
			this.annotations = extract(attributes, "annotation");
			this.ignoredTypes = resolveWhenPossible(extract(attributes, "ignored", "ignoredType"));
			this.parameterizedContainers = resolveWhenPossible(extract(attributes, "parameterizedContainer"));
			this.strategy = annotation.getValue("search", SearchStrategy.class).orElse(null);
			Set<ResolvableType> types = resolveWhenPossible(extractTypes(attributes));
			BeanTypeDeductionException deductionException = null;
			if (types.isEmpty() && this.names.isEmpty() && this.annotations.isEmpty()) {
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

		protected Set<String> extractTypes(MultiValueMap<String, Object> attributes) {
			return extract(attributes, "value", "type");
		}

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

		private void merge(Set<String> result, String... additional) {
			Collections.addAll(result, additional);
		}

		private Set<ResolvableType> resolveWhenPossible(Set<String> classNames) {
			if (classNames.isEmpty()) {
				return Collections.emptySet();
			}
			Set<ResolvableType> resolved = new LinkedHashSet<>(classNames.size());
			for (String className : classNames) {
				try {
					Class<?> type = resolve(className, this.context.getClassLoader());
					resolved.add(ResolvableType.forRawClass(type));
				}
				catch (ClassNotFoundException | NoClassDefFoundError ex) {
					resolved.add(ResolvableType.NONE);
				}
			}
			return resolved;
		}

		protected void validate(BeanTypeDeductionException ex) {
			if (!hasAtLeastOneElement(getTypes(), getNames(), getAnnotations())) {
				String message = getAnnotationName() + " did not specify a bean using type, name or annotation";
				if (ex == null) {
					throw new IllegalStateException(message);
				}
				throw new IllegalStateException(message + " and the attempt to deduce the bean's type failed", ex);
			}
		}

		private boolean hasAtLeastOneElement(Set<?>... sets) {
			for (Set<?> set : sets) {
				if (!set.isEmpty()) {
					return true;
				}
			}
			return false;
		}

		protected final String getAnnotationName() {
			return "@" + ClassUtils.getShortName(this.annotationType);
		}

		private Set<ResolvableType> deducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName())) {
				return deducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata);
			}
			return Collections.emptySet();
		}

		private Set<ResolvableType> deducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata) {
			try {
				return Set.of(getReturnType(context, metadata));
			}
			catch (Throwable ex) {
				throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
			}
		}

		private ResolvableType getReturnType(ConditionContext context, MethodMetadata metadata)
				throws ClassNotFoundException, LinkageError {
			// Safe to load at this point since we are in the REGISTER_BEAN phase
			ClassLoader classLoader = context.getClassLoader();
			ResolvableType returnType = getMethodReturnType(metadata, classLoader);
			if (isParameterizedContainer(returnType.resolve())) {
				returnType = returnType.getGeneric();
			}
			return returnType;
		}

		private boolean isParameterizedContainer(Class<?> type) {
			return (type != null) && this.parameterizedContainers.stream()
				.map(ResolvableType::resolve)
				.anyMatch((container) -> container != null && container.isAssignableFrom(type));
		}

		private ResolvableType getMethodReturnType(MethodMetadata metadata, ClassLoader classLoader)
				throws ClassNotFoundException, LinkageError {
			Class<?> declaringClass = resolve(metadata.getDeclaringClassName(), classLoader);
			Method beanMethod = findBeanMethod(declaringClass, metadata.getMethodName());
			return ResolvableType.forMethodReturnType(beanMethod);
		}

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

		private boolean isBeanMethod(Method method) {
			return method != null && MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
				.isPresent(Bean.class);
		}

		private SearchStrategy getStrategy() {
			return (this.strategy != null) ? this.strategy : SearchStrategy.ALL;
		}

		Set<ResolvableType> getTypes() {
			return this.types;
		}

		private ConditionContext getContext() {
			return this.context;
		}

		private Set<String> getNames() {
			return this.names;
		}

		private Set<String> getAnnotations() {
			return this.annotations;
		}

		private Set<ResolvableType> getIgnoredTypes() {
			return this.ignoredTypes;
		}

		private Set<ResolvableType> getParameterizedContainers() {
			return this.parameterizedContainers;
		}

		private ConditionMessage.Builder message() {
			return ConditionMessage.forCondition(this.annotationType, this);
		}

		private ConditionMessage.Builder message(ConditionMessage message) {
			return message.andCondition(this.annotationType, this);
		}

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

		SingleCandidateSpec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations) {
			super(context, metadata, annotations, ConditionalOnSingleCandidate.class);
		}

		@Override
		protected Set<String> extractTypes(MultiValueMap<String, Object> attributes) {
			Set<String> types = super.extractTypes(attributes);
			types.removeAll(FILTERED_TYPES);
			return types;
		}

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

		private void recordMatchedName(String name) {
			this.matchedNames.add(name);
			this.namesOfAllMatches.add(name);
		}

		private void recordUnmatchedName(String name) {
			this.unmatchedNames.add(name);
		}

		private void recordMatchedAnnotation(String annotation, Collection<String> matchingNames) {
			this.matchedAnnotations.put(annotation, matchingNames);
			this.namesOfAllMatches.addAll(matchingNames);
		}

		private void recordUnmatchedAnnotation(String annotation) {
			this.unmatchedAnnotations.add(annotation);
		}

		private void recordMatchedType(ResolvableType type, Collection<String> matchingNames) {
			this.matchedTypes.put(type.toString(), matchingNames);
			this.namesOfAllMatches.addAll(matchingNames);
		}

		private void recordUnmatchedType(ResolvableType type) {
			this.unmatchedTypes.add(type.toString());
		}

		boolean isAllMatched() {
			return this.unmatchedAnnotations.isEmpty() && this.unmatchedNames.isEmpty()
					&& this.unmatchedTypes.isEmpty();
		}

		boolean isAnyMatched() {
			return (!this.matchedAnnotations.isEmpty()) || (!this.matchedNames.isEmpty())
					|| (!this.matchedTypes.isEmpty());
		}

		Map<String, Collection<String>> getMatchedAnnotations() {
			return this.matchedAnnotations;
		}

		List<String> getMatchedNames() {
			return this.matchedNames;
		}

		Map<String, Collection<String>> getMatchedTypes() {
			return this.matchedTypes;
		}

		List<String> getUnmatchedAnnotations() {
			return this.unmatchedAnnotations;
		}

		List<String> getUnmatchedNames() {
			return this.unmatchedNames;
		}

		List<String> getUnmatchedTypes() {
			return this.unmatchedTypes;
		}

		Set<String> getNamesOfAllMatches() {
			return this.namesOfAllMatches;
		}

	}

	/**
	 * Exception thrown when the bean type cannot be deduced.
	 */
	static final class BeanTypeDeductionException extends RuntimeException {

		private BeanTypeDeductionException(String className, String beanMethodName, Throwable cause) {
			super("Failed to deduce bean type for " + className + "." + beanMethodName, cause);
		}

	}

}
