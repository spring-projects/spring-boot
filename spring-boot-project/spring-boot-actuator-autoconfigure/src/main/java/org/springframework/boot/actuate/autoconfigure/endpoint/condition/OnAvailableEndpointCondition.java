/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * A condition that checks if an endpoint is available (i.e. enabled and exposed).
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see ConditionalOnAvailableEndpoint
 */
class OnAvailableEndpointCondition extends SpringBootCondition {

	private static final String JMX_ENABLED_KEY = "spring.jmx.enabled";

	private static final String ENABLED_BY_DEFAULT_KEY = "management.endpoints.enabled-by-default";

	private static final Map<Environment, Set<EndpointExposureOutcomeContributor>> exposureOutcomeContributorsCache = new ConcurrentReferenceHashMap<>();

	private static final ConcurrentReferenceHashMap<Environment, Optional<Boolean>> enabledByDefaultCache = new ConcurrentReferenceHashMap<>();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		MergedAnnotation<ConditionalOnAvailableEndpoint> conditionAnnotation = metadata.getAnnotations()
			.get(ConditionalOnAvailableEndpoint.class);
		Class<?> target = getTarget(context, metadata, conditionAnnotation);
		MergedAnnotation<Endpoint> endpointAnnotation = getEndpointAnnotation(target);
		return getMatchOutcome(environment, conditionAnnotation, endpointAnnotation);
	}

	private Class<?> getTarget(ConditionContext context, AnnotatedTypeMetadata metadata,
			MergedAnnotation<ConditionalOnAvailableEndpoint> condition) {
		Class<?> target = condition.getClass("endpoint");
		if (target != Void.class) {
			return target;
		}
		Assert.state(metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName()),
				"EndpointCondition must be used on @Bean methods when the endpoint is not specified");
		MethodMetadata methodMetadata = (MethodMetadata) metadata;
		try {
			return ClassUtils.forName(methodMetadata.getReturnTypeName(), context.getClassLoader());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to extract endpoint id for "
					+ methodMetadata.getDeclaringClassName() + "." + methodMetadata.getMethodName(), ex);
		}
	}

	protected MergedAnnotation<Endpoint> getEndpointAnnotation(Class<?> target) {
		MergedAnnotations annotations = MergedAnnotations.from(target, SearchStrategy.TYPE_HIERARCHY);
		MergedAnnotation<Endpoint> endpoint = annotations.get(Endpoint.class);
		if (endpoint.isPresent()) {
			return endpoint;
		}
		MergedAnnotation<EndpointExtension> extension = annotations.get(EndpointExtension.class);
		Assert.state(extension.isPresent(), "No endpoint is specified and the return type of the @Bean method is "
				+ "neither an @Endpoint, nor an @EndpointExtension");
		return getEndpointAnnotation(extension.getClass("endpoint"));
	}

	private ConditionOutcome getMatchOutcome(Environment environment,
			MergedAnnotation<ConditionalOnAvailableEndpoint> conditionAnnotation,
			MergedAnnotation<Endpoint> endpointAnnotation) {
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnAvailableEndpoint.class);
		EndpointId endpointId = EndpointId.of(environment, endpointAnnotation.getString("id"));
		ConditionOutcome enablementOutcome = getEnablementOutcome(environment, endpointAnnotation, endpointId, message);
		ConditionOutcome exposureOutcome = (!enablementOutcome.isMatch()) ? null
				: getExposureOutcome(environment, conditionAnnotation, endpointAnnotation, endpointId, message);
		return (exposureOutcome != null) ? exposureOutcome
				: ConditionOutcome.noMatch(message.because("not enabled or exposed"));
	}

	private ConditionOutcome getEnablementOutcome(Environment environment,
			MergedAnnotation<Endpoint> endpointAnnotation, EndpointId endpointId, ConditionMessage.Builder message) {
		String key = "management.endpoint." + endpointId.toLowerCaseString() + ".enabled";
		Boolean userDefinedEnabled = environment.getProperty(key, Boolean.class);
		if (userDefinedEnabled != null) {
			return new ConditionOutcome(userDefinedEnabled,
					message.because("found property " + key + " with value " + userDefinedEnabled));
		}
		Boolean userDefinedDefault = isEnabledByDefault(environment);
		if (userDefinedDefault != null) {
			return new ConditionOutcome(userDefinedDefault, message
				.because("no property " + key + " found so using user defined default from " + ENABLED_BY_DEFAULT_KEY));
		}
		boolean endpointDefault = endpointAnnotation.getBoolean("enableByDefault");
		return new ConditionOutcome(endpointDefault,
				message.because("no property " + key + " found so using endpoint default of " + endpointDefault));
	}

	private Boolean isEnabledByDefault(Environment environment) {
		Optional<Boolean> enabledByDefault = enabledByDefaultCache.computeIfAbsent(environment,
				(ignore) -> Optional.ofNullable(environment.getProperty(ENABLED_BY_DEFAULT_KEY, Boolean.class)));
		return enabledByDefault.orElse(null);
	}

	private ConditionOutcome getExposureOutcome(Environment environment,
			MergedAnnotation<ConditionalOnAvailableEndpoint> conditionAnnotation,
			MergedAnnotation<Endpoint> endpointAnnotation, EndpointId endpointId, Builder message) {
		Set<EndpointExposure> exposures = getExposures(conditionAnnotation);
		Set<EndpointExposureOutcomeContributor> outcomeContributors = getExposureOutcomeContributors(environment);
		for (EndpointExposureOutcomeContributor outcomeContributor : outcomeContributors) {
			ConditionOutcome outcome = outcomeContributor.getExposureOutcome(endpointId, exposures, message);
			if (outcome != null && outcome.isMatch()) {
				return outcome;
			}
		}
		return null;
	}

	private Set<EndpointExposure> getExposures(MergedAnnotation<ConditionalOnAvailableEndpoint> conditionAnnotation) {
		EndpointExposure[] exposures = conditionAnnotation.getEnumArray("exposure", EndpointExposure.class);
		return replaceCloudFoundryExposure(
				(exposures.length == 0) ? EnumSet.allOf(EndpointExposure.class) : Arrays.asList(exposures));
	}

	@SuppressWarnings("removal")
	private Set<EndpointExposure> replaceCloudFoundryExposure(Collection<EndpointExposure> exposures) {
		Set<EndpointExposure> result = EnumSet.copyOf(exposures);
		if (result.remove(EndpointExposure.CLOUD_FOUNDRY)) {
			result.add(EndpointExposure.WEB);
		}
		return result;
	}

	private Set<EndpointExposureOutcomeContributor> getExposureOutcomeContributors(Environment environment) {
		Set<EndpointExposureOutcomeContributor> contributors = exposureOutcomeContributorsCache.get(environment);
		if (contributors == null) {
			contributors = new LinkedHashSet<>();
			contributors.add(new StandardExposureOutcomeContributor(environment, EndpointExposure.WEB));
			if (environment.getProperty(JMX_ENABLED_KEY, Boolean.class, false)) {
				contributors.add(new StandardExposureOutcomeContributor(environment, EndpointExposure.JMX));
			}
			contributors.addAll(loadExposureOutcomeContributors(environment));
			exposureOutcomeContributorsCache.put(environment, contributors);
		}
		return contributors;
	}

	private List<EndpointExposureOutcomeContributor> loadExposureOutcomeContributors(Environment environment) {
		ArgumentResolver argumentResolver = ArgumentResolver.of(Environment.class, environment);
		return SpringFactoriesLoader.forDefaultResourceLocation()
			.load(EndpointExposureOutcomeContributor.class, argumentResolver);
	}

	/**
	 * Standard {@link EndpointExposureOutcomeContributor}.
	 */
	private static class StandardExposureOutcomeContributor implements EndpointExposureOutcomeContributor {

		private final EndpointExposure exposure;

		private final String property;

		private final IncludeExcludeEndpointFilter<?> filter;

		StandardExposureOutcomeContributor(Environment environment, EndpointExposure exposure) {
			this.exposure = exposure;
			String name = exposure.name().toLowerCase().replace('_', '-');
			this.property = "management.endpoints." + name + ".exposure";
			this.filter = new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, environment, this.property,
					exposure.getDefaultIncludes());

		}

		@Override
		public ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
				ConditionMessage.Builder message) {
			if (exposures.contains(this.exposure) && this.filter.match(endpointId)) {
				return ConditionOutcome
					.match(message.because("marked as exposed by a '" + this.property + "' property"));
			}
			return null;
		}

	}

}
