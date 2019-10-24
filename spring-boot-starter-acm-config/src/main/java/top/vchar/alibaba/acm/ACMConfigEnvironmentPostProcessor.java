package top.vchar.alibaba.acm;

import com.alibaba.edas.acm.ConfigService;
import com.taobao.diamond.client.impl.TenantUtil;
import com.taobao.diamond.identify.CredentialService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p> 通过ACM拉取远程配置信息，更新本地配置 </p>
 *
 * @author vchar fred
 * @version 1.0
 * @create_date 2019/10/7 20:09
 */
public class ACMConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private final DeferredLog logger = new DeferredLog();

    private static final String ACM_PROPERTY_SOURCE_NAME = "alibaba.acm.config";

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
        if(propertySources.contains(ACM_PROPERTY_SOURCE_NAME)){
            return;
        }
        // 获取所有springboot的application配置
        List<PropertySource<?>> applicationConfig = propertySources.stream().filter(p -> p instanceof OriginTrackedMapPropertySource && p.getName().contains("applicationConfig")).collect(Collectors.toList());
        if(applicationConfig.size()==0){
            logger.warn("do not find applicationConfig PropertySource");
            return;
        }

        AcmProperties acmProperties = new AcmProperties();
        if(applicationConfig.size()==1){
            // only one application config
            loadAcmConfig((OriginTrackedMapPropertySource) applicationConfig.get(0), acmProperties);
        }else {
            for(PropertySource<?> propertySource: applicationConfig){
                loadAcmConfig((OriginTrackedMapPropertySource) propertySource, acmProperties);
            }
            String[] activeProfiles = environment.getActiveProfiles();
            if(activeProfiles!=null && activeProfiles.length>0){
                List<PropertySource<?>> activeSources = propertySources.stream().filter(p -> p instanceof OriginTrackedMapPropertySource && p.getName().contains("applicationConfig") && p.getName().contains(activeProfiles[0])).collect(Collectors.toList());
                if(activeSources.size() > 0){
                    loadAcmConfig((OriginTrackedMapPropertySource) activeSources.get(0), acmProperties);
                }
            }
        }

        //load acm config from vm, this is not necessary, acm sdk jar will read properties from vm first
        loadAcmConfigFromSystem(acmProperties);

        Map<String, Object> newSource = toMap(acmProperties);
        //set config to environment
        propertySources.addLast(new OriginTrackedMapPropertySource(ACM_PROPERTY_SOURCE_NAME, newSource));

        //init acm config
        acmInit(acmProperties);

        //get Remotely acm config
        Map<String, Object> config = loadConfig(acmProperties);
        if(!config.isEmpty()){
            newSource.putAll(config);
        }

        //Refresh config
        for(PropertySource<?> propertySource: applicationConfig){
            OriginTrackedMapPropertySource source  = (OriginTrackedMapPropertySource) propertySource;
            source.getSource().putAll(newSource);
        }
    }

    /**
     * read acm config
     * @param applicationPropertySource  environment config
     * @param acmProperties acm config
     */
    private void loadAcmConfig(OriginTrackedMapPropertySource applicationPropertySource, AcmProperties acmProperties){
        Map<String, Object> source =  applicationPropertySource.getSource();
        if(!source.isEmpty()){
            if(null!=source.get("alibaba.acm.application-data-id")){
                acmProperties.setApplicationDataId(source.get("alibaba.acm.application-data-id").toString());
            }
            if(null!=source.get("alibaba.acm.group")){
                acmProperties.setGroup(source.get("alibaba.acm.group").toString());
            }
            if(null!=source.get("alibaba.acm.endpoint")){
                acmProperties.setEndpoint(source.get("alibaba.acm.endpoint").toString());
            }
            if(null!=source.get("alibaba.acm.namespace")){
                acmProperties.setNamespace(source.get("alibaba.acm.namespace").toString());
            }
            if(null!=source.get("alibaba.acm.access-key")){
                acmProperties.setAccessKey(source.get("alibaba.acm.access-key").toString());
            }
            if(null!=source.get("alibaba.acm.secret-key")){
                acmProperties.setSecretKey(source.get("alibaba.acm.secret-key").toString());
            }
            if(null!=source.get("alibaba.acm.time-out")){
                acmProperties.setTimeOut(Integer.parseInt(source.get("alibaba.acm.time-out").toString()));
            }
            if(null!=source.get("alibaba.acm.ram-role-name")){
                acmProperties.setRamRoleName(source.get("alibaba.acm.ram-role-name").toString());
            }
            if(null!=source.get("alibaba.acm.data-id-list")){
                String dataIds = source.get("alibaba.acm.data-id-list").toString().trim();
                if(dataIds.length()>0){
                    acmProperties.setDataIdList(Arrays.asList(dataIds.split(",")));
                }
            }
            if(null!=source.get("alibaba.acm.open-kms-filter")){
                if("true".equals(source.get("alibaba.acm.open-kms-filter").toString())){
                    acmProperties.setOpenKMSFilter(true);
                }else {
                    acmProperties.setOpenKMSFilter(false);
                }
            }else {
                acmProperties.setOpenKMSFilter(false);
            }
            if(null!=source.get("alibaba.acm.region-id")){
                acmProperties.setRegionId(source.get("alibaba.acm.region-id").toString());
            }
        }
    }

    /**
     * load acm config from jvm
     * @param acmProperties acm config
     */
    private void loadAcmConfigFromSystem(AcmProperties acmProperties){
        // 参数先读取jvm参数
        String applicationDataId = System.getProperty("alibaba.acm.application-data-id");
        if(!StringUtils.isEmpty(applicationDataId)){
            acmProperties.setApplicationDataId(applicationDataId);
        }

        String dataIds = System.getProperty("alibaba.acm.data-id-list");
        if(!StringUtils.isEmpty(dataIds)){
            acmProperties.setDataIdList(Arrays.asList(dataIds.split(",")));
        }
        String group = System.getProperty("alibaba.acm.group");
        if(!StringUtils.isEmpty(group)){
            acmProperties.setGroup(group);
        }

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

        String accessKey = CredentialService.getInstance().getCredential().getAccessKey();
        if (!StringUtils.isEmpty(accessKey)) {
            acmProperties.setAccessKey(accessKey);
        }

        String secretKey = CredentialService.getInstance().getCredential().getSecretKey();
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

    private Map<String, Object> toMap(AcmProperties acmProperties){
        Map<String, Object> map = new HashMap<>();
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
        return map;
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
