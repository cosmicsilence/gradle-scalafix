package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import scalafix.interfaces.Scalafix
import scalafix.internal.interfaces.ScalafixInterfacesClassloader

import java.util.concurrent.ConcurrentHashMap

abstract class CachedClassloaders {

    private static final Logger logger = Logging.getLogger(CachedClassloaders)
    private static final ConcurrentHashMap<Object, ClassLoader> cache = new ConcurrentHashMap<>()

    private CachedClassloaders() {}

    static ClassLoader forScalafixCli(Project project, String scalafixCliCoordinates) {
        return cache.computeIfAbsent(scalafixCliCoordinates, {
            logger.info("Creating classloader for '${scalafixCliCoordinates}'")
            def cliDependency = project.dependencies.create(scalafixCliCoordinates)
            def cliConfiguration = project.configurations.detachedConfiguration(cliDependency)
            def parentClassloader = new ScalafixInterfacesClassloader(Scalafix.class.classLoader)
            classloaderFrom(cliConfiguration, parentClassloader)
        })
    }

    static ClassLoader forExternalRules(Configuration rulesConfiguration, ClassLoader scalafixCliClassloader) {
       def externalRuleCoordinates = rulesConfiguration.resolvedConfiguration.firstLevelModuleDependencies.collect {
            "${it.moduleGroup}:${it.moduleName}:${it.moduleVersion}"
        }.toSet().sort().join(";")
        def cacheKey = new Tuple2(externalRuleCoordinates, scalafixCliClassloader)
        return cache.computeIfAbsent(cacheKey, {
            logger.info("Creating classloader for '${externalRuleCoordinates}'")
            classloaderFrom(rulesConfiguration, scalafixCliClassloader)
        })
    }

    private static URLClassLoader classloaderFrom(Configuration configuration, ClassLoader parent) {
        def jarFiles = configuration.collect { it.toURI().toURL() }.toArray(new URL[0])
        return new URLClassLoader(jarFiles, parent)
    }
}
