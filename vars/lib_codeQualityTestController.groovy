def call(Map config) {
    withMaven(maven: config.b_config.project.builderVersion) {
        withSonarQubeEnv(config.sonarqube_env_name) {
            sh """
            ${config.sonarqube_home}/bin/sonar-scanner \
            -Dsonar.language=js \
            -Dsonar.projectKey=${config.sonar_qube_project_key} \
            -Dsonar.sources=.
            """
        }
    }

    timeout(time: 1, unit: 'HOURS') {
        def qg = waitForQualityGate()
        if (qg.status != 'OK') {
            error "Pipeline aborted due to quality gate failure: ${qg.status}"
        }
    }
}
