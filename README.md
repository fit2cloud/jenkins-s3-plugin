FIT2CLOUD AWS-s3-Plugin for Jenkins
====================
建立统一的artifacts仓库是后续的持续部署的前提。目前，建立artifacts仓库大致有如下三种选择：

1. FTP服务器：很多用户仍然在用这种方式存储Artifact
2. 专业的Artifacts存储仓库：比如Nexus, Artifactory等。
3. 对象存储服务：比如AWS S3等。

Jenkins是当前最常用的CI服务器，FIT2CLOUD Jenkins S3 Plugin的功能是：将构建后的artifact上传到S3的指定位置上去。
 	
一、安装说明
-------------------------

插件下载地址：http://repository-proxy.fit2cloud.com:8080/content/repositories/releases/org/jenkins-ci/plugins/jenkins-s3-plugin/0.6/jenkins-s3-plugin-0.6.hpi
在Jenkins中安装插件, 请到 Manage Jenkins | Advanced | Upload，上传插件(.hpi文件)
安装完毕后请重新启动Jenkins

二、配置说明
-------------------------

在使用插件之前，必须先在[Manage Jenkins | Configure System | 亚马逊S3账户设置]中配置亚马逊帐号的Access Key、Secret Key.


三、Post-build actions: 上传Artifact到亚马逊S3
-------------------------

在Jenkins Job的Post-build actions，用户可以设上传Artifact到亚马逊S3。需要填写的信息是：

1. Bucket名称: artifact要存放的bucket
2. 要上传的artifacts: 文件之间用;隔开。支持通配符描述，比如 text/*.zip
3. Object前缀设置：可以设置object key的前缀，支持Jenkins环境变量比如: "${JOB_NAME}/${BUILD_ID}/${BUILD_NUMBER}/"

假设一个job的名称是test，用户的设置如下

1. bucketName: f2c
2. 要上传的artifacts: hello.txt;hello1.txt
3. Object前缀: ${JOB_NAME}/${BUILD_ID}/${BUILD_NUMBER}

那么上传后的文件url为: 


四、插件开发说明
-------------------------

1. git clone git@github.com:fit2cloud/jenkins-s3-plugin.git
2. mvn -Declipse.workspace=jenkins-s3-plugin eclipse:eclipse eclipse:add-maven-repo
3. import project to eclipse
4. mvn jdi:run 进行本地调试
5. mvn package 打包生成hpi文件

如果有问题，请联系zhimin@fit2cloud.com
