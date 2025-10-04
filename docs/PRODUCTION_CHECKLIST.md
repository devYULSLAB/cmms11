# CMMS11 운영 서버 배포 가이드

## 🚀 빠른 배포 가이드

### 1. 서버 환경 준비
```bash
# 시스템 요구사항: Ubuntu 22.04+, Java 21+, 4GB RAM, 20GB 디스크
sudo apt update
sudo apt install openjdk-21-jdk mariadb-server mariadb-client
# sudo ufw allow 8080/tcp && sudo ufw allow 3306/tcp && sudo ufw enable
```

### 2. 데이터베이스 설정
```bash
# MariaDB 설정
sudo mysql_secure_installation
sudo mysql -e "CREATE DATABASE cmms11 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER 'cmms11_prod'@'localhost' IDENTIFIED BY '강력한_비밀번호';" //운영 기준 
sudo mysql -e "GRANT ALL PRIVILEGES ON cmms11.* TO 'cmms11_prod'@'localhost';"
```

### 3. 애플리케이션 배포
```bash
# 디렉토리 구조 생성
sudo mkdir -p /opt/cmms11/{storage/uploads,logs,scripts}
sudo chown -R $USER:$USER /opt/cmms11

# JAR 파일 빌드 및 배포
./gradlew bootJar --no-daemon
scp build/libs/cmms11-*.jar user@server:/opt/cmms11/

# 설정 파일 배치
sudo cp application-prod.yml /opt/cmms11/application.yml
sudo chown $USER:$USER /opt/cmms11/application.yml
```

### 4. 서비스 시작
```bash
# systemd 서비스 설정
sudo tee /etc/systemd/system/cmms11.service > /dev/null <<EOF
[Unit]
Description=CMMS11 Application
After=network.target mariadb.service

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/cmms11
ExecStart=/usr/bin/java -Xms1g -Xmx2g -Dfile.encoding=UTF-8 -jar /opt/cmms11/cmms11-*.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 서비스 시작
sudo systemctl daemon-reload
sudo systemctl enable cmms11
sudo systemctl start cmms11
sudo systemctl status cmms11
```

## 📋 상세 설정 가이드

### 1. 데이터베이스 백업 설정
```bash
# 자동 백업 설정
sudo mkdir -p /opt/backups/cmms11
sudo chown $USER:$USER /opt/backups/cmms11

# crontab에 백업 작업 추가 (매일 새벽 2시)
echo "0 2 * * * mysqldump -u cmms11_prod -p'비밀번호' cmms11 > /opt/backups/cmms11/cmms11_\$(date +\%Y\%m\%d).sql" | crontab -
```

### 2. 보안 설정
```bash
# SSL 인증서 설정 (Let's Encrypt)
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com

# 방화벽 설정 (필요시)
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw enable
```

### 3. 모니터링 설정
```bash
# 로그 로테이션 설정
sudo tee /etc/logrotate.d/cmms11 > /dev/null <<EOF
/opt/cmms11/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 $USER $USER
    postrotate
        systemctl reload cmms11
    endscript
}
EOF

# 시스템 모니터링 도구 설치
sudo apt install htop
```

### 4. 성능 최적화
```bash
# JVM 튜닝 (application.yml에서 설정)
JAVA_OPTS="-Xms1g -Xmx2g -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"

# MariaDB 최적화
sudo tee -a /etc/mysql/mariadb.conf.d/50-server.cnf > /dev/null <<EOF
[mysqld]
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
max_connections = 200
query_cache_size = 64M
EOF
sudo systemctl restart mariadb
```

## 🔧 관리 명령어

### 서비스 관리
```bash
# 서비스 시작/중지/재시작
sudo systemctl start cmms11
sudo systemctl stop cmms11
sudo systemctl restart cmms11
sudo systemctl status cmms11

# 로그 확인
sudo journalctl -u cmms11 -f
tail -f /opt/cmms11/logs/app.out
```

### 배포 업데이트
```bash
# 1. JAR 파일 빌드 및 배포
./gradlew bootJar --no-daemon
scp build/libs/cmms11-*.jar user@server:/opt/cmms11/

# 2. 서비스 재시작
sudo systemctl restart cmms11

# 3. 상태 확인
sudo systemctl status cmms11
curl http://localhost:8080/health
```

### 장애 대응
```bash
# 프로세스 확인
ps aux | grep java
sudo netstat -tlnp | grep :8080

# 로그 분석
sudo journalctl -u cmms11 --since "1 hour ago"
tail -f /opt/cmms11/logs/app.out

# 긴급 복구
sudo systemctl stop cmms11
mysql -u cmms11_prod -p cmms11 < /opt/backups/cmms11/backup.sql
sudo systemctl start cmms11
```

## ✅ 배포 체크리스트

### 배포 전 확인사항
- [ ] JAR 파일 빌드 완료 (`./gradlew bootJar --no-daemon`)
- [ ] application-prod.yml 설정 파일 준비
- [ ] 데이터베이스 연결 테스트
- [ ] 방화벽 설정 확인
- [ ] SSL 인증서 유효성 확인

### 배포 후 확인사항
- [ ] 애플리케이션 정상 시작 확인 (`sudo systemctl status cmms11`)
- [ ] 웹 페이지 접근 테스트 (`curl http://localhost:8080/health`)
- [ ] 데이터베이스 연결 테스트
- [ ] 파일 업로드/다운로드 테스트
- [ ] 로그 파일 생성 확인 (`tail -f /opt/cmms11/logs/app.out`)

## 📁 디렉토리 구조

```
/opt/cmms11/
├── *.jar                    # JAR 파일들 (와일드카드)
├── application.yml          # 설정 파일 (자동 발견)
├── logs/                    # 로그 디렉토리
│   └── app.out             # 로그 파일
├── scripts/                # 실행 스크립트들
├── storage/                # 파일 저장소
│   └── uploads/            # 업로드 파일들
└── backups/                # 백업 파일들
```

## 🔗 관련 스크립트

- **start-dev.sh**: 개발 환경 JAR 실행 (`/opt/cmms11/` 기준)
- **stop-dev.sh**: 개발 환경 프로세스 중지
- **start-prod.sh**: 운영 환경 JAR 실행 (`/opt/cmms11/` 기준)
- **PRODUCTION_CHECKLIST.md**: 운영 배포 가이드

모든 스크립트는 `/opt/cmms11/` 기준으로 일관된 구조를 사용합니다.
