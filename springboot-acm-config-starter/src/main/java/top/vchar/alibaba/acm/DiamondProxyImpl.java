package top.vchar.alibaba.acm;

import com.alibaba.edas.acm.ConfigService;
import com.alibaba.edas.acm.exception.ConfigException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * <p> get acm config </p>
 *
 * @author vchar fred
 * @version 1.0
 * @create_date 2019/10/11 22:16
 */
public class DiamondProxyImpl implements DiamondProxy{

    /**
     * get config
     * @param dataId dataId
     * @param group group
     * @param timeoutMs time out ms
     * @return return config info
     * @throws ConfigException config exception
     */
    @Override
    public String getConfig(String dataId, String group, long timeoutMs) throws ConfigException {
        return ConfigService.getConfig(dataId, group, timeoutMs);
    }

    /**
     * get a yml or properties config
     * @param dataId dataId
     * @param group group
     * @param timeoutMs time out ms
     * @return return config info
     */
    @Override
    public Properties getProperties(String dataId, String group, long timeoutMs){
        try {
            String data = getConfig(dataId, group,timeoutMs);
            if (!StringUtils.isEmpty(data)) {
                if (dataId.endsWith(".yaml") || dataId.endsWith(".yml")) {
                    YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
                    yamlFactory.setResources(new ByteArrayResource(data.getBytes(StandardCharsets.UTF_8)));
                    return yamlFactory.getObject();
                }
                Properties properties = new Properties();
                properties.load(new StringReader(data));
                return properties;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
