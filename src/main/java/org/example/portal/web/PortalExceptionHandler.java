package org.example.portal.web;
import org.example.portal.service.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
@ControllerAdvice
public class PortalExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    String oversizedUpload(Exception e,Model m) { m.addAttribute("message","파일당 10MB 이하의 첨부파일을 선택해주세요."); return "error"; }
    @ExceptionHandler(BusinessException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    String business(BusinessException e,Model m) { m.addAttribute("message",e.getMessage()); return "error"; }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingServletRequestParameterException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalid(Exception e,Model m) { m.addAttribute("message","입력값과 날짜 형식을 확인하세요."); return "error"; }
    @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class}) @ResponseStatus(HttpStatus.CONFLICT)
    String conflict(Exception e,Model m) { m.addAttribute("message","중복되거나 다른 요청에서 변경된 데이터입니다. 새로고침 후 다시 확인하세요."); return "error"; }
}
