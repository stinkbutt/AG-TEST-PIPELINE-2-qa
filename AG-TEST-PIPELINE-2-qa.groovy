pipeline {
    // step by step

    agent { label 'jenkins-slave-selenium' }
     
    environment {
        //JENKVER = "0.5"
      }

    stages {
        stage("Prepare") {
          steps {

            // Start fresh
            cleanWs()

            // Set node environment
            nvm('v6.2.0') {
    	    	    sh 'env'
              }
    	        
            // Get the repos  
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
            // Nightwatch needs to be manually installed. Install player-service and functional tests
            sh 'npm install nightwatch --prefix $WORKSPACE/mlg-qa/player-func-tests'
            sh 'npm install --prefix $WORKSPACE/video-player/player-service'
            sh 'npm install --prefix $WORKSPACE/mlg-qa/player-func-tests' 
            }  
          }

        stage("App Start"){
          steps{
            // Start video player in local mode, output to log and then wait 30 seconds for the app to start before proceeding to tests
            sh 'nohup npm run --prefix $WORKSPACE/video-player/player-service/ start-local >> player-service.log &'
            sh 'sleep 30'
              }
          }

        stage("Testing"){
          steps{
             sh 'npm run --prefix $WORKSPACE/mlg-qa/player-func-tests jenkins-test'
            }
          }
      }

    post {
      always {
          // Publish reports
          junit 'mlg-qa/player-func-tests/reports/**/*.xml'
          publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'video-player/player-service/tests/reports/unit/server/html', reportFiles: 'Server-Test-Results.html', reportName: 'HTML Report', reportTitles: ''])
          publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'video-player/player-service/tests/reports/unit/client/coverage', reportFiles: 'index.html', reportName: 'HTML Report', reportTitles: ''])
          echo 'Jenkins Job finished'
     	  }

      success {
          // slackSend channel: 'engineering-alerts', color: 'good', message: 'Finished ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)'
          echo 'Success - Yeah, baby, yeah!'

          // No reason to keep the files after a success
          deleteDir() /* clean up our workspace */
      	}

      unstable {
        // slackSend channel: 'engineering-alerts', color: 'good', message: 'Finished ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)'
          echo 'Unstable - I hope this is part of the unfreezing process :/'
     	 	}

      failure {
        // slackSend channel: 'engineering-alerts', color: 'good', message: 'Finished ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)'
          echo "Failed - Mr. Bigglesworth gets upset :("
      	}

      changed {
        // slackSend channel: 'engineering-alerts', color: 'good', message: 'Finished ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)'
          echo 'Changed - If you want us to have a relationship, you have to get it into your head that times have changed!'
      	}
      }