package com.laravelshao.register.client.dto;

/**
 * 心跳请求对象
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class HeartbeatRequest {

    /**
     * 服务名称
     */
    private String serviceName;
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

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    @Override
    public String toString() {
        return "HeartbeatRequest [serviceName=" + serviceName + ", serviceInstanceId=" + serviceInstanceId + "]";
    }
}