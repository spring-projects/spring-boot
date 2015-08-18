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

package org.springframework.boot.env;

import java.util.List;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * An {@link ApplicationListener} that responds to an
 * {@link ApplicationEnvironmentPreparedEvent} and calls all
 * {@link EnvironmentPostProcessor EnvironmentPostProcessors} that are available via
 * {@code spring.factories}.
 * <p>
 * Post-processors are called in the order defined by
 * {@link AnnotationAwareOrderComparator}.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 * @see SpringFactoriesLoader#loadFactories(Class, ClassLoader)
 */
public class EnvironmentPostProcessingApplicationListener implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		List<EnvironmentPostProcessor> postProcessors = SpringFactoriesLoader
				.loadFactories(EnvironmentPostProcessor.class, getClass()
						.getClassLoader());
		for (EnvironmentPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessEnvironment(event.getEnvironment(),
					event.getSpringApplication());
		}
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

}
