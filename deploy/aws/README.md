# AWS 외부 접속 테스트 준비

현재 상태: 배포 파일 준비. AWS 인증·서버·DB 방식·접속 도메인 확정 전이며 AWS 리소스를 생성하지 않았습니다.

## 구성

인터넷 → AWS Linux 서버 Caddy(HTTPS 443) → portal(내부 8080) → 선택한 DB.
PC를 꺼도 AWS에서 독립 실행되는 구성입니다. 로컬 compose.yaml과 Oracle 설정 파일은 사용하지 않습니다.

- compose.yaml: 앱 + HTTPS 프록시. DB 설정 파일 하나를 함께 지정해야 합니다.
- compose.demo.yaml: 새 H2 파일 DB. 로컬 Oracle 데이터를 옮기지 않는 기능 테스트용입니다. Oracle 호환성 검증을 대신하지 않습니다. 최초 실행 시 관리자 1명만 생성됩니다.
- compose.oracle.yaml: 별도 Oracle DB 연결. 스키마와 마이그레이션이 먼저 적용되어야 합니다.
- Dockerfile: 현재 앱 이미지에 테스트 DB용 쓰기 가능한 디렉터리를 추가합니다.

## 확정할 내용

1. AWS 계정 접근 및 기존 서버 유무. 계정 비밀번호·액세스 키는 채팅에 붙여 넣지 말고 AWS 콘솔/CLI 로그인으로 설정합니다.
2. 리전과 테스트 비용 한도.
3. 별도 더미 데이터로 테스트할지, 기존 Oracle도 이전할지 선택합니다.
4. 사용할 도메인 또는 서브도메인. DNS A 레코드가 서버 공인 IP를 가리켜야 합니다. 도메인이 없으면 테스트 주소와 HTTPS 방식을 먼저 정합니다.

## 이미지 준비

AWS 서버 구성이 확정되면 프로젝트 루트 PowerShell에서:

```powershell
docker compose build portal
docker build -f deploy/aws/Dockerfile -t portal:aws-test .
docker save -o deploy/aws/portal-aws-test.tar portal:aws-test
```

Linux x86_64 서버 기준입니다. ARM 서버에는 해당 아키텍처 이미지를 별도로 빌드합니다.
deploy/aws의 배포 파일과 이미지 tar를 서버에 전송합니다. 로컬 config·계정 비밀번호 파일·Oracle 자료를 자동으로 포함하지 않습니다.

## 서버에서 실행

Lightsail 또는 EC2의 Ubuntu에 Docker Engine + Compose 플러그인을 설치합니다. AWS 방화벽에서 80/443을 열고 SSH는 관리 접속으로 제한합니다. 앱 8080과 DB 포트는 인터넷에 공개하지 않습니다.

```sh
docker load -i portal-aws-test.tar
cp .env.example .env
chmod 600 .env
```

.env의 도메인을 실제 값으로 바꾸고 선택한 DB 방식에 필요한 값만 채웁니다. 공개 테스트 관리자에는 새 비밀번호를 사용합니다.

새 테스트 DB:

```sh
docker compose -f compose.yaml -f compose.demo.yaml config --quiet
docker compose -f compose.yaml -f compose.demo.yaml up -d
```

별도 Oracle DB:

```sh
docker compose -f compose.yaml -f compose.oracle.yaml config --quiet
docker compose -f compose.yaml -f compose.oracle.yaml up -d
```

DNS·80/443 연결이 준비되면 Caddy가 인증서를 발급합니다. https://실제도메인/login에서 확인합니다.
새 DB의 최초 관리자 ID는 admin, 비밀번호는 .env의 PORTAL_ADMIN_PASSWORD입니다. 기존 데이터가 있으면 초기 계정 설정을 다시 적용하지 않습니다.

## 검증 및 종료

- HTTPS → 관리자 로그인 → 테스트 직원 등록 → 권한별 메뉴 → 신청·승인·파일 첨부 → 재시작 후 자료 유지 확인.
- 상태 확인: 위와 동일한 compose 파일 조합으로 `ps` 또는 `logs --tail 100` 실행.
- AWS에서 host.docker.internal로 현재 PC의 Oracle에 연결할 수는 없습니다. DB를 AWS에 준비하거나 별도의 사설 연결 구성이 필요합니다.
- `down`은 컨테이너를 중지하고 볼륨을 보존합니다. `down -v`는 DB도 삭제하므로 사용하지 않습니다.
- 컨테이너를 꺼도 AWS 리소스 요금은 계속될 수 있습니다. 테스트 종료 시 백업과 AWS 리소스 정리를 별도로 확인합니다.

## 공식 참고 자료

- [Lightsail 요금](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-bundles.html): Linux 공인 IPv4·2GB 기본요금 월 $12 (2026-09-24 확인). 세금·추가 전송·스냅샷·도메인·별도 DB 비용 등은 별도일 수 있습니다.
- [AWS 브라우저 SSH](https://docs.aws.amazon.com/lightsail/latest/userguide/lightsail-how-to-connect-to-your-instance-virtual-private-server.html)
- [Ubuntu Docker 설치](https://docs.docker.com/engine/install/ubuntu/)
- [Caddy HTTPS 조건](https://caddyserver.com/docs/quick-starts/https)
