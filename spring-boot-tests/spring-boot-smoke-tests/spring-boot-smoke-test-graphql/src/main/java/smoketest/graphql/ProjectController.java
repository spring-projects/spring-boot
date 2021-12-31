/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
