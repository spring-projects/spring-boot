/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restdocs.test.autoconfigure;

import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;

/**
 * A customizer for {@link MockMvcRestDocumentationConfigurer}. If a
 * {@code RestDocsMockMvcConfigurationCustomizer} bean is found in the application context
 * it will be {@link #customize called} to customize the
 * {@code MockMvcRestDocumentationConfigurer} before it is applied. Intended for use only
 * when the attributes on {@link AutoConfigureRestDocs @AutoConfigureRestDocs} do not
 * provide sufficient customization.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface RestDocsMockMvcConfigurationCustomizer {

	/**
	 * Customize the given {@code configurer}.
	 * @param configurer the configurer
	 */
	void customize(MockMvcRestDocumentationConfigurer configurer);

}
