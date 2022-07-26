package com.laravelshao.register.client;

import com.laravelshao.register.client.dto.HeartbeatRequest;
import com.laravelshao.register.client.dto.HeartbeatResponse;
import com.laravelshao.register.client.dto.RegisterRequest;
import com.laravelshao.register.client.dto.RegisterResponse;

import java.util.UUID;

/**
 * 注册中心客户端
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class RegisterClient {

    /**
     * 心跳上报间隔时间
     */
    private static final Long HEARTBEAT_INTERVAL = 30 * 1000L;

    public static final String SERVICE_NAME = "inventory-service";
    public static final String IP = "192.168.31.207";
    public static final String HOSTNAME = "inventory01";
    public static final int PORT = 9000;

    /**
     * 服务实例id
     */
    private String serviceInstanceId;

    /**
     * HTTP 通信组件
     */
    private HttpSender httpSender;

    /**
     * 心跳线程
     */
    private HeartbeatWorker heartbeatWorker;

    /**
     * 是否运行标识(存在多线程并发写情况的变量一定要用 volatile，保证内存可见性)
     */
    private volatile Boolean isRunning;

    /**
     * 客户端缓存的注册表
     */
    private CachedServiceRegistry registry;

    public RegisterClient() {
        this.serviceInstanceId = UUID.randomUUID().toString().replace("-", "");
        this.httpSender = new HttpSender();
        this.heartbeatWorker = new HeartbeatWorker();
        this.isRunning = true;
        this.registry = new CachedServiceRegistry(this, httpSender);
    }

    /**
     * 注册客户端启动
     */
    public void start() {

        try {
            /**
             * 启动之后执行2件事情
             * 1.执行服务注册
             * 2.注册成功后每隔30秒上报一次心跳
             */
            // 服务注册线程
            RegisterWorker registerWorker = new RegisterWorker();
            registerWorker.start();

            // 设置阻塞等待当前线程执行完毕
            registerWorker.join();

            // 心跳线程
            this.heartbeatWorker.start();

            // 初始化客户端缓存的服务注册表组件
            this.registry.initialize();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册客户端关闭
     */
    public void shutdown() {
        this.isRunning = false;
        this.heartbeatWorker.interrupt();
        this.registry.destory();
        this.httpSender.cancel(SERVICE_NAME, serviceInstanceId);
    }

    /**
     * 判断是否运行
     *
     * @return
     */
    public Boolean isRunning() {
        return isRunning;
    }

    /**
     * 注册线程
     */
    private class RegisterWorker extends Thread {
        @Override
        public void run() {

            // 组装注册请求参数
            RegisterRequest regRequest = new RegisterRequest();
            regRequest.setServiceName(SERVICE_NAME);
            regRequest.setIp(IP);
            regRequest.setHostname(HOSTNAME);
            regRequest.setPort(PORT);
            regRequest.setServiceInstanceId(serviceInstanceId);

            // 执行注册操作
            RegisterResponse regResponse = httpSender.register(regRequest);
            System.out.println("服务注册结果：【" + regResponse.getStatus() + "】");
        }
    }

    /**
     * 心跳线程，注册成功后每隔30秒上报一次心跳
     */
    private class HeartbeatWorker extends Thread {

        @Override
        public void run() {

            // 组装心跳检测请求参数
            HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
            heartbeatRequest.setServiceName(SERVICE_NAME);
            heartbeatRequest.setServiceInstanceId(serviceInstanceId);

            HeartbeatResponse heartbeatResponse = null;

            while (isRunning) {
                try {
                    // 执行心跳上报
                    heartbeatResponse = httpSender.heartbeat(heartbeatRequest);
                    System.out.println("心跳上报结果：【" + heartbeatResponse.getStatus() + "】");
                    Thread.sleep(HEARTBEAT_INTERVAL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
