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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * {@link MvcEndpoint} to expose HAL-formatted JSON.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@ConfigurationProperties("endpoints.actuator")
public class HalJsonMvcEndpoint extends AbstractMvcEndpoint {
	private final ManagementServletContext managementServletContext;

	public HalJsonMvcEndpoint(ManagementServletContext managementServletContext) {
		super(getDefaultPath(managementServletContext), false);
		this.managementServletContext = managementServletContext;
	}

	private static String getDefaultPath(
			ManagementServletContext managementServletContext) {
		if (StringUtils.hasText(managementServletContext.getContextPath())) {
			return "";
		}
		return "/actuator";
	}

	@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResourceSupport links() {
		return new ResourceSupport();
	}

	protected final ManagementServletContext getManagementServletContext() {
		return this.managementServletContext;
	}
}
