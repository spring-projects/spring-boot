package org.springframework.boot.docs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

/**
 * Enabling [ConfigurationProperties] annotated types in [SpringBootApplication]
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
@SpringBootApplication
@ConfigurationPropertiesScan("com.example.app", "org.acme.another")
class MyApplication
// end::customizer[]
