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

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.dataformat.cbor.CBORMapper;
import tools.jackson.dataformat.cbor.CBORMapper.Builder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link CBORMapper} through {@link Builder CBORMapper.Builder} to fine-tune its
 * auto-configuration.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface CborMapperBuilderCustomizer {

	/**
	 * Customize the CBORMapper.Builder.
	 * @param cborMapperBuilder the builder to customize
	 */
	void customize(CBORMapper.Builder cborMapperBuilder);

}
