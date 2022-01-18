/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.validation.beanvalidation;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Configuration;

import org.springframework.boot.validation.beanvalidation.customize.Customizer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * This class will callback the {@link Customizer} which supply by application. For
 * example:
 *
 * <pre>{@code
 * &#64;Bean
 * public Customizer customizer() {
 *		return configuration -> {
 *			configuration.addValueExtractor(new CustomResultValueExtractor());
 *			configuration.xxx
 *			...
 *		};
 * }
 * }</pre>
 *
 * @author Dang Zhicairang
 * @since 2.6.2
 */
public class CustomizableLocalValidatorFactoryBean extends LocalValidatorFactoryBean {

	private List<Customizer> customizers;

	public void setCustomizers(List<Customizer> customizers) {
		this.customizers = customizers;
	}

	@Override
	protected void postProcessConfiguration(Configuration<?> configuration) {
		Optional.ofNullable(this.customizers)
				.ifPresent((list) -> list.forEach((customizer) -> customizer.customize(configuration)));
	}

}
