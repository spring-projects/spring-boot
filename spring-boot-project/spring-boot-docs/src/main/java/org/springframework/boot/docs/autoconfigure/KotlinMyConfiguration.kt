package org.springframework.boot.docs.autoconfigure

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

/**
 * Sample class showing how to disable specific Auto-configuration Classes
 */
// tag::customizer[]
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
class KotlinMyConfiguration
// end::customizer[]
