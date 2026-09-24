package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ChatService {
    private final PortalService portal;
    private final EmployeeRepository employees;
    private final ChatMessageRepository messages;
    private final Clock clock;
    public record Person(Long id,String name,String department,String position,boolean available,String profileImageUrl) {}
    public record Message(Long id,boolean mine,String content,LocalDateTime sentAt,LocalDateTime readAt) {}
    public record ThreadSummary(Person person,String preview,LocalDateTime sentAt,long unread) {}
    public record Inbox(long unread,List<ThreadSummary> threads) {}
    public record History(Person person,List<Message> messages,boolean hasMore) {}
    private Person person(Employee e){return new Person(e.getId(),e.getDisplayName(),e.getDepartment(),e.getPositionName(),e.isActive()&&!e.isDeleted(),e.isDeleted()?null:e.getProfileImageUrl());}
    private Message message(ChatMessage m,Long me){return new Message(m.getId(),m.getSender().getId().equals(me),m.getContent(),m.getSentAt(),m.getReadAt());}
    private void require(boolean condition,String message){if(!condition)throw new BusinessException(message);}
    public List<Person> people(String login,String query){
        var me=portal.current(login);String q=Objects.toString(query,"").strip().toLowerCase(Locale.ROOT);
        require(q.length()<=80,"검색어는 80자 이내로 입력하세요.");
        return employees.findByDeletedAtIsNullOrderByNameAsc().stream().filter(Employee::isActive).filter(e->!e.getId().equals(me.getId()))
            .filter(e->(e.getName()+" "+e.getDepartment()+" "+e.getPositionName()).toLowerCase(Locale.ROOT).contains(q)).limit(50).map(this::person).toList();
    }
    public Inbox inbox(String login){
        var me=portal.current(login);var latest=new HashMap<Long,Long>();var unread=new HashMap<Long,Long>();
        for(var r:messages.sentThreads(me.getId()))latest.put(((Number)r[0]).longValue(),((Number)r[1]).longValue());
        for(var r:messages.receivedThreads(me.getId())){long peer=((Number)r[0]).longValue();latest.merge(peer,((Number)r[1]).longValue(),Math::max);unread.put(peer,((Number)r[2]).longValue());}
        var ids=latest.values().stream().sorted(Comparator.reverseOrder()).limit(50).toList();
        var threads=ids.isEmpty()?List.<ThreadSummary>of():messages.summaries(ids).stream().sorted(Comparator.comparing(ChatMessage::getId).reversed()).map(m->{
            var peer=m.getSender().getId().equals(me.getId())?m.getRecipient():m.getSender();String body=m.getContent();
            return new ThreadSummary(person(peer),body.substring(0,Math.min(80,body.length())),m.getSentAt(),unread.getOrDefault(peer.getId(),0L));
        }).toList();
        return new Inbox(messages.countByRecipientIdAndReadAtIsNull(me.getId()),threads);
    }
    public History history(String login,Long peerId,Long before){
        var me=portal.current(login);require(!me.getId().equals(peerId),"본인에게는 쪽지를 보낼 수 없습니다.");
        var peer=employees.findById(peerId).orElseThrow(()->new BusinessException("직원을 찾을 수 없습니다."));
        require(before==null||before>0,"이전 메시지 기준을 확인하세요.");
        var rows=messages.history(me.getId(),peerId,before==null?Long.MAX_VALUE:before,PageRequest.of(0,51));
        var result=rows.stream().limit(50).sorted(Comparator.comparing(ChatMessage::getId)).map(m->message(m,me.getId())).toList();
        return new History(person(peer),result,rows.size()>50);
    }
    @Transactional public Message send(String login,Long peerId,String content,String clientId){
        var current=portal.current(login);
        var me=employees.lockById(current.getId()).orElseThrow(()->new BusinessException("계정을 확인하세요."));
        require(me.isActive()&&!me.isDeleted(),"이 계정으로 메시지를 보낼 수 없습니다.");
        String body=Objects.toString(content,"").strip();
        require(!body.isBlank()&&body.length()<=2000,"메시지는 1~2,000자로 입력하세요.");
        require(clientId!=null&&clientId.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"),"메시지 전송 번호를 확인하세요.");
        var previous=messages.findBySenderIdAndClientId(me.getId(),clientId);
        if(previous.isPresent()){
            var m=previous.get();require(m.getRecipient().getId().equals(peerId)&&m.getContent().equals(body),"이미 사용한 전송 번호입니다.");return message(m,me.getId());
        }
        require(!me.getId().equals(peerId),"본인에게는 쪽지를 보낼 수 없습니다.");
        var peer=employees.findById(peerId).orElseThrow(()->new BusinessException("직원을 찾을 수 없습니다."));
        require(peer.isActive()&&!peer.isDeleted(),"퇴사 또는 비활성 계정에는 메시지를 보낼 수 없습니다.");
        var m=new ChatMessage();m.setSender(me);m.setRecipient(peer);m.setContent(body);m.setClientId(clientId);m.setSentAt(LocalDateTime.now(clock));
        return message(messages.saveAndFlush(m),me.getId());
    }
    @Transactional public void read(String login,Long peerId,List<Long> ids){
        var me=portal.current(login);require(ids!=null&&!ids.isEmpty()&&ids.size()<=100&&ids.stream().allMatch(id->id!=null&&id>0),"읽음 처리할 메시지를 확인하세요.");
        messages.markRead(me.getId(),peerId,ids,LocalDateTime.now(clock));
    }
}
