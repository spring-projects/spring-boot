/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.bom.bomr.github;

import java.util.Base64;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * Standard implementation of {@link GitHub}.
 *
 * @author Andy Wilkinson
 */
final class StandardGitHub implements GitHub {

	private final String username;

	private final String password;

	StandardGitHub(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public GitHubRepository getRepository(String organization, String name) {
		return new StandardGitHubRepository(createRestClient(organization, name));
	}

	private RestClient createRestClient(String organization, String name) {
		return RestClient.builder()
			.baseUrl("https://api.github.com/repos/" + organization + "/" + name + "/")
			.configureMessageConverters((converters) -> converters.disableDefaults()
				.withJsonConverter(new JacksonJsonHttpMessageConverter()))
			.requestInterceptor((request, body, execution) -> {
				request.getHeaders().add("User-Agent", StandardGitHub.this.username);
				request.getHeaders()
					.add("Authorization", "Basic " + Base64.getEncoder()
						.encodeToString(
								(StandardGitHub.this.username + ":" + StandardGitHub.this.password).getBytes()));
				request.getHeaders().add("Accept", MediaType.APPLICATION_JSON_VALUE);
				return execution.execute(request, body);
			})
			.build();
	}

}
