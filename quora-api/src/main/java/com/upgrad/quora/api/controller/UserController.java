package com.upgrad.quora.api.controller;

import com.upgrad.quora.api.model.SigninResponse;
import com.upgrad.quora.api.model.SignoutResponse;
import com.upgrad.quora.api.model.SignupUserRequest;
import com.upgrad.quora.api.model.SignupUserResponse;
import com.upgrad.quora.service.business.UserBusinessService;
import com.upgrad.quora.service.entity.UserAuthTokenEntity;
import com.upgrad.quora.service.entity.UserEntity;

import com.upgrad.quora.service.exception.AuthenticationFailedException;
import com.upgrad.quora.service.exception.SignOutRestrictedException;

import com.upgrad.quora.service.exception.SignUpRestrictedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.UUID;

/**
 * This is UserController class for managing Api endpoints signup - "/user/signup",signin - "/user/signin" and signout - "/user/signout"
 */
@RestController
@RequestMapping("/")
public class UserController {


    @Autowired
    private UserBusinessService userBusinessService;

    //This endpoint is used to register a new user in the Quora Application.
    @RequestMapping(method= RequestMethod.POST, path="/user/signup", consumes= MediaType.APPLICATION_JSON_UTF8_VALUE, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SignupUserResponse> userSignup(final SignupUserRequest signupUserRequest) throws SignUpRestrictedException {

        //Created UserEntity instance
        final UserEntity userEntity = new UserEntity();

        //Setting UserEntity properties
        userEntity.setUuid(UUID.randomUUID().toString());
        userEntity.setFirstName(signupUserRequest.getFirstName());
        userEntity.setLastName(signupUserRequest.getLastName());
        userEntity.setUserName(signupUserRequest.getUserName());
        userEntity.setEmail(signupUserRequest.getEmailAddress());
        userEntity.setPassword(signupUserRequest.getPassword());
        userEntity.setSalt("1234abc");
        userEntity.setCountry(signupUserRequest.getCountry());
        userEntity.setAboutMe(signupUserRequest.getAboutMe());
        userEntity.setDob(signupUserRequest.getDob());
        userEntity.setRole("nonadmin");
        userEntity.setContactNumber(signupUserRequest.getContactNumber());

        //Calling the signup() from userBusinessService
        final UserEntity createdUserEntity = userBusinessService.signup(userEntity);

        //Getting the uuid of user and setting for user response
        SignupUserResponse userResponse = new SignupUserResponse().id(createdUserEntity.getUuid()).status("USER SUCCESSFULLY REGISTERED");

        //Returning the ResponseEntity with two parameters
        return new ResponseEntity<SignupUserResponse>(userResponse, HttpStatus.CREATED);
    }

    //This endpoint is used for user authentication. The user authenticates in the application and after successful authentication, JWT token is given to a user.
    @RequestMapping(method= RequestMethod.POST, path="/user/signin", consumes= MediaType.APPLICATION_JSON_UTF8_VALUE, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SigninResponse> signIn(@RequestHeader("authorization") final String authorization) throws AuthenticationFailedException {
        byte[] decode = Base64.getDecoder().decode(authorization.split("Basic ")[1]);
        String decodedText = new String(decode);
        String[] decodedArray = decodedText.split(":");
        UserAuthTokenEntity userAuthToken = userBusinessService.authenticate(decodedArray[0],decodedArray[1]);
        UserEntity user = userAuthToken.getUser();
        SigninResponse signinResponse= new SigninResponse().id(user.getUuid()).message("SIGNED IN SUCCESSFULLY");
        HttpHeaders headers = new HttpHeaders();
        // to send the auth token as header as it can not go in payload
        headers.add("access-token", userAuthToken.getAccessToken());
        return new ResponseEntity<SigninResponse>(signinResponse,headers, HttpStatus.OK);
    }

    //This endpoint is used to sign out from the Quora Application. The user cannot access any other endpoint once he is signed out of the application.
    @RequestMapping(method= RequestMethod.POST, path="/user/signout", produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SignoutResponse> signOut(@RequestHeader("authorization") final String authorization) throws SignOutRestrictedException {
        // String [] bearerToken = authorization.split("Bearer ");
        UserEntity signoutUser = userBusinessService.signOut(authorization);
        SignoutResponse signoutResponse = new SignoutResponse().id(signoutUser.getUuid()).message("SIGNED OUT SUCCESSFULLY");
        return new ResponseEntity<SignoutResponse>(signoutResponse,HttpStatus.OK);
    }

}
