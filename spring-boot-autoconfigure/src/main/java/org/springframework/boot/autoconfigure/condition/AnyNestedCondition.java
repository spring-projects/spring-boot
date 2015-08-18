/*
 * Copyright 2012-2015 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that will match when any nested class condition matches. Can be used
 * to create composite conditions, for example:
 *
 * <pre class="code">
 * static class OnJndiOrProperty extends AnyNestedCondition {
 *
 *    &#064;ConditionalOnJndi()
 *    static class OnJndi {
 *    }

 *    &#064;ConditionalOnProperty("something")
 *    static class OnProperty {
 *    }
 *
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public abstract class AnyNestedCondition extends SpringBootCondition implements
		ConfigurationCondition {

	private final ConfigurationPhase configurationPhase;

	public AnyNestedCondition(ConfigurationPhase configurationPhase) {
		Assert.notNull(configurationPhase, "ConfigurationPhase must not be null");
		this.configurationPhase = configurationPhase;
	}

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return this.configurationPhase;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		MemberConditions memberConditions = new MemberConditions(context, getClass()
				.getName());
		List<ConditionOutcome> outcomes = memberConditions.getMatchOutcomes();
		List<ConditionOutcome> match = new ArrayList<ConditionOutcome>();
		List<ConditionOutcome> nonMatch = new ArrayList<ConditionOutcome>();
		for (ConditionOutcome outcome : outcomes) {
			if (outcome.isMatch()) {
				match.add(outcome);
			}
			else {
				nonMatch.add(outcome);
			}
		}
		return new ConditionOutcome(match.size() > 0, "any match resulted in " + match
				+ " matches and " + nonMatch + " non matches");
	}

	private static class MemberConditions {

		private final ConditionContext context;

		private final MetadataReaderFactory readerFactory;

		private final Map<AnnotationMetadata, List<Condition>> memberConditions;

		public MemberConditions(ConditionContext context, String className) {
			this.context = context;
			this.readerFactory = new SimpleMetadataReaderFactory(
					context.getResourceLoader());
			String[] members = getMetadata(className).getMemberClassNames();
			this.memberConditions = getMemberConditions(members);
		}

		private Map<AnnotationMetadata, List<Condition>> getMemberConditions(
				String[] members) {
			MultiValueMap<AnnotationMetadata, Condition> memberConditions = new LinkedMultiValueMap<AnnotationMetadata, Condition>();
			for (String member : members) {
				AnnotationMetadata metadata = getMetadata(member);
				for (String[] conditionClasses : getConditionClasses(metadata)) {
					for (String conditionClass : conditionClasses) {
						Condition condition = getCondition(conditionClass);
						memberConditions.add(metadata, condition);
					}
				}
			}
			return Collections.unmodifiableMap(memberConditions);
		}

		private AnnotationMetadata getMetadata(String className) {
			try {
				return this.readerFactory.getMetadataReader(className)
						.getAnnotationMetadata();
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		@SuppressWarnings("unchecked")
		private List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
			MultiValueMap<String, Object> attributes = metadata
					.getAllAnnotationAttributes(Conditional.class.getName(), true);
			Object values = (attributes != null ? attributes.get("value") : null);
			return (List<String[]>) (values != null ? values : Collections.emptyList());
		}

		private Condition getCondition(String conditionClassName) {
			Class<?> conditionClass = ClassUtils.resolveClassName(conditionClassName,
					this.context.getClassLoader());
			return (Condition) BeanUtils.instantiateClass(conditionClass);
		}

		public List<ConditionOutcome> getMatchOutcomes() {
			List<ConditionOutcome> outcomes = new ArrayList<ConditionOutcome>();
			for (Map.Entry<AnnotationMetadata, List<Condition>> entry : this.memberConditions
					.entrySet()) {
				AnnotationMetadata metadata = entry.getKey();
				for (Condition condition : entry.getValue()) {
					outcomes.add(getConditionOutcome(metadata, condition));
				}
			}
			return Collections.unmodifiableList(outcomes);
		}

		private ConditionOutcome getConditionOutcome(AnnotationMetadata metadata,
				Condition condition) {
			String messagePrefix = "member condition on " + metadata.getClassName();
			if (condition instanceof SpringBootCondition) {
				ConditionOutcome outcome = ((SpringBootCondition) condition)
						.getMatchOutcome(this.context, metadata);
				String message = outcome.getMessage();
				return new ConditionOutcome(outcome.isMatch(), messagePrefix
						+ (StringUtils.hasLength(message) ? " : " + message : ""));
			}
			boolean matches = condition.matches(this.context, metadata);
			return new ConditionOutcome(matches, messagePrefix);
		}

	}

}
