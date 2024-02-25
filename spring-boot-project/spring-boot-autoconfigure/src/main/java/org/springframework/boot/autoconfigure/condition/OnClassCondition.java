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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} and {@link AutoConfigurationImportFilter} that checks for the
 * presence or absence of specific classes.
 *
 * @author Phillip Webb
 * @see ConditionalOnClass
 * @see ConditionalOnMissingClass
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class OnClassCondition extends FilteringSpringBootCondition {

	/**
	 * Retrieves the outcomes for the given auto configuration classes and auto
	 * configuration metadata. If there are more than one auto configuration classes and
	 * more than one processor is available, the work is split and performed in a
	 * background thread. Otherwise, the outcomes are resolved in the current thread.
	 * @param autoConfigurationClasses the auto configuration classes to retrieve outcomes
	 * for
	 * @param autoConfigurationMetadata the auto configuration metadata
	 * @return the array of condition outcomes for the auto configuration classes
	 */
	@Override
	protected final ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		// Split the work and perform half in a background thread if more than one
		// processor is available. Using a single additional thread seems to offer the
		// best performance. More threads make things worse.
		if (autoConfigurationClasses.length > 1 && Runtime.getRuntime().availableProcessors() > 1) {
			return resolveOutcomesThreaded(autoConfigurationClasses, autoConfigurationMetadata);
		}
		else {
			OutcomesResolver outcomesResolver = new StandardOutcomesResolver(autoConfigurationClasses, 0,
					autoConfigurationClasses.length, autoConfigurationMetadata, getBeanClassLoader());
			return outcomesResolver.resolveOutcomes();
		}
	}

	/**
	 * Resolves the outcomes of the auto-configuration classes in a threaded manner.
	 * @param autoConfigurationClasses the array of auto-configuration classes
	 * @param autoConfigurationMetadata the auto-configuration metadata
	 * @return an array of ConditionOutcome objects representing the resolved outcomes
	 */
	private ConditionOutcome[] resolveOutcomesThreaded(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		int split = autoConfigurationClasses.length / 2;
		OutcomesResolver firstHalfResolver = createOutcomesResolver(autoConfigurationClasses, 0, split,
				autoConfigurationMetadata);
		OutcomesResolver secondHalfResolver = new StandardOutcomesResolver(autoConfigurationClasses, split,
				autoConfigurationClasses.length, autoConfigurationMetadata, getBeanClassLoader());
		ConditionOutcome[] secondHalf = secondHalfResolver.resolveOutcomes();
		ConditionOutcome[] firstHalf = firstHalfResolver.resolveOutcomes();
		ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
		System.arraycopy(firstHalf, 0, outcomes, 0, firstHalf.length);
		System.arraycopy(secondHalf, 0, outcomes, split, secondHalf.length);
		return outcomes;
	}

	/**
	 * Creates an instance of {@link OutcomesResolver} by initializing a
	 * {@link StandardOutcomesResolver} with the given parameters. The
	 * {@link StandardOutcomesResolver} is then wrapped in a
	 * {@link ThreadedOutcomesResolver} to enable multi-threaded resolution of outcomes.
	 * @param autoConfigurationClasses an array of auto-configuration classes
	 * @param start the start index for auto-configuration classes
	 * @param end the end index for auto-configuration classes
	 * @param autoConfigurationMetadata the auto-configuration metadata
	 * @return an instance of {@link OutcomesResolver} for resolving outcomes
	 */
	private OutcomesResolver createOutcomesResolver(String[] autoConfigurationClasses, int start, int end,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		OutcomesResolver outcomesResolver = new StandardOutcomesResolver(autoConfigurationClasses, start, end,
				autoConfigurationMetadata, getBeanClassLoader());
		return new ThreadedOutcomesResolver(outcomesResolver);
	}

	/**
	 * Determines the outcome of a condition check based on the presence or absence of
	 * certain classes.
	 * @param context the condition context
	 * @param metadata the annotated type metadata
	 * @return the condition outcome
	 */
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ClassLoader classLoader = context.getClassLoader();
		ConditionMessage matchMessage = ConditionMessage.empty();
		List<String> onClasses = getCandidates(metadata, ConditionalOnClass.class);
		if (onClasses != null) {
			List<String> missing = filter(onClasses, ClassNameFilter.MISSING, classLoader);
			if (!missing.isEmpty()) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
					.didNotFind("required class", "required classes")
					.items(Style.QUOTE, missing));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
				.found("required class", "required classes")
				.items(Style.QUOTE, filter(onClasses, ClassNameFilter.PRESENT, classLoader));
		}
		List<String> onMissingClasses = getCandidates(metadata, ConditionalOnMissingClass.class);
		if (onMissingClasses != null) {
			List<String> present = filter(onMissingClasses, ClassNameFilter.PRESENT, classLoader);
			if (!present.isEmpty()) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnMissingClass.class)
					.found("unwanted class", "unwanted classes")
					.items(Style.QUOTE, present));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
				.didNotFind("unwanted class", "unwanted classes")
				.items(Style.QUOTE, filter(onMissingClasses, ClassNameFilter.MISSING, classLoader));
		}
		return ConditionOutcome.match(matchMessage);
	}

	/**
	 * Retrieves the candidates from the given metadata based on the specified annotation
	 * type.
	 * @param metadata the metadata containing the annotations
	 * @param annotationType the type of annotation to retrieve candidates for
	 * @return a list of candidate strings
	 */
	private List<String> getCandidates(AnnotatedTypeMetadata metadata, Class<?> annotationType) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(annotationType.getName(), true);
		if (attributes == null) {
			return null;
		}
		List<String> candidates = new ArrayList<>();
		addAll(candidates, attributes.get("value"));
		addAll(candidates, attributes.get("name"));
		return candidates;
	}

	/**
	 * Adds all items from the given list of objects to the provided list of strings.
	 * @param list the list of strings to add the items to
	 * @param itemsToAdd the list of objects containing the items to be added
	 * @throws NullPointerException if the itemsToAdd list is null
	 */
	private void addAll(List<String> list, List<Object> itemsToAdd) {
		if (itemsToAdd != null) {
			for (Object item : itemsToAdd) {
				Collections.addAll(list, (String[]) item);
			}
		}
	}

	private interface OutcomesResolver {

		ConditionOutcome[] resolveOutcomes();

	}

	/**
	 * ThreadedOutcomesResolver class.
	 */
	private static final class ThreadedOutcomesResolver implements OutcomesResolver {

		private final Thread thread;

		private volatile ConditionOutcome[] outcomes;

		/**
		 * Constructs a new ThreadedOutcomesResolver with the specified OutcomesResolver.
		 * @param outcomesResolver the OutcomesResolver to be used for resolving outcomes
		 */
		private ThreadedOutcomesResolver(OutcomesResolver outcomesResolver) {
			this.thread = new Thread(() -> this.outcomes = outcomesResolver.resolveOutcomes());
			this.thread.start();
		}

		/**
		 * Resolves the outcomes of the threaded execution.
		 * @return An array of ConditionOutcome objects representing the resolved
		 * outcomes.
		 */
		@Override
		public ConditionOutcome[] resolveOutcomes() {
			try {
				this.thread.join();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return this.outcomes;
		}

	}

	/**
	 * StandardOutcomesResolver class.
	 */
	private static final class StandardOutcomesResolver implements OutcomesResolver {

		private final String[] autoConfigurationClasses;

		private final int start;

		private final int end;

		private final AutoConfigurationMetadata autoConfigurationMetadata;

		private final ClassLoader beanClassLoader;

		/**
		 * Constructs a new StandardOutcomesResolver with the specified parameters.
		 * @param autoConfigurationClasses the array of auto-configuration classes
		 * @param start the start index of the auto-configuration classes to process
		 * @param end the end index of the auto-configuration classes to process
		 * @param autoConfigurationMetadata the auto-configuration metadata
		 * @param beanClassLoader the class loader to use for loading beans
		 */
		private StandardOutcomesResolver(String[] autoConfigurationClasses, int start, int end,
				AutoConfigurationMetadata autoConfigurationMetadata, ClassLoader beanClassLoader) {
			this.autoConfigurationClasses = autoConfigurationClasses;
			this.start = start;
			this.end = end;
			this.autoConfigurationMetadata = autoConfigurationMetadata;
			this.beanClassLoader = beanClassLoader;
		}

		/**
		 * Resolves the outcomes for the auto-configuration classes.
		 * @return an array of ConditionOutcome objects representing the resolved outcomes
		 */
		@Override
		public ConditionOutcome[] resolveOutcomes() {
			return getOutcomes(this.autoConfigurationClasses, this.start, this.end, this.autoConfigurationMetadata);
		}

		/**
		 * Retrieves the outcomes for a range of auto configuration classes based on their
		 * conditional on class metadata.
		 * @param autoConfigurationClasses an array of auto configuration class names
		 * @param start the starting index of the range
		 * @param end the ending index of the range
		 * @param autoConfigurationMetadata the auto configuration metadata
		 * @return an array of condition outcomes for the specified range of auto
		 * configuration classes
		 */
		private ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses, int start, int end,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			ConditionOutcome[] outcomes = new ConditionOutcome[end - start];
			for (int i = start; i < end; i++) {
				String autoConfigurationClass = autoConfigurationClasses[i];
				if (autoConfigurationClass != null) {
					String candidates = autoConfigurationMetadata.get(autoConfigurationClass, "ConditionalOnClass");
					if (candidates != null) {
						outcomes[i - start] = getOutcome(candidates);
					}
				}
			}
			return outcomes;
		}

		/**
		 * Retrieves the outcome of a condition based on the given candidates.
		 * @param candidates the candidates to evaluate
		 * @return the outcome of the condition
		 */
		private ConditionOutcome getOutcome(String candidates) {
			try {
				if (!candidates.contains(",")) {
					return getOutcome(candidates, this.beanClassLoader);
				}
				for (String candidate : StringUtils.commaDelimitedListToStringArray(candidates)) {
					ConditionOutcome outcome = getOutcome(candidate, this.beanClassLoader);
					if (outcome != null) {
						return outcome;
					}
				}
			}
			catch (Exception ex) {
				// We'll get another chance later
			}
			return null;
		}

		/**
		 * Retrieves the outcome of a condition based on the provided class name and class
		 * loader.
		 * @param className the name of the class to check
		 * @param classLoader the class loader to use for loading the class
		 * @return the outcome of the condition
		 */
		private ConditionOutcome getOutcome(String className, ClassLoader classLoader) {
			if (ClassNameFilter.MISSING.matches(className, classLoader)) {
				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
					.didNotFind("required class")
					.items(Style.QUOTE, className));
			}
			return null;
		}

	}

}
