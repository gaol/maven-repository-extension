Maven Repository Extension
==
 
This extension is used to limit the remote repository for specific artifacts, like we only need to download the artifacts from our inner repository for the artifacts with specific version string: 'x.x.x.redhat-y'


Usage
===

Run your maven command like this:

> mvn clean install -Dmaven.ext.class.path=<YOUR-PATH-TO>/maven-repository-extension.jar -Drepo.extension.enabled=true

Then you will notice a message: 'Red Hat Maven Repository Extenstion is loaded.' before the build
