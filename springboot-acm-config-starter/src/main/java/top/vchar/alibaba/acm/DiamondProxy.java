package top.vchar.alibaba.acm;

import com.alibaba.edas.acm.exception.ConfigException;

import java.util.Properties;

/**
 * <p> get acm config </p>
 *
 * @author vchar fred
 * @version 1.0
 * @create_date 2019/10/11 22:16
 */
public interface DiamondProxy {

    /**
     * get config
     * @param dataId dataId
     * @param group group
     * @param timeoutMs time out ms
     * @return return config info
     * @throws ConfigException config exception
     */
    String getConfig(String dataId, String group, long timeoutMs) throws ConfigException;

    /**
     * get a yml or properties config
     * @param dataId dataId
     * @param group group
     * @param timeoutMs time out ms
     * @return return config info
     */
    Properties getProperties(String dataId, String group, long timeoutMs);

}
