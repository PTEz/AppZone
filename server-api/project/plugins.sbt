libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.11.1"))

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.7")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.4.0")