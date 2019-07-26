
if(!node){
    error '节点参数node为空，请传入后重试！'
}

matcher = repo =~"[^/]+(?!.*/).*?(?=.git)"
repo_dir = matcher[0]
matcher=null
httpd="http://192.168.1.19:9002"
url="http://192.168.1.19:9000"

def sonar(){
    sonar_scanner_file = "sonar-scanner-cli-3.3.0.1492-linux.zip"
    sonar_scanner_path="${workspace}/sonar_scanner"
    if(fileExists("${sonar_scanner_file}"))
        echo "${sonar_scanner_file} is exists"
    else
        sh "wget -q ${httpd}/${sonar_scanner_file} && mkdir -p ${sonar_scanner_path} && unzip -q ${sonar_scanner_file} -d ${sonar_scanner_path}"
    dir(repo_dir) {
        sh "${sonar_scanner_path}/**/bin/sonar-scanner -Dsonar.projectKey=${repo_dir} -Dsonar.sources=. -Dsonar.host.url=${url} -Dsonar.login=${token} ${eparams}"  
    }
}
def checkout(){
    if(!repo||!branch||!credentialsId){
        error "git仓库信息不全，检查参数repo,branch,credentialsId"
    }
    dir(repo_dir) {
        git branch: branch, credentialsId: credentialsId, url: repo
    }
}
pipeline{
    agent{
        label "${node}"
    }
    stages{
        stage("pipeline"){
            steps{
                timestamps {
                    script{
                        checkout()
                        sonar()
                    }
                }
            }
        }
    }
}