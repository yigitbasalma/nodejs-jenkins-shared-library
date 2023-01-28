def call(Map config) {
    sh """
    export GIT_COMMIT=${GIT_COMMIT} \
    export SENTRY_AUTH_TOKEN=${config.SENTRY_AUTH_TOKEN} \
    npm install && \
    yarn install --immutable && \
        yarn build:locale --verbose ${config.b_config.project.requireTranslations ? 1 : ' --strict' } && \
        yarn build:apps
    """
}