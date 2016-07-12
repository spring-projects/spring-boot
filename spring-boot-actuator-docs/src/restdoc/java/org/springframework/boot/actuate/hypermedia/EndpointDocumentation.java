/*
 * Copyright 2012-2016 the original author or authors.
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
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SpringBootHypermediaApplication.class, loader = SpringBootContextLoader.class)
@WebAppConfiguration
@TestPropertySource(properties = { "spring.jackson.serialization.indent_output=true",
		"endpoints.health.sensitive=true", "endpoints.actuator.enabled=false" })
@DirtiesContext
@AutoConfigureRestDocs(EndpointDocumentation.RESTDOCS_OUTPUT_DIR)
@AutoConfigureMockMvc(alwaysPrint = false)
public class EndpointDocumentation {

	static final String RESTDOCS_OUTPUT_DIR = "target/generated-snippets";

	static final File LOG_FILE = new File("target/logs/spring.log");

	private static final Set<String> SKIPPED = Collections.<String>unmodifiableSet(
			new HashSet<String>(Arrays.asList("/docs", "/logfile", "/heapdump")));

	@Autowired
	private MvcEndpoints mvcEndpoints;

	@Autowired
	private TemplateEngine templates;

	@Autowired
	private MockMvc mockMvc;

	@BeforeClass
	public static void clearLog() {
		LOG_FILE.delete();
	}

	@Test
	public void logfile() throws Exception {
		this.mockMvc.perform(get("/logfile").accept(MediaType.TEXT_PLAIN))
				.andExpect(status().isOk()).andDo(document("logfile"));
	}

	@Test
	public void partialLogfile() throws Exception {
		FileCopyUtils.copy(getClass().getResourceAsStream("log.txt"),
				new FileOutputStream(LOG_FILE));
		this.mockMvc
				.perform(get("/logfile").accept(MediaType.TEXT_PLAIN)
						.header(HttpHeaders.RANGE, "bytes=0-1024"))
				.andExpect(status().isPartialContent())
				.andDo(document("partial-logfile"));
	}

	@Test
	public void endpoints() throws Exception {
		final File docs = new File("src/main/asciidoc");
		final Map<String, Object> model = new LinkedHashMap<String, Object>();
		final List<EndpointDoc> endpoints = new ArrayList<EndpointDoc>();
		model.put("endpoints", endpoints);
		for (MvcEndpoint endpoint : getEndpoints()) {
			final String endpointPath = (StringUtils.hasText(endpoint.getPath())
					? endpoint.getPath() : "/");
			if (!SKIPPED.contains(endpointPath)) {
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
