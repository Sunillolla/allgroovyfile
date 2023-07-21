pipeline {
    agent any
    tools {
        maven 'Maven-3.8.8'
        jdk 'JDK17'
    }
    environment {
        TOMCAT_CREDS = credentials('tomcat_credentials') // jenkins credentials
        NEXUS_VERSION = "nexus3"
        NEXUS_PROTOCOL = "http"
        NEXUS_URL= "34.125.75.196:8081"
       // NEXUS_REPO= "allrepo-releases"
       NEXUS_REPO= "new-repo-test"

    }
    stages {
        stage ('clone') {
            steps {
                // clone the repo, go to snippet generartor
                git credentialsId: 'github_credentials', url: 'https://github.com/Sunillolla/spring3-mvc-maven-xml-hello-world.git'
            }
        }
        stage ('Build') {
            steps {
                sh "mvn clean package -Dmaven.test.failure.ignore=true"
                // -Dcheckstyle.skip
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war', followSymlinks: false
                }
            }
        }
        stage ('Nexus') {
            steps {
                script {
                    pom = readMavenPom file: "pom.xml"
                files = findFiles(glob: "target/*.${pom.packaging}")
                // for verification
                echo "echo ${files[0].name} ${files[0].path} ${files[0].directory} ${files[0].length} ${files[0].lastModified}"
                artifactPath = files[0].path;
                artifactExists = fileExists artifactPath;
                // echo "If artifact exists"
                echo "${artifactExists}"

                if (artifactExists) {
                    echo "********** Artifact is available, going to deploy to nexus"
                    echo "File is : ${artifactPath} , Package is : ${pom.packaging} , Version is : ${pom.version} , GroupID is : ${pom.groupId}"
                    // we need to deploy to nexus , using a plugin called as nexusArtifactuploader
                    nexusArtifactUploader (
                        nexusVersion: "$env.NEXUS_VERSION",
                        protocol: "${env.NEXUS_PROTOCOL}",
                        nexusUrl: "${env.NEXUS_URL}", // env. | params. | pipelineparams.
                        groupId: "${pom.groupId}",
                        version: "${BUILD_NUMBER}",
                        repository: "${env.NEXUS_REPO}",
                        credentialsId: "nexus_creds",
                          artifacts: [
                            [
                                artifactId: pom.artifactId,
                                type: pom.packaging,
                                classifier:'',
                                file: artifactPath,

                            ]
                            

                          ]

                    )
                        
                    }
                
                else {
                    error "********** ${artifactPath} is not available ********** "
                }
                }
            }

        }
        stage ('Deploy to tomcat') {
            steps {
                // curl commands 
                sh "curl -v -u ${TOMCAT_CREDS_USR}:${TOMCAT_CREDS_PSW} -T /var/lib/jenkins/workspace/allpipeline/target/spring3-mvc-maven-xml-hello-world-1.0-SNAPSHOT.war 'http://34.125.127.26:8085//manager/text/deploy?path=/spring-hello&update=true'"
            }
        }
    }
}
