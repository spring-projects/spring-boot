/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Customizer for the auto-configured {@link ForwardedHeaderFilter}.
 *
 * @author Brian Clozel
 * @since 4.2.0
 */
@FunctionalInterface
public interface ForwardedHeaderFilterCustomizer {

	/**
	 * Customize the given {@link ForwardedHeaderFilter}.
	 * @param filter the filter to customize
	 */
	void customize(ForwardedHeaderFilter filter);

}
