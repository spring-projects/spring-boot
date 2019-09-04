import static org.junit.Assert.assertFalse

def file = new File(basedir, "build.log")
assertFalse 'Application should not have run', file.text.contains("Ooops, I haz been run")
assertFalse 'Should not attempt to stop the app', file.text.contains('Stopping application')
