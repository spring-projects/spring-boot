/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Settings to apply when configuring Hibernate.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class HibernateSettings {

	private Supplier<String> ddlAuto;

	private Collection<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;

	/**
     * Sets the DDL auto setting for Hibernate.
     * 
     * @param ddlAuto the supplier that provides the DDL auto setting
     * @return the updated HibernateSettings object
     */
    public HibernateSettings ddlAuto(Supplier<String> ddlAuto) {
		this.ddlAuto = ddlAuto;
		return this;
	}

	/**
     * Returns the value of ddlAuto.
     * 
     * @return the value of ddlAuto, or null if ddlAuto is not set
     */
    public String getDdlAuto() {
		return (this.ddlAuto != null) ? this.ddlAuto.get() : null;
	}

	/**
     * Sets the list of HibernatePropertiesCustomizer instances to customize the Hibernate properties.
     * 
     * @param hibernatePropertiesCustomizers the collection of HibernatePropertiesCustomizer instances
     * @return the HibernateSettings instance
     */
    public HibernateSettings hibernatePropertiesCustomizers(
			Collection<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
		this.hibernatePropertiesCustomizers = new ArrayList<>(hibernatePropertiesCustomizers);
		return this;
	}

	/**
     * Returns the collection of HibernatePropertiesCustomizer objects.
     *
     * @return the collection of HibernatePropertiesCustomizer objects
     */
    public Collection<HibernatePropertiesCustomizer> getHibernatePropertiesCustomizers() {
		return this.hibernatePropertiesCustomizers;
	}

}
