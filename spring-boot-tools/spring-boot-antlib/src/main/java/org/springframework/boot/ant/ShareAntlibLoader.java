package org.springframework.boot.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.springframework.util.StringUtils;

/**
 * Quiet task that establishes a reference to its loader.
 */
public class ShareAntlibLoader extends Task {
	private String refid;

	public ShareAntlibLoader(Project project) {
		super();
		setProject(project);
	}

	@Override
	public void execute() throws BuildException {
		if (!StringUtils.hasText(refid)) {
			throw new BuildException("@refid has no text");
		}
		getProject().addReference(refid, getClass().getClassLoader());
	}

	public void setRefid(String refid) {
		this.refid = refid;
	}

}
