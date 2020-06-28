job("GroovyCodeInterpreter") {
steps {


scm {
      github("Rajnish-TheGreat/Groovy_with_jenkins", "master")
    }
triggers {
      scm("* * * * *")
    }
shell("sudo cp -rvf * /groovy_web")
if(shell("ls /groovy_web/ | grep html")) {
      dockerBuilderPublisher {
            dockerFileDirectory("/groovy_web/")
            cloud("Kube_slave")
tagsString("rajnishthegreat/html:v1")
            pushOnSuccess(true)
      
            fromRegistry {
                  url("rajnishthegreat")
                  credentialsId("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            }
            pushCredentialsId("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            cleanImages(false)
            cleanupWithJenkinsJobDelete(false)
            noCache(false)
            pull(true)
      }
}
else {
      dockerBuilderPublisher {
            dockerFileDirectory("/groovy_web/")
            cloud("Kube_slave")
tagsString("rajnishthegreat/php:v1")
            pushOnSuccess(true)
      
            fromRegistry {
                  url("rajnishthegreat")
                  credentialsId("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            }
            pushCredentialsId("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            cleanImages(false)
            cleanupWithJenkinsJobDelete(false)
            noCache(false)
            pull(true)
      }
}
 }
}


job("GroovyDeployment") {


  triggers {
    upstream {
      upstreamProjects("GroovyCodeInterpreter")
      threshold("SUCCESS")
    }  
  }


  steps {
    if(shell("ls /groovy_web | grep html")) {


      shell("if sudo kubectl get pv html-pv-vol; then if sudo kubectl get pvc html-pv-vol-claim; then echo "volume present"; else kubectl create -f html-pv-vol-claim.yml; fi; else sudo kubectl create -f html-pv-vol.yml; sudo kubectl create -f html-pv-vol-claim.yml; fi; if sudo kubectl get deployments html-deploy; then sudo kubectl rollout restart deployment/html-deploy; sudo kubectl rollout status deployment/html-deploy; else sudo kubectl create -f web-deploy-html.yml; sudo kubectl create -f webserver_expose.yml; sudo kubectl get all; fi")       


  }


    else {


      shell("if sudo kubectl get pv php-pv-vol; then if sudo kubectl get pvc php-pv-vol-claim; then echo "volume present"; else kubectl create -f php-pv-vol-claim.yml; fi; else sudo kubectl create -f php-pv-vol.yml; sudo kubectl create -f php-pv-vol-claim.yml; fi; if sudo kubectl get deployments php-deploy; then sudo kubectl rollout restart deployment/php-deploy; sudo kubectl rollout status deployment/php-deploy; else sudo kubectl create -f web-deploy-php.yml; sudo kubectl create -f webserver_expose.yml; sudo kubectl get all; fi")


    }
  }
}


job("Monitor") {
  
  triggers {
     scm("* * * * *")
   }


steps {
    
    shell('export status=$(curl -siw "%{http_code}" -o /dev/null 192.168.99.105:30303); if [ $status -eq 200 ]; then exit 0; else python3 mail.py; exit 1; fi')
  }
}


job("Redeployment") {


triggers {
    upstream {
      upstreamProjects("Monitor")
      threshold("FAILURE")
    }
  }
  
  
  publishers {
    postBuildScripts {
      steps {
        downstreamParameterized {
  	  	  trigger("GroovyCodeInterpreter")
        }
      }
    }
  }
}