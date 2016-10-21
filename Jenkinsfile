// tell Jenkins how to build projects from this repository
node {
	try {
		stage 'Maven Clean'
		def mvnHome = tool 'M3'
		env.M2_HOME = "${mvnHome}"
		sh "${mvnHome}/bin/mvn --batch-mode -Dmaven.repo.local=local-maven-repository/ clean"
		
		stage 'Checkout'
		checkout scm
		
		dir('build') {
    		deleteDir()
		}
		
		stage 'Gradle Build'
		try {
			sh "./gradlew -PuseJenkinsSnapshots=true cleanLocalMavenRepo clean build createLocalMavenRepo --refresh-dependencies --continue"
			archive 'build/maven-repository/**/*.*'
		} finally {
			step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/*.xml'])
		}
		
		stage 'Maven Build'
		try {
			wrap([$class:'Xvnc', useXauthority: true]) {
				sh "${mvnHome}/bin/mvn --batch-mode --update-snapshots -fae -Dmaven.repo.local=local-maven-repository/ deploy"
			}
			archive 'build/**/*.*'
		} finally {
			step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
		}
				
		slackSend "Build Succeeded - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
		
	} catch (e) {
		slackSend color: 'danger', message: "Build Failed - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
		throw e
	}
}