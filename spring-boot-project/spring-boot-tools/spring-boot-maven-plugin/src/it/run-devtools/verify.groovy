import static org.junit.Assert.assertTrue

def file = new File(basedir, "build.log")
assertTrue 'Devtools should have been detected', file.text.contains('Fork mode disabled, devtools will be disabled')
assertTrue 'Application should have run', file.text.contains("I haz been run")

