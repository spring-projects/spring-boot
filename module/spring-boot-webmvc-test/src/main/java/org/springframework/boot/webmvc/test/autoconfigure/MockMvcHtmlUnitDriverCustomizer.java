/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * Callback interface that can be implemented by beans wishing to customize the MockMvc
 * {@link HtmlUnitDriver} to fine-tune its auto-configuration.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface MockMvcHtmlUnitDriverCustomizer {

	/**
	 * Customize the MockMvc HtmlUnitDriver.
	 * @param htmlUnitDriver the driver to customize
	 */
	void customize(HtmlUnitDriver htmlUnitDriver);

}
