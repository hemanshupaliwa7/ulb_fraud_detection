resolvers ++= Seq(
    "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype releases"  at "https://oss.sonatype.org/content/repositories/releases/"
)
resolvers += Resolver.sonatypeRepo("public")
//addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.5.0")
//addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.9.0")
