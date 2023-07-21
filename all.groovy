pipeline {
    agent any
    tools {
        maven 'Maven-3.8.8'
    }
    environment {
        TOMCAT_CREDS = credentials('tomcat_credentials') // jenkins credentials
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
                echo "If artifact exists"
                echo "${artifactExists}"
                }
            }

        }
        stage ('Deploy to tomcat') {
            steps {
                // curl commands 
                sh "curl -v -u ${TOMCAT_CREDS_USR}:${TOMCAT_CREDS_PSW} -T /var/lib/jenkins/workspace/allpipeline/target/spring3-mvc-maven-xml-hello-world-1.0-SNAPSHOT.war 'http://34.16.170.223:8085/manager/text/deploy?path=/spring-hello'"
            }
        }
    }
}
