/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.health.CompositeReactiveHealthIndicator;
import org.springframework.boot.actuate.health.CompositeReactiveHealthIndicatorFactory;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.ApplicationContext;

/**
 * Creates a {@link CompositeReactiveHealthIndicator} from beans in the
 * {@link ApplicationContext}.
 *
 * @author Phillip Webb
 */
final class HealthIndicatorBeansReactiveComposite {

	private HealthIndicatorBeansReactiveComposite() {
	}

	public static ReactiveHealthIndicator get(ApplicationContext applicationContext) {
		HealthAggregator healthAggregator = getHealthAggregator(applicationContext);
		return new CompositeReactiveHealthIndicatorFactory().createReactiveHealthIndicator(healthAggregator,
				applicationContext.getBeansOfType(ReactiveHealthIndicator.class),
				applicationContext.getBeansOfType(HealthIndicator.class));
	}

	private static HealthAggregator getHealthAggregator(ApplicationContext applicationContext) {
		try {
			return applicationContext.getBean(HealthAggregator.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return new OrderedHealthAggregator();
		}
	}

}
