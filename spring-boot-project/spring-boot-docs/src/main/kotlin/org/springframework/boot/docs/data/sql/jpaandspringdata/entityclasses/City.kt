/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.data.sql.jpaandspringdata.entityclasses

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import java.io.Serializable

@Entity
class City : Serializable {

	@Id
	@GeneratedValue
	private val id: Long? = null

	@Column(nullable = false)
	var name: String? = null
		private set

	// ... etc
	@Column(nullable = false)
	var state: String? = null
		private set

	// ... additional members, often include @OneToMany mappings

	protected constructor() {
		// no-args constructor required by JPA spec
		// this one is protected since it should not be used directly
	}

	constructor(name: String?, state: String?) {
		this.name = name
		this.state = state
	}

}