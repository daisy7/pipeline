pipeline{
    agent{
        label "192.168.1.76"
    }
    options {
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '20', numToKeepStr: '50')
    }
    stages{
        stage("备份gerrit"){
            steps{
                script{
                    sh '''
                        host=yangxw@101.91.119.66
id_rsa=./${host}id_rsa
chmod 600 ${id_rsa}
user_path=/home/userdata/yangxw
tar_file=gerrit*.tar.gz

ssh $host -i $id_rsa << EOF
rm -f $tar_file
tar -zcf gerrit-`date +%Y%m%d%H%M`.tar.gz ${user_path}/dockerdata/gerrit
EOF

scp -i $id_rsa $host:${user_path}/$tar_file .

find . -mtime +7 -name "gerrit*.tar.gz" -exec rm -rf {} \\;
                    '''
                }
            }
        }
    }
    post{
		failure{
            emailext body: '$DEFAULT_CONTENT', recipientProviders: [requestor()], replyTo: '$DEFAULT_REPLYTO', subject: '$DEFAULT_SUBJECT', to: '$DEFAULT_RECIPIENTS'
		}
	}
}