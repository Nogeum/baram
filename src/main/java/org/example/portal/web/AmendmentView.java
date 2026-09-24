package org.example.portal.web;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.service.*;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.security.access.AccessDeniedException;

@Component @RequiredArgsConstructor
public class AmendmentView {
    private final AmendmentService amendments;
    private final DepartmentAccess departments;
    public long pending(Employee actor,boolean review){return amendments.list(actor).stream().filter(r->review?amendments.canReview(actor,r):r.getEmployee().getId().equals(actor.getId())&&r.getStatus().equals("PENDING")).count();}
    public void populate(Model model,Employee actor,boolean review,String type,Long requestId,Long id){
        model.addAttribute("amendmentPage",id==null?"amendments":"amendment");
        model.addAttribute("amendments",amendments.list(actor).stream().filter(r->review?!r.getEmployee().getId().equals(actor.getId()):r.getEmployee().getId().equals(actor.getId())).toList());
        model.addAttribute("approvers",departments.approvers(actor));
        model.addAttribute("kinds",LeaveRequest.Kind.values());
        if(!review&&type!=null&&requestId!=null)model.addAttribute("original",amendments.ownOriginal(actor,type,requestId));
        if(id!=null){
            var r=amendments.detail(actor,id);
            if(!review&&!r.getEmployee().getId().equals(actor.getId()))throw new AccessDeniedException("본인의 신청내역만 조회할 수 있습니다.");
            model.addAttribute("amendment",r);model.addAttribute("canReviewAmendment",review&&amendments.canReview(actor,r));
        }
    }
}
