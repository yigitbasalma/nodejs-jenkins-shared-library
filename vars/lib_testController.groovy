def call(Map config) {
    def commitID = config.commitID
    def testCases = [:]

    // Remove build dir if exists
    sh """
    rm -rf build || true
    mkdir -p build/tests
    """

    // Create test network
    sh """
    docker network create --internal ${commitID}
    """

    if ( config.b_config.test.containsKey("containers") ) {
        for ( container in config.b_config.test.containers ) {
            sh """#!/bin/bash
            docker build --rm \
                -t ${container.name}:${commitID} \
                --build-arg VERSION="${commitID}" \
                -f ${container.dockerFilePath} \
                ${container.contextPath}
            """
        }
    }

    if ( config.b_config.test.containsKey("components") ) {
        for ( component in config.b_config.test.components ) {
            def envVariables = []
            def mounts = []

            if ( component.containsKey("env") ) {
                for ( env in component.env ) {
                    envVariables.add("-e ${env}")
                }
            }

            if ( component.containsKey("mounts") ) {
                for ( mount in component.mounts ) {
                    mounts.add("--mount type=${mount.type},destination=${mount.dest}")
                }
            }

            sh """
            docker run -d \
                --name="${commitID}-${component.name}" \
                --network-alias="${component.name}" \
                --network="${commitID}"\
                ${envVariables.unique().join(" ")} \
                ${mounts.unique().join(" ")} \
                ${component.image}
            """

        }
    }

    for ( testCase in config.b_config.test.cases ) { 
        testCases["${testCase.name}"] = "${testCase.mode}"(testCase, commitID)
    }

    parallel testCases
}

def container(Map config, String commitID) {
    def envVariables = []
    def volumes = []
    def extraParams = []
    def image = "${config.image}:${commitID}"
    def _script = config.script

    if ( ! config.script.startsWith("-") ) {
        _script = "./${config.script}"
    }

    if ( config.image.split(":").length == 2 ) {
        image = config.image
    }

    if ( config.containsKey("env") ) {
        for ( env in config.env ) {
            envVariables.add("-e ${env}")
        }
    }

    if ( config.containsKey("volumes") ) {
        for ( volume in config.volumes ) {
            volumes.add("-v ${volume.replace('$pwd', WORKSPACE)}")
        }
    }

    if ( config.containsKey("user") ) {
        extraParams.add("-u ${config.user}")
    }

    if ( config.containsKey("workdir") ) {
        extraParams.add("-w ${config.workdir}")
    }

    if ( config.containsKey("entrypoint") ) {
        extraParams.add("--entrypoint=${config.entrypoint}")
    }

    if ( config.containsKey("components") && config.components ) {
        extraParams.add("--network='${commitID}'")
    }

    if ( config.containsKey("init") ) {
        def initEnvVariables = []
        def initExtraParams = []
        def initImage = "${config.init.image}:${commitID}"

        if ( config.init.image.split(":").length == 2 ) {
            initImage = config.init.image
        }

        if ( config.init.containsKey("env") ) {
            for ( ienv in config.init.env ) {
                initEnvVariables.add("-e ${ienv}")
            }
        }

        if ( config.containsKey("components") && config.components ) {
            initExtraParams.add("--network='${commitID}'")
        }

        sh """#!/bin/bash
        docker run --rm -i \
            ${initExtraParams.unique().join(" ")} \
            ${initEnvVariables.unique().join(" ")} \
            ${initImage} \
            ./${config.init.script}
        """
    }

    return {
        timeout(time: config.wait, unit: "MINUTES") {
            stage("${config.name}") {
                script {
                    try {
                        sh """
                        docker run --rm -i \
                            ${extraParams.unique().join(" ")} \
                            ${envVariables.unique().join(" ")} \
                            ${volumes.unique().join(" ")} \
                            ${image} \
                            ${_script}
                        """
                    } catch (Exception e) {
                        currentBuild.result = "ABORTED"
                        error("${config.errMsg}")
                    }
                }
            }
        }
    }
}