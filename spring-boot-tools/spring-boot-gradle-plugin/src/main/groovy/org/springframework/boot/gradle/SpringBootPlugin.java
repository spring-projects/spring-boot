/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.springframework.boot.gradle.task.Repackage;
import org.springframework.boot.gradle.task.RunJar;

/**
 * Gradle 'Spring Boot' {@link Plugin}.
 * 
 * @author Phillip Webb
 */
public class SpringBootPlugin implements Plugin<Project> {

    private static final String REPACKAGE_TASK_NAME = "repackage";
    private static final String RUN_JAR_TASK_NAME = "runJar";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(BasePlugin.class);
        project.getPlugins().apply(JavaPlugin.class);
        project.getExtensions().create("springBoot", SpringBootPluginExtension.class);
        Repackage packageTask = addRepackageTask(project);
        ensureTaskRunsOnAssembly(project, packageTask);
        addRunJarTask(project);
    }

    private void addRunJarTask(Project project) {
        RunJar runJarTask = project.getTasks().create(RUN_JAR_TASK_NAME, RunJar.class);
        runJarTask.setDescription("Run the executable JAR/WAR");
        runJarTask.setGroup("Execution");
        runJarTask.dependsOn(REPACKAGE_TASK_NAME);
    }

    private Repackage addRepackageTask(Project project) {
        Repackage packageTask = project.getTasks().create(REPACKAGE_TASK_NAME, Repackage.class);
        packageTask.setDescription("Repackage existing JAR and WAR "
                + "archives so that they can be executed from the command " + "line using 'java -jar'");
        packageTask.setGroup(BasePlugin.BUILD_GROUP);
        packageTask.dependsOn(project.getConfigurations().getByName(Dependency.ARCHIVES_CONFIGURATION)
                .getAllArtifacts().getBuildDependencies());
        return packageTask;
    }

    private void ensureTaskRunsOnAssembly(Project project, Repackage task) {
        project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(task);
    }
}
