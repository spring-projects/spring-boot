/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hateoas.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties Properties} for Spring HATEOAS.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.hateoas")
public class HateoasProperties {

	/**
	 * Whether application/hal+json responses should be sent to requests that accept
	 * application/json.
	 */
	private boolean useHalAsDefaultJsonMediaType = true;

	public boolean isUseHalAsDefaultJsonMediaType() {
		return this.useHalAsDefaultJsonMediaType;
	}

	public void setUseHalAsDefaultJsonMediaType(boolean useHalAsDefaultJsonMediaType) {
		this.useHalAsDefaultJsonMediaType = useHalAsDefaultJsonMediaType;
	}

}
