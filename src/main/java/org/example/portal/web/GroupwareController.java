package org.example.portal.web;
import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.example.portal.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import java.security.Principal;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Controller @RequestMapping("/groupware") @RequiredArgsConstructor
public class GroupwareController {
    private final GroupwareService service;
    private final PortalService portal;
    private final DepartmentAccess departments;
    private final AmendmentService amendments;
    private final WorkCalendarService calendar;
    private final CompanyHolidayRepository holidays;
    private final NotificationRepository notifications;
    @ModelAttribute void common(Principal p,Model m){
        var me=service.actor(p.getName());m.addAttribute("me",me);m.addAttribute("admin",service.admin(me));m.addAttribute("managerView",false);m.addAttribute("base",service.admin(me)?"/admin":"/employee");m.addAttribute("today",portal.today());
        m.addAttribute("notifications",notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(me.getId()));m.addAttribute("unreadNotifications",notifications.countByRecipientIdAndReadFlagFalse(me.getId()));
    }
    private Employee me(Principal p){return service.actor(p.getName());}
    private String page(Model m,String tab,String title){m.addAttribute("groupwarePage",tab);m.addAttribute("page","gw-"+tab);m.addAttribute("title",title);return "portal";}
    private String done(RedirectAttributes f,String target){f.addFlashAttribute("success","처리되었습니다.");return "redirect:/groupware/"+target;}
    private void assignees(Model m,Employee a){m.addAttribute("assignees",List.of(a));}
    private void reviewers(Model m,Employee a){m.addAttribute("reviewers",service.directory("").stream().filter(e->!e.getId().equals(a.getId())&&departments.isManager(e)).toList());}
    @GetMapping({"","/"}) String root(){return "redirect:/groupware/tasks";}
    @GetMapping("/tasks") String tasks(Principal p,Model m,@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="") String status){
        var a=me(p);m.addAttribute("tasks",service.tasks(a,query,status));m.addAttribute("query",query);m.addAttribute("status",status);assignees(m,a);return page(m,"tasks","업무 관리");
    }
    @GetMapping("/tasks/{id}") String task(Principal p,Model m,@PathVariable Long id){
        var a=me(p);var t=service.task(a,id);m.addAttribute("task",t);m.addAttribute("comments",service.comments(a,id));m.addAttribute("files",service.files(a,"TASK",id));m.addAttribute("fileType","TASK");m.addAttribute("fileOwner",id);m.addAttribute("fileWritable",true);m.addAttribute("editableTask",a.getId().equals(t.getAssignee().getId()));assignees(m,a);return page(m,"task","업무 상세");
    }
    @PostMapping("/tasks") String saveTask(Principal p,@RequestParam(required=false) Long id,@RequestParam Long assigneeId,@RequestParam String title,@RequestParam String description,@RequestParam LocalDate dueDate,RedirectAttributes f){
        return done(f,"tasks/"+service.saveTask(p.getName(),id,assigneeId,title,description,dueDate));
    }
    @PostMapping("/tasks/{id}/status") String status(Principal p,@PathVariable Long id,@RequestParam String status,RedirectAttributes f){service.taskStatus(p.getName(),id,status);return done(f,"tasks/"+id);}
    @PostMapping("/tasks/{id}/comments") String comment(Principal p,@PathVariable Long id,@RequestParam String content,RedirectAttributes f){service.comment(p.getName(),id,content);return done(f,"tasks/"+id);}
    @GetMapping("/documents") String documents(Principal p,Model m){m.addAttribute("documents",service.documents(me(p)));reviewers(m,me(p));return page(m,"documents","전자결재");}
    @GetMapping("/documents/{id}") String document(Principal p,Model m,@PathVariable Long id){var a=me(p);var d=service.document(a,id);m.addAttribute("document",d);m.addAttribute("steps",service.steps(id));m.addAttribute("canReviewDocument",service.canReview(a,d));m.addAttribute("files",service.files(a,"DOCUMENT",id));m.addAttribute("fileType","DOCUMENT");m.addAttribute("fileOwner",id);m.addAttribute("fileWritable",a.getId().equals(d.getAuthor().getId())&&d.getStatus().equals("PENDING")&&d.getCurrentStep()==1);return page(m,"document","결재 문서");}
    @PostMapping("/documents") String createDocument(Principal p,@RequestParam String kind,@RequestParam String title,@RequestParam String content,@RequestParam(required=false) BigDecimal amount,@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate,@RequestParam List<Long> reviewerIds,RedirectAttributes f){return done(f,"documents/"+service.createDocument(p.getName(),kind,title,content,amount,startDate,endDate,reviewerIds));}
    @PostMapping("/documents/{id}/review") String reviewDocument(Principal p,@PathVariable Long id,@RequestParam boolean approve,@RequestParam(defaultValue="") String comment,RedirectAttributes f){service.reviewDocument(p.getName(),id,approve,comment);return done(f,"documents/"+id);}
    @PostMapping("/documents/{id}/cancel") String cancelDocument(Principal p,@PathVariable Long id,RedirectAttributes f){service.cancelDocument(p.getName(),id);return done(f,"documents/"+id);}
    @GetMapping("/directory") String directory(Principal p,Model m,@RequestParam(defaultValue="") String query){
        var list=service.directory(query);m.addAttribute("directory",list);m.addAttribute("query",query);m.addAttribute("departments",list.stream().map(Employee::getDepartment).distinct().sorted().toList());return page(m,"directory","조직도·직원 연락처");
    }
    @PostMapping("/directory/{id}") String profile(Principal p,@PathVariable Long id,@RequestParam(defaultValue="") String extension,@RequestParam(defaultValue="") String duty,RedirectAttributes f){service.profile(p.getName(),id,extension,duty);return done(f,"directory");}
    @GetMapping("/holidays") String holidays(Model m){m.addAttribute("holidays",holidays.findAll().stream().sorted(Comparator.comparing(CompanyHoliday::getHolidayDate)).toList());return page(m,"holidays","공휴일·회사 휴무일");}
    @PostMapping("/holidays") String holiday(Principal p,@RequestParam LocalDate holidayDate,@RequestParam String name,@RequestParam String kind,RedirectAttributes f){calendar.save(me(p),holidayDate,name,kind);return done(f,"holidays");}
    @PostMapping("/holidays/{id}/delete") String deleteHoliday(Principal p,@PathVariable Long id,RedirectAttributes f){calendar.delete(me(p),id);return done(f,"holidays");}
    @GetMapping("/reservations") String reservations(Principal p,Model m,@RequestParam(required=false) LocalDate date){date=date==null?portal.today():date;m.addAttribute("date",date);m.addAttribute("resources",service.resources());m.addAttribute("reservations",service.reservations(date,service.admin(me(p))));return page(m,"reservations",service.admin(me(p))?"회의실·장비 예약 허가":"회의실·장비 예약");}
    @PostMapping("/resources") String resource(Principal p,@RequestParam(required=false) Long id,@RequestParam String name,@RequestParam String kind,@RequestParam(defaultValue="") String location,@RequestParam(defaultValue="1") int capacity,@RequestParam(defaultValue="true") boolean active,RedirectAttributes f){service.saveResource(p.getName(),id,name,kind,location,capacity,active);return done(f,"reservations");}
    @PostMapping("/reservations") String reserve(Principal p,@RequestParam Long resourceId,@RequestParam String title,@RequestParam LocalDateTime startsAt,@RequestParam LocalDateTime endsAt,RedirectAttributes f){service.reserve(p.getName(),resourceId,title,startsAt,endsAt);return done(f,"reservations?date="+startsAt.toLocalDate());}
    @PostMapping("/reservations/{id}/review") String reviewReservation(Principal p,@PathVariable Long id,@RequestParam boolean approve,@RequestParam(defaultValue="") String comment,RedirectAttributes f){service.reviewReservation(p.getName(),id,approve,comment);return done(f,"reservations");}
    @GetMapping("/reservations/{id}") String reservation(@PathVariable Long id){return "redirect:/groupware/reservations?date="+service.reservationDate(id);}
    @PostMapping("/reservations/{id}/cancel") String cancelReservation(Principal p,@PathVariable Long id,RedirectAttributes f){service.cancelReservation(p.getName(),id);return done(f,"reservations");}
    @GetMapping("/library") String library(Principal p,Model m,@RequestParam(defaultValue="") String query){m.addAttribute("library",service.library(me(p),query));m.addAttribute("query",query);return page(m,"library","자료실");}
    @GetMapping("/library/{id}") String libraryDocument(Principal p,Model m,@PathVariable Long id){var a=me(p);var d=service.libraryDocument(a,id);m.addAttribute("sharedDocument",d);m.addAttribute("files",service.files(a,"LIBRARY",id));m.addAttribute("fileType","LIBRARY");m.addAttribute("fileOwner",id);m.addAttribute("fileWritable",service.admin(a)||a.getId().equals(d.getAuthor().getId()));return page(m,"library-document","자료 상세");}
    @PostMapping("/library") String saveLibrary(Principal p,@RequestParam(required=false) Long id,@RequestParam String title,@RequestParam String content,@RequestParam String visibility,RedirectAttributes f){return done(f,"library/"+service.saveLibrary(p.getName(),id,title,content,visibility));}
    private String fileTarget(String type,Long id){return switch(type){case "TASK"->"tasks/"+id;case "DOCUMENT"->"documents/"+id;case "LIBRARY"->"library/"+id;default->throw new BusinessException("첨부 대상이 올바르지 않습니다.");};}
    @PostMapping("/files") String upload(Principal p,@RequestParam String ownerType,@RequestParam Long ownerId,@RequestParam MultipartFile file,RedirectAttributes f){service.upload(p.getName(),ownerType,ownerId,file);return done(f,fileTarget(ownerType,ownerId));}
    @GetMapping("/files/{id}") ResponseEntity<byte[]> download(Principal p,@PathVariable Long id){var file=service.download(me(p),id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.getFilename(),StandardCharsets.UTF_8).build().toString()).header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(file.getData());}
    @GetMapping("/amendments") String amendments(Principal p,Model m,@RequestParam(required=false) String type,@RequestParam(required=false) Long requestId){
        return "redirect:/employee/request-status"+(type!=null&&requestId!=null?"?type="+amendments.ownOriginal(me(p),type,requestId).type()+"&requestId="+requestId:"");
    }
    @GetMapping("/amendments/{id}") String amendment(Principal p,Model m,@PathVariable Long id){var a=me(p);var r=amendments.detail(a,id);return "redirect:"+(r.getEmployee().getId().equals(a.getId())?"/employee/request-status":"/manager/approval")+"?amendmentId="+id;}
    @PostMapping("/amendments") String requestAmendment(Principal p,@RequestParam String type,@RequestParam Long requestId,@RequestParam String action,@RequestParam Long approverId,@RequestParam String reason,@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate,@RequestParam(required=false) String leaveKind,@RequestParam(required=false) LocalTime startTime,@RequestParam(required=false) LocalTime endTime,RedirectAttributes f){return done(f,"amendments/"+amendments.request(p.getName(),type,requestId,action,approverId,reason,startDate,endDate,leaveKind,startTime,endTime));}
    @PostMapping("/amendments/{id}/review") String reviewAmendment(Principal p,@PathVariable Long id,@RequestParam boolean approve,@RequestParam(defaultValue="") String comment,RedirectAttributes f){amendments.review(p.getName(),id,approve,comment);return done(f,"amendments/"+id);}
    @PostMapping("/amendments/{id}/cancel") String cancelAmendment(Principal p,@PathVariable Long id,RedirectAttributes f){amendments.cancel(p.getName(),id);return done(f,"amendments/"+id);}
}
