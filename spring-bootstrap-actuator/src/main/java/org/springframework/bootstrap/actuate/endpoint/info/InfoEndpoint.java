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

package org.springframework.bootstrap.actuate.endpoint.info;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Dave Syer
 */
@Controller
public class InfoEndpoint {

	private Map<String, Object> info;

	/**
	 * @param info
	 */
	public InfoEndpoint(Map<String, Object> info) {
		this.info = new LinkedHashMap<String, Object>(info);
		this.info.putAll(getAdditionalInfo());
	}

	@RequestMapping("${endpoints.info.path:/info}")
	@ResponseBody
	public Map<String, Object> info() {
		return this.info;
	}

	protected Map<String, Object> getAdditionalInfo() {
		return Collections.emptyMap();
	}

}
