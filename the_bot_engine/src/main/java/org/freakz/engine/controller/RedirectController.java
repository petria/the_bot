package org.freakz.engine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class RedirectController {

    @GetMapping("/redirect")
    public RedirectView redirectToUrl(@RequestParam String url) {
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(url);
        redirectView.setStatusCode(org.springframework.http.HttpStatus.FOUND);
        return redirectView;
    }

}
