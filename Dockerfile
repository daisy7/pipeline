FROM centos

RUN set -ex \
    && curl -o /etc/yum.repos.d/CentOS-Base.repo http://mirrors.aliyun.com/repo/Centos-7.repo \
    && curl -o /etc/yum.repos.d/epel.repo http://mirrors.aliyun.com/repo/epel-7.repo \
    && yum makecache \
    && yum install -y \
    #  安装清单：wget,git,zip,unzip,bzip2,python36,java8,sshd,srecord,make
    wget git zip unzip bzip2 python36 java-1.8.0-openjdk srecord make lzo \
    openssh-server passwd ssh-keygen \
    && echo 'root' | passwd root --stdin \
    && ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key -N "" -q && \
    ssh-keygen -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key -N "" -q && \
    ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key -N "" -q
EXPOSE 22
ENTRYPOINT ["/usr/sbin/sshd","-D"]