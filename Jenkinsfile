@Library('libpipelines@master') _

hose {
    MAIL = 'support'
    SLACKTEAM = 'stratiosecurity'
    MODULE = 'stratio-spark'
    REPOSITORY = 'spark'
    DEVTIMEOUT = 300
    RELEASETIMEOUT = 200
    BUILDTOOLVERSION = '3.5.0'
    PKGMODULESNAMES = ['stratio-spark']

    DEV = { config ->

        doPackage(config)
       // doUT(config)
        parallel(DOCKER1: {
                    doDocker(conf: config, dockerfile:"DockerfileDispatcher")
                }, DEPLOY: {
                    doDeploy(config)
                }, DOCKER2: {
                     doDocker(conf: config, dockerfile:"DockerfileHistory", image:"spark-stratio-history-server")
        }, failFast: config.FAILFAST)
     }


    INSTALLSERVICES = [
            ['DCOSCLI':   ['image': 'stratio/dcos-cli:0.4.15',
                           'volumes': ['stratio/paasintegrationpem:0.1.0'],
                           'env':     ['DCOS_IP=10.200.0.156',
                                      'SSL=true',
                                      'SSH=true',
                                      'TOKEN_AUTHENTICATION=true',
                                      'DCOS_USER=admin@demo.stratio.com',
                                      'DCOS_PASSWORD=1234',
                                      'BOOTSTRAP_USER=operador',
                                      'PEM_FILE_PATH=/paascerts/PaasIntegration.pem'],
                           'sleep':  10]]
        ]

    INSTALLPARAMETERS = """
        | -DDCOS_CLI_HOST=%%DCOSCLI#0
        | -DVAULT_HOST=10.200.0.157
        | -DVAULT_PORT=8200
        | -DDCOS_USER=admin@demo.stratio.com
                | -DDCOS_PASSWORD=1234
        | -DCLI_USER=root
        | -DCLI_PASSWORD=stratio
        | -DREMOTE_USER=root
        | -DREMOTE_PASSWORD=stratio
        | -DDCOS_IP=10.200.0.156
        | """.stripMargin().stripIndent()

    INSTALL = { config, params ->
      def ENVIRONMENTMAP = stringToMap(params.ENVIRONMENT)      
      doAT(conf: config, groups: ['installation'])
    }

}
