package com.thyagoronald;

import java.util.Arrays;

import com.thyagoronald.services.HeartbeatService;
import com.thyagoronald.services.SocketService;
import com.thyagoronald.utils.Logger;

public class App {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            String[] hosts = args[1].split("#");

            HeartbeatService heartbeatService = HeartbeatService.builder()
                    .hosts(Arrays.asList(hosts))
                    .intervalSeconds(5)
                    .build();
            SocketService socketService = new SocketService(port);

            new Thread(heartbeatService::start).start();

            socketService.startServer();
        } catch (Exception ex) {
            Logger.error(ex.getMessage());
        }
    }
}

// CreateUserDTO userDTO = CreateUserDTO.builder()
// .name("John Doe")
// .email("johndoe@gmail.com")
// .build();
// UserService userService = new UserService();
// userService.createTable();
// UserModel userModel = userService.create(userDTO);