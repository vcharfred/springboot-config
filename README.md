# Springboot 自定义配置中心

当前只有SpringCloud有一个使用git仓库做的配置中心组件，但是对于不使用微服务或者是只是使用Springboot时就没法操作了；
因此可以参考SpringCloud的配置中心实现的原理来实现一个适合Springboot的轻量级配置中心。

## 一、通过阿里云的ACM产品做配置中心

* [阿里云ACM免费开通使用](https://promotion.aliyun.com/ntms/yunparter/invite.html?userCode=yk3sqxxe)
* [阿里云ACM文档](https://help.aliyun.com/product/59604.html)

阿里云官方提供的工具包只有Java直接使用的以及spring cloud才可以使用的包，还有个是使用阿里云的nocos配置中心的sdk包。
但是如果就只是使用Springboot时则需要自己去实现；

spring-boot-starter-acm-config 是一个借鉴阿里云官方提供的sdk包进行重新封装的包；实现在springboot启动时，创建相关资源前，读取远程的配置来替换本地配置的效果；
使我们打的jar包或者是war包可以一次打包即可，其他相关配置通过读取远程的配置来处理。

如：将数据库、redis等配置放在ACM配置中心中。

### 如何使用
在springboot项目中引入spring-boot-starter-acm-config项目（记得本地自己编译哦）
        
         <dependency>
            <groupId>top.vchar.alibaba</groupId>
            <artifactId>spring-boot-starter-acm-config</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

在springboot的application.yml或者application.properties等启动配置文件中配置ACM相关配置

    alibaba:
      acm:
        group: DEFAULT_GROUP
        endpoint: acm.aliyun.com
        namespace: your namespace
        access-key: your access-key
        secret-key: your secret-key
        # 配置dataId必须添加文件后缀，支持properties或yml文件
        application-data-id: your start properties file data id
        # 对于properties或yml文件dataId必须添加文件后缀；多个使用英文逗号分隔
        data-id-list: dev.redis.yml,dev-druid-mysql.yml
        # 默认环境变量配置优先，若设置为false则会将环境变量中的值替换为配置文件中的值
        vm-priority: true
        
之后启动springboot项目即可


也可以通过Java启动参数来设置

    -Dalibaba.acm.application-data-id   设置 application-data-id
    -Dalibaba.acm.data-id-list          设置 data-id-list
    -Dalibaba.acm.group                 设置 group
    -Daddress.server.domain             设置 endpoint
    -Dtenant.id                         设置 namespace
    -Dram.role.name                     设置 授权用户名
    -Dalibaba.acm.access-key            设置 access-key
    -Dalibaba.acm.secret-key            设置 secret-key

> role.name优先级高于access-key和secret-key
    
<span style="color:red">注意jvm 环境设置优先级高于配置文件中的配置</span>

#### 注意
##### a.在本地使用HSF服务时注意在启动时添加如下jvm启动参数

    -Daddress.server.domain=endpoint
    如：
    -Daddress.server.domain=acm.aliyun.com
#### b.使用阿里云的EDAS和SEA时的区别

在EDAS或ECS服务器时不需要配置相关账号信息，只需要对对应的服务器授权即可，在程序启动时阿里云会自动注入相关账户信息；只需要配置要加载的配置文件即可；
SEA目前不支持ACM自动授权，因此若是jar包启动则需要设置启动参数（acm的sdk包默认jvm启动参数优先级最高）；若是war包则需要设置vm-priority为false，并springboot的配置文件中设置账户信息，工具包将会覆盖阿里云自动注入信息；

### 实现原理
实现``org.springframework.boot.env.EnvironmentPostProcessor``接口，在```postProcessEnvironment```中做配置更新，
同时设置排序序号大于```org.springframework.boot.context.config.ConfigFileApplicationListener```的序号，让在其之后加载配置信息，保证新修改的配置能够不被覆盖。

同时创建一个spring.factories文件，在里面配置如下信息，让springboot可以去扫描到

    # Environment Post Processors
    org.springframework.boot.env.EnvironmentPostProcessor=top.vchar.alibaba.acm.ACMConfigEnvironmentPostProcessor