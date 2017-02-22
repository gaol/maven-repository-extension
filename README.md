Ordered Maven Repository Extension
==
Sometimes you may find that Maven will try to download the artifacts from maven central repository although you know that the artifacts are not in the maven central repository at all. It wastes plenty of time on meanless trying. Maven tries to download the artifacts from all defined remote repositories one by one, there is no fixed order to know which repository will be tried first. If we can define the order for the repositories and even better define the order of the repositoires for specific artifacts, it would reduce such failure tests greatly.

Summary
== 
This extension is for that purpose, the order of the repositories can be defined outside the pom in a simple properties file.

It also has the ability to define order of repositories for specific artifacts by matching the maven 'G:A:V' in the properties file. Although it does not gurrantee that all the matching artifacts will follow the rule strictly, it increases the chance very much!

Usage
===

There are 2 options to use this Maven Extension:

 * Place the `ordered-repository-extension.jar` to the `$MAVEN_HOME/lib/ext/` directory in your maven installation.
Then you can enable the extension by:

> mvn clean install -Dordered.repository.extension.enabled=true

This is suitable for cases that you have your own Maven installation.

 * Alternative option is downloading or building the `ordered-repository-extension.jar` to any place in your local environment, then enable the extension by:

> mvn clean install -Dmaven.ext.class.path=&lt;YOUR-PATH-TO&gt;/ordered-repository-extension.jar -Dordered.repository.extension.enabled=true

This is suitable for the CI jobs, like in Jenkins job, it does not affect the Maven installation in your CI enviornment at all.


> When you see a message: 'Ordered Maven Repository Extenstion is loaded.' in the first part of your log, it is enabled.


An embeded `config.properties` is built in the jar, which is like this:

<pre>
regex.1 = [^\\n]*:[^\\n]*:[^\\n]*redhat-[^\\n]*
regex.1.repos = jboss-eap-7.1-product-repository, jboss-product-repository, jboss-public-repository-group
regex.1.description = All Red Hat product maven artifacts

regex.2=org.apache.maven.plugins:[^\\n]*:[^\\n]*
regex.2.repos=central,jboss-public-repository-group,jboss-eap-7.1-product-repository,jboss-product-repository
regex.2.description=All maven plugins maven artifacts

regex.3=org.[jboss|wildfly][^\\n]*:[^\\n]*:[^\\n]*
regex.3.repos=jboss-public-repository-group, central, jboss-eap-7.1-product-repository, jboss-product-repository
regex.3.description = All JBoss projects maven artifacts

# default repositories order
regex.default.repos = jboss-public-repository-group,central

# There are other repos defined in other poms, we need to define whether to include other repositories.
# These extra repos are appended to current ordered repo list
repos.included = regex.default, regex.1, regex.2, regex.3

</pre>

You can override any of default configuration by specifing a properties location by: 

> mvn clean install -Dordered.repository.extension.configure.url=&lt;YOUR-CONFIG-URL&gt; -Dordered.repository.extension.enabled=true

The url can be relative path in current classpath, a local file path or a remote URL.

Notes
==
> NOTE: This extension tries to extend the RepositorySystem to intercept which repository the artifact should be downloaded from. It was tested in Maven 3.3.9.



 
