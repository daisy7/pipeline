#common
host_ip=101.91.119.66
params=" --restart always \
-v /etc/localtime:/etc/localtime \
-v /etc/timezone:/etc/timezone \
--add-host jenkins.m2motive.com:$host_ip \
--add-host sonarqube.m2motive.com:$host_ip \
--add-host file.m2motive.com:$host_ip \
--add-host gitlab.m2motive.com:$host_ip"

#gitbook
docker run -p 4000:4000 --name gitbook \
    $params \
    -v /home/daisy/httpd/www/gitbook/gitbook:/srv/gitbook \
    -v /home/daisy/httpd/www/gitbook/html:/srv/html \
    fellah/gitbook
#jenkins-slave
docker run -d -p 2222:22 --name jenkins-slave \
    $params \
    jenkins-slave
#centos
docker run -it --name centos \
    centos
    bash
#mongo
WORKSPACE=/data/mongo/ &&
docker run -d -p 27017:27017 --name mongo \
    $params \
    -v $WORKSPACE:/data/db \
    mongo
#gitlab-ce
WORKSPACE=/home/daisy/gitlab &&
sudo docker run -d -p 443:443 -p 80:80 -p 222:22 --name gitlab \
    $params \
    -v $WORKSPACE/config:/etc/gitlab \
    -v $WORKSPACE/logs:/var/log/gitlab \
    -v $WORKSPACE/data:/var/opt/gitlab \
    gitlab/gitlab-ce
#httpd
WORKSPACE=/home/daisy/httpd &&
sudo docker run -d --name httpd \
    -p 9002:80 \
    -v $WORKSPACE/www/:/usr/local/apache2/htdocs/ \
    -v $WORKSPACE/logs/:/usr/local/apache2/logs/ \
    $params \
    httpd
    # -v $WORKSPACE/conf:/usr/local/apache2/conf \

#jenkins
WORKSPACE=/home/daisy/jenkins &&
sudo docker run -d --name jenkins \
    -p 9001:8080 \
    -p 50000:50000 \
    $params \
    -v $WORKSPACE:/var/jenkins_home \
    jenkins/jenkins:lts
#sonarqube
WORKSPACE=/home/daisy/sonarqube &&
sudo docker run -d --name sonarqube \
    -p 9000:9000 \
    $params \
    -v $WORKSPACE/conf:/opt/sonarqube/conf \
    -v $WORKSPACE/data:/opt/sonarqube/data \
    -v $WORKSPACE/logs:/opt/sonarqube/logs \
    -v $WORKSPACE/extensions:/opt/sonarqube/extensions \
    -e "SONARQUBE_JDBC_USERNAME=sonar" \
    -e "SONARQUBE_JDBC_PASSWORD=sonar" \
    -e "SONARQUBE_JDBC_URL=jdbc:mysql://192.168.1.19:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance&useSSL=false" \
    sonarqube:lts

#sonar sql
create database sonar;
CREATE USER 'sonar'@'%' IDENTIFIED WITH mysql_native_password BY 'sonar';
GRANT ALL PRIVILEGES ON *.* TO 'sonar'@'%';
#sonar-scanner
sonar-scanner \
  -Dsonar.projectKey=EC01 \
  -Dsonar.sources=. \
  -Dsonar.host.url=http://192.168.1.199:9000 \
  -Dsonar.login=059bf44a6740bafbc02ee5cfe426bb3d925f1406 \
  -Dsonar.sourceEncoding=GBK -X \
  -Dsonar.exclusions=**/*sqlite3*

#mysql5.7
WORKSPACE=/home/daisy/mysql57 &&
docker run -d --name mysql57 \
    -p 3306:3306 \
    -v $WORKSPACE/conf:/etc/mysql/conf.d \
    -v $WORKSPACE/logs:/logs \
    -v $WORKSPACE/data:/var/lib/mysql \
    -e MYSQL_ROOT_PASSWORD=123456 \
    $params \
    mysql:5.7
#mysql8
docker run -d --name mysql8 \
    -p 3306:3306 \
    -v $PWD/conf:/etc/mysql/conf.d
    -v $PWD/logs:/logs
    -v $PWD/data:/var/lib/mysql \
    -e MYSQL_ROOT_PASSWORD=123456 \
    $params \
    mysql
#samba
WORKSPACE=/home/samba &&
docker run -d --name samba \
    -p 139:139 \
    -p 445:445 \
    $params \
    -v $WORKSPACE:/mount \
    dperson/samba \
    -u "chenning;chenning;1001;hw1" \
    -u "wangkang;wangkang;1002;hw1" \
    -u "luoxiaolong;luoxiaolong;1003;hw2" \
    -u "yangjiacheng;yangjiacheng;1004;hw2" \
    -u "duhang;duhang;1005;hw2" \
    -u "yangjun;yangjun;1006;hw2" \
    -u "wanghao;wanghao;1007;hw2" \
    -u "zhupei;zhupei;1008;hw2" \
    -u "liyanbo;liyanbo;1009;hw2" \
    -u "yuemei;yuemei;1010;hw2" \
    -u "qusanlang;qusanlang;1011;hw3" \
    -u "fanbo;fanbo;1012;hw3" \
    -u "weitao;weitao;1013;dqa_hw" \
    -u "zhaozhanhua;zhaozhanhua;1014;dqa_hw" \
    -s "hardware;/mount/public;yes;no;yes;all;%S;%S" \
    -s "hw1;/mount/hw1;yes;no;yes;all;fanbo,chenning;@hw1" \
    -s "hw2;/mount/hw2;yes;no;yes;all;fanbo;@hw2" \
    -s "hw3;/mount/hw3;yes;no;yes;all;fanbo;@hw3" \
    -s "dqa_hw;/mount/dqa_hw;yes;no;yes;all;fanbo;@dqa_hw"

    <name> is how it's called for clients
    <path> path to share
    NOTE: for the default values, just leave blank
    [browsable] default:'yes' or 'no'
    [readonly] default:'yes' or 'no'
    [guest] allowed default:'yes' or 'no'
    [users] allowed default:'all' or list of allowed users
    [admins] allowed default:'none' or list of admin users
    [writelist] list of users that can write to a RO share
    [comment] description of share