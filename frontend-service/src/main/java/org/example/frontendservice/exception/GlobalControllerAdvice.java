package org.example.frontendservice.exception;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(SessionExpiredException.class)
    public String handleSessionExpired(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
