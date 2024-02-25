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

package org.springframework.boot.docs.data.sql.jpaandspringdata.entityclasses;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.envers.Audited;

/**
 * Country class.
 */
@Entity
public class Country implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	@Audited
	@Column(nullable = false)
	private String name;

	/**
     * Returns the ID of the country.
     *
     * @return the ID of the country
     */
    public Long getId() {
		return this.id;
	}

	/**
     * Sets the ID of the country.
     * 
     * @param id the ID to set
     */
    public void setId(Long id) {
		this.id = id;
	}

	/**
     * Returns the name of the country.
     *
     * @return the name of the country
     */
    public String getName() {
		return this.name;
	}

	/**
     * Sets the name of the country.
     * 
     * @param name the name of the country
     */
    public void setName(String name) {
		this.name = name;
	}

}
