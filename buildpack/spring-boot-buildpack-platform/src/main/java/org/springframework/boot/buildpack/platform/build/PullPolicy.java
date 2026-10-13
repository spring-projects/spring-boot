/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

/**
 * Image pull policy.
 *
 * @author Andrey Shlykov
 * @since 2.4.0
 */
public enum PullPolicy {

	/**
	 * Always pull the image from the registry.
	 */
	ALWAYS,

	/**
	 * Never pull the image from the registry.
	 */
	NEVER,

	/**
	 * Pull the image from the registry only if it does not exist locally.
	 */
	IF_NOT_PRESENT

}
