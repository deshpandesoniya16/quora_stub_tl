package com.upgrad.quora.service.business;


import com.upgrad.quora.service.dao.UserDao;
import com.upgrad.quora.service.entity.UserAuthTokenEntity;
import com.upgrad.quora.service.entity.UserEntity;

import com.upgrad.quora.service.exception.*;

import com.upgrad.quora.service.exception.SignUpRestrictedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.time.ZonedDateTime;


/**
 * This is UserBusinessService class for handling the business layer
 */
@Service
public class UserBusinessService {


    //Created instance of UserDao class
    @Autowired
    private UserDao userDao;

    //Created instance of PasswordCryptographyProvider class
    @Autowired
    private PasswordCryptographyProvider cryptographyProvider;

    //Controller calls this signup method
    @Transactional(propagation = Propagation.REQUIRED)
    public UserEntity signup(UserEntity userEntity) throws SignUpRestrictedException {

        final UserEntity userByUserName = userDao.getUserByUserName(userEntity.getUserName());
        final UserEntity userByEmail = userDao.getUserByEmail(userEntity.getEmail());

        //Checking the value of getUserByUserName() to throw SignUpRestrictedException
        if(userByUserName!=null){
            throw new SignUpRestrictedException("SGR-001","Try any other Username, this Username has already been taken");
        }
        //Checking the value of getUserByEmail() to throw SignUpRestrictedException
        if(userByEmail!=null){
            throw new SignUpRestrictedException("SGR-002","This user has already been registered, try with any other emailId");
        }

        //If the information is provided by a non-existing user, then save the user information in the database with encrypted password and call createUser() present in dao layer
        String[] encryptedPassword = cryptographyProvider.encrypt(userEntity.getPassword());
        userEntity.setSalt(encryptedPassword[0]);
        userEntity.setPassword(encryptedPassword[1]);
        return userDao.createUser(userEntity);
    }


    // Controller calls this Signin method
    @Transactional(propagation = Propagation.REQUIRED)
    public UserAuthTokenEntity authenticate (final String username , final String password) throws AuthenticationFailedException {
        UserEntity userEntity = userDao.getUserByUserName(username);
        if (userEntity == null) {
            throw new AuthenticationFailedException("ATH-001", "This username does not exist");
        }
        String encryptedPassword = cryptographyProvider.encrypt(password, userEntity.getSalt());
        if (encryptedPassword.equals(userEntity.getPassword())) {
            // new token will be created
            JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(encryptedPassword);
            // setting up the token variables in user_auth tables
            UserAuthTokenEntity userAuthTokenEntity = new UserAuthTokenEntity();
            userAuthTokenEntity.setUser(userEntity);
            userAuthTokenEntity.setUuid(userEntity.getUuid());
            final ZonedDateTime now = ZonedDateTime.now();
            final ZonedDateTime expiresAt = now.plusHours(8);
            userAuthTokenEntity.setAccessToken(jwtTokenProvider.generateToken(userEntity.getUuid(), now, expiresAt));
            userAuthTokenEntity.setLoginAt(now);
            userAuthTokenEntity.setExpiresAt(expiresAt);
            userDao.createAuthToken(userAuthTokenEntity);
            return userAuthTokenEntity;
        }
        else {
            throw new AuthenticationFailedException("ATH-002", "Password Failed");
        }

    }

    // Controller calls this Signout method
    @Transactional(propagation = Propagation.REQUIRED)
    public UserEntity signOut(final String accessToken) throws SignOutRestrictedException {
        UserAuthTokenEntity userAuthToken = userDao.checkToken(accessToken);
        UserEntity signoutUser = null;
        if (userAuthToken == null){
            throw new SignOutRestrictedException("SGR-001","User is not Signed in.");
        }
        else if (userAuthToken!= null && userAuthToken.getAccessToken().equals(accessToken)) {
            final ZonedDateTime now = ZonedDateTime.now();
            userAuthToken.setLogoutAt(now);
            signoutUser = userAuthToken.getUser();
            userDao.updateUserAuthToken(userAuthToken);
        }
        return signoutUser;
    }

    // Controller calls this getUser method
    @Transactional(propagation = Propagation.REQUIRED)
    public UserEntity getUser(final String id , final String authorizedToken) throws AuthorizationFailedException, UserNotFoundException {

        UserAuthTokenEntity userAuth =  userDao.checkToken(authorizedToken);


        if(userAuth == null)
        {
            throw new AuthorizationFailedException("ATHR-001","User has not signed in");
        }
        final ZonedDateTime signOutUserTime = userAuth.getLogoutAt();

        if(signOutUserTime!=null && userAuth!=null)
        {
            throw new AuthorizationFailedException("ATHR-002","User is signed out.Sign in first to get user details");
        }
        UserEntity user = userDao.getUserByUuid(id);

        if(user==null)
        {
            throw new UserNotFoundException("USR-001", "User with entered uuid does not exist .");

        }
        else {
            return user;
        }
    }

}
