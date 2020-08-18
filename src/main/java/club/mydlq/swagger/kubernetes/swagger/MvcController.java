package club.mydlq.swagger.kubernetes.swagger;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author dingliqiang
 */
@Controller
public class MvcController {

    @GetMapping("/")
    public String root(){
        return "/doc.html";
    }

}
