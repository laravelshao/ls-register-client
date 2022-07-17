package com.cl.register.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * 服务注册中心客户端缓存的注册表
 *
 * @author qinghua.shao
 * @date 2022/6/26
 * @since 1.0.0
 */
public class CachedServiceRegistry {

    /**
     * 服务注册表拉取间隔时间
     */
    private static final Long SERVICE_REGISTRY_FETCH_INTERVAL = 30 * 1000L;

    /**
     * 客户端缓存的服务注册表
     */
    //private AtomicReference<Applications> applications = new AtomicReference<>(new Applications());
    private AtomicStampedReference<Applications> applications = new AtomicStampedReference<>(new Applications(), 0);

    /**
     * 增量拉取注册表线程
     */
    public FetchDeltaRegistryWorker fetchDeltaRegistryWorker;

    /**
     * 注册客户端
     */
    public RegisterClient registerClient;

    /**
     * HTTP 通信组件
     */
    public HttpSender httpSender;

    /**
     * 代表了当前的本地缓存的服务注册表的一个版本号
     */
    private AtomicLong applicationsVersion = new AtomicLong(0L);

    /**
     * 客户端缓存注册表的读写锁
     */
    private ReentrantReadWriteLock applicationLock = new ReentrantReadWriteLock();
    private WriteLock applicationWriteLock = applicationLock.writeLock();
    private ReadLock applicationReadLock = applicationLock.readLock();

    public CachedServiceRegistry(RegisterClient registerClient, HttpSender httpSender) {
        this.fetchDeltaRegistryWorker = new FetchDeltaRegistryWorker();
        this.registerClient = registerClient;
        this.httpSender = httpSender;
    }

    /**
     * 初始化定时拉取任务
     */
    public void initialize() {

        // 启动拉取全量注册表线程
        FetchFullRegistryWorker fetchFullRegistryWorker = new FetchFullRegistryWorker();
        fetchFullRegistryWorker.start();

        // 启动拉取增量注册表线程
        this.fetchDeltaRegistryWorker.start();
    }

    /**
     * 销毁定时拉取任务
     */
    public void destory() {
        this.fetchDeltaRegistryWorker.interrupt();
    }

    /**
     * 全量注册表拉取工作线程
     */
    public class FetchFullRegistryWorker extends Thread {

        @Override
        public void run() {

            // 需要网络请求，结果一直卡着没有响应，卡住几分钟之后响应，但数据是几分钟前的版本，里面仅仅包含了30个服务实例
            // 全量拉注册表的线程突然苏醒过来了，此时将30个服务实例的旧版本的数据赋值给了本地缓存注册表
            // 一定要在发起网络请求之前，先拿到一个当时的版本号，这样在拉取之后进行设置时需要比较版本号

            // 全量拉取注册表
            fetchFullRegistry();
        }
    }

    /**
     * 全量拉取注册表
     */
    private void fetchFullRegistry() {

        // 一定要在全量拉取之前获取版本号
        long expectedVersion = applicationsVersion.get();

        // 拉取全量注册表
        Applications fetchedApplications = httpSender.fetchFullRegistry();

        if (applicationsVersion.compareAndSet(expectedVersion, expectedVersion + 1)) {

            // CAS 设置
            while (true) {
                Applications expectedApplications = applications.getReference();
                int expectedStamp = applications.getStamp();
                if (applications.compareAndSet(expectedApplications, fetchedApplications, expectedStamp, expectedStamp + 1)) {
                    break;
                }
            }
        }
    }

    /**
     * 增量注册表拉取工作线程
     */
    public class FetchDeltaRegistryWorker extends Thread {

        @Override
        public void run() {
            while (registerClient.isRunning()) {
                try {

                    // 一定要在拉取之前获取版本号
                    long expectedVersion = applicationsVersion.get();

                    // 拉取最近3分钟变化的服务实例
                    DeltaRegistry deltaRegistry = httpSender.fetchDeltaRegistry();

                    if (applicationsVersion.compareAndSet(expectedVersion, expectedVersion + 1)) {

                        // 合并增量注册表到本地缓存注册表
                        mergeDeltaRegistry(deltaRegistry);

                        // 校对注册表
                        reconcileRegistry(deltaRegistry);
                    }

                    Thread.sleep(SERVICE_REGISTRY_FETCH_INTERVAL);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 合并增量注册表到本地缓存注册表
     *
     * @param deltaRegistry 增量注册表
     */
    private void mergeDeltaRegistry(DeltaRegistry deltaRegistry) {

        try {
            applicationWriteLock.lock();

            // 获取封装对象中的服务注册表
            Map<String, Map<String, ServiceInstance>> registry = applications.getReference().getRegistry();

            // 获取增量注册表服务实例队列
            Queue<RecentlyChangedServiceInstance> recentlyChangedQueue = deltaRegistry.getRecentlyChangedQueue();

            for (RecentlyChangedServiceInstance recentlyChangedItem : recentlyChangedQueue) {

                String serviceName = recentlyChangedItem.serviceInstance.getServiceName();
                String serviceInstanceId = recentlyChangedItem.serviceInstance.getServiceInstanceId();

                if (Objects.equals(ServiceInstanceOperation.REGISTER, recentlyChangedItem.serviceInstanceOperation)) {
                    // 注册操作

                    // 判断本地全量注册表是否存在当前服务，没有则初始化
                    Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceName);
                    if (serviceInstanceMap == null) {
                        serviceInstanceMap = new HashMap<>();
                        registry.put(serviceName, serviceInstanceMap);
                    }

                    ServiceInstance serviceInstance = serviceInstanceMap.get(serviceInstanceId);
                    if (serviceInstance == null) {
                        // 将拉取到增量注册实例添加到本地全量注册表
                        serviceInstanceMap.put(serviceInstanceId, recentlyChangedItem.serviceInstance);
                    }
                } else if (Objects.equals(ServiceInstanceOperation.REMOVE, recentlyChangedItem.serviceInstanceOperation)) {
                    // 移除操作
                    Map<String, ServiceInstance> serviceInstanceMap = registry.get(serviceName);
                    if (serviceInstanceMap != null) {
                        // 从本地全量注册表中移除
                        serviceInstanceMap.remove(serviceInstanceId);
                    }
                }
            }
        } finally {
            applicationWriteLock.unlock();
        }
    }

    /**
     * 校正注册表(如果不一致则全量重新拉取校正)
     *
     * @param deltaRegistry 增量注册表
     */
    private void reconcileRegistry(DeltaRegistry deltaRegistry) {

        Map<String, Map<String, ServiceInstance>> registry = applications.getReference().getRegistry();

        // 校验服务端与客户端服务实例数量是否一致
        Long serverSideTotalCount = deltaRegistry.getServiceInstanceTotalCount();
        Long clientSideTotalCount = 0L;
        for (Map<String, ServiceInstance> serviceInstanceMap : registry.values()) {
            clientSideTotalCount += serviceInstanceMap.size();
        }

        // 不一致则执行全量重新拉取
        if (serverSideTotalCount != clientSideTotalCount) {
            Applications fetchedApplications = httpSender.fetchFullRegistry();

            // CAS 设置
            while (true) {
                //Applications expectedApplications = applications.get();
                Applications expectedApplications = applications.getReference();
                int expectedStamp = applications.getStamp();
                if (applications.compareAndSet(expectedApplications, fetchedApplications, expectedStamp, expectedStamp + 1)) {
                    break;
                }
            }
        }
    }

    /**
     * 服务实例操作类型
     */
    class ServiceInstanceOperation {

        /**
         * 注册
         */
        public static final String REGISTER = "register";
        /**
         * 删除
         */
        public static final String REMOVE = "remove";

    }

    /**
     * 获取服务注册表
     *
     * @return
     */
    public Map<String, Map<String, ServiceInstance>> getRegistry() {
        try {
            applicationReadLock.lock();
            return applications.getReference().getRegistry();
        } finally {
            applicationReadLock.unlock();
        }
    }

    /**
     * 最近变更的实例信息
     */
    static class RecentlyChangedServiceInstance {

        /**
         * 服务实例
         */
        ServiceInstance serviceInstance;
        /**
         * 发生变更的时间戳
         */
        Long changedTimestamp;
        /**
         * 变更操作
         */
        String serviceInstanceOperation;

        public RecentlyChangedServiceInstance(
                ServiceInstance serviceInstance,
                Long changedTimestamp,
                String serviceInstanceOperation) {
            this.serviceInstance = serviceInstance;
            this.changedTimestamp = changedTimestamp;
            this.serviceInstanceOperation = serviceInstanceOperation;
        }

        @Override
        public String toString() {
            return "RecentlyChangedServiceInstance [serviceInstance=" + serviceInstance + ", changedTimestamp="
                    + changedTimestamp + ", serviceInstanceOperation=" + serviceInstanceOperation + "]";
        }

    }
}
