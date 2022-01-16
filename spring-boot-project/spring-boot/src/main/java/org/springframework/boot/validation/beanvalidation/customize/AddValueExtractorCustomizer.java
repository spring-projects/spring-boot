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

package org.springframework.boot.validation.beanvalidation.customize;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Configuration;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * Add given collection of {@link ValueExtractor} into {@link Configuration}.
 *
 * @author Dang Zhicairang
 * @since 2.6.2
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AddValueExtractorCustomizer implements Customizer {

	private List<ValueExtractor> valueExtractors;

	public AddValueExtractorCustomizer(List<ValueExtractor> valueExtractors) {
		this.valueExtractors = valueExtractors;
	}

	public List<ValueExtractor> getValueExtractors() {
		return this.valueExtractors;
	}

	public void setValueExtractors(List<ValueExtractor> valueExtractors) {
		this.valueExtractors = valueExtractors;
	}

	@Override
	public void customize(Configuration<?> configuration) {
		Optional.ofNullable(this.getValueExtractors())
				.ifPresent((list) -> list.forEach(configuration::addValueExtractor));
	}

}
