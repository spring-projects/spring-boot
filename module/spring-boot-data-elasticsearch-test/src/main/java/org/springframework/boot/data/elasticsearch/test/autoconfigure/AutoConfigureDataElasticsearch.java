/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} for typical Data
 * Elasticsearch tests. Most tests should consider using
 * {@link DataElasticsearchTest @DataElasticsearchTest} rather than using this annotation
 * directly.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 * @see DataElasticsearchTest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureDataElasticsearch {

}
