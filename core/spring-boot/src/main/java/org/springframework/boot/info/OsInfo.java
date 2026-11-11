/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.info;

/**
 * Information about the Operating System the application is running on.
 *
 * @author Jonatan Ivanov
 * @since 2.7.0
 */
public class OsInfo {

	private final String name;

	private final String version;

	private final String arch;

	public OsInfo() {
		this.name = System.getProperty("os.name");
		this.version = System.getProperty("os.version");
		this.arch = System.getProperty("os.arch");
	}

	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}

	public String getArch() {
		return this.arch;
	}

}
