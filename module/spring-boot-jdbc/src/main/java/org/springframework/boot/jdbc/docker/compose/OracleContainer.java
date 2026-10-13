/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.docker.compose;

/**
 * Enumeration of supported Oracle containers.
 *
 * @author Andy Wilkinson
 */
enum OracleContainer {

	FREE("gvenzl/oracle-free", "freepdb1"),

	XE("gvenzl/oracle-xe", "xepdb1");

	private final String imageName;

	private final String defaultDatabase;

	OracleContainer(String imageName, String defaultDatabase) {
		this.imageName = imageName;
		this.defaultDatabase = defaultDatabase;
	}

	String getImageName() {
		return this.imageName;
	}

	String getDefaultDatabase() {
		return this.defaultDatabase;
	}

}
