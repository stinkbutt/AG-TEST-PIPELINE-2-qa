pipeline {
    // step by step

    agent { label 'jenkins-slave-selenium' }
     
    environment {
        JENKVER = "0.5"
      }

    stages {
        stage("Prepare") {
          steps {
            cleanWs()
            nvm('v6.2.0') {
    	    	    sh 'env'
              }
    	        
            dir("video-player"){
                git branch: 'develop', credentialsId: '7c585d84-10e0-43bb-b62b-641dcf4076ad', url: 'git@github.com:mlgdev/video-player.git'
            }
    		
      	    dir("mlg-qa"){
                  git branch: 'develop', credentialsId: '7c585d84-10e0-43bb-b62b-641dcf4076ad', url: 'git@github.com:mlgdev/mlg-qa.git'
    		    }
          }
        }

        stage("Build"){
      
          steps {
            sh 'npm install nightwatch --prefix $WORKSPACE/mlg-qa/player-func-tests'
            sh 'npm install --prefix $WORKSPACE/video-player/player-service'
            sh 'npm install --prefix $WORKSPACE/mlg-qa/player-func-tests' 
            }  
          }

        stage("App Start"){
          steps{
          sh 'nohup npm run --prefix $WORKSPACE/video-player/player-service/ start-local >> player-service.log &'
          sh 'sleep 30'
            }
          }

        stage("Testing"){
          steps{
             sh 'npm run --prefix $WORKSPACE/mlg-qa/player-func-tests jenkins-test'
            //sh 'npm stop --prefix $WORKSPACE/video-player/player-service/ start-local'
            }
          }
      }

    post {
      always {
          junit 'mlg-qa/player-func-tests/reports/**/*.xml'
          //publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'video-player/player-service/tests/reports/unit/server/html', reportFiles: 'Server-Test-Results.html', reportName: 'HTML Report', reportTitles: ''])
          //publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'video-player/player-service/tests/reports/unit/client/coverage', reportFiles: 'index.html', reportName: 'HTML Report', reportTitles: ''])
          echo 'Jenkins Job finished'
          
          //deleteDir() /* clean up our workspace */
     	  }

      success {
          //slackSend channel: 'engineering-alerts', color: 'good', message: 'Finished ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)'
          echo 'Success - Yeah, baby, yeah!'
      	}

      unstable {
          echo 'Unstable - I hope this is part of the unfreezing process :/'
     	 	}

      failure {
          echo "Failed - Mr. Bigglesworth gets upset :("
      	}

      changed {
          echo 'Changed - If you want us to have a relationship, you have to get it into your head that times have changed!'
      	}
      }
}