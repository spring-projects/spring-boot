/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.maven;

import javax.inject.Inject;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Package an application into an OCI image using a buildpack, but without forking the
 * lifecycle. This goal should be used when configuring a goal {@code execution} in your
 * build. To invoke the goal on the command-line, use {@code build-image} instead.
 *
 * @author Stephane Nicoll
 * @since 3.0.0
 */
@Mojo(name = "build-image-no-fork", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
		requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildImageNoForkMojo extends BuildImageMojo {

	@Inject
	public BuildImageNoForkMojo(MavenProjectHelper projectHelper) {
		super(projectHelper);
	}

}
