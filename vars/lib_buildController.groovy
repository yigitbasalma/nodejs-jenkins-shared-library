def call(Map config) {
    def builder = "npm"

    if ( config.b_config.project.builderVersion != "nodejs" ) {
        builder = tool config.b_config.project.builderVersion
    }

    sh """
    ${builder} install && \
    ${builder} run ${config.b_config.project.buildCommand ? config.b_config.project.buildCommand : 'build'}
    """
}