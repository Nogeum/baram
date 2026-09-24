const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve('docs/database');
const meta=JSON.parse(fs.readFileSync(path.join(root,'schema-metadata.json'),'utf8'));
const E=s=>String(s??'').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const definitions={
 employee:['직원·계정','인증, 조직 정보, 권한 및 연차 기준을 저장합니다.',{
 login_id:'로그인 ID. 중복 불가.',password:'BCrypt로 해시한 비밀번호. 평문 비밀번호가 아닙니다.',name:'직원 이름.',department:'소속 부서명. 별도의 부서 테이블이나 부서 FK는 없습니다.',position_name:'직급명. 사원·주임·대리·부장·본부장 등 문자열로 저장합니다.',role:'계정 역할: EMPLOYEE(직원), ADMIN(웹 관리자).',active:'계정 활성 여부. 1=활성, 0=비활성.',department_manager:'소속 부서 관리 권한. 1=보유, 0=없음. 본부장 권한은 직원 역할 + 본부장 직급 + 이 권한의 조합으로 판단합니다.',email:'이메일 주소.',phone:'연락처.',extension_number:'사내 내선번호.',job_description:'담당 업무 설명.',hire_date:'입사일. 신청 가능 기간 및 근태 집계의 기준으로 사용합니다.',annual_units:'연간 연차 부여량. 1단위=0.5일, 기본 30단위=15일. 잔여 연차 자체를 저장하는 컬럼은 아닙니다.',deleted_at:'계정 논리 삭제 시각. NULL이면 삭제되지 않은 계정.',deleted_by_login:'삭제한 관리자 로그인 ID 문자열. 직원 FK가 아닙니다.'}],
 attendance:['출퇴근 기록','직원별 근무일의 실제 출퇴근 시각과 판정 결과입니다.',{employee_id:'출퇴근 기록의 직원.',work_date:'근무일. employee_id와 묶어 중복을 금지합니다.',check_in:'실제 출근 시각.',check_out:'실제 퇴근 시각. 미퇴근이면 NULL.',late_arrival:'지각 여부. 09:00 이후 출근 시 1.',early_departure:'조퇴 여부. 18:00 이전 퇴근 시 1.'}],
 schedule:['개인·부서 배정 일정','개인 일정과 부서 관리자가 배정한 일정을 함께 저장합니다.',{employee_id:'일정 대상 직원.',title:'일정 제목.',event_date:'일정 날짜.',start_time:'시작 시각. HH:mm:ss 문자열; 시간 미지정 일정은 NULL.',end_time:'종료 시각. HH:mm:ss 문자열; 시간 미지정 일정은 NULL.',assigned:'1=부서 배정 일정, 0=개인 일정. 배정자 ID를 별도로 저장하지는 않습니다.',memo:'일정 메모.',color:'표시 색상: BLUE, RED, GREEN, ORANGE, PURPLE, TEAL.'}],
 leave:['휴가 신청','휴가 기간, 차감량과 결재 상태를 저장합니다.',{employee_id:'휴가 신청 직원.',start_date:'휴가 시작일.',end_date:'휴가 종료일.',kind:'ANNUAL(연차), HALF_AM(오전 반차), HALF_PM(오후 반차), SICK(병가).',charged_units:'차감 단위. 1=0.5일, 2=1일. 병가·비근무일 등은 규칙에 따라 0. 대기·승인 건을 합산해 잔여 연차를 계산합니다.',approver_id:'지정 결재자. DB에서는 NULL 허용하지만 새 신청 시 애플리케이션에서 필수입니다.',reviewer_id:'실제로 승인·반려한 직원. 미처리이면 NULL.'}],
 overtime:['초과근무 신청','이미 종료한 초과근무의 날짜·시간과 결재를 저장합니다.',{employee_id:'초과근무 신청 직원.',approver_id:'지정 결재자. 처리 시 실제 처리자로 갱신됩니다. 별도의 reviewer_id 컬럼은 없습니다.',work_date:'초과근무한 날짜. 현재 신청 규칙상 입사일부터 오늘까지.',start_time:'초과근무 시작 시각. HH:mm:ss 문자열.',end_time:'초과근무 종료 시각. HH:mm:ss 문자열. 신청 당시 미래 시각은 허용하지 않습니다.'}],
 correction:['출퇴근 정정 신청','원래 기록의 스냅샷과 요청한 정정 시간을 함께 보관합니다.',{employee_id:'정정 대상이자 신청 직원.',approver_id:'지정 결재자.',reviewer_id:'실제 승인·반려한 직원.',work_date:'정정할 근무일.',original_version:'신청 당시 출퇴근 기록의 version. 원래 기록이 없었으면 NULL.',original_check_in:'신청 당시 기존 출근 시각. 원래 기록이 없으면 NULL.',original_check_out:'신청 당시 기존 퇴근 시각. 원래 기록 또는 퇴근 시각이 없으면 NULL.',proposed_check_in:'신청한 변경 후 출근 시각.',proposed_check_out:'신청한 변경 후 퇴근 시각.'}],
 amendment:['승인 취소·변경 요청','이미 승인된 휴가·초과근무·정정의 취소 또는 변경을 별도로 결재합니다.',{employee_id:'취소·변경 요청 직원.',approver_id:'지정 결재자.',reviewer_id:'실제 승인·반려한 직원.',request_type:'원본 종류: LEAVE, OVERTIME, CORRECTION. request_id가 가리킬 테이블을 결정합니다.',request_id:'원본 신청 ID. request_type에 따라 leave/overtime/correction 중 하나를 논리 참조합니다. 물리 FK는 없습니다.',original_version:'취소·변경 요청 당시 원본 신청 version. 승인 시 원본 변경 여부를 검증합니다.',source_attendance_version:'정정 취소·변경 요청 당시 실제 출퇴근 기록 version. 해당 유형에서만 사용합니다.',replacement_id:'변경 승인으로 생성된 새 신청 ID. 원본과 동일 유형의 테이블을 논리 참조합니다. 취소·미처리는 NULL.',action:'CANCEL(승인 취소), CHANGE(내용 변경).',original_summary:'요청 당시 원본 종류·기간·시간의 요약 스냅샷.',start_date:'변경 후 시작일 또는 근무일. 취소 요청에서는 NULL 가능.',end_date:'변경 후 종료일. 초과근무·정정의 변경은 시작일과 같은 날짜.',leave_kind:'변경 후 휴가 종류. 휴가 변경 시 사용합니다.',start_time:'변경 후 시작 시각. 초과근무·정정 변경 시 HH:mm:ss 문자열.',end_time:'변경 후 종료 시각. 초과근무·정정 변경 시 HH:mm:ss 문자열.'}],
 task:['개인 업무','본인 업무의 마감일, 진행 상태와 알림 기준을 저장합니다.',{creator_id:'업무 등록자. 현재 신규 업무는 본인이 등록합니다.',assignee_id:'업무 담당자. 현재 직급과 관계없이 본인만 지정할 수 있습니다.',title:'업무 제목.',description:'상세 업무 내용. CLOB 저장, 현재 입력 제한 4,000자.',due_date:'마감일.',status:'WAITING(대기), IN_PROGRESS(진행 중), DONE(완료).',reminder_date:'마감 알림을 마지막으로 보낸 날짜. 같은 업무에 하루 여러 번 알리는 것을 방지합니다.'}],
 task_comment:['업무 댓글','업무별 댓글과 작성자를 저장합니다.',{task_id:'댓글 대상 업무.',author_id:'댓글 작성 직원.',content:'댓글 본문. CLOB 저장, 입력 제한 2,000자.'}],
 document:['전자결재 문서','구매 요청·지출결의·출장 신청의 본문과 현재 결재 단계를 저장합니다.',{author_id:'문서 작성 직원.',kind:'PURCHASE(구매 요청), EXPENSE(지출결의), TRIP(출장 신청).',title:'문서 제목.',content:'문서 본문. CLOB 저장, 입력 제한 4,000자.',amount:'구매·지출 금액. 전체 15자리 중 소수점 2자리. 출장 문서는 NULL.',start_date:'출장 시작일. 구매·지출에는 필수가 아닙니다.',end_date:'출장 종료일. 구매·지출에는 필수가 아닙니다.',current_step:'현재 결재 순번. 1부터 시작하며 승인 시 다음 순번으로 진행합니다.'}],
 document_step:['전자결재 단계','문서별 순차 결재자와 처리 이력을 저장합니다.',{document_id:'소속 전자결재 문서.',reviewer_id:'해당 순번의 지정 결재자. 대기 중에도 값이 존재합니다.',step_order:'문서 내 결재 순번. 1~5; 중복·순서는 애플리케이션에서 관리합니다.',status:'PENDING(대기), APPROVED(승인), REJECTED(반려). 문서 취소 시 미처리 단계는 PENDING으로 남을 수 있습니다.',review_comment:'해당 단계 결재 의견. Java 필드명은 comment입니다.',reviewed_at:'해당 단계 처리 시각. 미처리이면 NULL.'}],
 resource:['회의실·장비','예약 가능한 자원의 기본 정보를 저장합니다.',{name:'자원명. 현재 대회의실·소회의실·미팅룸1·2·3이 등록되어 있습니다. DB 고유 제약은 없습니다.',kind:'ROOM(회의실), EQUIPMENT(장비).',location:'위치 설명. 현재 예약 선택 목록은 이 값 대신 자원명만 표시합니다.',capacity:'수용 인원 또는 자원 수량 정보. 기본 1. 이 값만큼 동시 예약을 허용하는 구조는 아닙니다.',active:'사용 가능 여부. 1=사용, 0=사용 중지.'}],
 reservation:['자원 예약·허가','자원 예약 신청과 관리자 허가·반려 결과입니다.',{resource_id:'예약 대상 회의실·장비.',employee_id:'예약 신청 직원.',title:'예약 용도.',starts_at:'예약 시작 시각.',ends_at:'예약 종료 시각.',status:'PENDING(승인 대기), ACTIVE(예약 확정), REJECTED(반려), CANCELLED(취소). 기존 즉시 확정 예약은 ACTIVE를 유지합니다.',reviewer_id:'허가·반려한 관리자.',reviewed_at:'허가·반려 처리 시각. 현재 운영 DB는 TIMESTAMP(6)입니다.',review_comment:'허가·반려 의견. 반려 시 필수입니다.'}],
 library:['자료실','공유 문서의 본문과 공개 범위를 저장합니다.',{author_id:'자료 등록 직원.',title:'자료 제목.',content:'자료 본문. CLOB 저장, 입력 제한 4,000자.',visibility:'COMPANY(모든 부서), DEPARTMENT(등록 부서).',department:'등록 당시 부서명 스냅샷. DEPARTMENT 공개 범위를 판단합니다. 직원 부서와 자동 동기화하는 FK는 없습니다.',updated_at:'마지막 수정 시각.'}],
 file:['첨부파일','업무·전자결재·자료실 첨부파일을 하나의 테이블에 저장합니다.',{owner_type:'첨부 대상 종류: TASK, DOCUMENT, LIBRARY.',owner_id:'첨부 대상 ID. owner_type에 따라 task/document/library 중 하나를 논리 참조합니다. 물리 FK는 없습니다.',uploader_id:'파일 업로드 직원.',filename:'원본 파일명. 서버에서 경로·제어문자를 제거합니다.',file_size:'파일 크기(바이트). 애플리케이션 제한은 파일당 10MB입니다.',data:'파일 바이트 원본. Oracle BLOB에 저장합니다.'}],
 holiday:['공휴일·회사 휴무일','비근무일로 처리할 날짜를 저장합니다.',{holiday_date:'휴무 날짜. 하루당 하나만 등록 가능하도록 고유 제약이 있습니다.',name:'휴무 명칭.',kind:'PUBLIC(공휴일), COMPANY(회사 휴무일).',author_id:'휴무일을 등록한 관리자.'}],
 notice:['사내 공지','공지 제목·본문과 등록자를 저장합니다.',{author_id:'공지 작성 관리자.',title:'공지 제목.',content:'공지 본문. CLOB 저장, 현재 입력 제한 2,000자.'}],
 notification:['개인 알림','수신자별 알림과 읽음 여부, 이동 경로를 저장합니다.',{recipient_id:'알림 수신 직원 또는 관리자.',message:'알림 메시지.',read_flag:'읽음 여부. 1=읽음, 0=읽지 않음.',target_path:'알림 클릭 시 이동할 내부 경로. NULL인 기존 알림은 메시지 규칙으로 이동 경로를 계산합니다.'}]
};

definitions.employee[2].profile_file_id='현재 프로필 사진의 PORTAL_FILE.ID. NULL이면 이름 첫 글자를 표시합니다. 물리 FK 없이 서비스 트랜잭션에서 연결·교체·삭제를 관리합니다.';
definitions.chat_message=['직원 간 채팅','직원·관리자의 1:1 대화와 읽음 상태를 저장합니다. 로그인한 본인이 참여한 대화만 조회합니다.',{sender_id:'메시지 발신 직원. client_id와 복합 고유키를 구성합니다.',recipient_id:'메시지 수신 직원. 발신자와 동일한 직원은 CHECK 제약으로 금지합니다.',content:'메시지 본문. CLOB 저장, 공백 제거 후 1~2,000자.',client_id:'클라이언트가 생성한 UUID(36자). 같은 발신자의 전송 재시도로 중복 메시지가 생성되는 것을 방지합니다.',sent_at:'메시지 전송 시각. 애플리케이션의 한국 시간 기준.',read_at:'수신자가 화면에서 메시지를 확인한 시각. NULL이면 읽지 않음. 단순 대화 조회만으로는 갱신하지 않습니다.'}];
definitions.file[1]='업무·전자결재·자료실 첨부파일과 프로필 사진을 Oracle BLOB에 저장합니다.';
definitions.file[2].owner_type='대상 종류: TASK, DOCUMENT, LIBRARY, PROFILE(직원 프로필 사진).';
definitions.file[2].owner_id='owner_type에 따라 업무·결재·자료실·직원 ID를 논리 참조합니다. 물리 FK는 없습니다.';
definitions.file[2].filename='일반 첨부는 경로·제어문자를 제거한 원본 이름. 프로필 사진은 profile.png로 저장합니다.';
definitions.file[2].file_size='저장된 파일 크기(바이트). 일반 첨부 최대 10MB. 프로필 업로드는 JPG·PNG 최대 5MB, 1,600만 화소 이하이며 변환 후 크기를 저장합니다.';
definitions.file[2].data='파일 바이트 BLOB. 일반 첨부는 원본, 프로필은 중앙 정사각형으로 자른 256×256 PNG입니다. 프로필 원본·메타데이터는 보관하지 않습니다.';

const common={id:'기본키(PK). 모든 엔티티가 공유하는 PORTAL_SEQ 시퀀스로 생성합니다.',version:'JPA 낙관적 잠금 버전. 동시 변경 시 오래된 데이터로 덮어쓰는 것을 방지합니다.',created_at:'레코드 생성 시각. 애플리케이션의 한국 시간(KST) 기준.',requested_at:'신청 접수 시각.',reviewed_at:'승인·반려 처리 시각. 미처리이면 NULL.',reason:'신청 또는 취소·변경 사유.',review_comment:'결재 처리 의견. 반려할 때 애플리케이션에서 입력을 요구합니다.',status:'PENDING(승인 대기), APPROVED(승인), REJECTED(반려), CANCELLED(취소).'};
const order=['employee','attendance','schedule','leave','overtime','correction','amendment','task','task_comment','document','document_step','resource','reservation','library','file','holiday','notice','notification','chat_message'];
const tables=order.map(n=>'PORTAL_'+n.toUpperCase());
const cols=t=>meta.columns.filter(c=>c.table_name===t);
const keys=(t,c)=>meta.keys.filter(k=>k.table_name===t&&k.column_name===c);
const fks=meta.keys.filter(k=>k.constraint_type==='R');
const def=t=>definitions[t.toLowerCase().replace('portal_','')];
const desc=c=>def(c.table_name)[2][c.column_name.toLowerCase()]||common[c.column_name.toLowerCase()];
const type=c=>c.data_type==='NUMBER'?`NUMBER(${c.data_precision},${c.data_scale})`:['VARCHAR2','CHAR'].includes(c.data_type)?`${c.data_type}(${c.char_used==='C'?c.char_length:c.data_length} ${c.char_used==='C'?'CHAR':'BYTE'})`:c.data_type;
const keylabel=c=>keys(c.table_name,c.column_name).map(k=>({P:'PK',U:'UK',R:'FK → '+k.target_table+'.'+k.target_column}[k.constraint_type])).join('; ')||'—';
for(const c of meta.columns)if(!desc(c))throw Error('Missing description '+c.table_name+'.'+c.column_name);
if(tables.length!==new Set(meta.columns.map(c=>c.table_name)).size)throw Error('Table coverage');
const intro=[
'기준일: 2026-09-24 · 실행 중인 Oracle 11.2의 USER_* 메타데이터와 현재 엔티티·서비스 코드 기준입니다.',
`총 ${tables.length}개 테이블, ${meta.columns.length}개 컬럼, ${fks.length}개 물리 외래키, 공통 시퀀스 PORTAL_SEQ 1개입니다. 업무 데이터·계정 비밀번호는 문서에 포함하지 않았습니다.`,
'PK=기본키, FK=외래키, UK=고유 제약. NULL 허용과 DB 기본값은 실제 DB 기준이며, 업무상 필수 조건은 애플리케이션에서 더 엄격할 수 있습니다.',
'모든 테이블에는 id와 version이 있습니다. Oracle 11g의 Boolean은 NUMBER의 0/1, LocalTime은 HH:mm:ss 문자열로 저장합니다. TIMESTAMP는 시간대 자체를 저장하지 않고 애플리케이션이 한국 시간으로 해석합니다.',
'DB 기본값과 Java 초기값은 다릅니다. 예: 새 예약 PENDING, 업무 WAITING, 계정 active=true, annual_units=30 등은 주로 Java/서비스에서 설정합니다.',
'부서·직급·권한을 위한 별도 테이블은 없습니다. employee.department, position_name, role, department_manager 조합으로 처리합니다. 본부장도 별도 ROLE이 아닙니다.',
'운영 DB의 reservation.reviewed_at은 TIMESTAMP(6), 생성용 schema.sql은 TIMESTAMP(9)입니다. 이 설명서는 실제 운영 DB 타입을 우선합니다. 시간 컬럼의 VARCHAR2(255) 역시 실제 DB 길이이며 실제 값 형식은 HH:mm:ss입니다.'
];
const logical=[
['PORTAL_EMPLOYEE','PROFILE_FILE_ID','PORTAL_FILE.ID (OWNER_TYPE=PROFILE)','현재 프로필 파일 0..1. 물리 FK 없음. 사진 교체·삭제 시 이전 파일도 삭제합니다.'],
['PORTAL_FILE','OWNER_TYPE + OWNER_ID','TASK → PORTAL_TASK.ID / DOCUMENT → PORTAL_DOCUMENT.ID / LIBRARY → PORTAL_LIBRARY.ID / PROFILE → PORTAL_EMPLOYEE.ID','대상 하나에 첨부파일 0..N; 각 파일은 종류에 맞는 대상 1개. 물리 FK 없음.'],
['PORTAL_AMENDMENT','REQUEST_TYPE + REQUEST_ID','LEAVE → PORTAL_LEAVE.ID / OVERTIME → PORTAL_OVERTIME.ID / CORRECTION → PORTAL_CORRECTION.ID','원본 신청 하나에 요청 이력 0..N. 같은 원본의 대기 요청 중복은 코드에서 검사.'],
['PORTAL_AMENDMENT','REQUEST_TYPE + REPLACEMENT_ID','원본과 동일 종류의 새 신청 ID','변경 승인으로 생성된 신청을 연결. 취소·미처리에는 연결 없음.'],
['PORTAL_CORRECTION','EMPLOYEE_ID + WORK_DATE','PORTAL_ATTENDANCE의 동일 직원·근무일','대상 출퇴근 기록 0..1. 누락 기록 정정이 가능하므로 물리 FK 없음.'],
['PORTAL_EMPLOYEE','DEPARTMENT / POSITION_NAME','별도 마스터 테이블 없음','문자열 조직 정보이며 부서·직급 FK가 아닙니다.']
];
let md='# 포털 데이터베이스 ERD·컬럼 설명서\n\n'+intro.map(s=>'- '+s).join('\n')+'\n\n## ERD\n\n![전체 ERD](portal-erd.png)\n\n[확대용 SVG](portal-erd.svg) · [근태 상세](erd-attendance.png) · [업무·결재 상세](erd-collaboration.png) · [계정·공지 상세](erd-people.png) · [예약 상세](erd-resources.png)\n\n## 테이블 목록\n\n|테이블|설명|컬럼 수|\n|---|---|---:|\n';
let html=`<!doctype html><html lang="ko"><meta charset="utf-8"><title>포털 ERD·컬럼 설명서</title><style>body{font-family:'Malgun Gothic',sans-serif;color:#26334a;margin:40px;line-height:1.65}h1{font-size:30px}h2{margin-top:38px;color:#213d77}h3{margin-top:26px}table{border-collapse:collapse;width:100%;font-size:12px;margin:18px 0}th,td{border:1px solid #d9e0eb;padding:8px;text-align:left;vertical-align:top;overflow-wrap:anywhere}th{background:#edf2f9}code{font-size:11px}img{width:100%}.note{background:#f0f5fb;padding:18px;border-radius:9px}.controls{display:flex;gap:15px;position:sticky;top:0;background:white;padding:12px;border-bottom:1px solid #d9e0eb}input{padding:8px;width:320px}a{color:#2457aa}.table-section{break-before:page}tr{break-inside:avoid}thead{display:table-header-group}@media print{body{margin:0;font-size:11px}.controls{display:none}h2{margin-top:0}table{font-size:9px}td,th{padding:5px}a{color:inherit;text-decoration:none}.overview{break-before:page;break-after:page}.overview img{height:145mm;width:auto;max-width:100%;object-fit:contain;display:block;margin:auto}}</style><body><div class="controls"><input id="search" placeholder="테이블·컬럼·설명 검색"><button onclick="window.print()">인쇄 / PDF</button><a href="portal-erd.svg">ERD 확대</a></div><h1>포털 ERD · 컬럼 설명서</h1><div class="note">${intro.map(s=>'<p>'+E(s)+'</p>').join('')}</div><div class="overview"><h2>전체 관계도</h2><img src="portal-erd.svg"></div><h2>테이블 목록</h2><table><tr><th>테이블</th><th>설명</th><th>컬럼 수</th></tr>`;
for(const t of tables){md+=`|${t}|${def(t)[0]}|${cols(t).length}|\n`;html+=`<tr><td><a href="#${t}">${t}</a></td><td>${def(t)[0]}</td><td>${cols(t).length}</td></tr>`;}
html+='</table>';
const csv=[['테이블','테이블 설명','컬럼','Oracle 타입','NULL 허용','키 관계','DB 기본값','컬럼 설명']];
for(const t of tables){
 md+=`\n## ${t} — ${def(t)[0]}\n\n${def(t)[1]}\n\n|컬럼|Oracle 타입|NULL 허용|키·참조|DB 기본값|설명|\n|---|---|---|---|---|---|\n`;
 html+=`<section class="table-section" id="${t}"><h2>${t} · ${def(t)[0]}</h2><p>${def(t)[1]}</p><table><thead><tr>${['컬럼','Oracle 타입','NULL 허용','키·참조','DB 기본값','설명'].map(x=>'<th>'+x+'</th>').join('')}</tr></thead><tbody>`;
 for(const c of cols(t)){
  const row=[c.column_name,type(c),c.nullable==='Y'?'예':'아니오',keylabel(c),c.data_default?.trim()||'없음',desc(c)];
  md+='|'+row.map(x=>x.replaceAll('|','/').replaceAll('\n',' ')).join('|')+'|\n';html+='<tr>'+row.map((x,i)=>`<td${i===5?' style="width:40%"':''}>${E(x)}</td>`).join('')+'</tr>';csv.push([t,def(t)[0],...row]);
 }
 html+='</tbody></table>';
 const unique=meta.constraints.filter(c=>c.table_name===t&&['U','P'].includes(c.constraint_type)).map(c=>`${c.constraint_type==='P'?'PK':'UK'} ${meta.keys.filter(k=>k.constraint_name===c.constraint_name).map(k=>k.column_name).join(' + ')}`);
 const checks=meta.constraints.filter(c=>c.table_name===t&&c.constraint_type==='C'&&!/IS NOT NULL/i.test(c.search_condition||'')).map(c=>c.search_condition);
 md+='\n제약: '+unique.join('; ')+(checks.length?'; CHECK: '+checks.join('; '):'')+'\n';html+='<p><b>제약:</b> '+E(unique.join('; ')+(checks.length?'; CHECK: '+checks.join('; '):''))+'</p></section>';
}
md+='\n## 논리 관계 — 물리 FK 없음\n\n|테이블|연결 컬럼|대상|설명|\n|---|---|---|---|\n'+logical.map(r=>'|'+r.join('|')+'|').join('\n')+'\n';
html+='<section class="table-section"><h2>논리 관계 — 물리 FK 없음</h2><table>'+logical.map(r=>'<tr>'+r.map(c=>'<td>'+E(c)+'</td>').join('')+'</tr>').join('')+'</table>';
md+='\n## 물리 외래키 전체 목록\n\n부모 1개에는 자식 0..N개가 연결됩니다. 자식 FK가 NULL 가능하면 부모 연결은 0..1, NOT NULL이면 정확히 1개입니다. 모두 비식별 관계(자식 고유 ID 사용)입니다.\n\n|자식 테이블·컬럼|부모 테이블·컬럼|자식의 부모 연결|\n|---|---|---|\n';
html+='<h2>물리 외래키 전체 목록</h2><p>부모 1개 : 자식 0..N개. 자식 FK의 NULL 여부에 따라 부모 연결은 0..1 또는 1입니다.</p><table><tr><th>자식 컬럼</th><th>부모 컬럼</th><th>부모 연결</th></tr>';
for(const k of fks){const r=[k.table_name+'.'+k.column_name,k.target_table+'.'+k.target_column,cols(k.table_name).find(c=>c.column_name===k.column_name).nullable==='Y'?'0..1':'1'];md+='|'+r.join('|')+'|\n';html+='<tr>'+r.map(v=>'<td>'+E(v)+'</td>').join('')+'</tr>';}
html+='</table></section>';
const extras=['예약 중복은 DB UNIQUE가 아니라 자원 행 잠금과 PENDING/ACTIVE 시간대 겹침 검사로 방지합니다.','문서 단계(document_id, step_order)는 현재 DB 복합 UNIQUE가 없으며 생성 코드가 순서를 관리합니다.','잔여 연차·초과근무 분·근무 상태·부서별 통계는 계산값이며 전용 컬럼이 아닙니다.','현재 모든 FK는 NO ACTION 삭제 규칙입니다. 계정 삭제는 업무 기록을 보존하는 논리 삭제로 처리합니다.','휴가의 approver_id는 DB에서 NULL 허용합니다. 반면 신규 신청 화면과 서비스에서는 결재자 선택을 요구합니다.','초과근무는 approver_id에 실제 처리자를 덮어쓰므로 다른 신청처럼 지정자와 실제 처리자를 따로 보존하지 않습니다.','프로필 사진은 로그인 사용자만 조회할 수 있고, 본인 또는 관리자만 변경할 수 있습니다. 채팅 기록은 관리자도 본인이 참여한 대화만 열람할 수 있습니다.'];
md+='\n## 구현 시 참고\n\n'+extras.map(s=>'- '+s).join('\n')+'\n\n## 출처\n\n- 운영 Oracle USER_TAB_COLUMNS, USER_CONSTRAINTS, USER_CONS_COLUMNS, USER_INDEXES, USER_IND_COLUMNS, USER_SEQUENCES (schema-metadata.json)\n- src/main/java/org/example/portal/domain/*.java\n- src/main/java/org/example/portal/service/*.java\n- src/main/resources/db/oracle/schema.sql 및 migrations/*.sql\n';
html+='<section class="table-section"><h2>구현 시 참고</h2><ul>'+extras.map(s=>'<li>'+E(s)+'</li>').join('')+'</ul><p>출처: 운영 Oracle USER_* 메타데이터, 현재 domain/service Java 코드, schema.sql 및 migrations SQL. 스냅샷은 schema-metadata.json에 보관합니다.</p></section><script>document.querySelector("#search").addEventListener("input",e=>{const q=e.target.value.toLowerCase();document.querySelectorAll(".table-section").forEach(s=>s.hidden=q&&!s.textContent.toLowerCase().includes(q));});</script></body></html>';
html=html.replaceAll('portal-erd.svg','portal-erd-connected-ko.svg');
md=md.replace('portal-erd.png','portal-erd-connected-ko.png').replace('portal-erd.svg','portal-erd-connected-ko.svg');
fs.writeFileSync(path.join(root,'column-dictionary.md'),md);fs.writeFileSync(path.join(root,'column-dictionary.html'),html);fs.writeFileSync(path.join(root,'column-dictionary.csv'),'\ufeff'+csv.map(r=>r.map(v=>'"'+String(v).replaceAll('"','""')+'"').join(',')).join('\r\n'));
if(process.argv.includes('--dictionary-only')){console.log('Dictionary generated: '+tables.length+' tables / '+meta.columns.length+' columns.');return;}
// Reproducible Mermaid source contains every column and each physical foreign key.
let mermaid='erDiagram\n';
for(const t of tables){mermaid+=`  ${t} {\n`;for(const c of cols(t)){const markers=keys(t,c.column_name).map(k=>({P:'PK',U:'UK',R:'FK'}[k.constraint_type]));mermaid+=`    ${type(c).replaceAll(' ','_').replaceAll(',','_')} ${c.column_name}${markers.length?' '+markers.join(','):''}\n`;}mermaid+='  }\n';}
for(const k of fks)mermaid+=`  ${k.target_table} ${cols(k.table_name).find(c=>c.column_name===k.column_name).nullable==='Y'?'|o':'||'}..o{ ${k.table_name} : "${k.column_name}"\n`;
fs.writeFileSync(path.join(root,'portal-erd.mmd'),mermaid);
// SVG diagrams: employee relationships enter left ports; non-employee FKs use right ports.
const groups=[{id:'people',title:'계정 · 공지 · 휴무일',color:'#2854a2',tables:['employee','notice','notification','holiday']},{id:'attendance',title:'근태 · 일정 · 승인 변경',color:'#087e8b',tables:['attendance','schedule','leave','overtime','correction','amendment']},{id:'collaboration',title:'업무 · 전자결재 · 자료실',color:'#7450a6',tables:['task','task_comment','document','document_step','library','file']},{id:'resources',title:'자원 · 예약 허가',color:'#bc7015',tables:['resource','reservation']}];
function svgDiagram(selected,detail){
 const full=detail&&selected.length>1;
 const w=detail?selected.length*1000+80:2400, lane=detail?1000:580, cardW=detail?810:462, font=detail?16:15,rowH=detail?27:25;
 const cards=[],laneEnds=[], employeeReferences=[];
 for(let gi=0;gi<selected.length;gi++){
  const g=selected[gi],x=40+gi*lane,y0=detail?150:190;let y=y0+94;
  employeeReferences.push({x:x+60,y:y0,g});
  for(const n of g.tables){const t='PORTAL_'+n.toUpperCase();let fields=cols(t);
   if(!detail){const related=meta.keys.filter(k=>k.table_name===t).map(k=>k.column_name);fields=fields.filter(c=>related.includes(c.column_name)||['STATUS','KIND','OWNER_ID','OWNER_TYPE','REQUEST_ID','REQUEST_TYPE','REPLACEMENT_ID'].includes(c.column_name));}
   fields=[...fields].sort((a,b)=>(a.column_name==='ID'?-1:b.column_name==='ID'?1:0));
   const h=65+fields.length*rowH+12;cards.push({t,x:x+60,y,w:cardW,h,fields,g,gi});y+=h+38;
  }
  laneEnds.push(y);
 }
 const bottom=Math.max(...laneEnds)+20,height=bottom+(detail?215:265);let out=`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${height}" viewBox="0 0 ${w} ${height}"><defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0 0 L8 4 L0 8" fill="none" stroke="#63758b"/></marker></defs><rect width="100%" height="100%" fill="#f5f7fb"/><style>text{font-family:'Malgun Gothic',sans-serif}.mono{font-family:Consolas,monospace}</style>`;
 const text=(x,y,s,size=font,color='#26364e',bold=false,cls='')=>`<text x="${x}" y="${y}" font-size="${size}" fill="${color}"${bold?' font-weight="700"':''}${cls?' class="'+cls+'"':''}>${E(s)}</text>`;
 out+=text(40,54,full?'PORTAL · 전체 컬럼 ERD':detail?selected[0].title+' — 상세 ERD':'PORTAL · 현재 데이터베이스 ERD',detail?30:38,'#183353',true);
 out+=text(40,90,`2026-09-24 · Oracle 11.2 실측 · ${detail?'전체 컬럼 표시':'18 tables / 193 columns / 28 foreign keys'}`,detail?16:20,'#617189');
 out+=text(40,122,'실선 = 물리 FK · 점선 = 논리 연결 · N = 0..N · ? = FK NULL 허용 · UK* = 복합 고유키 일부',detail?14:18,'#617189');
 if(!detail)out+=text(40,153,'직원 참조는 모두 같은 PORTAL_EMPLOYEE입니다. 요약도에는 키·연결·상태 컬럼을 표시하며 전체 컬럼은 상세도/설명서를 참조하세요.',18,'#617189');
 // Physical FK lines and logical links behind cards.
 for(const ref of employeeReferences){const railX=ref.x-30;const members=cards.filter(c=>c.g===ref.g&&c.t!=='PORTAL_EMPLOYEE');
  const end=Math.max(ref.y+55,...members.map(c=>c.y+c.h-25));out+=`<path d="M${ref.x} ${ref.y+26}H${railX}V${end}" fill="none" stroke="${ref.g.color}" stroke-width="2" opacity=".6"/>`;
 }
 for(const k of fks){const child=cards.find(c=>c.t===k.table_name);if(!child)continue;const index=child.fields.findIndex(c=>c.column_name===k.column_name);if(index<0)continue;const yy=child.y+65+index*rowH-6;
  if(k.target_table==='PORTAL_EMPLOYEE'){out+=`<path d="M${child.x-30} ${yy}H${child.x}" fill="none" stroke="${child.g.color}" stroke-width="1.5" marker-end="url(#arrow)"/>`;}
  else{const parent=cards.find(c=>c.t===k.target_table);if(parent){const xx=child.x+child.w+22;out+=`<path d="M${parent.x+parent.w} ${parent.y+24}H${xx}V${yy}H${child.x+child.w}" fill="none" stroke="#63758b" stroke-width="1.8" marker-end="url(#arrow)"/>`;out+=text(xx+3,(parent.y+24+yy)/2,'1:N',11,'#617189');}}
 }
 // Logical references have no database foreign key.
 for(const [source,column,targets]of [['PORTAL_FILE','OWNER_ID',['PORTAL_TASK','PORTAL_DOCUMENT','PORTAL_LIBRARY']],['PORTAL_AMENDMENT','REQUEST_ID',['PORTAL_LEAVE','PORTAL_OVERTIME','PORTAL_CORRECTION']]]){
  const from=cards.find(c=>c.t===source);if(!from)continue;
  const yy=from.y+65+from.fields.findIndex(c=>c.column_name===column)*rowH-6;
  targets.forEach((target,i)=>{const dest=cards.find(c=>c.t===target);if(!dest)return;const xx=from.x+from.w+36+i*7;out+=`<path d="M${from.x+from.w} ${yy}H${xx}V${dest.y+43}H${dest.x+dest.w}" fill="none" stroke="#b98449" stroke-dasharray="5 5" stroke-width="1.2"/>`;});
 }
 for(const ref of employeeReferences){out+=`<rect x="${ref.x}" y="${ref.y}" width="${cardW}" height="58" rx="9" fill="${ref.g.color}"/>`+text(ref.x+14,ref.y+24,ref.g.title,18,'white',true)+text(ref.x+14,ref.y+46,'PORTAL_EMPLOYEE.ID  ·  1 / 0..1 → N',14,'#edf4ff');}
 for(const c of cards){out+=`<rect x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="white" stroke="#d0d9e8"/><path d="M${c.x+8} ${c.y}H${c.x+c.w-8}" stroke="${c.g.color}" stroke-width="4"/>`+text(c.x+14,c.y+24,c.t,detail?21:19,c.g.color,true,'mono')+text(c.x+14,c.y+46,def(c.t)[0],15,'#64738a');
  c.fields.forEach((field,i)=>{const yy=c.y+65+i*rowH;const kk=keys(c.t,field.column_name);const tags=kk.map(k=>k.constraint_type==='P'?'PK':k.constraint_type==='U'?(meta.keys.filter(x=>x.constraint_name===k.constraint_name).length>1?'UK*':'UK'):'FK'+(field.nullable==='Y'?'?':'')).join('/');out+=text(c.x+13,yy,tags,12,tags.includes('PK')?'#bb7114':c.g.color,true);out+=text(c.x+73,yy,field.column_name,detail?16:14,'#26364e',false,'mono');if(detail)out+=text(c.x+425,yy,type(field)+(field.nullable==='N'?'  NOT NULL':''),14,'#64738a',false,'mono');});
 }
 out+=`<rect x="40" y="${bottom}" width="${w-80}" height="${height-bottom-25}" rx="12" fill="#eaf0f8"/>`+text(60,bottom+32,'논리 연결 및 읽는 방법',20,'#183353',true);
 const notes=detail?['FK 행의 왼쪽 선은 직원(ID), 오른쪽 실선은 문서·업무·자원(ID) 참조입니다.','OWNER_TYPE + OWNER_ID → TASK / DOCUMENT / LIBRARY 중 하나 (점선).','REQUEST_TYPE + REQUEST_ID / REPLACEMENT_ID → LEAVE / OVERTIME / CORRECTION 중 하나.','정정 ↔ 출퇴근: 직원 + 근무일로 조회하는 논리 연결이며 별도 FK가 없습니다.','DB 컬럼 설명·NULL·기본값·고유 제약은 column-dictionary.html / .md에서 확인하세요.']:['직원(ID) → 각 테이블의 직원·작성자·담당자·결재자·수신자 FK. 직원 참조 노드는 같은 테이블을 반복 표시한 것입니다.','TASK → TASK_COMMENT, DOCUMENT → DOCUMENT_STEP, RESOURCE → RESERVATION은 실제 1:N FK입니다.','FILE.OWNER_TYPE + OWNER_ID → TASK / DOCUMENT / LIBRARY 중 하나. 점선은 물리 FK 없는 다형성 연결입니다.','AMENDMENT.REQUEST_TYPE + REQUEST_ID / REPLACEMENT_ID → LEAVE / OVERTIME / CORRECTION 중 하나.','CORRECTION은 EMPLOYEE_ID + WORK_DATE로 ATTENDANCE를 논리 조회합니다(누락 기록 허용).','계정·공지 / 근태 / 업무·결재 / 예약 상세 PNG·SVG에 모든 컬럼이 표시되어 있습니다.'];
 notes.forEach((n,i)=>out+=text(60,bottom+64+i*28,n,detail?14:17,'#4d6079'));
 return out+'</svg>';
}
fs.writeFileSync(path.join(root,'portal-erd.svg'),svgDiagram(groups,false));
const completeSvg=svgDiagram(groups,true);
for(const c of meta.columns){
 const start=completeSvg.indexOf('>'+c.table_name+'</text>');
 const next=completeSvg.indexOf('class="mono">PORTAL_',start+1);
 if(start<0||!completeSvg.slice(start,next<0?undefined:next).includes('>'+c.column_name+'</text>'))throw Error('Missing ERD column '+c.table_name+'.'+c.column_name);
}
fs.writeFileSync(path.join(root,'portal-erd-all-columns.svg'),completeSvg);
for(const g of groups)fs.writeFileSync(path.join(root,'erd-'+g.id+'.svg'),svgDiagram([g],true));
fs.writeFileSync(path.join(root,'README.md'),'# 포털 DB 문서\n\n- [전체 ERD PNG](portal-erd.png) / [SVG](portal-erd.svg)\n- [컬럼 설명서 HTML](column-dictionary.html) / [PDF](column-dictionary.pdf) / [Markdown](column-dictionary.md) / [CSV](column-dictionary.csv)\n- 상세 이미지: erd-people, erd-attendance, erd-collaboration, erd-resources (PNG/SVG)\n- 재생성: ExportSchemaMetadata.java → node scripts/build-database-docs.cjs → node scripts/render-database-docs.cjs\n- schema-metadata.json은 실제 Oracle 메타데이터만 포함하며 업무 데이터와 인증정보는 포함하지 않습니다.\n');
console.log(`Generated dictionary: ${tables.length} tables, ${meta.columns.length} documented columns, ${fks.length} physical FKs; 5 SVG diagrams.`);
