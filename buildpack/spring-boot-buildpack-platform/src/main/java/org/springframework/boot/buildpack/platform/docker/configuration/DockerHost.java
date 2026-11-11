/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.configuration;

import org.jspecify.annotations.Nullable;

/**
 * Docker host connection options.
 *
 * @author Scott Frederick
 * @since 2.4.0
 */
public class DockerHost {

	private final @Nullable String address;

	private final boolean secure;

	private final @Nullable String certificatePath;

	public DockerHost(@Nullable String address) {
		this(address, false, null);
	}

	public DockerHost(@Nullable String address, boolean secure, @Nullable String certificatePath) {
		this.address = address;
		this.secure = secure;
		this.certificatePath = certificatePath;
	}

	public @Nullable String getAddress() {
		return this.address;
	}

	public boolean isSecure() {
		return this.secure;
	}

	public @Nullable String getCertificatePath() {
		return this.certificatePath;
	}

}
