package com.bmdyy.bluebird.controller;

import com.bmdyy.bluebird.model.User;
import com.bmdyy.bluebird.security.services.UserDetailsImpl;
import com.bmdyy.bluebird.security.services.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {
   @Autowired
   JdbcTemplate jdbcTemplate;
   @Autowired
   UserDetailsServiceImpl userDetailsService;

   // Maps HTTP GET requests to /profile/{id} to this method
   @GetMapping({"/profile/{id}"})
   public String profile(@PathVariable int id, Model model, HttpServletResponse response) throws IOException {
      String sql;
      User user;
      try {
         // Prepare SQL query to retrieve user details based on the provided ID
         sql = "SELECT username, name, description, email, id FROM users WHERE id = ?";
         
         // Execute the query and map the result to a User object
         user = (User) this.jdbcTemplate.queryForObject(
                  sql, 
                  new Object[]{id}, 
                  new BeanPropertyRowMapper(User.class)
         );
      } catch (Exception var8) {
         // If an error occurs (e.g., user not found), redirect to the homepage
         response.sendRedirect("/");
         return null;
      }

      // Build another SQL query to retrieve all posts made by the user
      // Vulnerable to SQL Injection since user.getEmail() is concatenated directly into the query string
      sql = "SELECT text, to_char(posted_at, 'dd.mm.yyyy, hh:mi') as posted_at_nice, username, name, author_id " +
            "FROM posts JOIN users ON posts.author_id = users.id " +
            "WHERE email = '" + user.getEmail() + "' " +
            "ORDER BY posted_at DESC";

      // Execute the query and return a list of posts
      List posts = this.jdbcTemplate.queryForList(sql);

      // Add user and post data to the model to be rendered in the view
      model.addAttribute("user", user);
      model.addAttribute("posts", posts);

      // Get the currently authenticated user (from Spring Security)
      UserDetailsImpl userDetails = (UserDetailsImpl)
               SecurityContextHolder.getContext().getAuthentication().getPrincipal();

      // Add authenticated user details to the model
      model.addAttribute("userDetails", userDetails);

      // Return the view name "profile" (Spring will render profile.html or profile.jsp)
      return "profile";
   }

   @GetMapping({"/profile/edit"})
   public String editProfile(@RequestParam(required = false) String e, Model model) {
      UserDetailsImpl userDetails = (UserDetailsImpl)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      model.addAttribute("userDetails", userDetails);
      model.addAttribute("e", e);
      return "edit-profile";
   }

   // Maps HTTP POST requests to /profile/edit to this method
   @PostMapping({"/profile/edit"})
   public void editProfilePOST(
         @RequestParam String name,                    // user's name input
         @RequestParam String description,             // user's description input
         @RequestParam String email,                   // user's email input
         @RequestParam(required = false) String password,        // optional password input
         @RequestParam(required = false) String repeatPassword,  // optional repeated password input
         HttpServletResponse response) throws IOException {

      // Get the currently logged-in user (from Spring Security)
      UserDetailsImpl userDetails = (UserDetailsImpl)
               SecurityContextHolder.getContext().getAuthentication().getPrincipal();

      // Basic length validation to prevent excessively long input
      if (name.length() <= 50 && description.length() <= 100 && email.length() <= 100) {

         // Check if name field is empty
         if (name.isEmpty()) {
               // Redirect with error message if name is empty
               response.sendRedirect("/profile/edit?e=Name+can't+be+empty");
         } else {
               try {
                  // Check if the given email is already used by another user
                  String sql = "SELECT * FROM users WHERE email = ? AND id <> ?";
                  User user = (User) this.jdbcTemplate.queryForObject(
                           sql,
                           new Object[]{email, userDetails.getId()},
                           new BeanPropertyRowMapper(User.class)
                  );

                  // If query succeeds (email exists), redirect with error message
                  response.sendRedirect("/profile/edit?e=This+email+address+is+already+taken");

               } catch (Exception var11) {
                  // If query throws exception (email not found), continue updating profile

                  // Prepare SQL update query for user profile
                  // !!! Potential Second-Order SQL Injection vulnerability via "email" field !!!
                  String sql = "UPDATE users SET name = ?, description = ?, email = ?";

                  boolean passwordFlag = false; // indicates whether password should be updated

                  // If password fields are provided
                  if (password != null && repeatPassword != null) {
                     // Check if passwords match
                     if (!password.equals(repeatPassword)) {
                           // Redirect with error if passwords don't match
                           response.sendRedirect("/profile/edit?e=Passwords+don't+match");
                           return;
                     }

                     // Include password update in SQL query
                     passwordFlag = true;
                     sql = sql + ", password = ?";
                  }

                  // Append WHERE clause to identify the current user by ID
                  sql = sql + " WHERE id = ?";

                  // If password update is needed
                  if (passwordFlag) {
                     // Hash the new password using BCrypt before storing it
                     String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

                     // Execute SQL update with password included
                     this.jdbcTemplate.update(
                              sql,
                              new Object[]{name, description, email, passwordHash, userDetails.getId()}
                     );
                  } else {
                     // Execute SQL update without password change
                     this.jdbcTemplate.update(
                              sql,
                              new Object[]{name, description, email, userDetails.getId()}
                     );
                  }

                  // Redirect with success message
                  response.sendRedirect("/profile/edit?e=Details+updated!");
               }
         }
      } else {
         // Redirect if input fields exceed allowed length
         response.sendRedirect("/profile/edit?e=Some+values+are+too+long");
      }
   }
}
