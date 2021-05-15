/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

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
		RestTemplate restTemplate = new RestTemplate(
				Arrays.asList(new MappingJackson2HttpMessageConverter(new ObjectMapper())));
		restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {

			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				request.getHeaders().add("User-Agent", StandardGitHub.this.username);
				request.getHeaders().add("Authorization", "Basic " + Base64.getEncoder().encodeToString(
						(StandardGitHub.this.username + ":" + StandardGitHub.this.password).getBytes()));
				request.getHeaders().add("Accept", MediaType.APPLICATION_JSON_VALUE);
				return execution.execute(request, body);
			}

		});
		UriTemplateHandler uriTemplateHandler = new DefaultUriBuilderFactory(
				"https://api.github.com/repos/" + organization + "/" + name + "/");
		restTemplate.setUriTemplateHandler(uriTemplateHandler);
		return new StandardGitHubRepository(restTemplate);
	}

}
