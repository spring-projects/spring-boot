import static org.junit.Assert.assertTrue

def file = new File(basedir, "build.log")
assertTrue 'Start should have waited for application to be ready', file.text.contains("isReady: true")
assertTrue 'Shutdown should have been invoked', file.text.contains("Shutdown requested")
