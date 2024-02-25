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

package smoketest.graphql;

import java.util.Objects;

/**
 * Project class.
 */
public class Project {

	private String slug;

	private String name;

	/**
     * Constructs a new Project object with the specified slug and name.
     * 
     * @param slug the slug of the project
     * @param name the name of the project
     */
    public Project(String slug, String name) {
		this.slug = slug;
		this.name = name;
	}

	/**
     * Returns the slug of the Project.
     *
     * @return the slug of the Project
     */
    public String getSlug() {
		return this.slug;
	}

	/**
     * Sets the slug for the project.
     * 
     * @param slug the slug to be set
     */
    public void setSlug(String slug) {
		this.slug = slug;
	}

	/**
     * Returns the name of the Project.
     *
     * @return the name of the Project
     */
    public String getName() {
		return this.name;
	}

	/**
     * Sets the name of the project.
     * 
     * @param name the name of the project
     */
    public void setName(String name) {
		this.name = name;
	}

	/**
     * Compares this Project object to the specified object for equality.
     * Returns true if the specified object is also a Project object and
     * has the same slug as this Project object.
     *
     * @param o the object to be compared for equality with this Project object
     * @return true if the specified object is equal to this Project object, false otherwise
     */
    @Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Project project = (Project) o;
		return this.slug.equals(project.slug);
	}

	/**
     * Returns the hash code value for the Project object.
     * 
     * @return the hash code value based on the slug of the Project
     */
    @Override
	public int hashCode() {
		return Objects.hash(this.slug);
	}

}
