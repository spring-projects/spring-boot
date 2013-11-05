class FailingJUnitTests {
	@Test
	void passingTest() {
		assertTrue(true)
	}

	@Test
	void failureByAssertion() {
		assertTrue(false)
	}

	@Test
	void failureByException() {
		throw new RuntimeException("This should also be handled")
	}
}

class FailingSpockTest extends Specification {
	def "this should pass"() {
		expect:
		name.size() == length

		where:
		name    | length
		"Spock" | 5
	}

	def "this should fail on purpose as well"() {
		when:
		String text = "Greetings"

		then:
		//throw new RuntimeException("This should fail!")
		true == false
	}
}
