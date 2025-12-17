package com.bmdyy.bluebird.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServerInfoController {
   @GetMapping({"/server-info"})
   // Handles GET requests sent to the /server-info endpoint
   public String serverInfo(Model model) throws IOException {
      // Obtain the Runtime instance associated with the current Java process
      Runtime rt = Runtime.getRuntime();
      // Execute an external shell script using /bin/bash
      // The script path is /opt/bluebird/serverInfo.sh
      Process proc = rt.exec(new String[]{"/bin/bash", "/opt/bluebird/serverInfo.sh"});
      // Create a BufferedReader to read the standard output of the executed process
      BufferedReader stdInput = new BufferedReader(
               new InputStreamReader(proc.getInputStream())
      );
      // StringBuilder to accumulate the output of the script
      StringBuilder serverInfo = new StringBuilder();
      String line;
      // Read the output of the script line by line
      while ((line = stdInput.readLine()) != null) {
         serverInfo.append(line).append("\n");
      }
      // Add the collected server information to the model
      // This makes it accessible in the view (server-info.html)
      model.addAttribute("serverInfo", serverInfo);
      // Return the name of the view to be rendered
      return "server-info";
   }
}
