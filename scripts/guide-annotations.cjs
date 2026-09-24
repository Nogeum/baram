const path=require('path');
const roles={
PortalApplication:'프로그램의 출발점입니다. main에서 Spring Boot를 켜고 하위 패키지 부품을 찾습니다.',
SecurityConfig:'로그인·비밀번호 해시·주소별 권한·세션·공통 시계를 설정하는 문지기입니다.',
BootstrapData:'설정이 켜져 있고 DB가 비어 있을 때만 초기 직원과 공지를 만드는 시작 도우미입니다.',
TaskReminders:'정해진 주기에 업무 마감 알림 서비스를 호출합니다. 일반 요청이 없어도 예약된 작업이 실행될 수 있습니다.',
BaseEntity:'각 엔티티에 공통 ID와 수정 버전을 물려줍니다. 자기 테이블을 따로 만들지는 않습니다.',
BooleanNumberConverter:'Java의 true/false와 Oracle에서 사용하는 숫자 표현 사이를 변환합니다.',
LocalTimeTextConverter:'Java 시각과 DB의 시간 문자열을 서로 바꿉니다. 날짜·시간대와는 별개인 시각을 다룹니다.',
PortalService:'직원 계정·출퇴근·휴가·일정·공지·알림의 기본 업무 규칙을 담당합니다. 현재 사용자 확인과 연차 계산도 여기에 있습니다.',
DepartmentAccess:'부서 관리 권한, 소속 부서, 자기 결재 금지, 지정 결재자와 본부장 조건을 한곳에서 검사합니다.',
WorkCalendarService:'토·일요일과 등록 휴무일로 근무일을 계산하고 휴무일 변경 시 휴가 차감량을 다시 계산합니다.',
OvertimeService:'이미 종료된 초과근무의 신청·취소·승인을 처리하고 날짜·시간 겹침을 검사합니다.',
AttendanceCorrectionService:'출퇴근 원본의 스냅샷을 저장한 뒤 승인 때 원본이 바뀌지 않았는지 확인하고 시간을 수정합니다.',
AmendmentService:'이미 승인된 신청을 별도 취소·변경 요청으로 처리합니다. 원본과 새 신청을 연결해 이력을 보존합니다.',
AttendanceReportService:'출퇴근·휴가·근무일에서 근무 분, 초과 분, 지각·조퇴·결근 등 화면 통계를 계산합니다.',
DepartmentReportService:'접근 가능한 부서의 직원별 통계와 휴가·업무 달력 데이터를 만듭니다.',
AttendanceWorkbook:'월별 보고서를 XML과 ZIP으로 묶어 XLSX 파일로 만듭니다. 사용자 글자를 엑셀 수식으로 해석시키지 않습니다.',
GroupwareService:'개인 업무·댓글·전자결재·조직도·예약·자료실·첨부를 처리합니다. 공유 여부와 대상별 읽기·쓰기 권한을 함께 검사합니다.',
ChatService:'로그인한 참여자의 1:1 대화, 최근 목록, 직원 검색, 중복 전송 방지와 읽음 처리를 담당합니다.',
ProfileImageService:'사진 변경 권한을 확인하고 실제 JPG·PNG를 검사·축소한 뒤 파일과 직원의 연결을 갱신합니다.',
BusinessException:'업무 규칙에 맞지 않는 요청을 사용자 안내와 함께 중단할 때 사용하는 예외입니다.',
PortalController:'직원·관리자 화면 요청을 받아 필요한 모델을 만들고 기본 폼 처리를 서비스에 전달합니다.',
DepartmentController:'부서 관리자 화면·승인·스케줄 배정·보고서 다운로드 요청을 접수합니다.',
GroupwareController:'업무·전자결재·조직도·예약·자료실·파일 관련 주소를 서비스와 화면에 연결합니다.',
ChatController:'채팅의 GET·POST API를 접수하고 JSON으로 필요한 데이터만 반환합니다.',
ProfileImageController:'사진 조회는 PNG 응답으로, 업로드·삭제는 처리 뒤 페이지 이동으로 돌려줍니다.',
PortalExceptionHandler:'여러 화면에서 생기는 입력·충돌·용량 오류를 HTTP 상태와 안내 화면으로 바꿉니다.',
NotificationLinks:'알림이 눌렸을 때 이동할 내부 업무 주소를 선택합니다. 기존 알림의 목적지도 보완합니다.',
CalendarDay:'달력의 날짜 한 칸과 해당 날짜의 일정 목록을 묶는 화면용 데이터입니다.',
AmendmentView:'신청내역과 승인 대기함에 취소·변경 요청을 끼워 넣을 모델을 준비합니다.',
'portal.html':'상단바·좌측 메뉴·각 기능 화면 조각을 조립하는 로그인 후 공통 페이지입니다.',
'login.html':'로그인 ID와 비밀번호 입력 폼입니다. 폼 인증은 Spring Security가 처리합니다.',
'error.html':'실패한 요청에 대해 사용자가 읽을 안내를 표시하는 화면입니다.',
'chat.js':'채팅 화면의 상태·서버 요청·폴링·스크롤·읽음·작성 중 글·재시도를 관리합니다.',
'profile.js':'사진 로딩 실패 시 이미지 요소를 제거해 이름 첫 글자 대체 표시가 보이게 합니다.',
'employees.js':'직원 사이드 패널, 탭, 필터 변경, 수정 값 채우기, 10명 단위 목록 페이지를 관리합니다.',
'calendar.js':'개인 일정 달력의 상세·등록·수정 대화상자와 입력 상태를 관리합니다.',
'notifications.js':'알림 팝업을 열고 닫으며 읽음 처리 성공 뒤 표시를 갱신하거나 관련 페이지로 이동합니다.',
'overtime.js':'초과근무 입력의 날짜·시간 상태를 보조합니다. 최종 검사는 OvertimeService가 합니다.',
'account-delete.js':'계정 삭제 확인 대화상자와 정확한 대상 입력 등 클라이언트 동작을 담당합니다.',
'profile.html':'공통 프로필 아바타와 사진 업로드·삭제 폼을 정의하는 화면 조각입니다.',
'employees.html':'직원 목록·필터와 등록·수정 패널 및 사진 폼을 구성합니다.',
'groupware.html':'업무·댓글·전자결재·조직도·예약·자료실·첨부 화면을 페이지 종류에 맞게 선택합니다.',
'department-reports.html':'부서 달력과 월별 근태 보고서 화면을 구성합니다.',
'attendance-status.html':'근태 요약 통계와 주간 날짜별 상태를 표시합니다.',
'correction.html':'정정할 날짜·출퇴근 시각·결재자·사유와 최근 출퇴근 기록을 표시합니다.',
'chat.html':'고정 채팅 버튼·대화 목록·사람 검색·메시지 화면·입력 폼을 정의합니다.',
'calendar.html':'날짜별 일정 칸과 일정 입력·조회 대화상자를 정의합니다.',
'overtime.html':'초과근무 날짜·시작·종료 시각·결재자·사유 입력 화면입니다.',
'notifications.html':'상단 알림 아이콘·숫자 배지·목록·읽음 처리 폼을 정의합니다.',
'account-delete.html':'여러 화면이 함께 쓰는 계정 삭제 확인 대화상자입니다.',
'navigation.svg':'상위 메뉴에서 use로 재사용하는 선형 아이콘 모음입니다. 각 symbol의 ID가 아이콘 이름입니다.',
'pom.xml':'Java 17과 Spring Boot 및 DB·테스트 의존성, Maven 빌드 방법을 정하는 준비물 목록입니다.',
'Dockerfile':'소스를 Maven으로 빌드한 후 Java 실행 이미지에 JAR만 복사하는 다단계 빌드 설명서입니다.',
'compose.yaml':'컨테이너 이미지·포트·환경변수·마운트할 설정 파일을 함께 정합니다. 경로가 deploy/aws이면 그 배포 환경 구성을 뜻합니다.'
};
const methodNotes={
checkIn:'현재 직원과 날짜를 기준으로 중복 출근을 막고 서버 시각을 저장합니다.',checkOut:'오늘 출근 기록을 찾고 퇴근 시각이 비어 있을 때만 기록합니다.',current:'로그인 ID로 계정을 찾고 삭제·비활성 여부를 다시 확인합니다.',locked:'현재 직원 행을 잠근 상태로 가져와 변경 요청이 순서를 지키게 합니다.',remaining:'부여 단위에서 대기·승인 휴가 단위를 빼고 2로 나누어 일수를 계산합니다.',reserved:'해당 연도의 대기·승인 휴가 차감 단위만 합산합니다.',requestLeave:'결재자·기간·근무일·겹침·잔여량을 확인한 뒤 휴가 신청과 알림을 만듭니다.',cancelLeave:'본인의 대기 휴가인지 확인하고 취소 상태로 바꿉니다.',review:'결재 권한과 현재 상태를 검사한 후 승인 또는 반려합니다. 서비스별로 실제 반영 내용은 다릅니다.',request:'현재 서비스의 신청을 만들기 전에 대상·날짜·권한·중복 조건을 검사합니다.',cancel:'대상 소유권과 취소 가능한 상태인지 확인하고 취소합니다.',createEmployee:'로그인 ID·비밀번호·필수 정보를 검사하고 비밀번호를 해시해서 직원 객체를 저장합니다.',updateEmployee:'관리 권한을 확인하고 직원 정보·연차·활성 상태 등을 갱신합니다.',deleteEmployee:'실제 직원 행을 없애지 않고 삭제 상태와 관련 신청·권한을 정리합니다.',updateProfile:'본인의 연락처를 바꾸고 비밀번호 변경 시 현재 비밀번호를 확인합니다.',publish:'웹 관리자만 공지를 작성할 수 있도록 검사하고 저장합니다.',readNotification:'현재 사용자가 수신자인 알림만 읽음으로 바꿉니다.',eligibleApprover:'같은 부서의 활성 관리자인지와 자기 결재가 아닌지 확인합니다.',isManager:'역할·활성·삭제·부서 관리 플래그로 관리 자격을 판단합니다.',sameDepartment:'유효한 부서 관리자와 대상 직원이 같은 부서인지 확인합니다.',canReview:'현재 대상에 결재 가능한 사람인지 판단합니다. 구체 조건은 이 클래스 원문을 따라 읽으세요.',requireReview:'결재 자격이 없으면 접근 거부 예외를 내서 이후 작업을 막습니다.',workingDay:'주말과 등록된 휴무일을 제외한 근무일인지 판단합니다.',closedDates:'등록된 회사 휴무일의 날짜들을 중복 없는 Set으로 모읍니다.',recalculate:'대기·승인 휴가들의 차감량을 최신 휴무일 기준으로 다시 계산합니다.',canTask:'웹 관리자가 아니며 현재 업무 담당자가 본인인지 확인합니다.',task:'업무를 찾은 뒤 현재 사용자가 접근 가능한지 확인합니다.',comments:'업무 접근 권한을 먼저 확인한 뒤 해당 업무 댓글을 작성 시각 순서로 돌려줍니다.',comment:'접근 가능한 업무에 댓글을 저장하고 알림 대상을 고릅니다.',saveTask:'담당자를 본인으로 제한하고 업무 필수 내용·마감일을 저장합니다.',remindTasks:'오늘 또는 지난 마감 업무에 하루 한 번만 알림을 보내도록 검사합니다.',createDocument:'문서 종류와 금액·기간·결재자 순서를 검사하고 문서 및 각 결재 단계를 만듭니다.',reviewDocument:'현재 순번의 결재자만 처리하게 하고 다음 순번 또는 최종 상태로 진행합니다.',canDocument:'웹 관리자를 제외하고 작성자 또는 결재선 참여자만 문서를 볼 수 있게 합니다.',reserve:'자원 행을 잠근 뒤 대기·확정 예약과 시간 겹침을 검사하고 예약을 만듭니다.',reviewReservation:'관리자 허가·반려를 처리하고 결과를 신청자에게 알립니다.',canLibrary:'관리자·작성자·모든 부서 공개·등록 부서 여부로 자료실 접근을 판단합니다.',fileAccess:'업무·결재·자료실 등 첨부 대상 종류에 맞는 읽기·쓰기 권한을 확인합니다.',normalize:'실제 이미지 형식·용량·픽셀 수를 확인하고 중앙 256×256 PNG로 바꿉니다.',editable:'프로필을 바꾸려는 사람이 본인 또는 관리자이고 대상이 삭제되지 않았는지 확인합니다.',send:'현재 발신자·내용·UUID·수신자 조건을 확인하고 동일 재시도는 기존 메시지로 처리합니다.',history:'로그인한 나와 상대 사이의 메시지만 ID 기준으로 이전 기록까지 읽습니다.',inbox:'상대별 최근 메시지와 안 읽은 수를 모아 대화 목록을 만듭니다.',read:'로그인한 수신자에게 온 지정 상대의 메시지 ID만 읽음으로 갱신합니다.',people:'자기 자신과 비활성·삭제 계정을 제외하고 검색 조건에 맞는 직원 표시 정보를 만듭니다.',undo:'승인된 원본을 취소하거나 정정 전의 실제 출퇴근으로 되돌립니다.',replace:'변경 요청의 새 내용으로 해당 종류의 신청을 만들고 승인한 뒤 새 ID를 연결합니다.',original:'신청 종류에 따라 승인된 원본을 찾아 필요한 값의 스냅샷으로 묶습니다.',workedMinutes:'완료된 근무 구간에서 점심과 겹치는 초를 빼고 분 단위로 계산합니다.',monthly:'해당 월과 부서 범위에 맞는 직원별 통계 묶음을 만듭니다.',calendar:'월 달력의 날짜 칸에 승인 휴가와 업무 마감일 등을 연결합니다.',notify:'수신자와 메시지·이동 경로를 가진 알림 객체를 저장합니다.',required:'입력 문자열이 비어 있거나 제한 길이를 넘으면 오류를 냅니다.',optional:'필수가 아닌 문자열도 길이와 형식을 정리합니다.',require:'조건이 거짓이면 업무 예외를 던지는 공통 검사 도우미입니다.',allow:'권한 조건이 거짓이면 이후 업무 처리를 허용하지 않습니다.',page:'주소의 화면 종류에 맞는 모델을 준비하고 템플릿 이름을 반환합니다.',done:'한 번 표시할 성공 안내를 넣고 결과 페이지로 이동합니다.'
};
const configNotes={
'spring.application.name':'애플리케이션 이름입니다.','spring.profiles.default':'명시하지 않았을 때 적용할 프로필입니다.','spring.jpa.hibernate.ddl-auto':'validate는 스키마 확인, update는 구조 보완, create-drop은 시작 시 만들고 종료 시 지우는 테스트용 동작입니다.','spring.jpa.open-in-view':'false이면 화면 렌더링 내내 영속성 컨텍스트를 유지하는 방식을 사용하지 않습니다.','spring.thymeleaf.cache':'템플릿 캐시 여부입니다. false면 개발 중 변경 확인이 쉽습니다.','spring.datasource.url':'접속할 DB 종류와 주소입니다. 실제 인증정보는 이 학습서에 포함하지 않습니다.','spring.datasource.username':'DB에 로그인할 계정 항목입니다. 웹사이트 직원 로그인 계정과는 다릅니다.','spring.datasource.password':'DB 접속 비밀번호 설정 항목입니다. 실제 값은 복사하지 않습니다.','server.servlet.session.timeout':'서버 로그인 세션의 유휴 만료 시간을 정합니다.','server.servlet.session.cookie.http-only':'브라우저 JavaScript가 세션 쿠키를 읽지 못하게 하는 옵션입니다.','server.servlet.session.cookie.same-site':'사이트 간 쿠키 전송 정책입니다. CSRF 토큰과 같은 기능은 아닙니다.','server.error.include-message':'기본 오류 응답에서 내부 메시지를 노출할지 정합니다.','portal.bootstrap.enabled':'빈 DB에 초기 계정을 생성하는 부품을 켤지 정합니다.','spring.servlet.multipart.max-file-size':'파일 하나의 기본 업로드 제한입니다.','spring.servlet.multipart.max-request-size':'파일과 폼을 합친 요청 크기 제한입니다.'};
function info(file,source,dict){const base=path.basename(file),name=base.replace(/\.java$/,'');let role=roles[base]||roles[name];const table=source.match(/@Table\(name\s*=\s*"([^"]+)"/)?.[1]?.toUpperCase();if(!role&&table)role=`${table} 테이블과 연결된 ${dict[table]?.title||name} 엔티티입니다. 각 필드는 저장할 항목이고 표시용 메서드는 계산 결과를 돌려줍니다.`;if(!role&&file.includes('/repository/')){const entity=source.match(/JpaRepository<\s*(\w+)/)?.[1];role=`${entity||name} 자료를 조회·저장하는 DB 창구입니다. 기본 CRUD를 상속받고 메서드 이름 또는 @Query로 필요한 조건을 추가합니다. @Lock이 붙은 조회는 변경 순서를 지키기 위해 잠금을 요청합니다.`;}if(!role&&/Tests\.java$/.test(file))role=`${name.replace(/Tests$/,'')} 기능의 정상 동작과 금지 조건을 확인하는 자동 테스트입니다. 준비 → 실행 → 기대 결과 확인 순으로 읽습니다.`;if(!role&&base.endsWith('.css'))role=`${base}는 색상·크기·배치·모바일 조건을 정합니다. 선택자는 적용 대상, 중괄호 안의 속성은 꾸미기 규칙입니다. 업무 권한을 판단하는 코드는 아닙니다.`;if(!role&&base.endsWith('.properties'))role='이 파일이 적용되는 실행 프로필의 설정을 key=value로 지정합니다. 기본 설정과 실행 시 환경변수를 함께 확인합니다.';if(!role&&base.endsWith('.sql'))role=file.includes('/migrations/')?'기존 DB에 필요한 변경을 단계적으로 적용하는 SQL입니다. 서비스 실행 코드가 아니라 DB 구조를 갱신하는 명령입니다.':'새 Oracle 환경을 만들기 위한 테이블·시퀀스·제약·인덱스 SQL입니다. 기존 데이터가 있는 DB에 새 DB용 스크립트를 무작정 반복 실행하지 않습니다.';if(!role&&/Caddyfile/.test(base))role='HTTPS를 받는 Caddy 프록시가 앱으로 요청을 전달하는 설정입니다.';if(!role&&/\.ya?ml$/.test(base))role='Docker Compose 실행 환경의 이미지·포트·환경변수·볼륨을 지정합니다. 파일이 있는 배포 디렉터리의 환경을 기준으로 읽습니다.';return {role:role||'프로젝트 실행이나 검증에 사용하는 보조 파일입니다. 선언과 호출 관계를 원문에서 함께 확인하세요.',table};}
function explain(line,file,context={},dict={}){
 const s=line.trim(),out=[],add=t=>{if(t&&!out.includes(t))out.push(t);};
 if(!s)return '읽기 편하게 작업 덩어리를 나눈 빈 줄입니다. 실행되는 명령은 없습니다.';
 if(/^\/\/|^\/\*|^\*|^<!--|^--\s/.test(s))return '주석입니다. 실행하지 않는 설명 메모입니다. 오래된 설명과 실제 조건이 다르면 실행 코드를 우선 확인합니다.';
 if(/^[{}();,\s]+$/.test(s))return s.includes('}')?'이 코드 블록이나 객체·함수의 울타리를 닫습니다. 괄호가 여럿이면 안쪽부터 차례로 닫는 것입니다.':'새 블록의 울타리를 열거나 앞 문장을 구분·마무리합니다.';
 if(file.endsWith('.java')){
  if(s.startsWith('package '))return '이 클래스의 코드 주소(패키지)를 선언합니다. 같은 역할의 파일을 모으고 이름이 겹치는 것을 구분합니다.';
  if(s.startsWith('import '))return `${s.replace(/^import (static )?/,'').replace(/;$/,'')} 도구 이름을 이 파일에서 사용할 수 있도록 가져옵니다. import만으로 객체를 생성하거나 실행하지는 않습니다.`;
  const annotations=[['@SpringBootApplication','서버 시작점·자동 설정·하위 부품 검색의 기준을 표시합니다.'],['@RestController','반환 객체를 주로 JSON 응답으로 보내는 API 접수 부품입니다.'],['@ControllerAdvice','여러 컨트롤러의 공통 예외 처리 부품입니다.'],['@Controller','주소 요청을 받아 화면 또는 응답을 준비하는 부품입니다.'],['@Service','Spring이 관리할 업무 담당 부품으로 등록합니다.'],['@Component','Spring이 만들어 관리하는 일반 부품으로 등록합니다.'],['@RequiredArgsConstructor','Lombok이 final 필드 등을 받는 생성자를 만들어 필요한 도구를 주입받게 합니다.'],['@Getter','Lombok이 필드 값을 꺼내는 메서드를 만들어 줍니다.'],['@Setter','Lombok이 필드 값을 바꾸는 메서드를 만들어 줍니다.'],['@Entity','이 Java 객체를 DB에 저장하는 JPA 엔티티로 표시합니다.'],['@MappedSuperclass','공통 필드를 자식 엔티티 테이블에 물려줍니다. 자체 테이블은 만들지 않습니다.'],['@Version','동시에 수정했는지 비교할 수정 번호입니다.'],['@Id','DB 행을 유일하게 구별하는 기본키 항목입니다.'],['@GeneratedValue','ID를 코드에서 임의로 정하지 않고 지정한 생성 전략으로 받습니다.'],['@SequenceGenerator','ID 번호를 받을 DB 시퀀스와 배정 단위를 지정합니다.'],['@ManyToOne','여러 현재 행이 하나의 상대 엔티티를 참조하는 관계입니다. 보통 상대 ID가 FK 컬럼에 저장됩니다.'],['@Lob','큰 글자 또는 바이트 데이터를 CLOB/BLOB으로 다룹니다.'],['@Enumerated','enum 값을 DB에 어떤 형태로 저장할지 정합니다. STRING은 열거형 이름을 사용합니다.'],['@Lock','조회 대상 행의 DB 잠금을 요청합니다. PESSIMISTIC_WRITE는 경쟁하는 변경을 기다리게 하는 방식입니다.'],['@Modifying','조회문이 아니라 UPDATE/DELETE 같은 변경 쿼리임을 알려 줍니다.'],['@Bean','이 메서드가 돌려주는 객체를 Spring 공용 부품으로 등록합니다.'],['@Test','자동으로 실행할 테스트 한 건을 표시합니다.'],['@BeforeEach','각 테스트 전에 독립적인 준비 작업을 실행합니다.'],['@Autowired','Spring 테스트 등에서 필요한 부품을 연결받습니다.'],['@SpringBootTest','앱의 Spring 부품을 함께 켜서 통합 확인합니다.'],['@AutoConfigureMockMvc','서버 요청을 흉내 낼 MockMvc를 준비합니다.'],['@ActiveProfiles','테스트 등에 사용할 실행 설정 묶음을 지정합니다.'],['@Scheduled','정해진 주기에 이 메서드를 실행하도록 예약합니다.'],['@ConditionalOnProperty','특정 설정값일 때만 이 부품을 켭니다.'],['@Override','상위 타입이 정한 메서드를 이 클래스에서 구현하거나 바꿉니다.']];
  annotations.forEach(([a,d])=>{if(s.includes(a)&&!(a==='@Controller'&&/@ControllerAdvice|@RestController/.test(s)))add(d);});
  if(s.includes('@Transactional'))add(s.includes('readOnly=true')?'이 범위의 기본 DB 작업을 조회 중심 트랜잭션으로 설정합니다. 변경 메서드는 별도 @Transactional을 봅니다.':'이 메서드 또는 테스트의 DB 작업을 트랜잭션으로 묶습니다. 서비스는 성공 시 커밋, 런타임 오류 시 보통 롤백하며 테스트는 기본 롤백됩니다.');
  for(const m of s.matchAll(/@(Get|Post|Request)Mapping\(([^\n]+?)\)/g))add(`${m[1]==='Get'?'GET 조회':m[1]==='Post'?'POST 처리':'공통 주소'} 요청 ${m[2]}를 이 클래스나 메서드에 연결합니다.`);
  if(s.includes('@RequestParam'))add('폼 또는 URL 매개변수를 이름에 맞는 인자로 받습니다. required=false와 defaultValue는 입력이 없을 때의 규칙입니다.');
  if(s.includes('@PathVariable'))add('주소 경로의 {id} 같은 부분을 인자로 받습니다.');
  if(s.includes('Principal '))add('로그인한 사용자 정보가 Principal로 전달됩니다. 요청자가 임의로 적은 직원 ID 대신 인증 정보를 사용합니다.');
  if(s.includes('@ExceptionHandler'))add('지정 예외가 발생하면 이 처리기로 안내 응답을 만듭니다.');
  if(s.includes('@ResponseStatus'))add('반환할 HTTP 상태 코드를 지정합니다.');
  if(s.includes('@Table'))add('DB 테이블 이름과 테이블 단위 고유 제약 등을 지정합니다.');
  if(s.includes('@Column'))add('DB 컬럼 이름·최대 길이·필수 여부를 지정합니다. nullable=false는 NULL을 허용하지 않는다는 뜻입니다.');
  if(s.includes('@UniqueConstraint'))add('지정한 컬럼 조합의 중복을 DB에서도 막습니다.');
  if(s.includes('@Query')){add('엔티티·필드를 기준으로 한 JPQL을 지정합니다. :이름은 실행할 때 채울 매개변수입니다.');if(/join fetch/i.test(s))add('join fetch는 관련 객체도 이 조회에서 함께 가져오도록 합니다.');if(/group by/i.test(s))add('GROUP BY로 같은 상대 등 기준별로 묶고 MAX·SUM 같은 집계를 계산합니다.');if(/update /i.test(s))add('조건에 맞는 행만 갱신합니다. Java 객체 변경 감지와 별도의 일괄 UPDATE 경로입니다.');}
  const field=/\bprivate\s+(?:static\s+)?(?:final\s+)?([\w<>?,.]+)\s+(\w+)\s*(?:=|;)/.exec(s);
  if(field){const [,type,name]=field;if(s.includes('final '))add(`${name}은 ${type} 도구를 보관하는 필드입니다. 생성자로 받아 필드 참조가 바뀌지 않게 합니다.`);else {let column=s.match(/@Column\([^)]*name\s*=\s*"([^"]+)"/)?.[1]||name.replace(/[A-Z]/g,c=>'_'+c.toLowerCase());if(s.includes('@ManyToOne'))column+='_id';const desc=dict[context.table]?.columns?.[column.toUpperCase()];add(`${name}은 ${type} 타입의 저장 항목입니다.${desc?' '+desc:' 초기값이 있으면 새 객체의 시작 값으로 사용하며, 필드와 계산용 메서드를 구분합니다.'}`);}}
  const method=s.match(/\b(?:public|private|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>?,.\[\]]+\s+(\w+)\s*\(/);
  if(method)add(`${method[1]} 작업을 선언합니다. ${methodNotes[method[1]]||'괄호 안의 재료를 받아 중괄호 안을 실행합니다. 반환 타입은 결과의 종류이며 void이면 결과 값은 없습니다.'}`);
  if(/\brecord\s+/.test(s))add('서로 관련된 값들을 하나로 전달하기 위한 불변 데이터 묶음(record)을 선언합니다.');
  if(/\benum\s+/.test(s))add('가능한 상태나 종류의 목록을 enum으로 제한합니다. 코드 이름과 화면 표시 글자는 다를 수 있습니다.');
  if(/extends JpaRepository/.test(s))add('엔티티 타입과 ID 타입을 정해 기본 저장·조회·삭제 기능을 물려받습니다. 인터페이스 구현은 Spring Data가 준비합니다.');
  if(/\b(find|count|exists|delete)By\w+\(/.test(s)&&file.includes('/repository/'))add('메서드 이름의 By 뒤 필드와 조건을 읽어 조회를 만듭니다. @Query가 붙은 메서드는 지정 쿼리가 우선입니다.');
  if(s.includes('require(')||s.includes('allow('))add('괄호 안의 조건이 거짓이면 안내 예외로 중단합니다. 뒤의 한국어 문자열이 있다면 그 상황을 설명하는 실패 메시지입니다.');
  if(s.includes('.orElseThrow('))add('조회 결과가 있으면 꺼내고 없으면 예외를 던집니다. 빈 Optional을 그대로 사용하는 일을 막습니다.');
  if(s.includes('.lockBy'))add('이 대상의 잠금을 얻은 뒤 변경합니다. 다른 변경 경로와 잠금 순서를 맞추는 것이 중요합니다.');
  if(/\.save(?:AndFlush)?\(/.test(s))add(s.includes('saveAndFlush')?'객체 저장을 요청하고 flush로 SQL을 DB에 반영합니다. flush 자체는 최종 커밋과 다릅니다.':'객체를 영속성 관리에 저장하도록 요청합니다. 최종 성공 확정은 트랜잭션이 담당합니다.');
  if(/\.set[A-Z]\w*\(/.test(s))add('setter 호출로 객체의 항목을 바꿉니다. 관리 중인 엔티티라면 변경 트랜잭션의 끝에 DB 수정으로 반영될 수 있습니다.');
  if(s.includes('.delete(')||s.includes('.deleteById('))add('해당 데이터의 삭제를 요청합니다. 어떤 대상을 지우는지 확인하세요. 계정의 논리 삭제와는 다른 동작일 수 있습니다.');
  if(s.includes('.addAttribute('))add('이름표와 값을 Model에 담습니다. 템플릿은 같은 이름의 ${...}로 꺼내 씁니다.');
  if(s.includes('redirect:'))add('응답에서 다른 내부 주소로 이동하게 합니다. POST 처리 후 GET 화면을 다시 읽는 흐름에 사용합니다.');
  if(s.includes('new '))add('new 뒤 설계도로 새 객체를 만듭니다. 객체를 만들었다고 곧바로 DB에 저장된 것은 아닙니다.');
  if(s.includes('LocalDateTime.now(clock)')||s.includes('LocalDate.now(clock)'))add('주입받은 서버 시계로 현재 날짜 또는 시각을 구합니다. 테스트에서는 시계를 바꿔 경계 시간을 재현할 수 있습니다.');
  if(s.includes('encoder.encode'))add('비밀번호를 BCrypt 해시로 바꿉니다. 원문으로 복원하는 암호화가 아닙니다.');
  if(s.includes('encoder.matches'))add('입력한 비밀번호와 저장된 해시가 맞는지 검사합니다.');
  if(s.includes('.filter('))add('목록에서 조건을 만족하는 항목만 남깁니다.');
  if(s.includes('.map(')||s.includes('.mapToInt(')||s.includes('.mapToObj('))add('각 항목에서 필요한 값만 꺼내거나 새로운 모양으로 바꿉니다.');
  if(s.includes('.sorted('))add('Comparator 등 지정 기준에 따라 결과 순서를 정합니다. reversed가 붙으면 반대 순서입니다.');
  if(s.includes('.sum()')||s.includes('.count()'))add('남은 숫자들을 합하거나 항목 수를 셉니다.');
  if(s.includes('.toList()'))add('앞에서 고르거나 바꾼 결과를 List로 모읍니다.');
  if(/\bfor\s*\(/.test(s))add('여러 항목이나 날짜를 차례로 반복합니다. 종료 조건과 한 번에 무엇이 증가하는지 확인하세요.');
  if(/\bif\s*\(/.test(s))add('if의 조건이 참일 때만 해당 블록을 실행합니다. &&는 모두, ||는 하나 이상, !는 반대를 뜻합니다.');
  if(/\breturn\b/.test(s))add('현재 작업을 마치고 뒤의 결과를 호출자에게 돌려줍니다.');
  if(/\bthrow\b/.test(s))add('정상 흐름을 멈추고 예외를 바깥 처리기로 전달합니다.');
  if(/assertThat|andExpect|assertEquals|assertTrue|assertFalse|assertThrows/.test(s))add('실제 결과가 기대한 값·상태·오류인지 검사합니다. 다르면 테스트 실패입니다.');
  if(/mvc\.perform/.test(s))add('가짜 HTTP 요청을 앱에 보내 컨트롤러와 보안 흐름을 함께 확인합니다.');
  if(/try\s*\(|try\s*\{/.test(s))add('실패할 수 있는 작업을 실행하고, 자원은 사용 후 닫거나 catch/finally에서 정리합니다.');
  if(/ImageIO|BufferedImage|drawImage/.test(s))add('이미지 데이터를 읽거나 픽셀을 새 그림에 그립니다. 파일명만 믿지 않고 실제 이미지 내용을 다룹니다.');
  if(/ZipOutputStream|ZipEntry/.test(s))add('여러 파일을 ZIP 항목으로 묶습니다. XLSX도 이런 ZIP 안의 XML 파일 구조입니다.');
 } else if(/\.html$|\.svg$/.test(file)){
  if(s.includes('<!DOCTYPE'))add('브라우저에 HTML 문서 형식임을 알립니다.');
  const attrs=[['th:text','서버 값을 HTML로 실행하지 않고 안전한 글자로 표시합니다.'],['th:if','조건이 참일 때만 해당 요소를 만듭니다.'],['th:unless','조건이 거짓일 때만 해당 요소를 만듭니다.'],['th:each','목록을 반복해 직원·날짜·신청별 요소를 만듭니다.'],['th:action','폼을 보낼 내부 주소를 만듭니다. POST 폼에는 CSRF 처리가 함께 연결됩니다.'],['th:href','링크의 내부 이동 주소를 만듭니다.'],['th:value','입력칸에 서버의 현재 값을 넣습니다.'],['th:selected','현재 값과 일치하는 선택 항목을 고릅니다.'],['th:replace','공통 fragment 화면을 이 자리에 끼워 넣습니다.'],['th:fragment','다른 화면에서 사용할 공통 조각에 이름을 붙입니다.'],['th:case','현재 분기 값과 일치하는 화면 조각을 선택합니다.'],['th:attr','data-* 등 HTML 속성을 서버 값으로 채웁니다.'],['th:src','이미지·스크립트 파일의 주소를 만듭니다.'],['aria-','화면 상태와 요소의 의미를 보조기기·키보드 사용자에게도 알립니다.'],['multipart/form-data','파일을 전송할 수 있는 폼 포장 방식을 지정합니다.']];attrs.forEach(([a,d])=>{if(s.includes(a))add(d);});
  if(s.includes('<form'))add('입력값을 모아 제출하는 폼입니다. name은 서버 인자 이름, action은 받는 주소, method는 요청 방식입니다.');
  if(s.includes('<input')||s.includes('<textarea')||s.includes('<select'))add('사용자 입력·글 작성·목록 선택 요소입니다. required·maxlength 등 브라우저 검증과 서버 검증이 함께 필요합니다.');
  if(s.includes('<button'))add('클릭할 버튼입니다. 폼 안에서 type=button이 없으면 기본적으로 제출 버튼일 수 있습니다.');
  if(s.includes('<dialog'))add('열고 닫을 수 있는 대화상자입니다. JavaScript가 showModal/close 등을 호출합니다.');
  if(s.includes('<script'))add('화면 행동을 담당하는 JavaScript를 연결합니다. defer는 HTML 해석 뒤 실행되도록 합니다.');
  if(s.includes('<link'))add('CSS 등 외부 자원의 위치를 연결합니다.');
  if(s.includes('<img'))add('이미지를 표시합니다. 사진이 없는 경우의 대체 글자와 alt 의미도 확인하세요.');
  if(s.includes('<symbol')||s.includes('<path')||s.includes('<use'))add('SVG의 선·도형 또는 재사용 아이콘을 정의하거나 불러옵니다.');
  if(s.includes('<table'))add('행·열로 목록을 표시합니다. th는 제목 칸, td는 내용 칸입니다.');
  if(s.includes('<details'))add('누르면 펼치고 접을 수 있는 영역입니다. summary가 제목입니다.');
 } else if(file.endsWith('.js')){
  const rules=[[/querySelector|getElementById/,'ID나 CSS 선택자로 화면 요소를 찾아 변수에 연결합니다.'],[/addEventListener/,'클릭·입력·키보드 등의 이벤트가 발생했을 때 실행할 함수를 등록합니다.'],[/\b(?:const|let)\b/,'화면 요소나 현재 상태를 변수에 담습니다. const는 재대입하지 않는 이름, let은 다시 바뀔 수 있는 이름입니다.'],[/\basync\b|\bawait\b/,'서버 응답 같은 비동기 작업을 기다렸다가 다음 단계를 실행합니다.'],[/fetch\(/,'서버에 HTTP 요청을 보냅니다. URL·method·body·credentials·응답 상태를 함께 읽으세요.'],[/URLSearchParams|FormData/,'폼의 이름과 값을 서버가 읽을 수 있는 요청 데이터로 포장합니다.'],[/csrf/i,'변경 요청에 CSRF 토큰을 넣어 로그인 쿠키 외의 요청 확인 정보를 함께 보냅니다.'],[/response\.ok|response\.status|!r\.ok/,'HTTP 응답이 성공인지 확인하고 실패를 정상 성공처럼 처리하지 않습니다.'],[/textContent/,'값을 HTML 코드가 아니라 글자로 넣습니다. 메시지 본문을 실행시키지 않는 데 중요합니다.'],[/\.hidden\s*=/,'요소의 표시·숨김 상태를 바꿉니다.'],[/setAttribute/,'접근성 상태·주소 등 HTML 속성값을 갱신합니다.'],[/createElement|append\(|replaceChildren/,'화면 요소를 만들거나 자식을 붙이고 기존 내용을 교체합니다.'],[/preventDefault/,'폼 제출이나 링크 이동 등 브라우저의 기본 행동을 잠시 막고 이 함수의 흐름으로 처리합니다.'],[/\.disabled\s*=/,'버튼·입력칸을 사용 가능 또는 잠금 상태로 바꿉니다.'],[/setTimeout|clearTimeout/,'일정 시간 후 실행을 예약하거나 이전 예약을 취소합니다. 채팅 반복 조회·검색 지연에 사용합니다.'],[/setInterval|clearInterval/,'반복 작업을 예약하거나 해제합니다.'],[/isComposing|keyCode===229/,'한글 등 글자 조합 중 Enter를 전송으로 잘못 처리하지 않도록 확인합니다.'],[/getBoundingClientRect/,'화면에서 요소가 차지하는 위치를 읽어 실제로 보이는 메시지 등을 판단합니다.'],[/scrollTop|scrollHeight/,'스크롤 위치와 전체 높이를 이용해 대화 위치를 유지하거나 최신 메시지로 이동합니다.'],[/\.focus\(/,'키보드 입력 초점을 적절한 버튼이나 입력칸으로 옮깁니다.'],[/showModal|\.close\(/,'대화상자를 열거나 닫습니다.'],[/crypto\.randomUUID/,'메시지의 재전송을 식별할 UUID를 만듭니다. 같은 요청의 재시도는 이 값을 재사용합니다.'],[/revision|version!==revision/,'응답이 아직 같은 대화·화면에 속하는지 검사해 오래된 응답이 현재 화면을 덮지 않게 합니다.'],[/new Map|messages\.set|drafts\.|retries\./,'ID나 상대별로 메시지·작성 중 글·재시도 정보를 기억합니다. 페이지 새로고침 뒤까지 영구 저장되는 DB와는 다릅니다.'],[/classList/,'CSS 클래스 이름을 붙이거나 빼서 강조·상태 표시를 바꿉니다.'],[/\.filter\(/,'조건에 맞는 화면 요소나 데이터만 고릅니다.'],[/\.map\(/,'각 값을 다른 모양으로 바꾸어 새 목록을 만듭니다.'],[/\.forEach\(/,'목록의 각 항목에 같은 작업을 수행합니다.'],[/\bif\s*\(/,'조건에 따라 실행하거나 일찍 종료합니다.'],[/\breturn\b/,'현재 함수의 실행을 끝내고 결과를 돌려줍니다.'],[/\bcatch\b/,'요청·처리 실패를 잡아 오류 표시나 입력 보존을 처리합니다.'],[/\bfinally\b/,'성공·실패와 관계없이 버튼 잠금 등 임시 상태를 정리합니다.']];rules.forEach(([r,t])=>{if(r.test(s))add(t);});
 } else if(file.endsWith('.css')){
  const selectors=s.slice(0,s.indexOf('{')>=0?s.indexOf('{'):Math.min(s.length,90));add(`선택자 ${selectors.slice(0,150)} 등에 적용하는 스타일입니다. 한 줄에 여러 규칙이 있으면 } 다음부터 새 선택자가 시작합니다.`);
  const rules=[[/display\s*:\s*(flex|grid)/,'flex/grid로 여러 요소의 배치를 정합니다.'],[/position\s*:\s*fixed/,'스크롤과 관계없이 화면 기준으로 위치를 고정합니다.'],[/position\s*:\s*sticky/,'스크롤 중 지정한 경계에 닿으면 붙어 있도록 합니다.'],[/position\s*:\s*absolute/,'위치 기준이 되는 부모 안에서 좌표로 배치합니다.'],[/gap\s*:/,'형제 요소 사이 간격을 정합니다.'],[/padding\s*:/,'테두리 안쪽 여백을 정합니다.'],[/margin\s*:/,'요소 바깥쪽 여백을 정합니다.'],[/overflow/,'내용이 넘칠 때 숨기거나 스크롤할 방식을 정합니다.'],[/@media/,'화면 폭 등 조건에 맞을 때만 이 스타일을 적용합니다.'],[/font-size|font-weight|line-height/,'글자 크기·두께·줄 사이 간격을 정합니다.'],[/background|color\s*:/,'배경색과 글자색을 정합니다.'],[/border|box-shadow/,'테두리·모서리·그림자를 꾸밉니다.'],[/width|height/,'요소의 가로·세로 크기 또는 최소·최대 범위를 정합니다.'],[/z-index/,'겹치는 위치 요소들 사이 표시 순서를 정합니다.'],[/object-fit\s*:\s*cover/,'이미지 비율을 유지하며 영역을 채우고 넘는 부분을 자릅니다.'],[/\[hidden\]/,'hidden 속성이 붙은 요소의 숨김 스타일을 명시합니다.']];rules.forEach(([r,t])=>{if(r.test(s))add(t);});
 } else if(file.endsWith('.properties')){const [key,...v]=s.split('=');add(configNotes[key]||`${key} 설정에 ${v.join('=')||'빈 값'}을 지정합니다. 이 파일의 프로필과 환경변수 덮어쓰기 순서를 함께 확인하세요.`);if(s.includes('${'))add('${이름:기본값}은 외부 환경값을 우선 읽고 없으면 기본값을 사용하는 표현입니다.');
 } else if(file.endsWith('.sql')){
  if(/create table/i.test(s))add('새 표를 만들고 괄호 안에서 컬럼과 제약을 정합니다.');if(/alter table/i.test(s))add('기존 표의 구조를 변경합니다. 이 마이그레이션은 추가 컬럼·제약 등을 적용합니다.');if(/create sequence/i.test(s))add('고유 ID를 순서대로 내어 주는 시퀀스를 만듭니다.');if(/primary key/i.test(s))add('행을 구별하는 기본키입니다. 중복과 NULL을 허용하지 않습니다.');if(/foreign key|references/i.test(s))add('다른 표에 존재하는 ID를 참조하도록 물리 외래키 제약을 둡니다.');if(/unique/i.test(s))add('컬럼 또는 컬럼 조합의 중복을 막습니다.');if(/check\s*\(/i.test(s))add('값이 정해진 조건을 만족하는지 DB가 검사합니다.');if(/create index/i.test(s))add('조회에 사용할 색인을 만듭니다. 저장 공간과 변경 비용도 함께 늘어날 수 있습니다.');if(/varchar2|number|timestamp|\bdate\b|\bclob\b|\bblob\b/i.test(s))add('컬럼 이름과 Oracle 자료형을 정의합니다. VARCHAR2는 글자, NUMBER는 수, DATE/TIMESTAMP는 날짜·시각, CLOB/BLOB은 큰 글자/바이트입니다. NOT NULL이면 비울 수 없습니다.');
 } else if(path.basename(file)==='Dockerfile'){
  const key=s.split(' ')[0];add(({FROM:'다음 빌드 단계에서 사용할 기본 이미지를 선택합니다. AS는 단계 이름입니다.',WORKDIR:'이후 명령을 실행할 이미지 안 작업 폴더입니다.',COPY:'호스트 소스 또는 앞 빌드 단계 파일을 이미지 안으로 복사합니다.',RUN:'이미지를 만드는 동안 명령을 실행합니다. 실행 중 컨테이너의 요청과는 다릅니다.',USER:'컨테이너 프로세스를 실행할 사용자·그룹을 지정합니다.',EXPOSE:'사용할 포트를 문서화합니다. 외부 포트 공개는 Compose 등의 별도 설정입니다.',ENTRYPOINT:'컨테이너가 시작할 때 실행할 프로그램과 인자를 정합니다.'})[key]);
 } else if(file.endsWith('.xml')){if(s.includes('<dependency>'))add('프로젝트에서 사용할 라이브러리 한 묶음의 선언을 시작합니다.');if(s.includes('<groupId>'))add('라이브러리·프로젝트를 만든 조직의 식별 이름입니다.');if(s.includes('<artifactId>'))add('라이브러리 또는 빌드 도구의 실제 식별 이름입니다.');if(s.includes('<version>'))add('사용할 버전을 지정합니다. 부모 설정에서 버전을 물려받으면 생략할 수도 있습니다.');if(s.includes('<scope>'))add('컴파일·실행·테스트 등 어느 때 필요한 도구인지 정합니다.');if(s.includes('<plugin>'))add('코드 컴파일·포장 같은 빌드 작업 도구를 설정합니다.');if(s.includes('<java.version>'))add('이 프로젝트가 기준으로 사용하는 Java 버전입니다.');
 } else if(/\.ya?ml$/.test(file)){if(s.includes('ports:'))add('호스트와 컨테이너 포트를 연결할 목록입니다.');else if(s.includes('environment:'))add('앱 실행 때 전달할 환경변수 목록입니다.');else if(s.includes('volumes:'))add('호스트 파일·폴더와 컨테이너 사이의 마운트를 정의합니다.');else if(s.includes('read_only:'))add('컨테이너에서 연결한 파일을 수정하지 못하도록 읽기 전용으로 지정합니다.');else add('들여쓰기로 어느 서비스·설정에 속하는지 나타냅니다. 콜론 왼쪽은 이름, 오른쪽은 값입니다. 이미지·환경변수·포트의 대상 위치를 구분하세요.');}
 if(!out.length)add(file.endsWith('.java')?'타입·메서드 호출·값의 조합을 이어가는 Java 문장입니다. 점 왼쪽은 대상, 오른쪽은 필드나 작업이며 괄호 안이 전달값입니다. 이 파일의 역할과 앞뒤 조건을 함께 읽으세요.':file.endsWith('.js')?'현재 화면 상태나 함수의 결과를 계산·연결하는 JavaScript 문장입니다. 점 앞 대상과 괄호 안 인자, 앞뒤 이벤트 흐름을 함께 읽으세요.':/\.html$|\.svg$/.test(file)?'화면 구조를 만들거나 앞 요소를 닫습니다. 태그의 중첩이 부모·자식 관계와 표시 영역을 결정합니다.':'설정이나 데이터 구조의 한 부분입니다. 앞의 블록 이름과 들여쓰기·종료 기호를 함께 읽으세요.');
 return out.join(' ');
}
module.exports={info,explain};
