package com.cl.register.client;

/**
 * 服务实例对象
 * 定义了服务实例的所有信息：服务名称、IP地址、hostname、端口号、服务实例ID、契约信息(Lease)
 *
 * @author qinghua.shao
 * @date 2022/6/26
 * @since 1.0.0
 */
public class ServiceInstance {

    /**
     * 服务名称
     */
    private String serviceName;
    /**
     * ip地址
     */
    private String ip;
    /**
     * 主机名
     */
    private String hostname;
    /**
     * 端口号
     */
    private int port;
    /**
     * 服务实例id
     */
    private String serviceInstanceId;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    @Override
    public String toString() {
        return "ServiceInstance [serviceName=" + serviceName + ", ip=" + ip + ", hostname=" + hostname + ", port="
                + port + ", serviceInstanceId=" + serviceInstanceId + "]";
    }

}
