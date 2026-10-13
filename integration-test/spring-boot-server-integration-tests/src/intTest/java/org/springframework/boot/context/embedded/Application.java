/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.embedded;

import java.io.File;

/**
 * A pre-built application that can be launched.
 *
 * @author Andy Wilkinson
 */
class Application {

	private final String packaging;

	private final String container;

	Application(String packaging, String container) {
		this.packaging = packaging;
		this.container = container;
	}

	String getPackaging() {
		return this.packaging;
	}

	String getContainer() {
		return this.container;
	}

	File getArchive() {
		return new File("build/spring-boot-server-tests-app/build/libs/spring-boot-server-tests-app-" + this.container
				+ "." + this.packaging);
	}

}
