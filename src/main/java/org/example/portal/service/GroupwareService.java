package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class GroupwareService {
    private final PortalService portal;
    private final DepartmentAccess departments;
    private final EmployeeRepository employees;
    private final WorkTaskRepository tasks;
    private final TaskCommentRepository comments;
    private final ApprovalDocumentRepository documents;
    private final ApprovalStepRepository steps;
    private final SharedDocumentRepository library;
    private final StoredFileRepository files;
    private final BookableResourceRepository resources;
    private final ResourceReservationRepository reservations;
    private final NotificationRepository notifications;
    private final Clock clock;

    public Employee actor(String login){return portal.current(login);}
    public boolean admin(Employee e){return e.getRole()==Employee.Role.ADMIN;}
    private void allow(boolean condition){if(!condition)throw new AccessDeniedException("이 항목에 접근하거나 처리할 권한이 없습니다.");}
    private void require(boolean condition,String message){if(!condition)throw new BusinessException(message);}
    private String text(String value,int max){value=value==null?"":value.trim();require(!value.isBlank() && value.length()<=max,"필수 내용을 "+max+"자 이내로 입력하세요.");return value;}
    private boolean same(Employee a,Employee b){return a.getId().equals(b.getId());}
    private Employee employee(Long id){var e=employees.findById(id).orElseThrow(()->new BusinessException("직원이 없습니다."));require(e.isActive()&&!e.isDeleted(),"비활성 직원입니다.");return e;}
    public List<Employee> directory(String query){String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);return employees.findByDeletedAtIsNullOrderByNameAsc().stream().filter(Employee::isActive).filter(e->q.isEmpty()||(e.getName()+" "+e.getDepartment()+" "+e.getPositionName()+" "+Objects.toString(e.getJobDescription(),"")).toLowerCase(Locale.ROOT).contains(q)).toList();}
    @Transactional public void profile(String login,Long id,String extension,String duty){
        var a=actor(login);var e=employee(id);allow(admin(a)||same(a,e));
        require(extension.length()<=30&&duty.length()<=500,"내선은 30자, 담당 업무는 500자 이내로 입력하세요.");e.setExtensionNumber(extension.trim());e.setJobDescription(duty.trim());
    }
    public void notify(Employee recipient,String message,String path){if(recipient.isDeleted()||!recipient.isActive())return;var n=new Notification();n.setRecipient(recipient);n.setMessage(message);n.setTargetPath(path);n.setCreatedAt(LocalDateTime.now(clock));notifications.save(n);}
    private boolean canTask(Employee a,WorkTask t){return !admin(a)&&same(a,t.getAssignee());}
    public List<WorkTask> tasks(Employee a,String query,String status){
        return tasks.findAll().stream().filter(t->canTask(a,t)).filter(t->query==null||query.isBlank()||(t.getTitle()+" "+t.getAssignee().getName()).contains(query))
            .filter(t->status==null||status.isBlank()||("OVERDUE".equals(status)? !t.getStatus().equals("DONE")&&t.getDueDate().isBefore(portal.today()):t.getStatus().equals(status)))
            .sorted(Comparator.comparing(WorkTask::getDueDate).thenComparing(WorkTask::getId).reversed()).toList();
    }
    public WorkTask task(Employee a,Long id){var t=tasks.findById(id).orElseThrow(()->new BusinessException("업무가 없습니다."));allow(canTask(a,t));return t;}
    public List<TaskComment> comments(Employee a,Long id){task(a,id);return comments.findAll().stream().filter(c->c.getTask().getId().equals(id)).sorted(Comparator.comparing(TaskComment::getCreatedAt)).toList();}
    @Transactional public Long saveTask(String login,Long id,Long assignee,String title,String description,LocalDate due){
        var a=actor(login);var target=employee(assignee);allow(!admin(a)&&same(a,target));
        require(due!=null,"마감일을 선택하세요.");
        WorkTask t;
        if(id==null){t=new WorkTask();t.setCreator(a);t.setCreatedAt(LocalDateTime.now(clock));}
        else {t=tasks.lockById(id).orElseThrow(()->new BusinessException("업무가 없습니다."));allow(canTask(a,t));}
        t.setAssignee(target);t.setTitle(text(title,160));t.setDescription(text(description,4000));t.setDueDate(due);t.setReminderDate(null);tasks.saveAndFlush(t);
        if(!same(a,target))notify(target,"업무가 배정·변경되었습니다: "+t.getTitle(),"/groupware/tasks/"+t.getId());
        return t.getId();
    }
    @Transactional public void taskStatus(String login,Long id,String status){
        var a=actor(login);var t=tasks.lockById(id).orElseThrow(()->new BusinessException("업무가 없습니다."));allow(canTask(a,t));
        require(Set.of("WAITING","IN_PROGRESS","DONE").contains(status),"업무 상태를 확인하세요.");t.setStatus(status);
        if(!same(a,t.getCreator()))notify(t.getCreator(),"업무 상태가 변경되었습니다: "+t.getTitle(),"/groupware/tasks/"+id);
    }
    @Transactional public void comment(String login,Long id,String content){
        var a=actor(login);var t=task(a,id);var c=new TaskComment();c.setTask(t);c.setAuthor(a);c.setContent(text(content,2000));c.setCreatedAt(LocalDateTime.now(clock));comments.save(c);
        var recipients=new HashMap<Long,Employee>();recipients.put(t.getCreator().getId(),t.getCreator());recipients.put(t.getAssignee().getId(),t.getAssignee());recipients.remove(a.getId());
        recipients.values().forEach(e->notify(e,"업무에 새 댓글이 등록되었습니다: "+t.getTitle(),"/groupware/tasks/"+id));
    }
    @Transactional public void remindTasks(){
        var today=portal.today();
        for(var row:tasks.findAll()){
            if(row.getStatus().equals("DONE")||row.getDueDate().isAfter(today)||today.equals(row.getReminderDate()))continue;
            var t=tasks.lockById(row.getId()).orElseThrow();
            if(t.getStatus().equals("DONE")||today.equals(t.getReminderDate()))continue;
            notify(t.getAssignee(),(t.getDueDate().isBefore(today)?"마감일이 지난 업무: ":"오늘 마감 업무: ")+t.getTitle(),"/groupware/tasks/"+t.getId());t.setReminderDate(today);
        }
    }
    public List<ApprovalStep> steps(Long id){return steps.findAll().stream().filter(s->s.getDocument().getId().equals(id)).sorted(Comparator.comparingInt(ApprovalStep::getStepOrder)).toList();}
    private boolean canDocument(Employee a,ApprovalDocument d){return !admin(a)&&(same(a,d.getAuthor())||steps(d.getId()).stream().anyMatch(s->same(a,s.getReviewer())));}
    public List<ApprovalDocument> documents(Employee a){return documents.findAll().stream().filter(d->canDocument(a,d)).sorted(Comparator.comparing(ApprovalDocument::getCreatedAt).reversed()).toList();}
    public ApprovalDocument document(Employee a,Long id){var d=documents.findById(id).orElseThrow(()->new BusinessException("결재 문서가 없습니다."));allow(canDocument(a,d));return d;}
    public boolean canReview(Employee a,ApprovalDocument d){return d.getStatus().equals("PENDING")&&steps(d.getId()).stream().anyMatch(s->s.getStepOrder()==d.getCurrentStep()&&same(a,s.getReviewer()));}
    @Transactional public Long createDocument(String login,String kind,String title,String content,BigDecimal amount,LocalDate start,LocalDate end,List<Long> reviewerIds){
        var a=actor(login);allow(!admin(a));require(Set.of("PURCHASE","EXPENSE","TRIP").contains(kind),"문서 종류를 확인하세요.");
        var ids=reviewerIds.stream().filter(Objects::nonNull).toList();
        require(!ids.isEmpty()&&ids.size()<=5&&new HashSet<>(ids).size()==ids.size(),"서로 다른 결재자를 1~5명 지정하세요.");
        var reviewers=ids.stream().map(this::employee).toList();
        reviewers.forEach(e->{require(!same(a,e),"본인을 결재자로 선택할 수 없습니다.");require(departments.isManager(e),"관리 권한이 있는 결재자를 선택하세요.");});
        var heads=directory("").stream().filter(Employee::isDivisionHead).filter(e->e.getDepartment().equals(a.getDepartment())&&!same(a,e)).toList();
        if(!heads.isEmpty()) require(heads.stream().anyMatch(e->same(e,reviewers.get(reviewers.size()-1))),"소속 부서 본부장을 최종 결재자로 지정하세요.");
        for(int i=0;i<reviewers.size()-1;i++) require(!reviewers.get(i).isDivisionHead()||reviewers.get(i+1).isDivisionHead(),"본부장 이후에는 하위 직급 결재자를 지정할 수 없습니다.");
        if(kind.equals("TRIP"))require(start!=null&&end!=null&&!end.isBefore(start),"출장 기간을 입력하세요.");
        else require(amount!=null&&amount.signum()>0&&amount.scale()<=2&&amount.compareTo(new BigDecimal("9999999999999.99"))<=0,"금액은 0보다 크고 소수점 두 자리 이내여야 합니다.");
        var d=new ApprovalDocument();d.setAuthor(a);d.setKind(kind);d.setTitle(text(title,160));d.setContent(text(content,4000));d.setAmount(kind.equals("TRIP")?null:amount);d.setStartDate(start);d.setEndDate(end);d.setCreatedAt(LocalDateTime.now(clock));documents.saveAndFlush(d);
        int order=1;for(var e:reviewers){var s=new ApprovalStep();s.setDocument(d);s.setReviewer(e);s.setStepOrder(order++);steps.save(s);}
        notify(reviewers.get(0),"전자결재 요청: "+d.getTitle(),"/groupware/documents/"+d.getId());return d.getId();
    }
    @Transactional public void reviewDocument(String login,Long id,boolean approve,String comment){
        var a=actor(login);var d=documents.lockById(id).orElseThrow(()->new BusinessException("문서가 없습니다."));allow(canReview(a,d));
        require(departments.isManager(a),"결재 권한이 변경되었습니다.");require(!same(a,d.getAuthor()),"본인 결재는 불가합니다.");
        comment=approve?(comment==null?"":comment.trim()):text(comment,500);require(comment.length()<=500,"의견은 500자 이내로 입력하세요.");
        var line=steps(id);var step=line.stream().filter(s->s.getStepOrder()==d.getCurrentStep()).findFirst().orElseThrow();
        step.setStatus(approve?"APPROVED":"REJECTED");step.setComment(comment);step.setReviewedAt(LocalDateTime.now(clock));
        if(!approve)d.setStatus("REJECTED");
        else if(d.getCurrentStep()==line.size())d.setStatus("APPROVED");
        else {d.setCurrentStep(d.getCurrentStep()+1);notify(line.get(d.getCurrentStep()-1).getReviewer(),"전자결재 요청: "+d.getTitle(),"/groupware/documents/"+id);}
        notify(d.getAuthor(),"전자결재 처리: "+d.getTitle()+" ("+d.getStatusLabel()+")","/groupware/documents/"+id);
    }
    @Transactional public void cancelDocument(String login,Long id){
        var a=actor(login);var d=documents.lockById(id).orElseThrow(()->new BusinessException("문서가 없습니다."));allow(same(a,d.getAuthor()));require(d.getStatus().equals("PENDING"),"결재 대기 문서만 취소할 수 있습니다.");d.setStatus("CANCELLED");
        steps(id).stream().filter(s->s.getStatus().equals("PENDING")).forEach(s->notify(s.getReviewer(),"전자결재가 취소되었습니다: "+d.getTitle(),"/groupware/documents/"+id));
    }
    public boolean canLibrary(Employee a,SharedDocument d){return admin(a)||same(a,d.getAuthor())||d.getVisibility().equals("COMPANY")||a.getDepartment().equals(d.getDepartment());}
    public SharedDocument libraryDocument(Employee a,Long id){var d=library.findById(id).orElseThrow(()->new BusinessException("자료가 없습니다."));allow(canLibrary(a,d));return d;}
    public List<SharedDocument> library(Employee a,String query){return library.findAll().stream().filter(d->canLibrary(a,d)).filter(d->query==null||query.isBlank()||(d.getTitle()+" "+d.getContent()).contains(query)).sorted(Comparator.comparing(SharedDocument::getUpdatedAt).reversed()).toList();}
    @Transactional public Long saveLibrary(String login,Long id,String title,String content,String visibility){
        var a=actor(login);require(Set.of("COMPANY","DEPARTMENT").contains(visibility),"공개 범위를 선택하세요.");
        SharedDocument d;if(id==null){d=new SharedDocument();d.setAuthor(a);d.setDepartment(a.getDepartment());d.setCreatedAt(LocalDateTime.now(clock));}
        else {d=library.lockById(id).orElseThrow(()->new BusinessException("자료가 없습니다."));allow(admin(a)||same(a,d.getAuthor()));}
        d.setTitle(text(title,160));d.setContent(text(content,4000));d.setVisibility(visibility);d.setUpdatedAt(LocalDateTime.now(clock));library.saveAndFlush(d);return d.getId();
    }
    public List<BookableResource> resources(){return resources.findAll().stream().sorted(Comparator.comparing(BookableResource::getName)).toList();}
    @Transactional public void saveResource(String login,Long id,String name,String kind,String location,int capacity,boolean active){
        var a=actor(login);allow(admin(a));require(Set.of("ROOM","EQUIPMENT").contains(kind)&&capacity>0&&capacity<=10000,"자원 종류·수량을 확인하세요.");
        var r=id==null?new BookableResource():resources.lockById(id).orElseThrow(()->new BusinessException("자원이 없습니다."));
        r.setName(text(name,120));r.setKind(kind);require(location.length()<=200,"위치는 200자 이내입니다.");r.setLocation(location.trim());r.setCapacity(capacity);r.setActive(active);resources.save(r);
    }
    public List<ResourceReservation> reservations(LocalDate date){return reservations(date,false);}
    public List<ResourceReservation> reservations(LocalDate date,boolean includePending){return reservations.findAll().stream().filter(r->(includePending&&r.getStatus().equals("PENDING"))||(!r.getStartsAt().toLocalDate().isAfter(date)&&!r.getEndsAt().toLocalDate().isBefore(date))).sorted(Comparator.comparing(ResourceReservation::getStartsAt)).toList();}
    @Transactional public void reserve(String login,Long resourceId,String title,LocalDateTime start,LocalDateTime end){
        var a=actor(login);allow(!admin(a));require(start!=null&&end!=null&&end.isAfter(start)&&!start.isBefore(LocalDateTime.now(clock)),"미래의 올바른 예약 시간을 선택하세요.");require(Duration.between(start,end).toDays()<31,"예약은 30일 이내로 등록하세요.");
        var r=resources.lockById(resourceId).orElseThrow(()->new BusinessException("자원이 없습니다."));require(r.isActive(),"사용 중지된 자원입니다.");
        require(reservations.findAll().stream().noneMatch(b->b.getResource().getId().equals(resourceId)&&Set.of("ACTIVE","PENDING").contains(b.getStatus())&&start.isBefore(b.getEndsAt())&&end.isAfter(b.getStartsAt())),"이미 예약된 시간과 겹칩니다.");
        var b=new ResourceReservation();b.setResource(r);b.setEmployee(a);b.setTitle(text(title,160));b.setStartsAt(start);b.setEndsAt(end);b.setCreatedAt(LocalDateTime.now(clock));reservations.save(b);
        notify(a,"자원 예약 승인 대기: "+r.getName(),"/groupware/reservations/"+b.getId());
        directory("").stream().filter(this::admin).forEach(e->notify(e,"자원 예약 허가 요청: "+r.getName(),"/groupware/reservations/"+b.getId()));
    }
    @Transactional public void cancelReservation(String login,Long id){
        var a=actor(login);var row=reservations.findById(id).orElseThrow(()->new BusinessException("예약이 없습니다."));resources.lockById(row.getResource().getId());
        var b=reservations.lockById(id).orElseThrow();allow(admin(a)||same(a,b.getEmployee()));require(Set.of("ACTIVE","PENDING").contains(b.getStatus()),"취소 가능한 예약이 아닙니다.");b.setStatus("CANCELLED");notify(b.getEmployee(),"자원 예약 취소: "+b.getResource().getName(),"/groupware/reservations/"+id);
    }
    @Transactional public void reviewReservation(String login,Long id,boolean approve,String comment){
        var a=actor(login);allow(admin(a));
        var row=reservations.findById(id).orElseThrow(()->new BusinessException("예약이 없습니다."));
        var resource=resources.lockById(row.getResource().getId()).orElseThrow();
        var b=reservations.lockById(id).orElseThrow();require(b.getStatus().equals("PENDING"),"승인 대기 예약만 처리할 수 있습니다.");
        if(approve){require(resource.isActive(),"사용 중지된 자원입니다.");require(b.getStartsAt().isAfter(LocalDateTime.now(clock)),"시작 시간이 지난 예약은 반려 후 다시 신청하세요.");}
        comment=approve?(comment==null?"":comment.trim()):text(comment,500);require(comment.length()<=500,"의견은 500자 이내입니다.");
        b.setStatus(approve?"ACTIVE":"REJECTED");b.setReviewer(a);b.setReviewedAt(LocalDateTime.now(clock));b.setReviewComment(comment);
        notify(b.getEmployee(),"자원 예약 "+(approve?"허가: ":"반려: ")+resource.getName(),"/groupware/reservations/"+id);
    }
    public LocalDate reservationDate(Long id){return reservations.findById(id).orElseThrow(()->new BusinessException("예약이 없습니다.")).getStartsAt().toLocalDate();}
    public void fileAccess(Employee a,String type,Long id,boolean write){
        switch(type){
            case "TASK" -> task(a,id);
            case "DOCUMENT" -> {var d=document(a,id);if(write)allow(same(a,d.getAuthor())&&d.getStatus().equals("PENDING")&&d.getCurrentStep()==1);}
            case "LIBRARY" -> {var d=libraryDocument(a,id);if(write)allow(admin(a)||same(a,d.getAuthor()));}
            default -> throw new BusinessException("첨부 대상이 올바르지 않습니다.");
        }
    }
    public List<StoredFileRepository.FileInfo> files(Employee a,String type,Long id){fileAccess(a,type,id,false);return files.metadata(type,id);}
    public StoredFile download(Employee a,Long id){var f=files.findById(id).orElseThrow(()->new BusinessException("파일이 없습니다."));fileAccess(a,f.getOwnerType(),f.getOwnerId(),false);return f;}
    @Transactional public void upload(String login,String type,Long id,MultipartFile upload){
        if("DOCUMENT".equals(type))documents.lockById(id).orElseThrow(()->new BusinessException("문서가 없습니다."));
        var a=actor(login);fileAccess(a,type,id,true);require(upload!=null&&!upload.isEmpty()&&upload.getSize()<=10*1024*1024,"10MB 이하의 파일을 선택하세요.");
        String name=Objects.toString(upload.getOriginalFilename(),"file").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");require(!name.isBlank()&&name.length()<=200,"파일명이 너무 길거나 올바르지 않습니다.");
        var f=new StoredFile();f.setUploader(a);f.setOwnerType(type);f.setOwnerId(id);f.setFilename(name);f.setFileSize(upload.getSize());f.setCreatedAt(LocalDateTime.now(clock));
        try{f.setData(upload.getBytes());}catch(java.io.IOException e){throw new BusinessException("파일을 읽을 수 없습니다.");}files.save(f);
    }
}
