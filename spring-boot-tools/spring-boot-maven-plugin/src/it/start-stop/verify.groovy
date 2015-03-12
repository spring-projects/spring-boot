import static org.junit.Assert.assertTrue

def file = new File(basedir, "build.log")
assertTrue 'Shutdown should have been invoked', file.text.contains("Shutdown requested")
assertTrue 'Application should have terminated', file.text.contains("Application has terminated gracefully")

