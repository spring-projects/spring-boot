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

package org.springframework.boot.batch.autoconfigure;

import org.springframework.core.convert.support.ConfigurableConversionService;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link ConfigurableConversionService} that is used by the batch infrastructure while
 * retaining its default auto-configuration.
 *
 * @author Claudio Nave
 * @since 4.0.0
 */
@FunctionalInterface
public interface BatchConversionServiceCustomizer {

	/**
	 * Customize the {@link ConfigurableConversionService}.
	 * @param configurableConversionService the ConfigurableConversionService to customize
	 */
	void customize(ConfigurableConversionService configurableConversionService);

}
