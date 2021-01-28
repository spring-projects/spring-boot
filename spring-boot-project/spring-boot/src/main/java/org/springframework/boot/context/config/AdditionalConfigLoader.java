/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.config;

/**
 * Load additional configs using
 * {@link org.springframework.core.io.support.SpringFactoriesLoader}
 * <p>
 * Example: In this special scenario, where you want to have a spring-boot-starter with
 * its own configname-profile.properties with configured properties for multiple profiles,
 * you can implement this interface, so spring will include the defined configName in its
 * configs.
 *
 * If you want to order your configs you can annotate your class with
 * {@link org.springframework.core.annotation.Order}. Implementations without annotation
 * will get implicitly the lowest precedence by
 * {@link org.springframework.core.io.support.SpringFactoriesLoader}.
 *
 * Example implementation: <pre class="code">
 * class AdditionalCommonConfigLoader implements AdditionalConfigLoader {
 *
 *     &#064;Override
 *     public String useAdditionalConfig() {
 *         return "commons";
 *     }
 * }
 * </pre>
 * <p>
 * Add the following line to the spring-boot-starters {@code spring.factories}:
 * `org.springframework.boot.context.config.AdditionalConfigLoader=example.AdditionalCommonConfigLoader`
 *
 * Spring would now load an additional files like `commons.yaml` or `commons-local.yaml`.
 *
 * @author Kevin Raddatz
 * @since 28.01.2021
 * @see org.springframework.core.annotation.Order
 * @see org.springframework.core.io.support.SpringFactoriesLoader
 */
public interface AdditionalConfigLoader {

	String useAdditionalConfig();

}
