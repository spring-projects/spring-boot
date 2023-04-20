package org.springframework.boot.context.properties.bind

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.source.ConfigurationPropertyName
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource
import org.springframework.core.ResolvableType

/**
 * Tests for `ConstructorParametersBinder`.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class KotlinConstructorParametersBinderTests {

	@Test
	fun `Bind to class should create bound bean`() {
		val source = MockConfigurationPropertySource(mapOf("foo.int-value" to "12",
				"foo.int-value" to "12",
				"foo.long-value" to "34",
				"foo.boolean-value" to "true",
				"foo.string-value" to "foo",
				"foo.enum-value" to "foo-bar"))
		val binder = Binder(source)
		val bean = binder.bind("foo", Bindable.of(ExampleValueBean::class.java)).get()
		assertThat(bean.intValue).isEqualTo(12)
		assertThat(bean.longValue).isEqualTo(34)
		assertThat(bean.booleanValue).isTrue()
		assertThat(bean.stringValue).isEqualTo("foo")
		assertThat(bean.enumValue).isEqualTo(ExampleEnum.FOO_BAR)
	}

	@Test
	fun `Bind to class when has no prefix should create bound bean`() {
		val source = MockConfigurationPropertySource(mapOf("int-value" to "12",
				"long-value" to "34",
				"boolean-value" to "true",
				"string-value" to "foo",
				"enum-value" to "foo-bar"))
		val binder = Binder(source)
		val bean = binder.bind(ConfigurationPropertyName.of(""),
				Bindable.of(ExampleValueBean::class.java)).get()
		assertThat(bean.intValue).isEqualTo(12)
		assertThat(bean.longValue).isEqualTo(34)
		assertThat(bean.booleanValue).isTrue()
		assertThat(bean.stringValue).isEqualTo("foo")
		assertThat(bean.enumValue).isEqualTo(ExampleEnum.FOO_BAR)
	}

	@Test
	fun `Bind to data class should create bound bean`() {
		val source = MockConfigurationPropertySource(mapOf("foo.int-value" to "12",
				"foo.long-value" to "34",
				"foo.boolean-value" to "true",
				"foo.string-value" to "foo",
				"foo.enum-value" to "foo-bar"))
		val binder = Binder(source)
		val bean = binder.bind("foo", Bindable.of(ExampleDataClassBean::class.java)).get()
		val expectedBean = ExampleDataClassBean(intValue = 12,
				longValue = 34,
				booleanValue = true,
				stringValue = "foo",
				enumValue = ExampleEnum.FOO_BAR)
		assertThat(bean).isEqualTo(expectedBean)
	}

	@Test
	fun `Bind to class with multiple constructors and primary constructor should bind`() {
		val source = MockConfigurationPropertySource("foo.int-value", "12")
		val binder = Binder(source)
		val bindable = binder.bind("foo", Bindable.of(
				MultipleConstructorsWithPrimaryConstructorBean::class.java))
		assertThat(bindable.isBound).isTrue()
		assertThat(bindable.get().intValue).isEqualTo(12)
	}

	@Test
	fun `Bind to class with multiple constructors should not bind`() {
		val source = MockConfigurationPropertySource("foo.int-value", "12")
		val binder = Binder(source)
		val bindable = binder.bind("foo", Bindable.of(
				MultipleConstructorsBean::class.java))
		assertThat(bindable.isBound).isFalse()
	}

	@Test
	fun `Bind to class with only default constructor should not bind`() {
		val source = MockConfigurationPropertySource("foo.int-value", "12")
		val binder = Binder(source)
		val bindable = binder.bind("foo", Bindable.of(
				DefaultConstructorBean::class.java))
		assertThat(bindable.isBound).isFalse()
	}

	@Test
	fun `Bind to class should bind nested`() {
		val source = MockConfigurationPropertySource(mapOf("foo.value-bean.int-value" to "123",
				"foo.value-bean.long-value" to "34",
				"foo.value-bean.boolean-value" to "true",
				"foo.value-bean.string-value" to "foo"))
		val binder = Binder(source)
		val bean = binder.bind("foo", Bindable.of(ExampleNestedBean::class.java)).get()
		assertThat(bean.valueBean.intValue).isEqualTo(123)
		assertThat(bean.valueBean.longValue).isEqualTo(34)
		assertThat(bean.valueBean.booleanValue).isTrue()
		assertThat(bean.valueBean.stringValue).isEqualTo("foo")
		assertThat(bean.valueBean.enumValue).isNull()
	}

	@Test
	fun `Bind to class with no value for optional should use null`() {
		val source = MockConfigurationPropertySource("foo.string-value", "foo")
		val binder = Binder(source)
		val bean = binder.bind("foo", Bindable.of(
				ExampleValueBean::class.java)).get()
		assertThat(bean.intValue).isNull()
		assertThat(bean.longValue).isNull()
		assertThat(bean.booleanValue).isNull()
		assertThat(bean.stringValue).isEqualTo("foo")
		assertThat(bean.enumValue).isNull()
	}

	@Test
	fun `Bind to class with no value for primitive should use default value`() {
		val source = MockConfigurationPropertySource("foo.string-value", "foo")
		val binder = Binder(source)
		val bean = binder.bind("foo", Bindable.of(
				ExamplePrimitiveDefaultBean::class.java)).get()
		assertThat(bean.intValue).isEqualTo(0)
		assertThat(bean.longValue).isEqualTo(0)
		assertThat(bean.booleanValue).isFalse()
		assertThat(bean.stringValue).isEqualTo("foo")
		assertThat(bean.enumValue).isNull()
	}

	@Test
	fun `Bind to class with no value and default value should return unbound`() {
		val source = MockConfigurationPropertySource("foo.string-value", "foo")
		val binder = Binder(source)
		assertThat(binder.bind("foo", Bindable.of(
				ExampleDefaultValueBean::class.java)).isBound).isFalse()
	}

	@Test
	fun `Bind or create to class with no value and default value should return default value`() {
		val source = MockConfigurationPropertySource("foo.string-value", "foo")
		val binder = Binder(source)
		val bean = binder.bindOrCreate("foo", Bindable.of(
				ExampleDefaultValueBean::class.java))
		assertThat(bean.intValue).isEqualTo(5)
		assertThat(bean.stringsList).containsOnly("a", "b", "c")
		assertThat(bean.customList).containsOnly("x,y,z")
	}

	@Test
	fun `Bind to data class with no value should use default value`() {
		val source = MockConfigurationPropertySource("foo.enum-value", "foo-bar")
		val binder = Binder(source)
		val bean = binder.bind("foo", Bindable.of(ExampleDataClassBean::class.java)).get()
		val expectedBean = ExampleDataClassBean(intValue = 5,
				longValue = 42,
				booleanValue = false,
				stringValue = "my data",
				enumValue = ExampleEnum.FOO_BAR)
		assertThat(bean).isEqualTo(expectedBean)
	}

	@Test
	fun `Bind to data class with generics`() {
		val source = MockConfigurationPropertySource("foo.value.bar", "baz")
		val binder = Binder(source)
		val type = ResolvableType.forClassWithGenerics(Map::class.java, String::class.java,
				String::class.java)
		val bean = binder.bind("foo", Bindable
				.of<GenericValue<Map<String, String>>>(ResolvableType.forClassWithGenerics(GenericValue::class.java, type)))
				.get()
		assertThat(bean.value["bar"]).isEqualTo("baz")
	}

	@Test
	fun `Bind to named constructor parameter`() {
		val source = MockConfigurationPropertySource("foo.string-value", "test")
		val binder = Binder(source)
		val bean = binder.bind("foo", Bindable.of(ExampleNamedParameterBean::class.java)).get()
		assertThat(bean.stringDataValue).isEqualTo("test")
	}

	@Test
	fun `Bind to singleton object`() {
		val source = MockConfigurationPropertySource("foo.string-value", "test")
		val binder = Binder(source)
		val bean = binder.bind("foo", Bindable.of(ExampleSingletonBean::class.java)).get()
		assertThat(bean.stringValue).isEqualTo("test")
	}

	class ExampleValueBean(val intValue: Int?, val longValue: Long?,
						   val booleanValue: Boolean?, val stringValue: String?,
						   val enumValue: ExampleEnum?)

	class ExamplePrimitiveDefaultBean(val intValue: Int = 0, val longValue: Long = 0,
									  val booleanValue: Boolean = false, val stringValue: String?,
									  val enumValue: ExampleEnum?)


	enum class ExampleEnum {

		FOO_BAR,

		BAR_BAZ
	}

	@Suppress("unused", "UNUSED_PARAMETER")
	class MultipleConstructorsWithPrimaryConstructorBean(val intValue: Int) {
		constructor(intValue: Int, longValue: Long) : this(intValue)
	}

	@Suppress("unused", "UNUSED_PARAMETER")
	class MultipleConstructorsBean {
		constructor(intValue: Int)

		constructor(intValue: Int, longValue: Long) : this(intValue)
	}

	class DefaultConstructorBean

	class ExampleNestedBean(val valueBean: ExampleValueBean)

	class ExampleDefaultValueBean(val intValue: Int = 5,
								  val stringsList: List<String> = listOf("a", "b", "c"),
								  val customList: List<String> = listOf("x,y,z"))

	data class ExampleDataClassBean(val intValue: Int = 5, val longValue: Long = 42,
									val booleanValue: Boolean = false,
									val stringValue: String = "my data",
									val enumValue: ExampleEnum = ExampleEnum.BAR_BAZ)

	data class ExampleNamedParameterBean(@Name("stringValue") val stringDataValue: String)

	data class GenericValue<T>(
		val value: T
	)

	object ExampleSingletonBean {
		var stringValue: String? = null
	}

}