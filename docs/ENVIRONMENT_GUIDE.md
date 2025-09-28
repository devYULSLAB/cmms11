# CMMS11 환경별 실행 가이드

## 환경 구성

### 개발 환경 (Windows)
- **OS**: Windows 10/11
- **Java**: OpenJDK 17 이상
- **데이터베이스**: MariaDB (localhost:3306)
- **프로필**: dev

### 프로덕션 환경 (Ubuntu)
- **OS**: Ubuntu 20.04 LTS 이상
- **Java**: OpenJDK 17 이상
- **데이터베이스**: MariaDB (localhost:3306)
- **프로필**: prod

## 설정 파일 구조

```
src/main/resources/
├── application.yml              # 기본 설정 (dev 프로필 활성화)
├── application-dev.yml          # 개발 환경 설정
├── application-prod.yml         # 프로덕션 환경 설정
├── database-dev.properties      # 개발 DB 계정
└── database-prod.properties     # 프로덕션 DB 계정
```

## 개발 환경 실행 (Windows)

### 1. 데이터베이스 설정
```sql
-- 개발용 데이터베이스 생성
CREATE DATABASE cmms11 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cmms11_dev'@'localhost' IDENTIFIED BY 'cmms';
GRANT ALL PRIVILEGES ON cmms11.* TO 'cmms11_dev'@'localhost';
FLUSH PRIVILEGES;
```

### 2. 애플리케이션 실행
```cmd
# 스크립트 사용
scripts\start-dev.bat

# 또는 직접 실행
gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

### 3. 애플리케이션 중지
```cmd
scripts\stop-dev.bat
```

## 프로덕션 환경 실행 (Ubuntu)

### 1. 서버 준비
```bash
# Java 설치
sudo apt update
sudo apt install openjdk-17-jdk

# MariaDB 설치
sudo apt install mariadb-server mariadb-client

# 방화벽 설정
sudo ufw allow 8080/tcp
sudo ufw enable
```

### 2. 데이터베이스 설정
```sql
-- 프로덕션 데이터베이스 생성
CREATE DATABASE cmms11 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cmms11_prod'@'localhost' IDENTIFIED BY '강력한_비밀번호';
GRANT ALL PRIVILEGES ON cmms11.* TO 'cmms11_prod'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 환경 변수 설정
```bash
# 환경 변수 설정
export DB_PASSWORD="강력한_비밀번호"
export AWS_S3_BUCKET="prodYULSLAB-bucket"   //지금은 사용 안 함 

# 또는 /etc/environment에 영구 설정
sudo nano /etc/environment
```

### 4. 애플리케이션 실행
```bash
# 스크립트 실행 권한 부여
chmod +x scripts/start-prod.sh scripts/stop-prod.sh

# 애플리케이션 시작
./scripts/start-prod.sh
```

### 5. 애플리케이션 중지
```bash
./scripts/stop-prod.sh
```

## 환경별 주요 차이점

| 구분 | 개발 환경 | 프로덕션 환경 |
|------|-----------|---------------|
| 프로필 | dev | prod |
| DDL 모드 | update | validate |
| Flyway | 비활성화 | 활성화 |
| 로깅 레벨 | DEBUG | INFO |
| 파일 저장소 | storage/uploads | /opt/cmms11/storage/uploads |
| 로그 파일 | 콘솔 | /opt/cmms11/logs/cmms11.log |
| 컨텍스트 패스 | /cmms11 | /cmms11 |
| JVM 옵션 | -Xms512m -Xmx1024m | -Xms1g -Xmx2g |

## 문제 해결

### 개발 환경 문제
- **포트 충돌**: `netstat -ano | findstr :8080`으로 포트 사용 확인
- **Java 버전**: `java -version`으로 Java 17 이상 확인
- **데이터베이스 연결**: MariaDB 서비스 실행 상태 확인

### 프로덕션 환경 문제
- **서비스 상태**: `sudo systemctl status cmms11`
- **로그 확인**: `sudo journalctl -u cmms11 -f`
- **권한 문제**: `/opt/cmms11` 디렉토리 권한 확인
- **메모리 부족**: `free -h`로 메모리 사용량 확인

## 보안 고려사항

1. **데이터베이스 비밀번호**: 강력한 비밀번호 사용
2. **환경 변수**: 민감한 정보는 환경 변수로 관리
3. **방화벽**: 필요한 포트만 개방
4. **SSL/TLS**: 프로덕션 환경에서는 HTTPS 사용 권장
5. **백업**: 정기적인 데이터베이스 및 파일 백업

자세한 운영 서버 환경 점검 사항은 `docs/PRODUCTION_CHECKLIST.md`를 참조하세요.
