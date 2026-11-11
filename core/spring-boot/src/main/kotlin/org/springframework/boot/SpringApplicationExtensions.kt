/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.Assert
import org.springframework.util.ClassUtils
import org.springframework.util.ReflectionUtils
import kotlin.reflect.KClass

/**
 * Top-level function acting as a Kotlin shortcut allowing to write
 * `runApplication<MyApplication>(arg1, arg2)` instead of
 * `SpringApplication.run(MyApplication::class.java, arg1, arg2)`.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
inline fun <reified T : Any> runApplication(vararg args: String): ConfigurableApplicationContext =
		SpringApplication.run(T::class.java, *args)

/**
 * Top-level function acting as a Kotlin shortcut allowing to write
 * `runApplication<MyApplication>(arg1, arg2) { // SpringApplication customization ... }`
 * instead of instantiating `SpringApplication` class, customize it and then invoking
 * `run(arg1, arg2)`.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
inline fun <reified T : Any> runApplication(
				vararg args: String, init: SpringApplication.() -> Unit): ConfigurableApplicationContext =
		SpringApplication(T::class.java).apply(init).run(*args)

/**
 * Top-level function acting as a Kotlin shortcut allowing to write
 * `fromApplication<MyApplication>().with(...)`. This method assumes that
 * the `main` function is declared in the same file as `T`.
 *
 * @author Phillip Webb
 * @since 3.1.1
 */
inline fun <reified T : Any> fromApplication(): SpringApplication.Augmented {
	val type = T::class
	val ktClassName = "${type.qualifiedName}Kt"
	try {
		val ktClass = ClassUtils.resolveClassName(ktClassName, type.java.classLoader)
		val mainMethod = ReflectionUtils.findMethod(ktClass, "main", Array<String>::class.java)
		Assert.notNull(mainMethod, "Unable to find main method")
		return SpringApplication.from { ReflectionUtils.invokeMethod(mainMethod!!, null, it) }
	} catch (ex: Exception) {
		throw IllegalStateException("Unable to use 'fromApplication' with ${type.qualifiedName}")
	}
}

/**
 * Extension function that allows [SpringApplication.Augmented.with] to work with Kotlin classes.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @since 3.1.1
 */
fun SpringApplication.Augmented.with(vararg types: KClass<*>): SpringApplication.Augmented {
	return this.with(*types.map(KClass<*>::java).toTypedArray())!!
}

