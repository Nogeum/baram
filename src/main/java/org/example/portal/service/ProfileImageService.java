package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ProfileImageService {
    private final PortalService portal;
    private final EmployeeRepository employees;
    private final StoredFileRepository files;
    private final Clock clock;
    private Employee editable(String login,Long id) {
        var actor=portal.current(login);
        if(!actor.getId().equals(id)&&actor.getRole()!=Employee.Role.ADMIN)throw new BusinessException("본인 사진만 변경할 수 있습니다.");
        var employee=employees.lockById(id).orElseThrow(()->new BusinessException("직원을 찾을 수 없습니다."));
        if(employee.isDeleted())throw new BusinessException("삭제된 직원입니다.");
        return employee;
    }
    @Transactional public void upload(String login,Long id,MultipartFile upload) {
        var e=editable(login,id);
        byte[] image=normalize(upload);
        Long old=e.getProfileFileId();
        var file=new StoredFile();file.setOwnerType("PROFILE");file.setOwnerId(id);file.setUploader(portal.current(login));
        file.setFilename("profile.png");file.setData(image);file.setFileSize(image.length);file.setCreatedAt(LocalDateTime.now(clock));
        e.setProfileFileId(files.saveAndFlush(file).getId());
        if(old!=null)files.deleteById(old);
    }
    @Transactional public void remove(String login,Long id) {
        var e=editable(login,id);Long old=e.getProfileFileId();e.setProfileFileId(null);
        if(old!=null)files.deleteById(old);
    }
    public byte[] image(String login,Long id) {
        portal.current(login);
        var e=employees.findById(id).orElse(null);
        if(e==null||e.isDeleted()||e.getProfileFileId()==null)return null;
        return files.findById(e.getProfileFileId()).filter(f->"PROFILE".equals(f.getOwnerType())&&id.equals(f.getOwnerId())).map(StoredFile::getData).orElse(null);
    }
    private byte[] normalize(MultipartFile file) {
        if(file==null||file.isEmpty()||file.getSize()>5*1024*1024)throw new BusinessException("5MB 이하의 JPG 또는 PNG 사진을 선택하세요.");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext())throw new BusinessException("JPG 또는 PNG 사진만 등록할 수 있습니다.");
            var reader=readers.next();
            try {
                String format=reader.getFormatName();
                if(!format.equalsIgnoreCase("JPEG")&&!format.equalsIgnoreCase("PNG"))throw new BusinessException("JPG 또는 PNG 사진만 등록할 수 있습니다.");
                reader.setInput(input,true,true);
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1||height<1||(long)width*height>16000000)throw new BusinessException("사진은 1,600만 화소 이하로 선택하세요.");
                var original=reader.read(0);int side=Math.min(width,height);
                var result=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
                var g=result.createGraphics();
                try {g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(original,0,0,256,256,(width-side)/2,(height-side)/2,(width+side)/2,(height+side)/2,null);} finally {g.dispose();}
                var out=new ByteArrayOutputStream();ImageIO.write(result,"png",out);return out.toByteArray();
            } finally {reader.dispose();}
        } catch(IOException|IllegalArgumentException ex) {throw new BusinessException("사진을 읽을 수 없습니다. 다른 JPG 또는 PNG 파일을 선택하세요.");}
    }
}
