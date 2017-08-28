package com.oauth.service;

import com.oauth.entities.User;
import com.oauth.repositories.UserRepository;
import com.retail.merchant.domains.AccessTokenResponse;
import com.retail.merchant.domains.Response;
import com.retail.util.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

@Service
public class OauthService {

        @Autowired
        private EmailService emailService;

        @Autowired
        private UserRepository userRepository;

        public Response userSignUp(User user) {
            Response response = new Response();
            String verifyToken = SecurityService.getAccessToken();
            try {
                User existingUser = userRepository.findByEmail(user.getEmail());

                if (existingUser == null) {
                    String plainPassword = user.getPassword();
                    String passwordHash = SecurityService.getMDHash(plainPassword);
                    user.setPassword(passwordHash);
                    user.setVerified(false);
                    user.setVerifyToken(verifyToken);
                    User createdUser = userRepository.save(user);
                    boolean mailStatus = sendVerificationMail(user);
                    if (mailStatus) {
                        response.setStatus("201");
                        response.setUserMessage("user Created with EmailId :: " + createdUser.getEmail()
                                + "  please check your mail for account activation link");
                    } else {
                        userRepository.delete(user.getId());
                        response.setStatus("500");
                        response.setUserMessage("invalid email..! Please check your mail once");
                    }

                } else {
                    response.setStatus("500");
                    response.setUserMessage("user Already exists with EmailId :: " + existingUser.getEmail());
                }
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return response;

        }
        public Response verifyUser(String token) {
            Response response = new Response();
            String accessToken = SecurityService.getAccessToken();
            User auth = userRepository.findByVerifyToken(token);
            if (auth != null && auth.isVerified() != true) {
                auth.setVerified(true);
                auth.setAccessToken(accessToken);
                userRepository.save(auth);
                response.setUserMessage("Account verified.. Thanks for you time");
                response.setStatus("200");
            } else {
                response.setStatus("401");
                response.setUserMessage("Sorry ! Bad try..");
            }

            return response;
        }
        public AccessTokenResponse accessToken(User user) {
            AccessTokenResponse response = new AccessTokenResponse();
            try{
                String passwordHash = SecurityService.getMDHash(user.getPassword());
                User auth = userRepository.findByEmailInAndPasswordIn(user.getEmail(), passwordHash);
                if (auth != null) {
                    response.setAccessToken(auth.getAccessToken());
                    response.setEmail(auth.getEmail());
                    response.setDeveloperMSG("user message");
                } else {
                    response.setDeveloperMSG("User not found");
                }}catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }
        private boolean sendVerificationMail(User user) {
            boolean status = emailService.sendVerificationMail(user);
            return status;
        }
        public Response logout(String accessToken) {
            Response response = new Response();
            User auth = userRepository.findByAccessToken(accessToken);
            if (auth != null) {
                String newAccessToken = SecurityService.getAccessToken();
                auth.setAccessToken(newAccessToken);
                userRepository.save(auth);
                response.setStatus("200");
                response.setUserMessage("user logout");
            }else {
                response.setStatus("404");
                response.setUserMessage("user not found");
            }
            return response;
        }
        public Response getStatus(String accessToken) {
            Response response = new Response();
            User merchantAuth = userRepository.findByAccessToken(accessToken);
            if (merchantAuth != null) {
                boolean verified = merchantAuth.isVerified();
                if (verified) {
                    response.setStatus("true");
                    response.setUserMessage("merchant verification is done");
                }else {
                    response.setStatus("false");
                    response.setUserMessage("merchant verification is not done");
                }
                return response;
            } else {
                return null;
            }

        }
    }

