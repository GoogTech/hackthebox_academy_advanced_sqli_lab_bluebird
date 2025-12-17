## Introduction
It’s a Java SpringBoot application that primarily focuses on code review, identifying SQLi vulnerabilities in the code, and demonstrating advanced SQL injection techniques. It's developed by the HackTheBox Academy, and comes from the module of `Advanced SQL Injections`.

## How to run it
```java
java -jar BlueBird-0.0.1-SNAPSHOT.jar
```
Note that the PostgreSQL database source file is: `bluebird_backup.sql`

## App properties
```sh
spring.datasource.url= jdbc:postgresql://localhost:5432/bluebird
spring.datasource.username= bbuser
spring.datasource.password= bbpassword
```

## Questions
### Question 1 - Common Character Bypasses
Use any of the techniques to exploit this SQL injection vulnerability on the target over port 8080. What is the password hash of the user whose email is `Amy.Mcwilliams@proton.me`?

### Question 2 - Error-Based SQL Injection
Take a look at how the password-reset links are generated in `forgotPOST()`. Use the `error-based SQLi` to dump the required information on the target over port 8080, and enter what the value of '`passwordResetLink`' for the user '`potus4`' would be.

### Question 3 - Second-Order SQL Injection
Exploit the `second-order SQL` injection in BlueBird on the target over port 8080. What is the password hash of '`betrayedApples3`'?

## Notes
> Hope my thoughts give you some tips: `/skill assessment/`
