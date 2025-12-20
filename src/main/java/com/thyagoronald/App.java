package com.thyagoronald;

import com.thyagoronald.factories.HostFactory;
import com.thyagoronald.services.HeartbeatService;
import com.thyagoronald.services.SocketService;
import com.thyagoronald.utils.Logger;

public class App {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            String[] hosts = args[1].split("#");

            HeartbeatService heartbeatService = HeartbeatService.builder()
                .hosts(HostFactory.createHostPojos(hosts))
                .intervalSeconds(5)
                .build();
            SocketService socketService = new SocketService(port);

            new Thread(heartbeatService::start).start();

            socketService.startServer();
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            Logger.error("Invalid arguments: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            Logger.error("Illegal argument: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            Logger.error("Illegal state: " + ex.getMessage());
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