package com.retail.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.retail.domains.AccessTokenResponse;
import com.retail.domains.Response;
import com.retail.entities.MarchantAuth;
import com.retail.repositories.MarchantAuthRepository;
import com.retail.util.EmailService;

@Service
public class MarchantService {
	
	@Autowired
	EmailService emailService;

	public Response marchantSignUp(MarchantAuth marchant, MarchantAuthRepository marchantAuthRepository) {
		Response response = new Response();
		String verifyToken = UUID.randomUUID().toString();
		MarchantAuth existingMarchant = marchantAuthRepository.findByEmail(marchant.getEmail());
		if (existingMarchant == null) {
			marchant.setVerified(false);
			marchant.setVerifyToken(verifyToken);
			MarchantAuth createdMarchant = marchantAuthRepository.save(marchant);
			boolean mailStatus = sendVerificationMail(marchant, verifyToken);
			if (mailStatus) {
				response.setStatus("201");
				response.setUserMessage("Marchant Created with EmailId :: " + createdMarchant.getEmail()
						+ "  please check your mail for account activation link");
			} else {
				response.setStatus("500");
				response.setUserMessage("invalid email..! Please check your mail once");
			}

		} else {
			response.setStatus("500");
			response.setUserMessage("Marchant Already exists with EmailId :: " + existingMarchant.getEmail());
		}
		return response;

	}

	public Response verifyMarchant(String token, MarchantAuthRepository authRepository) {
		Response response = new Response();
		String accessToken = UUID.randomUUID().toString();
		MarchantAuth auth = authRepository.findByVerifyToken(token);
		if (auth != null && auth.isVerified() != true) {
			auth.setVerified(true);
			auth.setAccessToken(accessToken);
			authRepository.save(auth);
			response.setUserMessage("Account verified.. Thanks for you time");
			response.setStatus("200");
		} else {
			response.setStatus("401");
			response.setUserMessage("Sorry ! Bad try..");
		}

		return response;
	}

	public AccessTokenResponse accessToken(MarchantAuth user, MarchantAuthRepository authRepository) {
		AccessTokenResponse response = new AccessTokenResponse();
		MarchantAuth auth = authRepository.findByEmailInAndPasswordIn(user.getEmail(), user.getPassword());
		if (auth != null) {
			response.setAccessToken(auth.getAccessToken());
			response.setEmail(auth.getEmail());
			response.setDeveloperMSG("user message");
		} else {
			response.setDeveloperMSG("User not found");
		}
		return response;
	}

	private boolean sendVerificationMail(MarchantAuth marchant, String verifyToken) {
		boolean status = emailService.sendMailToMarchant(marchant.getEmail(), verifyToken);
		return status;
	}

}
