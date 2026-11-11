/*
 * Copyright 2012-present the original author or authors.
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
