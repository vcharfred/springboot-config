package top.vchar.alibaba.acm;

import com.alibaba.edas.acm.ConfigService;
import com.taobao.diamond.client.impl.TenantUtil;
import com.taobao.diamond.identify.CredentialService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnumerableCompositePropertySource;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.Ordered;
import org.springframework.core.env.*;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * <p> 通过ACM拉取远程配置信息，更新本地配置 </p>
 *
 * @author vchar fred
 * @version 1.0
 * @create_date 2019/10/19 21:57
 */
public class ACMConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private final DeferredLog logger = new DeferredLog();

    private static final String ACM_PROPERTY_SOURCE_LOADED = "alibaba.acm.config.loaded";

    /**
     * The default order for the processor.
     */
    public static final int DEFAULT_ORDER = ConfigFileApplicationListener.DEFAULT_ORDER + 1;

    private int order = DEFAULT_ORDER;

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        // 获取所有springboot的application配置
        PropertySource<?> applicationConfigurationProperties = propertySources.get("applicationConfigurationProperties");
        if(null==applicationConfigurationProperties){
            logger.warn("don't find applicationConfigurationProperties info");
            return;
        }

        if(null!=applicationConfigurationProperties.getProperty(ACM_PROPERTY_SOURCE_LOADED)){
            return;
        }

        //load acm config from applicationConfigurationProperties
        AcmProperties acmProperties = loadAcmConfig(applicationConfigurationProperties);

        //load acm config from vm, this is not necessary, acm sdk jar will read properties from vm first
        loadAcmConfigFroVm(acmProperties);

        if(null==acmProperties.getEndpoint() || null==acmProperties.getNamespace()){
            logger.warn("no alibaba acm config endpoint and namespace");
            return;
        }

        if(null==acmProperties.getRamRoleName()
                && null==acmProperties.getAccessKey() && null==acmProperties.getSecretKey()){
            logger.error("no alibaba acm role info, you should config 'ram-role-name' or 'access-key'、 'secret-key'");
            throw new RuntimeException("no alibaba acm role info, you should config 'ram-role-name' or 'access-key'、 'secret-key'");
        }

        //init acm config
        acmInit(acmProperties);

        //get Remotely acm config
        Map<String, Object> newSource = loadConfig(acmProperties);
        toMap(acmProperties, newSource);
        newSource.put(ACM_PROPERTY_SOURCE_LOADED, "Y");

        //Refresh config
        List<EnumerableCompositePropertySource> sourceList = (List<EnumerableCompositePropertySource>) applicationConfigurationProperties.getSource();
        EnumerableCompositePropertySource enumerablePropertySource = sourceList.get(0);
        Collection<PropertySource<?>> source2 = enumerablePropertySource.getSource();
        for (PropertySource<?> source : source2) {
            try {
                if (source instanceof PropertiesPropertySource) {
                    PropertiesPropertySource propertySource = (PropertiesPropertySource) source;
                    Map<String, Object> properties = propertySource.getSource();
                    if(properties!=null){
                        newSource.forEach(properties::put);
                    }
                } else {
                    if (source instanceof MapPropertySource) {
                        MapPropertySource mapPropertySource = (MapPropertySource) source;
                        Map<String, Object> mapSource = mapPropertySource.getSource();
                        if (null != mapSource && !mapSource.isEmpty()) {
                            mapSource.putAll(newSource);
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }



    /**
     * load acm config from applicationConfigurationProperties
     * @param applicationConfigurationProperties applicationConfigurationProperties
     * @return return acm config
     */
    private AcmProperties loadAcmConfig(PropertySource<?> applicationConfigurationProperties){
        AcmProperties acmProperties = new AcmProperties();
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.application-data-id")){
            acmProperties.setApplicationDataId(applicationConfigurationProperties.getProperty("alibaba.acm.application-data-id").toString());
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.group")){
            acmProperties.setGroup(applicationConfigurationProperties.getProperty("alibaba.acm.group").toString());
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.endpoint")){
            acmProperties.setEndpoint(applicationConfigurationProperties.getProperty("alibaba.acm.endpoint").toString());
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.namespace")){
            acmProperties.setNamespace(applicationConfigurationProperties.getProperty("alibaba.acm.namespace").toString());
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.access-key")){
            acmProperties.setAccessKey(applicationConfigurationProperties.getProperty("alibaba.acm.access-key").toString());
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.secret-key")){
            acmProperties.setSecretKey(applicationConfigurationProperties.getProperty("alibaba.acm.secret-key").toString());
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.time-out")){
            acmProperties.setTimeOut(Integer.parseInt(applicationConfigurationProperties.getProperty("alibaba.acm.time-out").toString()));
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.ram-role-name")){
            acmProperties.setRamRoleName(applicationConfigurationProperties.getProperty("alibaba.acm.ram-role-name").toString());
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.data-id-list")){
            String dataIds = applicationConfigurationProperties.getProperty("alibaba.acm.data-id-list").toString().trim();
            if(dataIds.length()>0){
                acmProperties.setDataIdList(Arrays.asList(dataIds.split(",")));
            }
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.open-kms-filter")){
            if("true".equals(applicationConfigurationProperties.getProperty("alibaba.acm.open-kms-filter").toString())){
                acmProperties.setOpenKMSFilter(true);
            }else {
                acmProperties.setOpenKMSFilter(false);
            }
        }else {
            acmProperties.setOpenKMSFilter(false);
        }
        if(null!=applicationConfigurationProperties.getProperty("alibaba.acm.region-id")){
            acmProperties.setRegionId(applicationConfigurationProperties.getProperty("alibaba.acm.region-id").toString());
        }
        return acmProperties;
    }

    /**
     * load acm config from vm
     * @param acmProperties  acm config in applicationConfigurationProperties
     */
    private void loadAcmConfigFroVm(AcmProperties acmProperties){
        // 参数先读取jvm参数
        String endpoint = System.getProperty("address.server.domain");
        if (!StringUtils.isEmpty(endpoint)) {
            acmProperties.setEndpoint(endpoint);
        }
        String namespace = TenantUtil.getUserTenant();
        if (!StringUtils.isEmpty(namespace)) {
            acmProperties.setNamespace(namespace);
        }

        String ramRoleName = System.getProperty("ram.role.name");
        if(ramRoleName!=null){
            acmProperties.setRamRoleName(ramRoleName);
        }

        String accessKey = System.getProperty("alibaba.acm.access-key");
        String secretKey = System.getProperty("alibaba.acm.secret-key");

        if(StringUtils.isEmpty(accessKey)){
            accessKey = CredentialService.getInstance().getCredential().getAccessKey();
        }
        if (!StringUtils.isEmpty(accessKey)) {
            acmProperties.setAccessKey(accessKey);
        }

        if(StringUtils.isEmpty(secretKey)){
            secretKey = CredentialService.getInstance().getCredential().getSecretKey();
        }
        if (!StringUtils.isEmpty(secretKey)) {
            acmProperties.setSecretKey(secretKey);
        }

    }

    /**
     * init acm config
     * @param acmProperties acm properties
     */
    private void acmInit(AcmProperties acmProperties){
        try{
            Properties properties = new Properties();
            properties.put("endpoint", acmProperties.getEndpoint());//地域
            properties.put("namespace", acmProperties.getNamespace());//命名空间id
            // 通过 ECS 实例 RAM 角色访问 ACM
            if(null!=acmProperties.getRamRoleName()){
                properties.put("ramRoleName", acmProperties.getRamRoleName());
            }
            if(null!=acmProperties.getAccessKey()){
                properties.put("accessKey", acmProperties.getAccessKey());
            }
            if(null!=acmProperties.getSecretKey()){
                properties.put("secretKey", acmProperties.getSecretKey());
            }
            if(acmProperties.getOpenKMSFilter()){
                // this is encrypt set
                properties.put("openKMSFilter", true);
                properties.put("regionId", acmProperties.getRegionId());
            }
            ConfigService.init(properties);
        }catch (Exception e){
            logger.error("init alibaba acm Exception: "+ e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * get remotely acm config
     *
     * @param acmProperties acm properties
     * @return return config
     */
    private Map<String, Object> loadConfig(AcmProperties acmProperties) {
        Map<String, Object> source = new HashMap<>();
        String group = acmProperties.getGroup();
        if(group==null){
            group = "DEFAULT_GROUP";
        }

        int timeOut = acmProperties.getTimeOut();

        Map<String, Object> applicationMap = null;
        if(null!=acmProperties.getApplicationDataId() && acmProperties.getApplicationDataId().length()>0){
            applicationMap = loadConfig(acmProperties.getApplicationDataId(), group, timeOut);
        }

        List<String> dataIdList = acmProperties.getDataIdList();

        if(null!=applicationMap && !applicationMap.isEmpty() && null!=applicationMap.get("alibaba.acm.data-id-list")){
            String dataIdListStr = applicationMap.get("alibaba.acm.data-id-list").toString();
            try{
                if(null!=dataIdListStr && dataIdListStr.length()>0){
                    dataIdList = Arrays.asList(dataIdListStr.split(","));
                }
            }catch (Exception e){
                logger.error("load acm config data-id-list error", e);
            }
        }

        if(null!=dataIdList && dataIdList.size()>0){
            dataIdList.sort(Comparator.naturalOrder());
            for(String dataId:dataIdList){
                if(null!=dataId && dataId.length()>0){
                    Map<String, Object> map = loadConfig(dataId, group, timeOut);
                    if(null!=map && !map.isEmpty()){
                        source.putAll(map);
                    }
                }
            }
        }

        if(null!=applicationMap && !applicationMap.isEmpty()){
            source.putAll(applicationMap);
        }

        return source;
    }

    private Map<String, Object> loadConfig(String dataId, String group, int timeOut){
        try{

            if(null!=dataId && dataId.trim().length()>0){
                DiamondProxy diamondProxy = new DiamondProxyImpl();
                if(dataId.endsWith(".yaml") || dataId.endsWith(".yml") || dataId.endsWith(".properties")){
                    Properties properties = diamondProxy.getProperties(dataId, group, timeOut);
                    Map<String, Object> source = toMap(properties);
                    if(!source.isEmpty()){
                        return source;
                    }
                }else {
                    //not yaml file or properties file
                    String config = diamondProxy.getConfig(dataId, group, timeOut);
                    if(null!=config && config.length()>0){
                        Map<String, Object> source = new HashMap<>();
                        source.put(dataId, config);
                        return source;
                    }
                }
            }
        }catch (Exception e){
            logger.error(dataId+" get remotely acm config Exception: "+e.getMessage(), e);
        }
        return null;
    }

    private void toMap(AcmProperties acmProperties, Map<String, Object> map){
        if(null!=acmProperties.getApplicationDataId()){
            map.put("alibaba.acm.application-data-id", acmProperties.getApplicationDataId());
        }
        if(null!=acmProperties.getDataIdList()){
            map.put("alibaba.acm.data-id-list", acmProperties.getDataIdList());
        }
        if(null!=acmProperties.getGroup()){
            map.put("alibaba.acm.group", acmProperties.getGroup());
        }
        if(null!=acmProperties.getEndpoint()){
            map.put("alibaba.acm.endpoint", acmProperties.getEndpoint());
        }
        if(null!=acmProperties.getNamespace()){
            map.put("alibaba.acm.namespace", acmProperties.getNamespace());
        }
        if(null!=acmProperties.getAccessKey()){
            map.put("alibaba.acm.access-key", acmProperties.getAccessKey());
        }
        if(null!=acmProperties.getSecretKey()){
            map.put("alibaba.acm.secret-key", acmProperties.getSecretKey());
        }
        map.put("alibaba.acm.time-out", acmProperties.getTimeOut());
        if(null!=acmProperties.getRamRoleName()){
            map.put("alibaba.acm.ram-role-name", acmProperties.getRamRoleName());
        }
        map.put("alibaba.acm.open-kms-filter", acmProperties.getOpenKMSFilter());
        if(null!=acmProperties.getRegionId()){
            map.put("alibaba.acm.region-id", acmProperties.getRegionId());
        }

    }

    private Map<String, Object> toMap(Properties properties) {
        Map<String, Object> result = new HashMap<>();
        if(null!=properties && !properties.isEmpty()){
            for(Map.Entry<Object, Object> entry:properties.entrySet()){
                if(null!=entry.getKey()){
                    if(null!=entry.getValue()){
                        result.put(entry.getKey().toString(), entry.getValue());
                    }else {
                        result.put(entry.getKey().toString(), null);
                    }
                }
            }
        }
        return result;
    }

}
