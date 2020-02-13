package io.github.cosmicsilence.scalafix

/**
 * Created by tomas.mccandless on 2/12/20.
 */
class ScalafixTestConstants {

    public static final String BUILD_SCRIPT = '''
plugins {
    id 'scala\'
    id 'io.github.cosmicsilence.scalafix\'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation "org.scala-lang:scala-library:2.12.10"
}

tasks.withType(ScalaCompile) {
    // needed for RemoveUnused rule
    scalaCompileOptions.additionalParameters = [ "-Ywarn-unused" ]
}
'''

    // uses a var, and unused import
    public static final String FLAWED_SCALA_CODE = '''
package io.github.cosmicsilence.scalafix

import scala.util.Random

object HelloWorld extends App {
  var msg: String = "hello, world!"
  println(msg)
}
'''

    public static final String FIXED_SCALA_CODE = '''
package io.github.cosmicsilence.scalafix


object HelloWorld extends App {
  var msg: String = "hello, world!"
  println(msg)
}
'''
}
