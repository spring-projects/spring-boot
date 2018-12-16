/*
 * Copyright 2012-2018 the original author or authors.
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

package io.virtualan.mapping.to;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A pet for sale in the pet store.
 */

public class Pet {

	@JsonProperty("id")
	private Long id = null;

	@JsonProperty("category")
	private Category category = null;

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("photoUrls")
	@Valid
	private List<String> photoUrls = new ArrayList<>();

	@JsonProperty("tags")
	@Valid
	private List<Tag> tags = null;

	@JsonProperty("status")
	private StatusEnum status = null;

	public Pet id(Long id) {
		this.id = id;
		return this;
	}

	/**
	 * Get id.
	 * @return id
	 **/

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Pet category(Category category) {
		this.category = category;
		return this;
	}

	/**
	 * Get category.
	 * @return category
	 **/
	@Valid

	public Category getCategory() {
		return this.category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public Pet name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Get name.
	 * @return name
	 **/
	@NotNull

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Pet photoUrls(List<String> photoUrls) {
		this.photoUrls = photoUrls;
		return this;
	}

	public Pet addPhotoUrlsItem(String photoUrlsItem) {
		this.photoUrls.add(photoUrlsItem);
		return this;
	}

	/**
	 * Get photoUrls.
	 * @return photoUrls
	 **/
	@NotNull

	public List<String> getPhotoUrls() {
		return this.photoUrls;
	}

	public void setPhotoUrls(List<String> photoUrls) {
		this.photoUrls = photoUrls;
	}

	public Pet tags(List<Tag> tags) {
		this.tags = tags;
		return this;
	}

	public Pet addTagsItem(Tag tagsItem) {
		if (this.tags == null) {
			this.tags = new ArrayList<>();
		}
		this.tags.add(tagsItem);
		return this;
	}

	/**
	 * Get tags.
	 * @return tags.
	 **/

	@Valid

	public List<Tag> getTags() {
		return this.tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public Pet status(StatusEnum status) {
		this.status = status;
		return this;
	}

	/**
	 * pet status in the store.
	 * @return status
	 **/

	public StatusEnum getStatus() {
		return this.status;
	}

	public void setStatus(StatusEnum status) {
		this.status = status;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Pet pet = (Pet) o;
		return Objects.equals(this.id, pet.id)
				&& Objects.equals(this.category, pet.category)
				&& Objects.equals(this.name, pet.name)
				&& Objects.equals(this.photoUrls, pet.photoUrls)
				&& Objects.equals(this.tags, pet.tags)
				&& Objects.equals(this.status, pet.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.category, this.name, this.photoUrls, this.tags,
				this.status);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Pet {\n");

		sb.append("    id: ").append(toIndentedString(this.id)).append("\n");
		sb.append("    category: ").append(toIndentedString(this.category)).append("\n");
		sb.append("    name: ").append(toIndentedString(this.name)).append("\n");
		sb.append("    photoUrls: ").append(toIndentedString(this.photoUrls))
				.append("\n");
		sb.append("    tags: ").append(toIndentedString(this.tags)).append("\n");
		sb.append("    status: ").append(toIndentedString(this.status)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}

}
