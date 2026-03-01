/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.autoconfigure.metrics;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link MethodInterceptor} that creates {@link Observation observations} for Spring Data
 * repository methods annotated with {@link Observed @Observed}.
 *
 * @author Kwonneung Lee
 */
class ObservedRepositoryMethodInterceptor implements MethodInterceptor {

	private static final String DEFAULT_OBSERVATION_NAME = "method.observed";

	private static final String CLASS_KEY = "class";

	private static final String METHOD_KEY = "method";

	private static final String STATE_KEY = "state";

	private static final String SUCCESS_STATE = "SUCCESS";

	private static final String ERROR_STATE = "ERROR";

	private final SingletonSupplier<ObservationRegistry> registrySupplier;

	private final Class<?> repositoryInterface;

	ObservedRepositoryMethodInterceptor(Supplier<ObservationRegistry> registrySupplier,
			Class<?> repositoryInterface) {
		this.registrySupplier = (registrySupplier instanceof SingletonSupplier<ObservationRegistry> singleton)
				? singleton : SingletonSupplier.of(registrySupplier);
		this.repositoryInterface = repositoryInterface;
	}

	@Override
	public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		Observed observed = ObservedAnnotations.get(method, this.repositoryInterface);
		if (observed == null) {
			return invocation.proceed();
		}
		Observation observation = createObservation(method, observed);
		return proceedWithObservation(invocation, observation);
	}

	private Observation createObservation(Method method, Observed observed) {
		ObservationRegistry registry = this.registrySupplier.get();
		Assert.state(registry != null, "'registry' must not be null");
		String repositoryName = this.repositoryInterface.getSimpleName();
		String name = observed.name().isEmpty() ? DEFAULT_OBSERVATION_NAME : observed.name();
		Observation observation = Observation.createNotStarted(name, registry)
			.contextualName(repositoryName + "#" + method.getName())
			.lowCardinalityKeyValue(CLASS_KEY, repositoryName)
			.lowCardinalityKeyValue(METHOD_KEY, method.getName());
		addLowCardinalityKeyValues(observation, observed);
		return observation;
	}

	private @Nullable Object proceedWithObservation(MethodInvocation invocation, Observation observation) throws Throwable {
		observation.start();
		try (Observation.Scope scope = observation.openScope()) {
			Object result = invocation.proceed();
			observation.lowCardinalityKeyValue(STATE_KEY, SUCCESS_STATE);
			return result;
		}
		catch (Throwable ex) {
			observation.lowCardinalityKeyValue(STATE_KEY, ERROR_STATE);
			observation.error(ex);
			throw ex;
		}
		finally {
			observation.stop();
		}
	}

	private void addLowCardinalityKeyValues(Observation observation, Observed observed) {
		String[] keyValues = observed.lowCardinalityKeyValues();
		Assert.state((keyValues.length % 2) == 0, "'lowCardinalityKeyValues' must contain an even number of entries");
		for (int i = 0; i < keyValues.length; i += 2) {
			observation.lowCardinalityKeyValue(keyValues[i], keyValues[i + 1]);
		}
	}

}
