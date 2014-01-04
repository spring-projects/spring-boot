class MyCommand implements Command {

	String name = "foo"

	String description = "My script command"

	String help = "No options"

	String usageHelp = "Not very useful"
	
	Collection<String> optionsHelp = ["No options"]
	
	boolean optionCommand = false

	void run(String... args) {
		println "Hello ${args[0]}"
	}

}