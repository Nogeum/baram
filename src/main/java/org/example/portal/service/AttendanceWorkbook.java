package org.example.portal.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Small, text/numeric-only OOXML workbook. User values are inline strings, never formulas. */
public final class AttendanceWorkbook {
    private AttendanceWorkbook() {}
    public static byte[] write(DepartmentReportService.Monthly report) {
        var rows=new ArrayList<List<?>>();
        rows.add(List.of("월별 근태 보고서",report.month().toString(),report.department()));
        rows.add(List.of("기준","조회 월의 오늘까지 집계 · 점심 12~13시 중 겹치는 시간 제외 · 완료 근무의 하루 8시간 초과분"));
        rows.add(List.of("안내","현재 소속 부서 기준 · 공휴일은 평일 처리 · 휴가일수는 병가/반차 포함 · 미퇴근 기록의 근무시간은 0"));
        rows.add(List.of("사번","로그인 ID","직원","상태","근무일","근무시간(분)","초과근무(분)","지각","조퇴","결근","휴가일수","미퇴근"));
        for(var row:report.rows()) {
            var e=row.employee();var t=row.totals();
            rows.add(List.of(e.getId(),e.getLoginId(),e.getDisplayName(),e.isDeleted()?"삭제":e.isActive()?"재직":"비활성",t.workDays(),t.workMinutes(),t.overtimeMinutes(),t.lateDays(),t.earlyDays(),t.absentDays(),t.leaveDays(),t.missingCheckouts()));
        }
        var sheet=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"4\" topLeftCell=\"A5\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"12\" width=\"18\" customWidth=\"1\"/></cols><sheetData>");
        for(int i=0;i<rows.size();i++) {
            sheet.append("<row r=\"").append(i+1).append("\">");
            for(int j=0;j<rows.get(i).size();j++) {
                Object value=rows.get(i).get(j);String cell=""+(char)('A'+j)+(i+1);
                if(value instanceof Number)sheet.append("<c r=\"").append(cell).append("\"><v>").append(value).append("</v></c>");
                else sheet.append("<c r=\"").append(cell).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(value.toString())).append("</t></is></c>");
            }
            sheet.append("</row>");
        }
        sheet.append("</sheetData><autoFilter ref=\"A4:L").append(rows.size()).append("\"/></worksheet>");
        try(var bytes=new ByteArrayOutputStream();var zip=new ZipOutputStream(bytes,StandardCharsets.UTF_8)) {
            entry(zip,"[Content_Types].xml","<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            entry(zip,"_rels/.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"월별 근태\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            entry(zip,"xl/worksheets/sheet1.xml",sheet.toString());zip.finish();return bytes.toByteArray();
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private static void entry(ZipOutputStream zip,String name,String value)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(value.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    private static String xml(String value){return value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]","").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
