package com.projet.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projet.dto.AuthRequest;
import com.projet.dto.AuthResponse;
import com.projet.entity.User;
import com.projet.repository.UserRepository;
import com.projet.security.JwtUtil;
import com.projet.service.UserService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {


@Autowired
private UserRepository userRepository;


@Autowired
private UserService userService;


@Autowired
private JwtUtil jwtUtil;


@Autowired
private AuthenticationManager authenticationManager;


@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
if (userRepository.findByEmail(request.getEmail()) != null) {
return ResponseEntity.badRequest().body("Email already exists");
}


User saved = userService.register(request.getEmail(), request.getPassword());
return ResponseEntity.ok("User registered: " + saved.getEmail());
}


@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
try {
authenticationManager.authenticate(
new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
);
} catch (AuthenticationException e) {
return ResponseEntity.status(401).body("Invalid credentials");
}


String token = jwtUtil.generateToken(request.getEmail());
return ResponseEntity.ok(new AuthResponse(token));
}

@GetMapping("/me")
public ResponseEntity<?> me() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
        return ResponseEntity.status(401).body("Not authenticated");
    }

    UserDetails userDetails = (UserDetails) auth.getPrincipal();
    User u = userRepository.findByEmail(userDetails.getUsername());
    if (u == null) return ResponseEntity.status(404).body("User not found");
    u.setPassword(null);
    return ResponseEntity.ok(u);
}
}
