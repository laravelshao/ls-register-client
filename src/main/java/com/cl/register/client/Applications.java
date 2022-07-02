package com.cl.register.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 完整的服务实例的信息
 *
 * @author qinghua.shao
 * @date 2022/7/2
 * @since 1.0.0
 */
public class Applications {

    private Map<String, Map<String, ServiceInstance>> registry = new HashMap<>();

    public Applications() {
    }

    public Applications(Map<String, Map<String, ServiceInstance>> registry) {
        this.registry = registry;
    }

    public Map<String, Map<String, ServiceInstance>> getRegistry() {
        return registry;
    }

    public void setRegistry(Map<String, Map<String, ServiceInstance>> registry) {
        this.registry = registry;
    }
}
