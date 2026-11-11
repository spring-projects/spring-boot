/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.dataformat.cbor.CBORMapper.Builder;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link XmlMapper} through {@link Builder XmlMapper.Builder} to fine-tune its
 * auto-configuration.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface XmlMapperBuilderCustomizer {

	/**
	 * Customize the XmlMapper.Builder.
	 * @param xmlMapperBuilder the builder to customize
	 */
	void customize(XmlMapper.Builder xmlMapperBuilder);

}
