/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.dataformat.cbor.CBORFactory;
import tools.jackson.dataformat.cbor.CBORFactoryBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link CBORFactory} through {@link CBORFactoryBuilder} to fine-tune its
 * auto-configuration.
 *
 * @author Andy Wilkinson
 * @since 4.1.0
 */
@FunctionalInterface
public interface CborFactoryBuilderCustomizer {

	/**
	 * Customize the CBORFactoryBuilder.
	 * @param cborFactoryBuilder the builder to customize
	 */
	void customize(CBORFactoryBuilder cborFactoryBuilder);

}
