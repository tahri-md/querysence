package com.example.querysence.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import com.example.querysence.model.dto.AuthRequest;
import com.example.querysence.model.dto.AuthResponse;
import com.example.querysence.model.dto.UserDto;
import com.example.querysence.model.dto.UserRegister;
import com.example.querysence.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/auth")
public class UserController {
    @Autowired
    UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody UserRegister registredUser) {
        return ResponseEntity.ok(userService.register(registredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(userService.login(authRequest));
    } 
    @GetMapping("/testing")
    public String testing( ) {
        return new String("testing api");
    }
      @GetMapping("/testings")
    public String testings( ) {
        return new String("testing apis");
    }
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfos(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyInfos(authentication));
    }
    
    

    // @GetMapping("/logout")
    // public String getMethodName(@ String param) {
    //     return new String();
    // }
    
    
    
}
