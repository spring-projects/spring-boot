/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.hypermedia;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(SpringBootHypermediaApplication.class)
@WebAppConfiguration
@TestPropertySource(properties = { "spring.jackson.serialization.indent_output=true",
		"endpoints.health.sensitive=true", "endpoints.actuator.enabled=false" })
@DirtiesContext
public class EndpointDocumentation {

	private static final String RESTDOCS_OUTPUT_DIR = "target/generated-snippets";

	@Rule
	public final RestDocumentation restDocumentation = new RestDocumentation(
			RESTDOCS_OUTPUT_DIR);

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private MvcEndpoints mvcEndpoints;

	@Autowired
	@Qualifier("metricFilter")
	private Filter metricFilter;

	@Autowired
	@Qualifier("webRequestLoggingFilter")
	private Filter traceFilter;

	@Autowired
	private TemplateEngine templates;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.addFilters(this.metricFilter, this.traceFilter)
				.apply(documentationConfiguration(this.restDocumentation)).build();
	}

	@Test
	public void logfile() throws Exception {
		this.mockMvc.perform(get("/logfile").accept(MediaType.TEXT_PLAIN))
				.andExpect(status().isOk()).andDo(document("logfile"));
	}

	@Test
	public void endpoints() throws Exception {

		final File docs = new File("src/main/asciidoc");

		final Map<String, Object> model = new LinkedHashMap<String, Object>();
		final List<EndpointDoc> endpoints = new ArrayList<EndpointDoc>();
		model.put("endpoints", endpoints);
		for (MvcEndpoint endpoint : getEndpoints()) {
			final String endpointPath = StringUtils.hasText(endpoint.getPath())
					? endpoint.getPath() : "/";

			if (!endpointPath.equals("/docs") && !endpointPath.equals("/logfile")) {
				String output = endpointPath.substring(1);
				output = output.length() > 0 ? output : "./";
				this.mockMvc.perform(get(endpointPath).accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andDo(document(output))
						.andDo(new ResultHandler() {
							@Override
							public void handle(MvcResult mvcResult) throws Exception {
								EndpointDoc endpoint = new EndpointDoc(docs,
										endpointPath);
								endpoints.add(endpoint);
							}
						});
			}
		}
		File file = new File(RESTDOCS_OUTPUT_DIR + "/endpoints.adoc");
		file.getParentFile().mkdirs();
		PrintWriter writer = new PrintWriter(file, "UTF-8");
		try {
			Template template = this.templates.createTemplate(
					new File("src/restdoc/resources/templates/endpoints.adoc.tpl"));
			template.make(model).writeTo(writer);
		}
		finally {
			writer.close();
		}
	}

	private Collection<? extends MvcEndpoint> getEndpoints() {
		List<? extends MvcEndpoint> endpoints = new ArrayList<MvcEndpoint>(
				this.mvcEndpoints.getEndpoints());
		Collections.sort(endpoints, new Comparator<MvcEndpoint>() {
			@Override
			public int compare(MvcEndpoint o1, MvcEndpoint o2) {
				return o1.getPath().compareTo(o2.getPath());
			}
		});
		return endpoints;
	}

	public static class EndpointDoc {

		private String path;
		private String custom;
		private String title;

		public EndpointDoc(File rootDir, String path) {
			this.title = path;
			this.path = path.equals("/") ? "" : path;
			String custom = path.substring(1) + ".adoc";
			if (new File(rootDir, custom).exists()) {
				this.custom = custom;
			}
		}

		public String getTitle() {
			return this.title;
		}

		public String getPath() {
			return this.path;
		}

		public String getCustom() {
			return this.custom;
		}

	}

}
