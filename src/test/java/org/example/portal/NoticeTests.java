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
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class NoticeTests {
    @Autowired MockMvc mvc;
    @Autowired PortalService portal;
    @Autowired NoticeRepository notices;
    @Autowired StoredFileRepository files;
    @Autowired EmployeeRepository employees;
    Employee admin,otherAdmin,worker,manager;
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        admin=create("na"+suffix,Employee.Role.ADMIN,false);
        otherAdmin=create("nb"+suffix,Employee.Role.ADMIN,false);
        worker=create("nw"+suffix,Employee.Role.EMPLOYEE,false);
        manager=create("nm"+suffix,Employee.Role.EMPLOYEE,true);
    }
    Employee create(String login,Employee.Role role,boolean manager) {
        return portal.createEmployee(login,"Test1234!",login,"개발팀","부장",role,"","",LocalDate.of(2020,1,1),manager);
    }
    @AfterEach void cleanup() {
        var ids=Set.of(admin.getId(),otherAdmin.getId(),worker.getId(),manager.getId());
        files.deleteAll(files.findAll().stream().filter(f->ids.contains(f.getUploader().getId())).toList());
        notices.deleteAll(notices.findAll().stream().filter(n->ids.contains(n.getAuthor().getId())).toList());
        employees.deleteAllById(ids);
    }
    MockMultipartFile file(String name,String body) { return new MockMultipartFile("attachments",name,"text/plain",body.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    Long publish() { return portal.saveNotice(admin.getLoginId(),null,"새 공지","안내 내용",List.of(file("안내.txt","hello")),List.of()); }
    Long fileId(Long id) { return files.metadata("NOTICE",id).get(0).getId(); }

    @Test void multipartCreateAndEmployeeDownloadAndAdminControls() throws Exception {
        mvc.perform(multipart("/admin/notices").file(file("안내.txt","hello")).file(file("일정.txt","schedule"))
            .param("title","첨부 공지").param("content","내용").with(user(admin.getLoginId()).roles("ADMIN")).with(csrf()))
            .andExpect(redirectedUrl("/admin/notification"));
        var notice=notices.findAll().stream().filter(n->n.getAuthor().getId().equals(admin.getId())).findFirst().orElseThrow();
        assertThat(files.metadata("NOTICE",notice.getId())).hasSize(2);
        mvc.perform(get("/employee/main").with(user(worker.getLoginId()).roles("EMPLOYEE")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("안내.txt")))
            .andExpect(content().string(not(containsString("공지 수정"))));
        mvc.perform(get("/admin/notification").with(user(admin.getLoginId()).roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("공지 수정")))
            .andExpect(content().string(containsString("공지와 첨부파일 삭제 확인")));
        var file=files.metadata("NOTICE",notice.getId()).stream().filter(f->f.getFilename().equals("안내.txt")).findFirst().orElseThrow();
        mvc.perform(get("/notices/files/"+file.getId()).with(user(worker.getLoginId()).roles("EMPLOYEE")))
            .andExpect(status().isOk()).andExpect(content().bytes("hello".getBytes()))
            .andExpect(header().string("Content-Disposition",startsWith("attachment;")))
            .andExpect(header().string("X-Content-Type-Options","nosniff"));
        mvc.perform(get("/notices/files/"+file.getId())).andExpect(status().is3xxRedirection());
    }
    @Test void anotherAdminCanEditPreservingAuthorThenReplaceAttachmentAndDelete() throws Exception {
        Long id=publish(),oldFile=fileId(id);
        var original=notices.findById(id).orElseThrow();
        mvc.perform(post("/admin/notices/"+id+"/edit").param("title","수정 공지").param("content","수정 내용")
            .with(user(otherAdmin.getLoginId()).roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection());
        var edited=notices.findById(id).orElseThrow();
        assertThat(edited.getTitle()).isEqualTo("수정 공지");
        assertThat(edited.getAuthor().getId()).isEqualTo(admin.getId());
        assertThat(edited.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(files.existsById(oldFile)).isTrue();
        mvc.perform(multipart("/admin/notices/"+id+"/edit").file(file("교체.txt","new"))
            .param("title","최종 공지").param("content","본문").param("removeFileIds",oldFile.toString())
            .with(user(otherAdmin.getLoginId()).roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(files.existsById(oldFile)).isFalse();
        assertThat(files.metadata("NOTICE",id)).extracting(StoredFileRepository.FileInfo::getFilename).containsExactly("교체.txt");
        Long newFile=fileId(id);
        mvc.perform(post("/admin/notices/"+id+"/delete").with(user(otherAdmin.getLoginId()).roles("ADMIN")).with(csrf()))
            .andExpect(redirectedUrl("/admin/notification"));
        assertThat(notices.existsById(id)).isFalse();
        assertThat(files.existsById(newFile)).isFalse();
        mvc.perform(get("/notices/files/"+newFile).with(user(worker.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isBadRequest());
    }
    @Test void employeesIncludingDepartmentManagersCannotMutateAndCsrfIsRequired() throws Exception {
        Long id=publish();
        for(var actor:List.of(worker,manager)) {
            for(String path:List.of("/admin/notices","/admin/notices/"+id+"/edit","/admin/notices/"+id+"/delete")) {
                mvc.perform(post(path).param("title","공격").param("content","내용").with(user(actor.getLoginId()).roles("EMPLOYEE")).with(csrf()))
                    .andExpect(status().isForbidden());
            }
            assertThatThrownBy(()->portal.deleteNotice(actor.getLoginId(),id)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            assertThatThrownBy(()->portal.saveNotice(actor.getLoginId(),id,"변경","내용",List.of(),List.of()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
        mvc.perform(post("/admin/notices/"+id+"/delete").with(user(admin.getLoginId()).roles("ADMIN"))).andExpect(status().isForbidden());
        assertThat(notices.findById(id).orElseThrow().getTitle()).isEqualTo("새 공지");
    }
    @Test void removingOtherNoticesAttachmentRollsBackEdit() throws Exception {
        Long first=publish(),second=publish(),foreignFile=fileId(second);
        mvc.perform(post("/admin/notices/"+first+"/edit").param("title","변경 시도").param("content","내용")
            .param("removeFileIds",foreignFile.toString()).with(user(admin.getLoginId()).roles("ADMIN")).with(csrf()))
            .andExpect(status().isBadRequest());
        assertThat(notices.findById(first).orElseThrow().getTitle()).isEqualTo("새 공지");
        assertThat(files.existsById(foreignFile)).isTrue();
    }
    @Test void oversizedAttachmentRollsBackTextAndFileRemoval() throws Exception {
        Long id=publish(),oldFile=fileId(id);
        var oversized=new MockMultipartFile("attachments","big.bin","application/octet-stream",new byte[10*1024*1024+1]);
        mvc.perform(multipart("/admin/notices/"+id+"/edit").file(oversized).param("title","변경 시도").param("content","내용")
            .param("removeFileIds",oldFile.toString()).with(user(admin.getLoginId()).roles("ADMIN")).with(csrf()))
            .andExpect(status().isBadRequest());
        assertThat(notices.findById(id).orElseThrow().getTitle()).isEqualTo("새 공지");
        assertThat(files.existsById(oldFile)).isTrue();
        assertThat(files.metadata("NOTICE",id)).hasSize(1);
    }
    @Test void nonNoticeFileCannotBeDownloadedThroughNoticeEndpoint() throws Exception {
        Long id=publish(),fid=fileId(id);
        var stored=files.findById(fid).orElseThrow();stored.setOwnerType("LIBRARY");files.save(stored);
        mvc.perform(get("/notices/files/"+fid).with(user(worker.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isBadRequest());
    }
    @Test void invalidTextAndMissingNoticeAreRejected() throws Exception {
        Long id=publish();
        mvc.perform(post("/admin/notices/"+id+"/edit").param("title"," ").param("content","내용")
            .with(user(admin.getLoginId()).roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post("/admin/notices/9223372036854775807/delete").with(user(admin.getLoginId()).roles("ADMIN")).with(csrf()))
            .andExpect(status().isBadRequest());
        assertThat(notices.findById(id).orElseThrow().getTitle()).isEqualTo("새 공지");
    }
}
