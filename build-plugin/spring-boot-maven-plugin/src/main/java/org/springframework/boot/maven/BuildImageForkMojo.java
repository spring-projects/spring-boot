/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import javax.inject.Inject;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Package an application into an OCI image using a buildpack, forking the lifecycle to
 * make sure that {@code package} ran. This goal is suitable for command-line invocation.
 * If you need to configure a goal {@code execution} in your build, use
 * {@code build-image-no-fork} instead.
 *
 * @author Stephane Nicoll
 * @since 3.0.0
 */
@Mojo(name = "build-image", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
		requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class BuildImageForkMojo extends BuildImageMojo {

	@Inject
	public BuildImageForkMojo(MavenProjectHelper projectHelper) {
		super(projectHelper);
	}

}
