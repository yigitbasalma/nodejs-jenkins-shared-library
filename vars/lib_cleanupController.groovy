def call(Map config) {
    sh """
    # Remove containers that may still run
    docker network ls | awk '\$2 ~ /^'"${config.commitID}"'/ { print \$1 }' \
        | xargs --no-run-if-empty docker network inspect \
        | jq '.[] | .Containers | keys | .[]' -r \
        | xargs --no-run-if-empty docker rm -f

    # Delete built images by IMAGE ID (except base & tests image)
    docker image ls | awk '\$1 !~ /^twisto-base\$|^prebuild_checks\$|^twisto.*tests\$|^anonymizer/ && \$2 ~ /^'"${config.commitID}"'\$/ { print \$3 }' | xargs --no-run-if-empty docker image rm -f
    # Delete base and tests but not latest
    docker image ls | awk '\$1 ~ /^twisto-base\$|^prebuild_checks\$|^twisto.*tests\$|^anonymizer/ && \$2 !~ /^latest\$/ {print \$1":"\$2}' | xargs --no-run-if-empty docker rmi -f

    # Remove networks
    docker network ls | awk '\$2 ~ /^'"tripping-${config.commitID}"'/ { print \$1 }' | xargs --no-run-if-empty docker network rm

    # Remove not necessary images
    docker image prune -f
    # Remove stopped containers
    docker container prune -f
    # Remove not mounted volumes
    docker volume prune -f
    # Remove unused networks
    docker network prune -f

    rm -rf ${WORKSPACE}/*
    """
}