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

package org.springframework.boot.autoconfigure.graphql.data;

import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;

/**
 * Callback interface that can be implemented by beans wishing to customize properties of
 * {@link AnnotatedControllerConfigurer} whilst retaining default auto-configuration.
 *
 * @author Max Hovens
 * @since 3.3.0
 */
@FunctionalInterface
public interface AnnotatedControllerConfigurerCustomizer {

	/**
	 * Customize the {@link AnnotatedControllerConfigurer} instance.
	 * @param configurer the configurer to customize
	 */
	void customize(AnnotatedControllerConfigurer configurer);

}
