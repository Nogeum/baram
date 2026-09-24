# EC2 웹사이트 + 현재 PC의 Oracle

RDS/S3 없이 기존 Oracle에 직접 연결하는 테스트 구성입니다. 웹에서 변경한 데이터는 원본 PC의 Oracle에 저장됩니다. PC와 Oracle, SSH 터널을 켜두어야 하며 절전이나 네트워크 단절 시 DB 연결이 끊깁니다.

## 연결

PC PowerShell에서 (키 경로와 EC2 IP는 실제 값 사용):

```powershell
Test-NetConnection 127.0.0.1 -Port 1521
ssh -i "C:\portal.pem" -N -T -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -R 127.0.0.1:11521:127.0.0.1:1521 ubuntu@3.35.3.77
```

터널 명령은 정상 연결 시 출력 없이 실행을 유지합니다. 종료는 Ctrl+C입니다. 오류 또는 프롬프트 복귀는 연결 상태 확인이 필요합니다. 이 명령은 로컬 Oracle 연결 성공까지 보장하지 않습니다.

EC2의 별도 SSH 터미널에서:

```bash
ss -lnt 'sport = :11521'
```

127.0.0.1:11521에서만 대기해야 합니다. 보안 그룹에 1521, 11521, 8080을 추가하지 않습니다. sshd의 GatewayPorts도 변경할 필요 없습니다.

## 앱 설정 (터널 확인 후)

compose.pc-oracle.yaml 및 Caddyfile.pc-oracle을 EC2 ~/portal에 복사합니다. 이 Compose는 Linux EC2 전용 독립 파일이며 기존 compose.yaml/compose.oracle.yaml과 병합하지 않습니다. 두 컨테이너는 호스트 네트워크를 사용합니다. 앱은 127.0.0.1:8080에만 바인딩하며 Caddy만 외부 웹 접속을 받습니다.

PC의 config/application-oracle-local.properties를 EC2 ~/portal/application-oracle-local.properties로 SCP 전송합니다. 이 파일은 DB 비밀번호를 포함하므로 출력하거나 채팅에 붙이지 않습니다. EC2에서 sudo chmod 600 application-oracle-local.properties 및 sudo chown 10001:10001 application-oracle-local.properties를 실행하여 앱 컨테이너의 UID가 읽도록 설정합니다. 기존 설정을 읽기 전용으로 마운트하고 접속 URL만 환경변수로 터널 주소로 덮어씁니다. 부트스트랩은 꺼져 있습니다.

EC2의 .env.pc-oracle에는 PORTAL_IMAGE 및 PORTAL_HOSTNAME만 설정하면 됩니다. ORACLE_SERVICE_NAME은 기본 orcl입니다. 실제 DB 비밀번호는 환경변수 파일에 복사할 필요가 없습니다. 파일 권한은 chmod 600으로 제한합니다.

PORTAL_HOSTNAME은 EC2 공인 IP를 가리키는 DNS 이름으로 확정해야 합니다. 현재 설정은 HTTPS 및 Secure 세션 쿠키를 사용하므로 HTTP IP 주소만으로 로그인 테스트하지 않습니다. 이 구성은 기존 이미지를 사용하므로 앱 소스 변경/재빌드는 필요하지 않습니다.

현재 선택한 테스트 주소는 https://3-35-3-77.sslip.io/login 입니다. DNS A 레코드가 3.35.3.77로 응답하는 것을 확인했습니다. EC2 IP가 바뀌면 터널/SCP 대상과 테스트 주소를 모두 변경해야 합니다. sslip.io는 외부 DNS 서비스입니다. 인증서는 Caddy 실행 후 EC2의 80/443 연결이 가능한 상태에서 발급됩니다. 인증서 발급과 실제 앱 접속은 별도 확인이 필요합니다.

```bash
cd ~/portal
echo 'PORTAL_HOSTNAME=3-35-3-77.sslip.io' > .env.pc-oracle
chmod 600 .env.pc-oracle
sudo docker compose --env-file .env.pc-oracle -f compose.pc-oracle.yaml config --quiet
sudo docker compose --env-file .env.pc-oracle -f compose.pc-oracle.yaml up -d
sudo docker compose --env-file .env.pc-oracle -f compose.pc-oracle.yaml ps
```

Java 힙은 128~384MB로 설정되어 있습니다. 전체 프로세스 메모리는 힙보다 큽니다. free -h로 2GB 스왑 설정도 확인합니다. 최종 검증은 앱의 Oracle 연결, 로그인 및 실제 기능 테스트로 진행합니다.

## 테스트 종료

EC2에서 sudo docker compose --env-file .env.pc-oracle -f compose.pc-oracle.yaml down으로 앱을 중지한 뒤 PC 터널에서 Ctrl+C를 누릅니다. EC2를 중지해도 디스크 등의 비용은 남습니다. PC DB에 이미 저장된 테스트 내역은 그대로 유지됩니다.

## 참고

- https://man.openbsd.org/ssh (-R, -N)
- https://docs.docker.com/engine/network/drivers/host/
- https://sslip.io/
- https://caddyserver.com/docs/automatic-https
