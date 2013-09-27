package org.springframework.boot.gradle.task;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.process.internal.DefaultExecAction;
import org.gradle.process.internal.ExecAction;

public class Run extends DefaultTask {

    private File file;

    @TaskAction
    public void run() {
        Project project = getProject();
        project.getTasks().withType(Jar.class, new Action<Jar>() {
            @Override
            public void execute(Jar archive) {
                file = archive.getArchivePath();
            }
        });
        if (file != null && file.exists()) {
            ExecAction action = new DefaultExecAction(getServices().get(FileResolver.class));
            action.setExecutable(System.getProperty("java.home") + "/bin/java");
            action.args("-jar", file);
            action.execute();
        }
    }
}
