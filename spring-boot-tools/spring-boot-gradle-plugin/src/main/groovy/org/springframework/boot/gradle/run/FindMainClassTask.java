
package org.springframework.boot.gradle.run;

import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ApplicationPluginConvention;
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

		String mainClass = null;

		// Try the SpringBoot extension setting
		SpringBootPluginExtension bootExtension = project.getExtensions().getByType(
				SpringBootPluginExtension.class);
		if (bootExtension.getMainClass() != null) {
			mainClass = bootExtension.getMainClass();
		}

		ApplicationPluginConvention application = (ApplicationPluginConvention) project.getConvention().getPlugins().get(
				"application");
		// Try the Application extension setting
		if (mainClass == null && application.getMainClassName() != null) {
			mainClass = application.getMainClassName();
		}

		Task runTask = getProject().getTasks().getByName("run");
		if (mainClass == null && runTask.hasProperty("main")) {
			mainClass = (String) runTask.property("main");
		}

		if (mainClass == null) {
			// Search
			SourceSet mainSourceSet = SourceSets.findMainSourceSet(project);
			if (mainSourceSet != null) {
				project.getLogger().debug(
						"Looking for main in: "
								+ mainSourceSet.getOutput().getClassesDir());
				try {
					mainClass = MainClassFinder.findSingleMainClass(mainSourceSet.getOutput().getClassesDir());
					project.getLogger().info("Computed main class: " + mainClass);
				} catch (IOException ex) {
					throw new IllegalStateException("Cannot find main class", ex);
				}
			}
		}

		project.getLogger().info("Found main: " + mainClass);

		if (bootExtension.getMainClass() == null) {
			bootExtension.setMainClass(mainClass);
		}
		if (application.getMainClassName() == null) {
			application.setMainClassName(mainClass);
		}
		if (!runTask.hasProperty("main")) {
			runTask.setProperty("main", mainClass);
		}

		return mainClass;
	}
}
