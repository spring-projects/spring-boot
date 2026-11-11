/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.scan.combined.c;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Madhura Bhave
 */
public class CombinedConfiguration {

	@Component
	@ConfigurationProperties("b")
	static class MyProperties {

	}

}
