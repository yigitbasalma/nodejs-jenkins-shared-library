def call(Map config) {
    withSonarQubeEnv(config.sonarqube_env_name) {
        sh """
        ${config.sonarqube_home}/bin/sonar-scanner \
        -Dsonar.language=js \
        -Dsonar.projectKey=${config.sonar_qube_project_key} \
        -Dsonar.sources=.
        """
    }

    def taskID = sh(
        script: "python3 -c 'print(\"\".join([\"=\".join(i.strip().split(\"=\")[1:]) for i in open(\".scannerwork/report-task.txt\", \"r\").readlines() if i.startswith(\"ceTaskUrl\")]))'",
        returnStdout: true
    ).trim()
    def count = 0

    withCredentials([string(credentialsId: 'sonarqube01-token', variable: 'TOKEN')]) {
        while(count <= 60) {
            def taskState = sh(
                script: "python3 -c 'import requests;print(requests.get(\"${taskID}\", headers={\"Authorization\": \"Bearer ${TOKEN}\"}).json()[\"task\"][\"status\"])'",
                returnStdout: true
            ).trim()

            if ( taskState == "SUCCESS" ) {
                break
            }

            sleep(10)
            count++
        }
    }
}
