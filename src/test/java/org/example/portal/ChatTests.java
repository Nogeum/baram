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
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ChatTests {
    @Autowired ChatService chat;
    @Autowired PortalService portal;
    @Autowired EmployeeRepository employees;
    @Autowired ChatMessageRepository messages;
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    Employee a,b,c;
    @BeforeEach void setup(){a=employee("a",Employee.Role.EMPLOYEE);b=employee("b",Employee.Role.EMPLOYEE);c=employee("admin",Employee.Role.ADMIN);}
    Employee employee(String name,Employee.Role role){return portal.createEmployee("chat_"+name+"_"+UUID.randomUUID().toString().substring(0,8),"Testing123!",name,"채팅검증","사원",role,"","",LocalDate.of(2026,1,1));}
    ChatService.Message send(Employee from,Employee to,String body){return chat.send(from.getLoginId(),to.getId(),body,UUID.randomUUID().toString());}
    @Test void privateHistoryCannotBeReadByOtherEmployeesOrAdministrators() throws Exception {
        var m=send(a,b,"private <script>not executable</script>");
        assertThat(chat.history(b.getLoginId(),a.getId(),null).messages()).extracting(ChatService.Message::id).containsExactly(m.id());
        assertThat(chat.history(c.getLoginId(),a.getId(),null).messages()).isEmpty();
        assertThat(chat.inbox(c.getLoginId()).threads()).isEmpty();
        mvc.perform(get("/api/chat/threads/"+a.getId()).with(user(c.getLoginId()).roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.messages").isEmpty());
        mvc.perform(get("/api/chat/inbox")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/api/chat/threads/"+b.getId()).with(user(a.getLoginId()).roles("EMPLOYEE")).param("content","no csrf").param("clientId",UUID.randomUUID().toString())).andExpect(status().isForbidden());
    }
    @Test void readsAreExplicitAndScopedToRecipientAndPeer(){
        var first=send(a,b,"one");var second=send(a,b,"two");var other=send(c,b,"other peer");
        chat.history(b.getLoginId(),a.getId(),null);assertThat(chat.inbox(b.getLoginId()).unread()).isEqualTo(3);
        chat.read(c.getLoginId(),a.getId(),List.of(first.id()));assertThat(chat.inbox(b.getLoginId()).unread()).isEqualTo(3);
        chat.read(b.getLoginId(),a.getId(),List.of(first.id(),other.id()));em.clear();
        assertThat(chat.inbox(b.getLoginId()).unread()).isEqualTo(2);
        var history=chat.history(a.getLoginId(),b.getId(),null).messages();
        assertThat(history.get(0).readAt()).isNotNull();assertThat(history.get(1).readAt()).isNull();
        chat.read(b.getLoginId(),a.getId(),List.of(first.id()));assertThat(chat.inbox(b.getLoginId()).unread()).isEqualTo(2);
    }
    @Test void retriesAreIdempotentAndSpoofedSendersAreIgnored() throws Exception {
        String token=UUID.randomUUID().toString();var first=chat.send(a.getLoginId(),b.getId(),"hello",token);
        assertThat(chat.send(a.getLoginId(),b.getId(),"hello",token).id()).isEqualTo(first.id());
        assertThatThrownBy(()->chat.send(a.getLoginId(),c.getId(),"hello",token)).isInstanceOf(BusinessException.class);
        mvc.perform(post("/api/chat/threads/"+b.getId()).with(user(a.getLoginId()).roles("EMPLOYEE")).with(csrf())
                .param("content","from authenticated sender").param("clientId",UUID.randomUUID().toString()).param("senderId",c.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mine").value(true));
        assertThat(chat.history(c.getLoginId(),b.getId(),null).messages()).isEmpty();
        assertThat(chat.history(a.getLoginId(),b.getId(),null).messages()).hasSize(2);
    }
    @Test void historySupportsOlderPagesWithoutLosingMessages(){
        for(int i=0;i<55;i++)send(a,b,"message "+i);
        var latest=chat.history(b.getLoginId(),a.getId(),null);assertThat(latest.messages()).hasSize(50);assertThat(latest.hasMore()).isTrue();
        var older=chat.history(b.getLoginId(),a.getId(),latest.messages().get(0).id());assertThat(older.messages()).hasSize(5);assertThat(older.hasMore()).isFalse();
        assertThat(older.messages().get(4).id()).isLessThan(latest.messages().get(0).id());
        var inbox=chat.inbox(b.getLoginId());assertThat(inbox.unread()).isEqualTo(55);assertThat(inbox.threads().get(0).preview()).isEqualTo("message 54");
    }
    @Test void validatesContentAndBlocksInactiveOrDeletedAccounts(){
        assertThatThrownBy(()->send(a,a,"self")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->send(a,b," \n ")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->send(a,b,"x".repeat(2001))).isInstanceOf(BusinessException.class);
        send(a,b,"existing record");b.setActive(false);employees.saveAndFlush(b);
        assertThatThrownBy(()->send(a,b,"inactive")).isInstanceOf(BusinessException.class);
        assertThat(chat.people(a.getLoginId(),"")).extracting(ChatService.Person::id).doesNotContain(a.getId(),b.getId());
        assertThat(chat.history(a.getLoginId(),b.getId(),null).person().available()).isFalse();
        b.setDeletedAt(LocalDateTime.now());employees.saveAndFlush(b);
        assertThat(chat.history(a.getLoginId(),b.getId(),null).messages()).hasSize(1);
        assertThatThrownBy(()->chat.inbox(b.getLoginId())).isInstanceOf(BusinessException.class);
    }
}
