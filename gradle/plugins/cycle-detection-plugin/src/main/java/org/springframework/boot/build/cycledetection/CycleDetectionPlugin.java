/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.cycledetection;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.initialization.Settings;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * A {@link Settings} {@link Plugin plugin} to detect cycles between a build's projects.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class CycleDetectionPlugin implements Plugin<Settings> {

	private static final Pattern layerPattern = Pattern.compile("^:(.+?):.*");

	@Override
	public void apply(Settings settings) {
		settings.getGradle().getTaskGraph().whenReady(this::detectCycles);
	}

	private void detectCycles(TaskExecutionGraph taskGraph) {
		Map<Project, Set<Project>> dependenciesByProject = getProjectsAndDependencies(taskGraph);
		assertNoCycles(createGraph(dependenciesByProject, Project::getPath), "Project cycles detected:\n");
		assertNoCycles(createGraph(dependenciesByProject, this::getLayer), "Layer cycles detected:\n");
	}

	private Map<Project, Set<Project>> getProjectsAndDependencies(TaskExecutionGraph taskGraph) {
		Map<Project, Set<Project>> dependenciesByProject = new HashMap<>();
		for (Task task : taskGraph.getAllTasks()) {
			Project project = task.getProject();
			Set<Project> dependencies = dependenciesByProject.computeIfAbsent(project, (key) -> new LinkedHashSet<>());
			taskGraph.getDependencies(task)
				.stream()
				.map(Task::getProject)
				.filter((taskProject) -> !taskProject.equals(project))
				.forEach(dependencies::add);
		}
		return dependenciesByProject;
	}

	private Graph<String, DefaultEdge> createGraph(Map<Project, Set<Project>> dependenciesByProject,
			Function<Project, String> vertexExtractor) {
		Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
		dependenciesByProject.keySet().stream().map(vertexExtractor).filter(Objects::nonNull).forEach(graph::addVertex);
		dependenciesByProject.forEach((project, dependencies) -> {
			String source = vertexExtractor.apply(project);
			dependencies.stream().map(vertexExtractor).filter(Objects::nonNull).forEach((target) -> {
				if (source != null && !Objects.equals(source, target)) {
					graph.addEdge(source, target);
				}
			});
		});
		return graph;
	}

	private String getLayer(Project project1) {
		Matcher matcher = layerPattern.matcher(project1.getPath());
		return (matcher.matches()) ? matcher.group(1) : null;
	}

	private void assertNoCycles(Graph<String, DefaultEdge> projects, String str) {
		List<List<String>> cycles = new TarjanSimpleCycles<>(projects).findSimpleCycles();
		if (!cycles.isEmpty()) {
			StringBuilder message = new StringBuilder(str);
			for (List<String> cycle : cycles) {
				cycle.add(cycle.get(0));
				message.append("  " + String.join(" -> ", cycle) + "\n");
			}
			throw new GradleException(message.toString());
		}
	}

}
