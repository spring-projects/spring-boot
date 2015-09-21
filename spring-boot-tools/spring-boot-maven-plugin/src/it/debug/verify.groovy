def file = new File(basedir, "build.log")
return file.text.contains("Listening for transport dt_socket at address: 5005")
return file.text.contains("I haz been run")

