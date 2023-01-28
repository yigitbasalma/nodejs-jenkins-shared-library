def call(Map config, String sshKeyFile) {
    // SSH key file permission
    sh "chmod 600 ${sshKeyFile}"
    container_repository = config.container_artifact_repo_address

    if ( config.scope == "branch" && params.IMAGE == "" ) {
        currentBuild.result = "ABORTED"
        error("You have to set IMAGE_ID parameter for branch deployment.")
    }

    // Configure init
    image = params.IMAGE

    // Set container id global
    env.CONTAINER_IMAGE_ID = image

    config.b_config.deploy.each { it ->
        def name = it.name.replace("\$Identifier", config.container_repo).replace("_", "-")
        def path = it.path.replace("\$Identifier", config.container_repo)

        if ( config.scope == "branch" ) {
            path = "${path}/${config.target_branch}"
        }

        "${it.type}"(config, image, it.repo, path, name, sshKeyFile, container_repository)
    }
}

def argocd(Map config, String image, String repo, String path, String appName, String sshKeyFile, String containerRepository) {
    // Change image version on argocd repo and push
    sh """
    ${config.script_base}/argocd/argocd.py --image "${containerRepository}/${appName}:${image}" -r ${repo} --application-path ${path} --environment ${config.environment} --key-file "${sshKeyFile}"
    """
}