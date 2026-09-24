package org.example.portal.web;

import lombok.RequiredArgsConstructor;
import org.example.portal.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.security.Principal;
import java.util.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chat;
    @GetMapping("/people") public List<ChatService.Person> people(Principal p,@RequestParam(defaultValue="") String query){return chat.people(p.getName(),query);}
    @GetMapping("/inbox") public ChatService.Inbox inbox(Principal p){return chat.inbox(p.getName());}
    @GetMapping("/threads/{id}") public ChatService.History history(Principal p,@PathVariable Long id,@RequestParam(required=false) Long before){return chat.history(p.getName(),id,before);}
    @PostMapping("/threads/{id}") public ChatService.Message send(Principal p,@PathVariable Long id,@RequestParam String content,@RequestParam String clientId){return chat.send(p.getName(),id,content,clientId);}
    @PostMapping("/threads/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(Principal p,@PathVariable Long id,@RequestParam List<Long> ids){chat.read(p.getName(),id,ids);}
    @ExceptionHandler(BusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(BusinessException ex){return Map.of("message",ex.getMessage());}
}
