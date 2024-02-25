/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.data.redis;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/**
 * PersonHash class.
 */
@RedisHash("persons")
public class PersonHash {

	@Id
	private String id;

	private String description;

	/**
     * Returns the ID of the person.
     *
     * @return the ID of the person
     */
    public String getId() {
		return this.id;
	}

	/**
     * Sets the ID of the person.
     * 
     * @param id the ID to set
     */
    public void setId(String id) {
		this.id = id;
	}

	/**
     * Returns the description of the PersonHash object.
     *
     * @return the description of the PersonHash object
     */
    public String getDescription() {
		return this.description;
	}

	/**
     * Sets the description of the person.
     * 
     * @param description the description to be set
     */
    public void setDescription(String description) {
		this.description = description;
	}

}
