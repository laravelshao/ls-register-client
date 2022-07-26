package com.laravelshao.register.client;

import com.laravelshao.register.client.dto.HeartbeatRequest;
import com.laravelshao.register.client.dto.HeartbeatResponse;
import com.laravelshao.register.client.dto.RegisterRequest;
import com.laravelshao.register.client.dto.RegisterResponse;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * HTTP 通信组件
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class HttpSender {

    /**
     * 发送注册请求
     *
     * @param request
     * @return
     */
    public RegisterResponse register(RegisterRequest request) {

        System.out.println("服务实例【" + request + "】，发送请求进行注册......");

        // 收到register-server响应之后，封装一个Response对象
        RegisterResponse response = new RegisterResponse();
        response.setStatus(RegisterResponse.SUCCESS);

        return response;
    }

    /**
     * 发送心跳请求
     *
     * @param request
     * @return
     */
    public HeartbeatResponse heartbeat(HeartbeatRequest request) {

        System.out.println("服务实例【" + request + "】，发送请求进行心跳......");

        HeartbeatResponse response = new HeartbeatResponse();
        response.setStatus(RegisterResponse.SUCCESS);

        return response;
    }

    /**
     * 拉取全量服务注册表
     *
     * @return
     */
    public Applications fetchFullRegistry() {
        Map<String, Map<String, ServiceInstance>> registry =
                new HashMap<String, Map<String, ServiceInstance>>();

        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setHostname("finance-service-01");
        serviceInstance.setIp("192.168.31.207");
        serviceInstance.setPort(9000);
        serviceInstance.setServiceInstanceId("FINANCE-SERVICE-192.168.31.207:9000");
        serviceInstance.setServiceName("FINANCE-SERVICE");

        Map<String, ServiceInstance> serviceInstances = new HashMap<String, ServiceInstance>();
        serviceInstances.put("FINANCE-SERVICE-192.168.31.207:9000", serviceInstance);

        registry.put("FINANCE-SERVICE", serviceInstances);

        System.out.println("拉取注册表：" + registry);

        return new Applications(registry);
    }

    /**
     * 拉取增量服务注册表
     *
     * @return
     */
    public DeltaRegistry fetchDeltaRegistry() {
        LinkedList<CachedServiceRegistry.RecentlyChangedServiceInstance> recentlyChangedQueue = new LinkedList<>();

        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setHostname("order-service-01");
        serviceInstance.setIp("192.168.31.288");
        serviceInstance.setPort(9000);
        serviceInstance.setServiceInstanceId("ORDER-SERVICE-192.168.31.288:9000");
        serviceInstance.setServiceName("ORDER-SERVICE");

        CachedServiceRegistry.RecentlyChangedServiceInstance recentlyChangedItem = new CachedServiceRegistry.RecentlyChangedServiceInstance(
                serviceInstance,
                System.currentTimeMillis(),
                "register");

        recentlyChangedQueue.add(recentlyChangedItem);

        System.out.println("拉取增量注册表：" + recentlyChangedQueue);

        DeltaRegistry deltaRegistry = new DeltaRegistry(recentlyChangedQueue, 2L);

        return deltaRegistry;
    }

    /**
     * 服务下线
     *
     * @param serviceName       服务名称
     * @param serviceInstanceId 服务实例id
     */
    public void cancel(String serviceName, String serviceInstanceId) {
        System.out.println("服务实例下线【" + serviceName + ", " + serviceInstanceId + "】");
    }
}
