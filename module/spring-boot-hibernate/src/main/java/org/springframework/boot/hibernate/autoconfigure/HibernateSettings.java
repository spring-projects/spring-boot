/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Settings to apply when configuring Hibernate.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class HibernateSettings {

	private @Nullable Supplier<String> ddlAuto;

	private @Nullable Collection<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;

	public HibernateSettings ddlAuto(@Nullable Supplier<String> ddlAuto) {
		this.ddlAuto = ddlAuto;
		return this;
	}

	public @Nullable String getDdlAuto() {
		return (this.ddlAuto != null) ? this.ddlAuto.get() : null;
	}

	public HibernateSettings hibernatePropertiesCustomizers(
			Collection<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
		this.hibernatePropertiesCustomizers = new ArrayList<>(hibernatePropertiesCustomizers);
		return this;
	}

	public @Nullable Collection<HibernatePropertiesCustomizer> getHibernatePropertiesCustomizers() {
		return this.hibernatePropertiesCustomizers;
	}

}
