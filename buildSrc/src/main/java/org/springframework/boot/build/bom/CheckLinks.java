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

package org.springframework.boot.build.bom;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.org.apache.http.client.config.CookieSpecs;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;

/**
 * Task to check that links are working.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public abstract class CheckLinks extends DefaultTask {

	private static final ErrorHandler NOOP_ERROR_HANDLER = (request, response) -> {
	};

	private final BomExtension bom;

	@Inject
	public CheckLinks(BomExtension bom) {
		this.bom = bom;
	}

	@TaskAction
	void releaseNotes() {
		RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		RestClient restClient = RestClient.builder()
			.requestFactory(requestFactory)
			.defaultStatusHandler((status) -> true, NOOP_ERROR_HANDLER)
			.build();
		for (Library library : this.bom.getLibraries()) {
			library.getLinks().forEach((name, links) -> links.forEach((link) -> {
				URI uri;
				try {
					uri = new URI(link.url(library));
					ResponseEntity<String> response = restClient.head().uri(uri).retrieve().toEntity(String.class);
					System.out.printf("[%3d] %s - %s (%s)%n", response.getStatusCode().value(), library.getName(), name,
							uri);
				}
				catch (URISyntaxException ex) {
					throw new RuntimeException(ex);
				}
			}));
		}
	}

}
