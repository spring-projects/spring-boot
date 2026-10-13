/*
 * Copyright 2012-present the original author or authors.
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
