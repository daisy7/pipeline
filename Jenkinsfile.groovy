def Decorate(msg){
    return "# "*45+msg+" #"*45
}
//复制环境配置文件
def copy_env_conf(){
    echo Decorate('复制环境的配置文件')
    if(!env_ip){
        env_ip=node
    }
    sh "/bin/cp env_configs/config_${env_ip}/* FSO-install/FSO_install/fso_test/config/"
}
//自定义更新覆盖fso脚本
def update_upgrade_script(){
    dir('FSO-install') {
        git url: 'http://isource.huawei.com/cloudos-infra/FSO-install.git'
        sh "/bin/cp ../FSO-install-fsp/ssh_base.py FSO_install/site-packages/fsoutil"
        sh "/bin/cp ../FSO-install-fsp/do_upgrade.py ../FSO-install-fsp/do_rollback.py FSO_install/fso_test/upg/test_normal"
        sh "/bin/cp ../FSO-install-fsp/upg_util.py FSO_install/fso_test/upg/upg_base"
        sh "/bin/cp ../FSO-install-fsp/const.py FSO_install/site-packages/fsoapi/CPS"
    }
}
//环境升级

def getPyPath(){
    return "export PYTHONPATH=$WORKSPACE/FSO-install/FSO_install/site-packages/"
}

def custom(){
    if(custom_service)
        return "--custom=${custom_service}"
    return ""
}

def upgrade(){
    echo Decorate('开始升级')
    update_upgrade_script()
    copy_env_conf()
    def pypath=getPyPath()
    def custom_str=custom()
    sh "${pypath} && python FSO-install/FSO_install/fso_test/upg/test_normal/do_upgrade.py ${custom_str}"
    script{
        if(after_upgrade){
            sh after_upgrade
        }
    }
    echo Decorate('升级成功')
}
//环境回退
def rollback(){
    echo Decorate('开始回退')
    update_upgrade_script()
    copy_env_conf()
    def pypath=getPyPath()
    def custom_str=custom()
    sh "${pypath} && python FSO-install/FSO_install/fso_test/upg/test_normal/do_rollback.py ${custom_str}"
    echo Decorate('回退成功')
}
//cid相关，构建，下载，推包
def cid(){
    echo Decorate("cid_info")
    def str=''
    cid_info.split().each{
        str+="'"+it+"' "
    }
    sh "python .cid/cid_service.py ${str}"
    echo Decorate("cid_info")
    if(after_cid){
        sh after_cid
    }
}
//nosetests测试
def nosetests(){
    echo Decorate("开始执行${nosetests_type} nosetests阶段")
    def nosetests_result_dir="nosetests_result"
    sh "rm -rf ${nosetests_result_dir}/*"
    sh "mkdir -p ${nosetests_result_dir}"
    dir('DevTests') {
        git branch:test_git_branch,credentialsId: credential, url: test_git
        if(before_nosetests){
            sh before_nosetests
        }
        if(!nosetests_dir)
            nosetests_dir="./ecs_tempest_plugin/tests/scenario/upgrade"
        if(!nosetests_params)
            nosetests_params="-s -v  --processes=4 --process-timeout=4800 --process-restartworker -m '^test_' --html-jinja-template ./report_for_cloud_test.jinja2 --with-xunit"
        sh "nosetests  ${nosetests_params} --xunit-file=../${nosetests_result_dir}/nosetests_${nosetests_type}.xml --with-html-output --html-out-file=../${nosetests_result_dir}/nosetests_${nosetests_type}.html  ${nosetests_dir}/${nosetests_type} ||exit 0"
    }
    sh "python check_nosetests_result.py ${nosetests_result_dir}"
    echo Decorate("执行${nosetests_type} nosetests阶段完成")
}

def choose_branch(){
    if (branch){
        git branch: branch, credentialsId: '1bdadda3-4c11-422b-82f6-504019f9155a', url: 'http://code-xa.huawei.com/wWX570237/CloudPipeline.git'
    }
    sh "git branch"
}
if(!node){
    error 'node参数为空，请传入后重试！'
}
pipeline{
    agent{
        label "${node}"
    }
    stages{
        stage("pipeline"){
            steps{
                timestamps {
                echo '阶段信息: '+choose_stage
                    script{
                        choose_branch()
                        choose_stage=choose_stage.split(',')
                        choose_stage.each{stage->
                            echo '执行阶段:'+stage
                            if(stage.contains('cid')){
                                cid()
                            }
                            else if(stage.contains('nosetests')){
                                if(test_count)
                                    test_count=Integer.parseInt(test_count)
                                else
                                    test_count=1
                                if(test_interval)
                                    test_interval=Integer.parseInt(test_interval)
                                else
                                    test_interval=0
                                for(int i = 1;i<=test_count;i++) {
                        	     echo "######第${i}次执行测试#####"
                        	     nosetests()
                        	     sleep test_interval
                                }
                            }
                            else if(stage.contains('upgrade')){
                                upgrade()
                            }
                            else if(stage.contains('rollback')){
                                rollback()
                            }
                            else{
                                error '不存在要执行的阶段，检查choose_stage参数信息'
                            }
                        }
                    }
                }
            }
        }
    }
}
