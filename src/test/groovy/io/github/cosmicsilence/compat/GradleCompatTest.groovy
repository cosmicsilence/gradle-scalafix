package io.github.cosmicsilence.compat

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import spock.lang.Specification

class GradleCompatTest extends Specification {

    private Project mockProject
    private Gradle gradle

    def setup() {
        mockProject = GroovyMock(Project)
        gradle = Mock(Gradle)
        mockProject.gradle >> gradle
    }

    def 'isVersion4 should return true if the gradle version is 4.x'() {
        given:
        gradle.gradleVersion >> '4.9'

        when:
        boolean isVersion4 = GradleCompat.isVersion4(mockProject)

        then:
        isVersion4 == true
    }

    def 'isVersion4 should return false if the gradle version is 5.x'() {
        given:
        gradle.gradleVersion >> '5.5'

        when:
        boolean isVersion4 = GradleCompat.isVersion4(mockProject)

        then:
        isVersion4 == false
    }
}
