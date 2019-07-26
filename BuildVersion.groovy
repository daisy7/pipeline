if(!node){
    error '节点参数node为空，请传入后重试！'
}
matcher = repo =~"[^/]+(?!.*/).*?(?=.git)"//正则匹配仓库的后缀
repo_dir = matcher[0]
matcher=null
outside="http://file.m2motive.com:9000"//外部文件服务器地址
inside="http://192.168.1.19:9002"//内部文件服务器地址
suffix="VersionRelease/${repo_dir}"//版本发布地址
inside_release="${inside}/${suffix}"
outside_release="${outside}/${suffix}"
projectTemplate="${inside}/ProjectTemplate"//项目依赖文件地址
systemVersion="${inside}/SystemVersion"//项目依赖系统版本地址
inside_vpBin="${inside}/VpBin"//vp编译使用外网服务器
outside_vpBin="${outside}/VpBin"//vp编译使用内网服务器

template="";doc="";ap_bin="";vp_bin="";software="";ap_dir="";vp_dir=""//全局变量

def make_template(){
    dir(repo_dir) {
        template="${workspace}/${repo_dir}/template"//版本包解压路径
        doc="${template}/Doc"
        ap_bin="\"${template}/R&D/ap_bin\""//版本包中apbin路径
        vp_bin="\"${template}/R&D/vp_bin\""//版本包中vpbin路径
        software="${template}/SoftWare"
        ap_dir="${software}/ap"
        vp_dir="${software}/vp"
        sh "rm -rf ${template}"//每次清空上次目录
        sh "mkdir -p ${doc} ${ap_bin} ${vp_bin} ${ap_dir} ${vp_dir}"
    }
}
def checkout(){
    if(!repo||!branch){
        error "git仓库信息不全，检查参数repo,branch,credentialsId"
    }
    dir(repo_dir) {//在项目目录下拉取最新的代码
        git branch: branch, credentialsId: credentialsId, url: repo
    }
}
def ap(){
    def ap_path="${workspace}/${repo_dir}/project/**/ap/"
    def gcc_file="gcc-eglibc-locale-internal-arm-oe-linux-gnueabi-6.4.0.tar.bz2"
    if(fileExists("${gcc_file}"))//判断gcc编译工具是否存在
        echo "${gcc_file} is exists"
    else//下载并解压gcc编译工具
        sh "wget -q ${projectTemplate}/${gcc_file} && mkdir -p gcc && tar -jxf ${gcc_file} -C gcc"
    dir(repo_dir) {//修改ap config.rc的gcc工具的配置路径并编译工程
        sh "cd ${ap_path} && sed s#^TEMP_PATH=.*/#TEMP_PATH=${workspace}/# config.rc -i && sed s#^TOOLS=.*/#TOOLS=${workspace}/# config.rc -i && ls -l && . ${ap_path}/config.rc && make clean && make all"
    }
    sh "/bin/cp -r ${ap_path}/bin/* ${ap_bin}"//复制ap编译的产物
}
def vp(){
    def set_path="set path=C:/Program Files/Git/usr/bin;C:/Program Files/Git/mingw64/bin;C:/Program Files (x86)/Renesas Electronics/CS+/CC;"

    checkout()

    vp_path="${repo_dir}/project/m2m*/vp"

    bat label: '', script: "${set_path} && cd ${vp_path} && del /f/s/q bin \n for /f \"delims=\" %%a in ('dir /s/a/b *.mtpj') do set mtpj=%%a \n CubeSuite+.exe /bb %mtpj%"

    def vpbinFile="${repo_dir}-vpbin.tgz"
    bat label: '', script: "${set_path} && cd ${vp_path}/bin && curl -O ${outside_vpBin}/${repo_dir}-vpbin_com.tgz && tar -xzvf ${repo_dir}-vpbin_com.tgz && del/f/s/q ${repo_dir}-vpbin_com.tgz && dir && tar -czvf ${vpbinFile} * && curl -X PUT --data-binary @${vpbinFile} ${outside_vpBin}/${vpbinFile}"
}
def mulpart(){
    sh "wget -q ${inside_vpBin}/${repo_dir}-vpbin.tgz && tar -xzvf ${repo_dir}-vpbin.tgz -C ${vp_bin} && rm -vf ${repo_dir}-vpbin.tgz"//下载vp编译的产物
    sh "srec_cat ${vp_bin}/*Padded.mot -o ${vp_bin}/eagle.s19 -Line_Length=46 -address-length=4 -Execution_Start_Address=0xffffffff -disable=data-count"//srec_cat工具生成s19文件
    sh "cd ${vp_bin} && sed '1d;\$d' *Release.mot > tmp &&  sed '1 r tmp' *Padded.mot > ${fbl}.mot && rm -f tmp && mv -f ${fbl}.mot ${vp_dir}"//合并vp的俩个文件，并复制到版本包的路径
    dir("${software}/mulpart") {
        sh "wget -q -O flash_driver.s19 ${projectTemplate}/${repo_dir}-flash_driver.s19 && wget -q ${projectTemplate}/mulpart.conf"
        sh "mv ${vp_bin}/eagle.s19 ."//复制s19文件
        try {
            sh "sed -i s/^VP_FILENUM=.*/VP_FILENUM=${VP_FILENUM}/ mulpart.conf"
        } catch (err) {
            echo "${err},[VP_FILENUM use default from mulpart.conf]"
        }
        sh "sha1sum eagle.s19 |cut -d ' ' -f 1|xargs -i sed -i s/^VP_CHKSUM=.*/VP_CHKSUM={}/ mulpart.conf && cat mulpart.conf"//修改配置 sha1
        sh "tar -cvf mulpart.tar *"
        sh "mv -f mulpart.tar ../"
    }
    sh "rm -rf ${template}/**/*@tmp ${software}/mulpart"

    template_file=create_ubi
    sh "rm -vrf ${template_file} && wget -q ${projectTemplate}/${template_file}.zip && unzip -o ${template_file}.zip -d ${template_file} && chmod 755 -R ${template_file}"

    sh "/bin/cp -f ${software}/mulpart.tar ${template_file}/mdm9607-data1/.default"//复制mulpart文件到工具目录

    sh "cd ${template_file}/mdm9607-data1/conf && echo -e \"${m2m_version}\"> m2m_version && cat m2m_version && wget -O project_info ${projectTemplate}/${repo_dir}-project_info && cat project_info"//生成配置文件

    sh "/bin/cp -r ${ap_bin}/* ${template_file}/mdm9607-app/appfs_a/bin/"//复制ap bin下文件

    sh "cd ${repo_dir} && echo -e \"${m2m_version}\" | sed /^APVer=.*/p -n|cut -d '=' -f 2 |xargs -i echo {}.zip > tmp && APVerFile=`cat tmp` && rm -f tmp && if [ ! -f \"\$APVerFile\" ];then rm -rf CL0* && wget -q ${systemVersion}/\$APVerFile; fi && unzip -o \$APVerFile -d '${ap_dir}'"//根据参数判断是否存在系统版本文件，如果不存在则下载，解压版本包指定目录

    backup = backup == "true"?"&& cd ${ap_dir} && python3 ${workspace}/backup_images.py sbl1.mbn:sbl tz.mbn rpm.mbn appsboot.mbn:aboot mdm9607-boot.img:boot NON-HLOS.ubi:modem mdm9607-app.ubi:app mdm9607-data1.ubi:data1 mdm9607-sysfs.ubi:system backup.bin && ls -l":""

    sh "cd ${template_file} && sh -x create_usr_ubi_mcp*.sh all && /bin/cp image/*.ubi ${ap_dir} ${backup}"//复制生成的ubi文件到版本包指定目录并打包backup.bin
}

def dist(){
    dir(repo_dir){
        tar="${repo_dir}-${new Date().format('yyyyMMddHHmm')}.zip"
        sh "cd ${template} && zip -r ${tar} * && curl -X PUT --data-binary @${tar} ${inside_release}/${tar}"//压缩版本包目录并上传版本包到文件服务器
    }
    echo "please download ${outside_release}/${tar}"
}

pipeline{
    agent{
        label "${node}"
    }
    // options {
    //     buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '30', numToKeepStr: '100')
    // }
    // parameters {
        // string defaultValue: '192.168.1.19', description: '运行的节点', name: 'node', trim: true
        // string defaultValue: 'ssh://wangkaikai@101.91.119.66:29418/T3_Project.git', description: '构建的git仓库地址', name: 'repo', trim: true
        // string defaultValue: 'master', description: '仓库分支', name: 'branch', trim: true
        // string defaultValue: '9911912c-4698-431b-acdd-a104b7afaec8', description: '下载仓库的凭证', name: 'credentialsId', trim: true
        // string defaultValue: 'APVer=CL01B22_03_011SPI\\nSMVer=BE48GT5038\\nAPDate=`date +%Y-%m-%d`', description: '设置m2m_version版本信息使用\\n换行', name: 'm2m_version', trim: true
        // string defaultValue: '101.91.118.224', description: '编译vp使用的windows执行机', name: 'vp_agent', trim: true
        // string defaultValue: 'm2m_be48_gt3_fbl_Padded', description: 'vp产物文件名', name: 'fbl', trim: true
        // string defaultValue: 'create_ubi_2k', description: '使用的哪个版本的create_ubi工具', name: 'create_ubi', trim: true
        // booleanParam defaultValue: true, description: '是否使用全区备份', name: 'backup'
    // }
    stages{
        stage("下载代码"){
            steps{
                script{
                    checkout()
                }
            }
        }
        stage("构造产物目录结构"){
            steps{
                script{
                    make_template()
                }
            }
        }
        stage("并行编译ap/vp"){
            parallel {
                stage("编译ap"){
                    steps{
                        script{
                            ap()
                        }
                    }
                }
                stage("编译vp"){
                    agent{
                        label vp_agent
                    }
                    steps{
                        script{
                            vp()
                        }
                    }
                }
            }
        }
        stage("制作mulpart"){
            steps{
                script{
                    mulpart()
                }
            }
        }
        stage("发布版本"){
            steps{
                script{
                    dist()
                }
            }
        }
    }
    post{
		always{
            emailext body: '$DEFAULT_CONTENT', recipientProviders: [requestor()], replyTo: '$DEFAULT_REPLYTO', subject: '$DEFAULT_SUBJECT', to: '$DEFAULT_RECIPIENTS'
		}
	}
}