# Hướng dẫn cấu hình UTF-8 Encoding

## Vấn đề
Khi chạy server trên Windows VPS, các ký tự tiếng Việt có thể hiển thị không đúng do vấn đề encoding UTF-8.

## Giải pháp

### 1. Cấu hình NetBeans
- Mở NetBeans
- Vào `Tools` → `Options` → `Editor` → `Formatting`
- Chọn `Java` → `Encoding` → Chọn `UTF-8`
- Vào `Tools` → `Options` → `Miscellaneous` → `Files`
- Đặt `Encoding` thành `UTF-8`

### 2. Cấu hình Gradle (Đã được cập nhật)
File `build.gradle` và `gradle.properties` đã được cấu hình để sử dụng UTF-8 encoding.

### 3. Chạy server trên Windows

#### Cách 1: Sử dụng Batch Script
```batch
run-server-utf8.bat
```

#### Cách 2: Sử dụng PowerShell Script
```powershell
.\run-server-utf8.ps1
```

#### Cách 3: Chạy thủ công
```batch
chcp 65001
java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar build/libs/nrotuonglai-1.0.0.jar
```

### 4. Cấu hình Windows Console
- Mở Command Prompt hoặc PowerShell
- Chạy lệnh: `chcp 65001` để đặt console thành UTF-8
- Hoặc sử dụng Windows Terminal (khuyến nghị)

### 5. Kiểm tra encoding file
```bash
file -bi src/Dragon/services/tutien/TutienCombineService.java
```
Kết quả phải là: `text/x-java; charset=utf-8`

## Lưu ý
- Luôn sử dụng UTF-8 encoding khi lưu file Java
- Đảm bảo NetBeans được cấu hình đúng encoding
- Sử dụng script được cung cấp để chạy server trên Windows
- Kiểm tra console encoding trước khi chạy ứng dụng
