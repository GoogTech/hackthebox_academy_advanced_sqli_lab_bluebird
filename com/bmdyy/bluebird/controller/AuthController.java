package com.bmdyy.bluebird.controller;

import com.bmdyy.bluebird.model.User;
import com.bmdyy.bluebird.security.jwt.AuthEntryPointJwt;
import com.bmdyy.bluebird.security.jwt.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin(
   origins = {"*"},
   maxAge = 3600L
)
@Controller
public class AuthController {
   @Autowired
   JdbcTemplate jdbcTemplate;
   @Autowired
   JwtUtils jwtUtils;
   @Autowired
   AuthenticationManager authenticationManager;
   @Value("${bluebird.app.jwtCookieName}")
   private String jwtCookieName;
   @Value("${bluebird.app.jwtExpirationMs}")
   private int jwtExpirationMs;
   private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

   @GetMapping({"/signup"})
   public String signupGET(@RequestParam(required = false) String e, Model model) {
      model.addAttribute("e", e);
      return "signup";
   }

   @GetMapping({"/login"})
   public String loginGET(@RequestParam(required = false) String e, @RequestParam(required = false) String logout, Model model, HttpServletResponse response) throws IOException {
      if (logout != null) {
         SecurityContextHolder.clearContext();
         Cookie jwtCookie = new Cookie(this.jwtCookieName, (String)null);
         jwtCookie.setMaxAge(0);
         jwtCookie.setHttpOnly(true);
         response.addCookie(jwtCookie);
         response.sendRedirect("/");
      }

      model.addAttribute("e", e);
      return "login";
   }

   @GetMapping({"/forgot"})
   public String forgotGET(@RequestParam(required = false) String e, Model model) {
      model.addAttribute("e", e);
      return "forgot";
   }

   private boolean verifyResetCode(int uid, String code) {
      String sql = "SELECT * FROM users WHERE id = ?";
      User user = (User)this.jdbcTemplate.queryForObject(sql, new Object[]{uid}, new BeanPropertyRowMapper(User.class));
      String passwordResetHash = DigestUtils.md5DigestAsHex((uid + ":" + user.getEmail() + ":" + user.getPassword()).getBytes());
      return passwordResetHash.equals(code);
   }

   @GetMapping({"/reset"})
   public String resetGET(@RequestParam int uid, @RequestParam String code, @RequestParam(required = false) String e, Model model, HttpServletResponse response) throws IOException {
      try {
         if (this.verifyResetCode(uid, code)) {
            model.addAttribute("uid", uid);
            model.addAttribute("code", code);
            model.addAttribute("e", e);
            return "reset";
         }
      } catch (Exception ex) {
         System.out.println(ex.getMessage());
      }

      response.sendRedirect("/forgot?e=Invalid+reset+code");
      return null;
   }

   @PostMapping({"/reset"})
   public void resetPOST(@RequestParam String password, @RequestParam String repeatPassword, @RequestParam String code, @RequestParam String uid, HttpServletResponse response) throws IOException {
      if (!uid.isEmpty() && !code.isEmpty() && !password.isEmpty() && !repeatPassword.isEmpty()) {
         if (!password.equals(repeatPassword)) {
            response.sendRedirect("/reset?uid=" + uid + "&code=" + code + "&e=The+passwords+you+entered+do+not+match");
         } else if (!this.verifyResetCode(Integer.parseInt(uid), code)) {
            response.sendRedirect("/forgot?e=Invalid+reset+code");
         } else {
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            String sql = "UPDATE users SET password = ? WHERE id = ?";
            this.jdbcTemplate.update(sql, new Object[]{passwordHash, Integer.parseInt(uid)});
            response.sendRedirect("/login?e=Password+was+reset");
         }
      } else {
         response.sendRedirect("/reset?uid=" + uid + "&code=" + code + "&e=Please+fill+out+all+fields");
      }
   }

   @PostMapping({"/forgot"})
   public String forgotPOST(@RequestParam String email, Model model, HttpServletRequest request, HttpServletResponse response) throws IOException {
      // Check if the email field is empty
      if (email.isEmpty()) {
         // Redirect back to the forgot password page with an error message
         response.sendRedirect("/forgot?e=Please+fill+out+all+fields");
         return null;
      } else {
         // Validate email format using a simple regular expression
         Pattern p = Pattern.compile("^.*@[A-Za-z]*\\.[A-Za-z]*$");
         Matcher m = p.matcher(email);
         // If the email format is invalid
         if (!m.matches()) {
               // Redirect back to the forgot password page with an invalid email message
               response.sendRedirect("/forgot?e=Invalid+email!");
               return null;
         } else {
               try {
                  // Construct a SQL query to find the user with the given email
                  // !!! WARNING: Vulnerable to SQL Injection — do not use string concatenation in production !!!
                  String sql = "SELECT * FROM users WHERE email = '" + email + "'";
                  // Execute the query and map the result to a User object
                  User user = (User) this.jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper(User.class));
                  // Generate a password reset hash using MD5 (id + email + password)
                  Long var10000 = user.getId();
                  String passwordResetHash = DigestUtils.md5DigestAsHex(
                     ("" + var10000 + ":" + user.getEmail() + ":" + user.getPassword()).getBytes()
                  );
                  // Build the password reset link
                  var10000 = user.getId();
                  String passwordResetLink = "https://bluebird.htb/reset?uid=" + var10000 + "&code=" + passwordResetHash;
                  // Log the link — in real applications, this should be emailed to the user
                  logger.error("TODO- Send email with link [" + passwordResetLink + "]");
                  // Redirect to confirmation message
                  response.sendRedirect("/forgot?e=Please+check+your+email+for+the+password+reset+link");
                  return null;
               } catch (EmptyResultDataAccessException var11) {
                  // If no user was found with the provided email
                  response.sendRedirect("/forgot?e=Email+does+not+exist");
                  return null;
               } catch (Exception var12) {
                  // Get client IP address (check X-FORWARDED-FOR first for proxy support)
                  String ipAddress = request.getHeader("X-FORWARDED-FOR");
                  if (ipAddress == null) {
                     ipAddress = request.getRemoteAddr();
                  }
                  // If the request is from localhost (127.0.1.1), show detailed error info
                  if (ipAddress.equals("127.0.1.1")) {
                     model.addAttribute("errorMsg", var12.getMessage());
                     model.addAttribute("errorStackTrace", Arrays.toString(var12.getStackTrace()));
                  } else {
                     // Otherwise, show a generic error message to the user
                     model.addAttribute("errorMsg", "500 Internal Server Error");
                     model.addAttribute("errorStackTrace", "Something happened on our side. Please try again later.");
                  }
                  // Return the error view
                  return "error";
               }
         }
      }
   }

   @PostMapping({"/signup"})
   // Handles POST requests sent to the /signup endpoint
   public void signupPOST(
         @RequestParam String name,              // User's full name from the signup form
         @RequestParam String username,          // Desired username
         @RequestParam String email,             // User's email address
         @RequestParam String password,          // User's password
         @RequestParam String repeatPassword,    // Password confirmation field
         HttpServletResponse response            // HTTP response used for redirects
   ) throws IOException {
      // Check that none of the required fields are empty
      if (!name.isEmpty() && !username.isEmpty() && !email.isEmpty()
               && !password.isEmpty() && !repeatPassword.isEmpty()) {
         // Verify that the password and confirmation password match
         if (!password.equals(repeatPassword)) {
               // Redirect back to signup page if passwords do not match
               response.sendRedirect("/signup?e=The+passwords+you+entered+do+not+match");
         } else {
               try {
                  // Check if the username already exists in the database
                  String sql = "SELECT * FROM users WHERE username = ?";
                  User user = (User) this.jdbcTemplate.queryForObject(
                           sql,
                           new Object[]{username},
                           new BeanPropertyRowMapper(User.class)
                  );
                  // If a user is found, redirect with an error message
                  response.sendRedirect("/signup?e=This+username+is+already+taken");

               } catch (Exception var10) {
                  try {
                     // Check if the email address is already registered
                     String sql = "SELECT * FROM users WHERE email = ?";
                     User user = (User) this.jdbcTemplate.queryForObject(
                              sql,
                              new Object[]{email},
                              new BeanPropertyRowMapper(User.class)
                     );
                     // If a user is found, redirect with an error message
                     response.sendRedirect("/signup?e=This+email+address+is+already+taken");

                  } catch (Exception var9) {
                     // Hash the user's password using BCrypt with a cost factor of 12
                     String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                     // Insert the new user into the database
                     // !!! NOTE: This query is built via string concatenation and is vulnerable to SQL injection !!!
                     String sql = "INSERT INTO users (name, username, email, password) VALUES ('"
                              + name + "', '" + username + "', '" + email + "', '" + passwordHash + "')";
                     // Different from 
                     this.jdbcTemplate.update(sql);
                     // Redirect to the login page after successful registration
                     response.sendRedirect("/login?e=Account+was+created");
                  }
               }
         }
      } else {
         // Redirect back to signup page if any required field is empty
         response.sendRedirect("/signup?e=Please+fill+out+all+fields");
      }
   }

   @PostMapping({"/login"})
   public void loginPOST(@RequestParam String username, @RequestParam String password, HttpServletResponse response) throws IOException {
      try {
         Authentication authentication = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
         SecurityContextHolder.getContext().setAuthentication(authentication);
         String jwt = this.jwtUtils.generateJwtToken(authentication);
         Cookie jwtCookie = new Cookie(this.jwtCookieName, jwt);
         jwtCookie.setMaxAge(this.jwtExpirationMs);
         jwtCookie.setHttpOnly(true);
         response.addCookie(jwtCookie);
         response.sendRedirect("/");
      } catch (BadCredentialsException var7) {
         response.sendRedirect("/login?e=Invalid+username+or+password");
      }

   }
}
