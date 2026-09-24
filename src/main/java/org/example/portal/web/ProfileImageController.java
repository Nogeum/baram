package org.example.portal.web;

import lombok.RequiredArgsConstructor;
import org.example.portal.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller @RequiredArgsConstructor @RequestMapping("/profiles")
public class ProfileImageController {
    private final ProfileImageService images;
    @GetMapping("/{id}/image") ResponseEntity<byte[]> image(Principal p,@PathVariable Long id) {
        byte[] data=images.image(p.getName(),id);
        if(data==null)return ResponseEntity.notFound().header("Cache-Control","no-store").build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(data);
    }
    @PostMapping("/{id}/image") String upload(Principal p,@PathVariable Long id,@RequestParam MultipartFile file,@RequestParam(defaultValue="directory") String destination,RedirectAttributes flash) {
        try {images.upload(p.getName(),id,file);flash.addFlashAttribute("success","프로필 사진을 저장했습니다.");}
        catch(BusinessException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return target(destination);
    }
    @PostMapping("/{id}/image/delete") String remove(Principal p,@PathVariable Long id,@RequestParam(defaultValue="directory") String destination,RedirectAttributes flash) {
        try {images.remove(p.getName(),id);flash.addFlashAttribute("success","기본 이미지로 변경했습니다.");}
        catch(BusinessException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return target(destination);
    }
    private String target(String destination){return "redirect:"+switch(destination){case "mypage"->"/employee/mypage";case "employees"->"/admin/employee-manage";default->"/groupware/directory";};}
}
