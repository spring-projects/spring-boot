/*
 * Copyright 2021-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.tasks.bundling;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import org.springframework.boot.buildpack.platform.build.Cache;

/**
 * Configuration for an image building cache.
 *
 * @author Scott Frederick
 * @since 2.6.0
 */
public class CacheSpec {

	private final ObjectFactory objectFactory;

	private Cache cache = null;

	@Inject
	public CacheSpec(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	public Cache asCache() {
		return this.cache;
	}

	/**
	 * Configures a volume cache using the given {@code action}.
	 * @param action the action
	 */
	public void volume(Action<VolumeCacheSpec> action) {
		if (this.cache != null) {
			throw new GradleException("Each image building cache can be configured only once");
		}
		VolumeCacheSpec spec = this.objectFactory.newInstance(VolumeCacheSpec.class);
		action.execute(spec);
		this.cache = Cache.volume(spec.getName().get());
	}

	/**
	 * Configuration for an image building cache stored in a Docker volume.
	 */
	public abstract static class VolumeCacheSpec {

		/**
		 * Returns the name of the cache.
		 * @return the cache name
		 */
		@Input
		public abstract Property<String> getName();

	}

}
