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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Makes a test {@link EndpointExposureOutcomeContributor} available via
 * {@link SpringFactoriesLoader}.
 *
 * @author Andy Wilkinson
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@WithResource(name = "META-INF/spring.factories",
		content = """
				org.springframework.boot.actuate.autoconfigure.endpoint.condition.EndpointExposureOutcomeContributor=\
				org.springframework.boot.actuate.autoconfigure.endpoint.condition.WithTestEndpointOutcomeExposureContributor.TestEndpointExposureOutcomeContributor
				""")
public @interface WithTestEndpointOutcomeExposureContributor {

	class TestEndpointExposureOutcomeContributor implements EndpointExposureOutcomeContributor {

		private final IncludeExcludeEndpointFilter<?> filter;

		TestEndpointExposureOutcomeContributor(Environment environment) {
			this.filter = new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, environment,
					"management.endpoints.test.exposure");
		}

		@Override
		public ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
				Builder message) {
			if (this.filter.match(endpointId)) {
				return ConditionOutcome.match();
			}
			return null;
		}

	}

}
