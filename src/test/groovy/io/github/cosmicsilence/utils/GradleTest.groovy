package io.github.cosmicsilence.utils

import org.gradle.api.Project
import spock.lang.Specification

class GradleTest extends Specification {

    Project mockProject
    def gradle

    def setup() {
        mockProject = GroovyMock(Project)
        gradle = Mock(org.gradle.api.invocation.Gradle)
        mockProject.gradle >> gradle
    }

    def 'isVersion4 should return true if the gradle version is 4.x'() {
        given:
        gradle.gradleVersion >> '4.9'

        when:
        boolean isVersion4 = Gradle.isVersion4(mockProject)

        then:
        isVersion4 == true
    }

    def 'isVersion4 should return true if the gradle version is 5.x'() {
        given:
        gradle.gradleVersion >> '5.5'

        when:
        boolean isVersion4 = Gradle.isVersion4(mockProject)

        then:
        isVersion4 == false
    }
}
