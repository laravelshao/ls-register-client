package com.cl.register.client.dto;

/**
 * 注册响应对象
 *
 * @author qinghua.shao
 * @date 2022/6/19
 * @since 1.0.0
 */
public class RegisterResponse {

    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

    /**
     * 注册响应状态：SUCCESS、FAILURE
     */
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}