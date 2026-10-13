/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.metadata;

import javax.tools.Diagnostic;

/**
 * Thrown to indicate that some meta-data is invalid. Define the severity to determine
 * whether it has to fail the build.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class InvalidConfigurationMetadataException extends RuntimeException {

	private final Diagnostic.Kind kind;

	public InvalidConfigurationMetadataException(String message, Diagnostic.Kind kind) {
		super(message);
		this.kind = kind;
	}

	public Diagnostic.Kind getKind() {
		return this.kind;
	}

}
