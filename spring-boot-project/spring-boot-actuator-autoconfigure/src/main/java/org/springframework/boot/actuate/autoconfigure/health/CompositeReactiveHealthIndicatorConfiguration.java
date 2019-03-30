/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeReactiveHealthIndicator;
import org.springframework.boot.actuate.health.DefaultReactiveHealthIndicatorRegistry;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistry;
import org.springframework.core.ResolvableType;

/**
 * Reactive variant of {@link CompositeHealthIndicatorConfiguration}.
 *
 * @param <H> the health indicator type
 * @param <S> the bean source type
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public abstract class CompositeReactiveHealthIndicatorConfiguration<H extends ReactiveHealthIndicator, S> {

	@Autowired
	private HealthAggregator healthAggregator;

	protected ReactiveHealthIndicator createHealthIndicator(Map<String, S> beans) {
		if (beans.size() == 1) {
			return createHealthIndicator(beans.values().iterator().next());
		}
		ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry();
		beans.forEach(
				(name, source) -> registry.register(name, createHealthIndicator(source)));
		return new CompositeReactiveHealthIndicator(this.healthAggregator, registry);
	}

	@SuppressWarnings("unchecked")
	protected H createHealthIndicator(S source) {
		Class<?>[] generics = ResolvableType
				.forClass(CompositeReactiveHealthIndicatorConfiguration.class, getClass())
				.resolveGenerics();
		Class<H> indicatorClass = (Class<H>) generics[0];
		Class<S> sourceClass = (Class<S>) generics[1];
		try {
			return indicatorClass.getConstructor(sourceClass).newInstance(source);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create indicator " + indicatorClass
					+ " for source " + sourceClass, ex);
		}
	}

}
