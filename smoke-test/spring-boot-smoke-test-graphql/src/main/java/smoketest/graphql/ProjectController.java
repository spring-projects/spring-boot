/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.graphql;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProjectController {

	private final List<Project> projects;

	public ProjectController() {
		this.projects = Arrays.asList(new Project("spring-boot", "Spring Boot"),
				new Project("spring-graphql", "Spring GraphQL"), new Project("spring-framework", "Spring Framework"));
	}

	@QueryMapping
	public Optional<Project> project(@Argument String slug) {
		return this.projects.stream().filter((project) -> project.getSlug().equals(slug)).findFirst();
	}

}
