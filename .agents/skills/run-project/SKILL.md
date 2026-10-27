---
name: run-project
description: "Khởi động toàn bộ môi trường chạy project VSAT Compass: Docker Desktop, PostgreSQL (vsat-pg), Spring Boot API (vsat-api), Android Emulator (Pixel_10_Pro), thiết lập adb reverse port 18080, mở ứng dụng mobile và khởi chạy Web Admin (React Vite port 5173)."
---

# Skill Chạy Toàn Bộ Project VSAT Compass

Sử dụng skill này khi cần khởi động hoặc kiểm tra trạng thái toàn bộ môi trường chạy dự án VSAT Compass, bao gồm:
1. Cơ sở dữ liệu PostgreSQL (`vsat-pg` - port 5432).
2. Backend Spring Boot API (`vsat-api` - port 18080).
3. Máy ảo Android (`Pixel_10_Pro` - adb reverse 18080).
4. Web Admin Quản trị Học thuật V-SAT (`web-admin` - port 5173).

## Quy trình khởi động tự động

Chạy script PowerShell tại thư mục gốc project:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-project.ps1
```

Để dừng toàn bộ hệ thống (dừng Web Admin, tắt máy ảo Android, và dừng container Docker):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\stop-project.ps1
```

---

## Chi tiết các thành phần trong hệ thống

### 1. Web Admin Portal (React 19 + Vite)
- **URL**: `http://localhost:5173`
- **Thư mục mã nguồn**: `web-admin/`
- **Khởi động riêng**:
  ```powershell
  cd web-admin
  npm run dev
  ```

### 2. Docker & Backend Services
- **Docker Desktop**: Tự khởi động ngầm nếu chưa chạy.
- **PostgreSQL Database** (`vsat-pg`): Port host `5432`.
- **Spring Boot Backend** (`vsat-api`): Port host `18080` (map vào container port `8080`).
- **Health Check**: `http://localhost:18080/api/v1/actuator/health`

### 3. Android Emulator & ADB Reverse
- **Máy ảo**: `Pixel_10_Pro`
- **Reverse Port 18080**:
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:18080 tcp:18080
  ```
- **Khởi động App**: `com.example.v_sat_compass/.ui.SplashActivity`

---

## Thông tin tài khoản đăng nhập mẫu

| Role | Email | Password |
|---|---|---|
| Student | `student@vsat.com` | `Passw0rd123` |
| Admin | `admin@vsat.com` | `Admin@123` |
