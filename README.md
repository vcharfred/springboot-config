# Springboot 自定义配置中心
当前只有SpringCloud有一个使用git仓库做的配置中心组件，但是对于不使用微服务或者是只是使用Springboot时就没法操作了；
因此可以参考SpringCloud的配置中心实现的原理来实现一个适合Springboot的轻量级配置中心。

## 一、通过阿里云的ACM产品做配置中心
阿里云官方提供的工具包只有Java直接使用的以及springcloud才可以使用的包，还有个是使用阿里云的nocos配置中心的sdk包。
但是如果就只是使用Springboot时则需要自己去实现；

springboot-acm-config-starter 是一个借鉴阿里云官方提供的sdk包进行重新封装的包；实现在springboot启动时，创建相关资源前，读取远程的配置来替换本地配置的效果；
使我们打的jar包或者是war包可以一次打包即可，其他相关配置通过读取远程的配置来处理。

如：将数据库、redis等配置放在ACM配置中心中。

### 如何使用
在springboot项目中引入springboot-acm-config-starter项目
        
        <dependency>
              <groupId>top.vchar.alibaba.acm</groupId>
              <artifactId>springboot-acm-config-starter</artifactId>
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
        # 配置dataId必须添加文件后缀，支持properties或yml文件；多个使用英文逗号分隔
        data-id-list: dev.system.yml,dev.properties
        
之后启动springboot项目即可

### 实现原理
实现``org.springframework.boot.env.EnvironmentPostProcessor``接口，在```postProcessEnvironment```中做配置更新，
同时设置排序序号大于```org.springframework.boot.context.config.ConfigFileApplicationListener```的序号，让在其之后加载配置信息，保证新修改的配置能够不被覆盖。

同时创建一个spring.factories文件，在里面配置如下信息，让springboot可以去扫描到

    # Environment Post Processors
    org.springframework.boot.env.EnvironmentPostProcessor=top.vchar.alibaba.acm.ACMConfigEnvironmentPostProcessor


## 二、通过git仓库实现
    TODO
## 三、读取启动时设置的jvm参数或者是配置文件
    TODO