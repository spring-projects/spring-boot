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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * All {@link BuildpackResolver} instances that can be used to resolve
 * {@link BuildpackReference BuildpackReferences}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class BuildpackResolvers {

	private static final List<BuildpackResolver> resolvers = getResolvers();

	private BuildpackResolvers() {
	}

	private static List<BuildpackResolver> getResolvers() {
		List<BuildpackResolver> resolvers = new ArrayList<>();
		resolvers.add(BuilderBuildpack::resolve);
		resolvers.add(DirectoryBuildpack::resolve);
		resolvers.add(TarGzipBuildpack::resolve);
		resolvers.add(ImageBuildpack::resolve);
		return Collections.unmodifiableList(resolvers);
	}

	/**
	 * Resolve a collection of {@link BuildpackReference BuildpackReferences} to a
	 * {@link Buildpacks} instance.
	 * @param context the resolver context
	 * @param references the references to resolve
	 * @return a {@link Buildpacks} instance
	 */
	static Buildpacks resolveAll(BuildpackResolverContext context, Collection<BuildpackReference> references) {
		Assert.notNull(context, "Context must not be null");
		if (CollectionUtils.isEmpty(references)) {
			return Buildpacks.EMPTY;
		}
		List<Buildpack> buildpacks = new ArrayList<>(references.size());
		for (BuildpackReference reference : references) {
			buildpacks.add(resolve(context, reference));
		}
		return Buildpacks.of(buildpacks);
	}

	private static Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
		Assert.notNull(reference, "Reference must not be null");
		for (BuildpackResolver resolver : resolvers) {
			Buildpack buildpack = resolver.resolve(context, reference);
			if (buildpack != null) {
				return buildpack;
			}
		}
		throw new IllegalArgumentException("Invalid buildpack reference '" + reference + "'");
	}

}
