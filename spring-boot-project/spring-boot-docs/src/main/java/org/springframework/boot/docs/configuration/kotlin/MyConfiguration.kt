package org.springframework.boot.docs.configuration.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Enabling [ConfigurationProperties] annotated types in [Configuration]
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AcmeProperties::class)
class MyConfiguration
// end::customizer[]
