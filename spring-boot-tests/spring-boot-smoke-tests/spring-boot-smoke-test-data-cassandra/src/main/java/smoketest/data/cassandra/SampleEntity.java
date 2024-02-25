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

package smoketest.data.cassandra;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * SampleEntity class.
 */
@Table
public class SampleEntity {

	@PrimaryKey
	private String id;

	private String description;

	/**
	 * Returns the ID of the SampleEntity.
	 * @return the ID of the SampleEntity
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets the ID of the SampleEntity.
	 * @param id the ID to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the description of the SampleEntity.
	 * @return the description of the SampleEntity
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the description of the SampleEntity.
	 * @param description the new description to be set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

}
