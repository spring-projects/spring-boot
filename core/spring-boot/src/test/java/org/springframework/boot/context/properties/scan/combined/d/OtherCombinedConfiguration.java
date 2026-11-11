/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.scan.combined.d;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Madhura Bhave
 */
public class OtherCombinedConfiguration {

	@RestController
	@ConfigurationProperties("c")
	static class MyControllerProperties {

	}

}
