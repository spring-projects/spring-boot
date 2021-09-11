/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

/**
 * Strategy interface used to resolve a {@link BuildpackReference} to a {@link Buildpack}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @see BuildpackResolvers
 */
interface BuildpackResolver {

	/**
	 * Attempt to resolve the given {@link BuildpackReference}.
	 * @param context the resolver context
	 * @param reference the reference to resolve
	 * @return a resolved {@link Buildpack} instance or {@code null}
	 */
	Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference);

}
