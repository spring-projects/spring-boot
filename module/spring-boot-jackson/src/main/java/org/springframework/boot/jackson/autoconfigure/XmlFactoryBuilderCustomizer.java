/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.XmlFactoryBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link XmlFactory} through {@link XmlFactoryBuilder} to fine-tune its
 * auto-configuration.
 *
 * @author Andy Wilkinson
 * @since 4.1.0
 */
@FunctionalInterface
public interface XmlFactoryBuilderCustomizer {

	/**
	 * Customize the XmlFactoryBuilder.
	 * @param xmlFactoryBuilder the builder to customize
	 */
	void customize(XmlFactoryBuilder xmlFactoryBuilder);

}
