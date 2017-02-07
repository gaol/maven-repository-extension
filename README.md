Maven Repository Extension
==
Sometimes you may find that Maven will try to download the artifacts from maven central repository although you know that the artifacts are not in the maven central repository at all. Maven tries to download the artifacts from all defined remote repositories one by one. The order of the defined repositories are unknown at all, so it will waste much of time of `meaningless tring`.

Summary
== 
This extension is used to limit the remote repository for specific artifacts, like we may need to download the artifacts from the inner repository for the artifacts with specific version string: `x.x.x.redhat-y`

This extension has also the ability to order the repositories alphabetictly.

Usage
===

Run your maven command like this:

> mvn clean install -Dmaven.ext.class.path=&lt;YOUR-PATH-TO&gt;/maven-repository-extension.jar -Drepo.extension.enabled=true

When you notice a message: 'Red Hat Maven Repository Extenstion is loaded.' before the build, it is enabled.


Notes
==
> NOTE: This extension tries to extend the RepositorySystem to intercept which repository the artifact should be downloaded from. It was tested in Maven 3.3.9.


