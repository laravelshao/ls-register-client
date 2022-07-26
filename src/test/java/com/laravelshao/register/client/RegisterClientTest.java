package com.laravelshao.register.client;

/**
 * 注册中心客户端测试
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class RegisterClientTest {

    public static void main(String[] args) throws Exception {

        RegisterClient registerClient = new RegisterClient();
        registerClient.start();

        Thread.sleep(35000L);

        // 关闭
        registerClient.shutdown();
    }
}
