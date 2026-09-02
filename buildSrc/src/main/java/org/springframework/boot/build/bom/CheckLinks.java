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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.org.apache.http.client.config.CookieSpecs;

import org.springframework.boot.build.bom.Library.Link;
import org.springframework.boot.build.bom.Library.LinkType;
import org.springframework.boot.build.bom.Library.LinkedVersion;
import org.springframework.boot.build.bom.ResolvedBom.ResolvedLibrary;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
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

	private final Configuration resolvedBom;

	@Inject
	public CheckLinks(BomExtension bom, Configuration resolvedBom) {
		this.bom = bom;
		this.resolvedBom = resolvedBom;
	}

	@TaskAction
	void check() {
		RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		RestClient restClient = RestClient.builder()
			.requestFactory(requestFactory)
			.defaultStatusHandler((status) -> true, NOOP_ERROR_HANDLER)
			.build();
		this.bom.getLibraries().forEach((library) -> check(restClient, library));
	}

	private void check(RestClient restClient, Library library) {
		ResolvedBom resolvedBom = ResolvedBom.readFrom(this.resolvedBom.getSingleFile());
		ResolvedLibrary resolvedLibrary = resolvedBom.library(library);
		DependencyVersion libraryVersion = library.getVersion();
		String libraryName = library.getName();
		library.getLinks().forEachLink((type, link) -> check(restClient, type, link, libraryName, libraryVersion));
		library.getModuleLinks().forEach((module, links) -> {
			String moduleName = "%s (%s)".formatted(library.getName(), module);
			String moduleVersion = resolvedLibrary.module(module).version();
			links.forEachLink((type, link) -> check(restClient, type, link, moduleName, moduleVersion));
		});
	}

	private void check(RestClient restClient, LinkType type, Link link, String name, Object version) {
		try {
			URI uri = new URI(link.url(new LinkedVersion(version)));
			ResponseEntity<String> response = restClient.head().uri(uri).retrieve().toEntity(String.class);
			int statusCode = response.getStatusCode().value();
			System.out.printf("[%3d] %s - %s (%s)%n", statusCode, name, type, uri);
		}
		catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

}
