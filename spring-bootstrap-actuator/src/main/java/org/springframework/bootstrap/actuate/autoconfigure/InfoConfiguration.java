/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.autoconfigure;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.servlet.Servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.bootstrap.actuate.info.InfoEndpoint;
import org.springframework.bootstrap.bind.PropertiesConfigurationFactory;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for /info endpoint.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnMissingBean({ InfoEndpoint.class })
public class InfoConfiguration {

	@Resource(name = "infoMap")
	private Map<String, Object> infoMap;

	@Autowired
	@Qualifier("gitInfo")
	private GitInfo gitInfo;

	@Bean
	public Map<String, Object> applicationInfo() {
		LinkedHashMap<String, Object> info = new LinkedHashMap<String, Object>();
		info.putAll(this.infoMap);
		if (this.gitInfo.getBranch() != null) {
			info.put("git", this.gitInfo);
		}
		return info;
	}

	@Bean
	public InfoEndpoint infoEndpoint() {
		return new InfoEndpoint(applicationInfo());
	}

	@Configuration
	public static class InfoPropertiesConfiguration {

		@Autowired
		private ConfigurableEnvironment environment = new StandardEnvironment();

		@Bean
		public PropertiesConfigurationFactory<GitInfo> gitInfo() throws IOException {
			PropertiesConfigurationFactory<GitInfo> factory = new PropertiesConfigurationFactory<GitInfo>(
					new GitInfo());
			factory.setTargetName("git");
			Properties properties = new Properties();
			if (new ClassPathResource("git.properties").exists()) {
				properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource(
						"git.properties"));
			}
			factory.setProperties(properties);
			return factory;
		}

		@Bean
		public PropertiesConfigurationFactory<Map<String, Object>> infoMap() {
			PropertiesConfigurationFactory<Map<String, Object>> factory = new PropertiesConfigurationFactory<Map<String, Object>>(
					new LinkedHashMap<String, Object>());
			factory.setTargetName("info");
			factory.setPropertySources(this.environment.getPropertySources());
			return factory;
		}

	}

	public static class GitInfo {
		private String branch;
		private Commit commit = new Commit();

		public String getBranch() {
			return this.branch;
		}

		public void setBranch(String branch) {
			this.branch = branch;
		}

		public Commit getCommit() {
			return this.commit;
		}

		public static class Commit {
			private String id;
			private String time;

			public String getId() {
				return this.id == null ? "" : (this.id.length() > 7 ? this.id.substring(
						0, 7) : this.id);
			}

			public void setId(String id) {
				this.id = id;
			}

			public String getTime() {
				return this.time;
			}

			public void setTime(String time) {
				this.time = time;
			}
		}
	}
}
