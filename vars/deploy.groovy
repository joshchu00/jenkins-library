def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  node {

    docker.withTool('docker-latest') {
      
      stage('Git Pull') {
        git url: config.gitURL, branch: config.gitBranch
      }
      stage('Deploy') {
        withCredentials([sshUserPrivateKey(credentialsId: config.deployPrivateKey, keyFileVariable: 'PRIVATEKEY')]) {
          configFileProvider([configFile(fileId: config.deployInventory, variable: 'INVENTORY')]) {
            def privateKeyBase64 = sh(script: 'cat $PRIVATEKEY | base64 -w 0', returnStdout: true)
            def inventoryBase64 = sh(script: 'cat $INVENTORY | base64 -w 0', returnStdout: true)
            withEnv(["ANSIBLE_HOST_KEY_CHECKING=False"]) {
              docker.image(config.deployImage).inside() {
                sh "echo ${privateKeyBase64} | base64 -d > /home/jenkins/.ssh/id_rsa"
                sh "echo ${inventoryBase64} | base64 -d > /home/jenkins/inventory"
                sh "ansible-playbook -i /home/jenkins/inventory ${config.deployPlaybook}"
              }
            }
          }
        }
      }
    }
  }
}
