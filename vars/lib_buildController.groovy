def call(Map config) {
    sh """
    npm install && \
    npm run ${config.b_config.project.buildCommand ? config.b_config.project.buildCommand : 'build'}
    """
}