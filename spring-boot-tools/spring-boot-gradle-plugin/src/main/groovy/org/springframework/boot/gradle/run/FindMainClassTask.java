package org.springframework.boot.gradle.run;

import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.springframework.boot.gradle.SpringBootPluginExtension;
import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Task to find and set the 'mainClassName' convention when it's missing by searching the
 * main source code.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class FindMainClassTask extends DefaultTask {

	@TaskAction
	public void setMainClassNameProperty() {
		Project project = getProject();
		if (project.property("mainClassName") == null) {
			project.setProperty("mainClassName", findMainClass());
		}
	}

	private String findMainClass() {
		Project project = getProject();

		// Try the SpringBoot extension setting
		SpringBootPluginExtension bootExtension = project.getExtensions().getByType(
				SpringBootPluginExtension.class);
		if(bootExtension.getMainClass() != null) {
			return bootExtension.getMainClass();
		}

		// Search
		SourceSet mainSourceSet = SourceSets.findMainSourceSet(project);
		if (mainSourceSet == null) {
			return null;
		}
		project.getLogger().debug(
				"Looking for main in: " + mainSourceSet.getOutput().getClassesDir());
		try {
			String mainClass = MainClassFinder.findSingleMainClass(mainSourceSet
					.getOutput().getClassesDir());
			project.getLogger().info("Computed main class: " + mainClass);
			return mainClass;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot find main class", ex);
		}
	}
}
