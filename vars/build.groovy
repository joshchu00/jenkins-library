def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  node {

    docker.withTool('docker-latest') {

      def image

      stage('Git Pull') {
        git url: config.gitURL, branch: config.gitBranch
      }
      stage('Project Build') {
        docker.image(config.buildImage).inside {
          sh 'yarn'
          sh 'yarn build'
        }
      }
      stage('Docker Build') {
        image = docker.build(config.dockerName)
      }
      stage('Docker Push') {
        docker.withRegistry('', 'DockerHub') {
          image.push()
        }
      }
    }
  }
}