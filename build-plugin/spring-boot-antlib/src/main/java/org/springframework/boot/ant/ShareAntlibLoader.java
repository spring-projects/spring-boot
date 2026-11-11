/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * Quiet task that establishes a reference to its loader.
 *
 * @author Matt Benson
 * @since 1.3.0
 */
public class ShareAntlibLoader extends Task {

	private @Nullable String refid;

	public ShareAntlibLoader(Project project) {
		setProject(project);
	}

	@Override
	public void execute() throws BuildException {
		if (!StringUtils.hasText(this.refid)) {
			throw new BuildException("@refid has no text");
		}
		getProject().addReference(this.refid, getClass().getClassLoader());
	}

	public void setRefid(@Nullable String refid) {
		this.refid = refid;
	}

}
