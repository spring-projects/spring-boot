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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.Environment;
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
 * @see ConditionalOnAvailableEndpoint
 */
class OnAvailableEndpointCondition extends SpringBootCondition {

	private static final String JMX_ENABLED_KEY = "spring.jmx.enabled";

	private static final String ENABLED_BY_DEFAULT_KEY = "management.endpoints.enabled-by-default";

	private static final Map<Environment, Set<ExposureFilter>> exposureFiltersCache = new ConcurrentReferenceHashMap<>();

	private static final ConcurrentReferenceHashMap<Environment, Optional<Boolean>> enabledByDefaultCache = new ConcurrentReferenceHashMap<>();

	/**
     * Determines the match outcome for the given condition context and annotated type metadata.
     * 
     * @param context the condition context
     * @param metadata the annotated type metadata
     * @return the match outcome
     */
    @Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		MergedAnnotation<ConditionalOnAvailableEndpoint> conditionAnnotation = metadata.getAnnotations()
			.get(ConditionalOnAvailableEndpoint.class);
		Class<?> target = getTarget(context, metadata, conditionAnnotation);
		MergedAnnotation<Endpoint> endpointAnnotation = getEndpointAnnotation(target);
		return getMatchOutcome(environment, conditionAnnotation, endpointAnnotation);
	}

	/**
     * Retrieves the target class for the conditional endpoint.
     * 
     * @param context the condition context
     * @param metadata the annotated type metadata
     * @param condition the merged annotation for ConditionalOnAvailableEndpoint
     * @return the target class for the conditional endpoint
     * @throws IllegalStateException if the endpoint id extraction fails
     */
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

	/**
     * Retrieves the merged annotation of type {@link Endpoint} for the given target class.
     * 
     * @param target the target class to retrieve the annotation from
     * @return the merged annotation of type {@link Endpoint} if present, otherwise null
     * @throws IllegalStateException if no endpoint is specified and the return type of the @Bean method is neither an @Endpoint, nor an @EndpointExtension
     */
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

	/**
     * Determines the outcome of the condition for a given environment, condition annotation, and endpoint annotation.
     * 
     * @param environment The environment in which the condition is being evaluated.
     * @param conditionAnnotation The merged annotation of the ConditionalOnAvailableEndpoint condition.
     * @param endpointAnnotation The merged annotation of the Endpoint.
     * @return The outcome of the condition evaluation.
     */
    private ConditionOutcome getMatchOutcome(Environment environment,
			MergedAnnotation<ConditionalOnAvailableEndpoint> conditionAnnotation,
			MergedAnnotation<Endpoint> endpointAnnotation) {
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnAvailableEndpoint.class);
		EndpointId endpointId = EndpointId.of(environment, endpointAnnotation.getString("id"));
		ConditionOutcome enablementOutcome = getEnablementOutcome(environment, endpointAnnotation, endpointId, message);
		if (!enablementOutcome.isMatch()) {
			return enablementOutcome;
		}
		Set<EndpointExposure> exposuresToCheck = getExposuresToCheck(conditionAnnotation);
		Set<ExposureFilter> exposureFilters = getExposureFilters(environment);
		for (ExposureFilter exposureFilter : exposureFilters) {
			if (exposuresToCheck.contains(exposureFilter.getExposure()) && exposureFilter.isExposed(endpointId)) {
				return ConditionOutcome.match(message.because("marked as exposed by a 'management.endpoints."
						+ exposureFilter.getExposure().name().toLowerCase() + ".exposure' property"));
			}
		}
		return ConditionOutcome.noMatch(message.because("no 'management.endpoints' property marked it as exposed"));
	}

	/**
     * Determines the enablement outcome of an endpoint based on the environment and endpoint annotations.
     * 
     * @param environment the environment to check for properties
     * @param endpointAnnotation the merged annotation of the endpoint
     * @param endpointId the ID of the endpoint
     * @param message the builder for the condition message
     * @return the enablement outcome of the endpoint
     */
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

	/**
     * Determines if the endpoint is enabled by default in the given environment.
     * 
     * @param environment the environment in which to check the default enabled status
     * @return {@code true} if the endpoint is enabled by default, {@code false} if it is disabled by default,
     *         or {@code null} if the default enabled status is not specified
     */
    private Boolean isEnabledByDefault(Environment environment) {
		Optional<Boolean> enabledByDefault = enabledByDefaultCache.computeIfAbsent(environment,
				(ignore) -> Optional.ofNullable(environment.getProperty(ENABLED_BY_DEFAULT_KEY, Boolean.class)));
		return enabledByDefault.orElse(null);
	}

	/**
     * Returns a set of EndpointExposure values to check based on the provided ConditionalOnAvailableEndpoint annotation.
     * 
     * @param conditionAnnotation the ConditionalOnAvailableEndpoint annotation to extract the exposure values from
     * @return a set of EndpointExposure values to check
     */
    private Set<EndpointExposure> getExposuresToCheck(
			MergedAnnotation<ConditionalOnAvailableEndpoint> conditionAnnotation) {
		EndpointExposure[] exposure = conditionAnnotation.getEnumArray("exposure", EndpointExposure.class);
		return (exposure.length == 0) ? EnumSet.allOf(EndpointExposure.class)
				: new LinkedHashSet<>(Arrays.asList(exposure));
	}

	/**
     * Retrieves the set of exposure filters based on the given environment.
     * 
     * @param environment the environment to retrieve the exposure filters for
     * @return the set of exposure filters
     */
    private Set<ExposureFilter> getExposureFilters(Environment environment) {
		Set<ExposureFilter> exposureFilters = exposureFiltersCache.get(environment);
		if (exposureFilters == null) {
			exposureFilters = new HashSet<>(2);
			if (environment.getProperty(JMX_ENABLED_KEY, Boolean.class, false)) {
				exposureFilters.add(new ExposureFilter(environment, EndpointExposure.JMX));
			}
			if (CloudPlatform.CLOUD_FOUNDRY.isActive(environment)) {
				exposureFilters.add(new ExposureFilter(environment, EndpointExposure.CLOUD_FOUNDRY));
			}
			exposureFilters.add(new ExposureFilter(environment, EndpointExposure.WEB));
			exposureFiltersCache.put(environment, exposureFilters);
		}
		return exposureFilters;
	}

	/**
     * ExposureFilter class.
     */
    static final class ExposureFilter extends IncludeExcludeEndpointFilter<ExposableEndpoint<?>> {

		private final EndpointExposure exposure;

		/**
         * Constructs a new ExposureFilter with the specified environment and endpoint exposure.
         * 
         * @param environment the environment to filter
         * @param exposure the endpoint exposure to apply
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
		private ExposureFilter(Environment environment, EndpointExposure exposure) {
			super((Class) ExposableEndpoint.class, environment,
					"management.endpoints." + getCanonicalName(exposure) + ".exposure", exposure.getDefaultIncludes());
			this.exposure = exposure;

		}

		/**
         * Returns the canonical name for the given EndpointExposure.
         * 
         * @param exposure the EndpointExposure to get the canonical name for
         * @return the canonical name
         */
        private static String getCanonicalName(EndpointExposure exposure) {
			if (EndpointExposure.CLOUD_FOUNDRY.equals(exposure)) {
				return "cloud-foundry";
			}
			return exposure.name().toLowerCase();
		}

		/**
         * Returns the exposure of the ExposureFilter.
         *
         * @return the exposure of the ExposureFilter
         */
        EndpointExposure getExposure() {
			return this.exposure;
		}

		/**
         * Checks if the given endpoint ID is exposed.
         * 
         * @param id the endpoint ID to check
         * @return true if the endpoint ID is exposed, false otherwise
         */
        boolean isExposed(EndpointId id) {
			return super.match(id);
		}

	}

}
