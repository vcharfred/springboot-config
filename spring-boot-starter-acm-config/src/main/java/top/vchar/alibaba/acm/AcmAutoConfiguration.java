package top.vchar.alibaba.acm;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;

/**
 * <p> 配置文件监听器 </p>
 *
 * @author vchar fred
 * @version 1.0
 * @create_date 2019/10/9 20:12
 */
@Configuration
@EnableConfigurationProperties({AcmProperties.class})
public class AcmAutoConfiguration implements ApplicationContextAware {

    @Autowired
    private AcmProperties acmProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        List<String> dataIdList = acmProperties.getDataIdList();
        if(null!=dataIdList && dataIdList.size()>0){
            ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();

            //TODO 监听配置文件变更监听，刷新配置

        }
    }
}
