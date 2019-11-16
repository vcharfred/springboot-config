package top.vchar.alibaba.acm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * <p> ACM配置信息 </p>
 *
 * @author vchar fred
 * @version 1.0
 * @create_date 2019/10/9 20:08
 */
@ConfigurationProperties(prefix = "alibaba.acm")
public class AcmProperties {

    /**
     * springboot 启动时加载的配置文件
     */
    private String applicationDataId = "application.yml";

    /**
     * need load other config file
     */
    private List<String> dataIdList;

    /**
     * diamond group
     */
    private String group = "DEFAULT_GROUP";

    /**
     * the AliYun endpoint for ACM
     */
    private String endpoint;

    /**
     * ACM namespace
     */
    private String namespace;

    /**
     * ACM access key
     */
    private String accessKey;

    /**
     * ACM secret key
     */
    private String secretKey;

    /**
     * timeout to get configuration
     */
    private int timeOut = 3000;
    /**
     * name of ram role granted to ECS
     */
    private String ramRoleName;

    /**
     * openKMSFilter
     */
    private boolean openKMSFilter = false;

    /**
     * regionId
     */
    private String regionId;

    /**
     * system config priority
     */
    private boolean vmPriority = true;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRamRoleName() {
        return ramRoleName;
    }

    public void setRamRoleName(String ramRoleName) {
        this.ramRoleName = ramRoleName;
    }

    public String getApplicationDataId() {
        return applicationDataId;
    }

    public void setApplicationDataId(String applicationDataId) {
        this.applicationDataId = applicationDataId;
    }

    public List<String> getDataIdList() {
        return dataIdList;
    }

    public void setDataIdList(List<String> dataIdList) {
        this.dataIdList = dataIdList;
    }

    public boolean getOpenKMSFilter() {
        return openKMSFilter;
    }

    public void setOpenKMSFilter(boolean openKMSFilter) {
        this.openKMSFilter = openKMSFilter;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public boolean getVmPriority() {
        return vmPriority;
    }

    public void setVmPriority(boolean vmPriority) {
        this.vmPriority = vmPriority;
    }

    @Override
    public String toString() {
        return "AcmProperties{" +
                "applicationDataId='" + applicationDataId + '\'' +
                ", dataIdList=" + dataIdList +
                ", group='" + group + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", namespace='" + namespace + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", timeOut=" + timeOut +
                ", ramRoleName='" + ramRoleName + '\'' +
                ", openKMSFilter=" + openKMSFilter +
                ", regionId='" + regionId + '\'' +
                ", vmPriority=" + vmPriority +
                '}';
    }
}
