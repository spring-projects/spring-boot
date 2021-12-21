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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest(ProjectsController.class)
class ProjectControllerTests {

	@Autowired
	private GraphQlTester graphQlTester;

	@Test
	void shouldFindSpringGraphQl() {
		this.graphQlTester.query("{ project(slug: \"spring-graphql\") { name } }").execute().path("project.name")
				.entity(String.class).isEqualTo("Spring GraphQL");
	}

	@Test
	void shouldNotFindUnknownProject() {
		this.graphQlTester.query("{ project(slug: \"spring-unknown\") { name } }").execute().path("project.name")
				.valueDoesNotExist();
	}

}
