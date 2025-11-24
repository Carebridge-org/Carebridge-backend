// java
package com.carebridge.controllers;

import com.carebridge.config.HibernateConfig;
import com.carebridge.dao.UserDAO;
import io.javalin.http.Context;
import com.carebridge.entities.User;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserController {
    private final UserDAO uDAO = UserDAO.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();


    // Handler used by Routes::get -> userController::getAllUsers
    public void getAllUsers(Context ctx) {
        List<User> users = u ;
        ctx.json(users);
    }

    // Handler used by Routes::post -> userController::createUser
    public void createUser(Context ctx) {
        User user = ctx.bodyAsClass(User.class);
        userService.saveUser(user);
        ctx.status(201).json(user);
    }
}
