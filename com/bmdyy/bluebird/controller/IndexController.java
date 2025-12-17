package com.bmdyy.bluebird.controller;

import com.bmdyy.bluebird.model.User;
import com.bmdyy.bluebird.security.services.UserDetailsImpl;
import com.bmdyy.bluebird.security.services.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController {
   @Autowired
   JdbcTemplate jdbcTemplate;
   @Autowired
   UserDetailsServiceImpl userDetailsService;

   @GetMapping({"/"})
   public String index(@RequestParam(required = false) String q, Model model) {
      String sql;
      if (q != null) {
         sql = "SELECT text, to_char(posted_at, 'dd.mm.yyyy, hh:mi') as posted_at_nice, username, name, author_id FROM posts JOIN users ON posts.author_id = users.id WHERE text LIKE '%" + q + "%' ORDER BY posted_at DESC";
      } else {
         sql = "SELECT text, to_char(posted_at, 'dd.mm.yyyy, hh:mi') as posted_at_nice, username, name, author_id FROM posts JOIN users ON posts.author_id = users.id ORDER BY posted_at DESC";
      }

      List<Map<String, Object>> posts = this.jdbcTemplate.queryForList(sql);
      model.addAttribute("posts", posts);
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication.getName().equals("anonymousUser")) {
         return "home-logged-out";
      } else {
         UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();
         model.addAttribute("userDetails", userDetails);
         return "home-logged-in";
      }
   }

   @GetMapping({"/find-user"})
   // Method to find users by username
   public String findUser(@RequestParam String u, Model model, HttpServletResponse response) throws IOException {
      
      // Define a regular expression pattern to detect single quotes or suspicious input like SQL injection attempts
      Pattern p = Pattern.compile("'|(.*'.*'.*)");
      // Match the input username 'u' against the pattern
      // !!! Just match if the whole string is single or at least two single quotes !!!
      Matcher m = p.matcher(u);
      
      // Convert the input username to lowercase for further checks
      String u2 = u.toLowerCase();

      // Check if the input does not contain spaces and does not match the suspicious pattern
      if (!u2.contains(" ") && !m.matches()) {
         try {
            // Construct a SQL query using the user input
            // WARNING: This approach is vulnerable to SQL injection (string concatenation)
            String sql = "SELECT * FROM users WHERE username LIKE '%" + u + "%'";
            
            // Execute the SQL query and map the result set to a list of User objects
            List<User> users = this.jdbcTemplate.query(sql, new BeanPropertyRowMapper(User.class));
            
            // Get the currently authenticated user’s details from the Spring Security context
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            // Add the current user details and the query results to the model for rendering in the view
            model.addAttribute("userDetails", userDetails);
            model.addAttribute("users", users);
            
            // Return the view name for displaying the found users
            return "find-user";
         
         } catch (BadSqlGrammarException e) {
            // Handle SQL syntax errors (e.g., malformed queries)
            System.out.println(e.getSQLException().getMessage());
            model.addAttribute("errorMsg", "Invalid search query");
            return "error";
         
         } catch (Exception e) {
            // Handle any other unexpected exceptions
            e.printStackTrace();
            model.addAttribute("errorMsg", "Invalid search query");
            return "error";
         }
      
      } else {
         // If the input is invalid (contains spaces or suspicious characters), show an error message
         model.addAttribute("errorMsg", "Illegal search term");
         return "error";
      }
   }
}
