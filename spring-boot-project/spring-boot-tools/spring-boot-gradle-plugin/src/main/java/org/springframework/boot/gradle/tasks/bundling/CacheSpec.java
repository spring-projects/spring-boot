/*
 * Copyright 2021-2021 the original author or authors.
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
import org.gradle.api.tasks.Input;

import org.springframework.boot.buildpack.platform.build.Cache;

/**
 * Configuration for an image building cache.
 *
 * @author Scott Frederick
 * @since 2.6.0
 */
public class CacheSpec {

	private Cache cache = null;

	@Inject
	public CacheSpec() {

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
		VolumeCacheSpec spec = new VolumeCacheSpec();
		action.execute(spec);
		this.cache = Cache.volume(spec.getName());
	}

	/**
	 * Configuration for an image building cache stored in a Docker volume.
	 */
	public static class VolumeCacheSpec {

		private String name;

		/**
		 * Returns the name of the cache.
		 * @return the cache name
		 */
		@Input
		public String getName() {
			return this.name;
		}

		/**
		 * Sets the name of the cache.
		 * @param name the cache name
		 */
		public void setName(String name) {
			this.name = name;
		}

	}

}
