package org.example.portal;

import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.example.portal.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ProfileImageTests {
    @Autowired ProfileImageService images;
    @Autowired PortalService portal;
    @Autowired ChatService chat;
    @Autowired StoredFileRepository files;
    @Autowired MockMvc mvc;
    Employee a,b,admin;
    @BeforeEach void setup(){a=create(Employee.Role.EMPLOYEE);b=create(Employee.Role.EMPLOYEE);admin=create(Employee.Role.ADMIN);}
    Employee create(Employee.Role role){return portal.createEmployee("photo_"+UUID.randomUUID().toString().substring(0,8),"Testing123!","사진테스트","개발","사원",role,"","",LocalDate.of(2026,1,1));}
    MockMultipartFile photo() throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(400,200,BufferedImage.TYPE_INT_RGB),"png",out);return new MockMultipartFile("file","test.png","image/png",out.toByteArray());}
    @Test void normalizedPhotoAppearsInDirectoryAndChatAndCanBeReplacedAndRemoved() throws Exception {
        images.upload(a.getLoginId(),a.getId(),photo());Long old=a.getProfileFileId();
        var result=ImageIO.read(new ByteArrayInputStream(images.image(b.getLoginId(),a.getId())));
        assertThat(result.getWidth()).isEqualTo(256);assertThat(result.getHeight()).isEqualTo(256);
        assertThat(chat.people(b.getLoginId(),"").stream().filter(p->p.id().equals(a.getId())).findFirst().orElseThrow().profileImageUrl()).isEqualTo(a.getProfileImageUrl());
        mvc.perform(get("/profiles/"+a.getId()+"/image").with(user(b.getLoginId()))).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store"));
        images.upload(admin.getLoginId(),a.getId(),photo());assertThat(files.existsById(old)).isFalse();Long replacement=a.getProfileFileId();
        images.remove(a.getLoginId(),a.getId());assertThat(a.getProfileImageUrl()).isNull();assertThat(files.existsById(replacement)).isFalse();
        mvc.perform(get("/profiles/"+a.getId()+"/image").with(user(b.getLoginId()))).andExpect(status().isNotFound());
    }
    @Test void otherEmployeesCannotChangePhotosAndRequestsRequireAuthenticationAndCsrf() throws Exception {
        assertThatThrownBy(()->images.upload(b.getLoginId(),a.getId(),photo())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->images.remove(b.getLoginId(),a.getId())).isInstanceOf(BusinessException.class);
        mvc.perform(get("/profiles/"+a.getId()+"/image")).andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/profiles/"+a.getId()+"/image").file(photo()).with(user(a.getLoginId()))).andExpect(status().isForbidden());
        mvc.perform(multipart("/profiles/"+a.getId()+"/image").file(photo()).with(user(a.getLoginId())).with(csrf()).param("destination","https://example.com"))
            .andExpect(redirectedUrl("/groupware/directory")).andExpect(flash().attributeExists("success"));
    }
    @Test void invalidImagesCannotReplaceExistingPhoto() throws Exception {
        images.upload(a.getLoginId(),a.getId(),photo());Long original=a.getProfileFileId();
        for(var bad:new MockMultipartFile[]{new MockMultipartFile("file","fake.png","image/png","<svg/>".getBytes()),new MockMultipartFile("file",new byte[5*1024*1024+1]),new MockMultipartFile("file",new byte[0])}) {
            assertThatThrownBy(()->images.upload(a.getLoginId(),a.getId(),bad)).isInstanceOf(BusinessException.class);
            assertThat(a.getProfileFileId()).isEqualTo(original);
        }
    }
}
