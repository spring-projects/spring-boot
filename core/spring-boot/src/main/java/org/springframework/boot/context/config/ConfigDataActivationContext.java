/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.style.ToStringCreator;

/**
 * Context information used when determining when to activate
 * {@link ConfigDataEnvironmentContributor contributed} {@link ConfigData}.
 *
 * @author Phillip Webb
 */
class ConfigDataActivationContext {

	private final @Nullable CloudPlatform cloudPlatform;

	private final @Nullable Profiles profiles;

	/**
	 * Create a new {@link ConfigDataActivationContext} instance before any profiles have
	 * been activated.
	 * @param environment the source environment
	 * @param binder a binder providing access to relevant config data contributions
	 */
	ConfigDataActivationContext(Environment environment, Binder binder) {
		this.cloudPlatform = deduceCloudPlatform(environment, binder);
		this.profiles = null;
	}

	/**
	 * Create a new {@link ConfigDataActivationContext} instance with the given
	 * {@link CloudPlatform} and {@link Profiles}.
	 * @param cloudPlatform the cloud platform
	 * @param profiles the profiles
	 */
	ConfigDataActivationContext(@Nullable CloudPlatform cloudPlatform, @Nullable Profiles profiles) {
		this.cloudPlatform = cloudPlatform;
		this.profiles = profiles;
	}

	private @Nullable CloudPlatform deduceCloudPlatform(Environment environment, Binder binder) {
		for (CloudPlatform candidate : CloudPlatform.values()) {
			if (candidate.isEnforced(binder)) {
				return candidate;
			}
		}
		return CloudPlatform.getActive(environment);
	}

	/**
	 * Return a new {@link ConfigDataActivationContext} with specific profiles.
	 * @param profiles the profiles
	 * @return a new {@link ConfigDataActivationContext} with specific profiles
	 */
	ConfigDataActivationContext withProfiles(Profiles profiles) {
		return new ConfigDataActivationContext(this.cloudPlatform, profiles);
	}

	/**
	 * Return the active {@link CloudPlatform} or {@code null}.
	 * @return the active cloud platform
	 */
	@Nullable CloudPlatform getCloudPlatform() {
		return this.cloudPlatform;
	}

	/**
	 * Return profile information if it is available.
	 * @return profile information or {@code null}
	 */
	@Nullable Profiles getProfiles() {
		return this.profiles;
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("cloudPlatform", this.cloudPlatform);
		creator.append("profiles", this.profiles);
		return creator.toString();
	}

}
