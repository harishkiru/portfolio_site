package dev.harish.portfolio;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OwnerPageController {
    @GetMapping({"/home", "/photos", "/contact", "/about", "/owner"})
    public String ownerPage() { return "forward:/index.html"; }
}
