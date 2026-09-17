package com.ziyadsamhaoui.messagingchatservice;

import org.springframework.boot.SpringApplication;

public class TestMessagingUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(MessagingUserServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
