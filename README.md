# WorldQuant Brain Tool

Công cụ Java (JavaFX desktop) tự động xử lý alpha trên nền tảng [WorldQuant Brain](https://platform.worldquantbrain.com): kiểm tra prod correlation, đặt tên, đánh dấu favorite, gửi mail thông báo và theo dõi tiến độ để chạy tiếp sau khi bị dừng.

## 1. Yêu cầu

- Java 17+ (`java -version` để kiểm tra)
- Maven 3.6+

## 2. Cấu hình (bắt buộc trước khi chạy)

```bash
cp config.properties.example config.properties
```

Mở `config.properties` và điền:

| Nhóm | Key chính | Ý nghĩa |
|------|-----------|---------|
| Cookie | `wq.cookie` | Cookie đăng nhập WorldQuant Brain (bắt buộc) |
| Email | `smtp.*`, `email.recipient` | Gửi mail khi tìm được alpha đạt yêu cầu |
| Xử lý | `thread.pool.size` | Số luồng chạy song song (nên để 2–3) |
| Ngưỡng | `alpha.min.correlation` | Ngưỡng corr để phân loại alpha (mặc định 0.7) |
| Filter | `filter.regular.*`, `filter.super.*` | Region, khoảng ngày, limit… để lọc alpha |

**Cách lấy cookie:** đăng nhập platform.worldquantbrain.com → F12 → tab Network → copy header `Cookie` của request bất kỳ → dán vào `wq.cookie` (hoặc cập nhật trong tab **Session** của app).

## 3. Build file JAR

```bash
mvn package -q
```

JAR nằm tại: `target/worldquant-brain-tool-1.0-SNAPSHOT-desktop.jar`

> **Lưu ý macOS:** nếu project nằm trong thư mục đồng bộ iCloud (`~/Documents`, `~/Desktop`), copy project ra `/tmp` rồi build để tránh lỗi ghi file:
> ```bash
> rsync -a --exclude='target/' ./ /tmp/wq-build/
> cd /tmp/wq-build && mvn package -q
> ```

## 4. Chạy ứng dụng

### Cách 1 — Desktop app (khuyên dùng)

Đặt `config.properties` cùng thư mục với JAR, rồi:

```bash
java -jar worldquant-brain-tool-1.0-SNAPSHOT-desktop.jar
```

Các tab: **Session** (cookie, kiểm tra phiên) · **Config** (cấu hình chung, email) · **Filters** (bộ lọc alpha) · **Jobs** (chạy/dừng tiến trình) · **Logs** (log realtime) · **Results** (tiến độ đã lưu).

### Cách 2 — Chạy tiến trình trực tiếp bằng Maven

```bash
# Regular Alpha
mvn exec:java -Dexec.mainClass="demo.webapp.regular.RegularAlphaUtils"

# Super Alpha
mvn exec:java -Dexec.mainClass="demo.webapp.regular.SuperAlphaUtils"

# Regular for Gen Super
mvn exec:java -Dexec.mainClass="demo.webapp.regular.RegularAlphaForGenSuperAlphaUtils"

# Mark Failed Alphas
mvn exec:java -Dexec.mainClass="demo.webapp.regular.MarkFailedAlphasUtils"

# Xóa tiến độ cũ, chạy lại từ đầu: thêm -Dexec.args="--clear"
```

### Cách 3 — Chạy theo lịch (Quartz)

```bash
# Regular mỗi 60 phút, Super mỗi 120 phút
mvn exec:java -Dexec.mainClass="demo.webapp.scheduler.SchedulerApp" -Dexec.args="--regular 60 --super 120"

# Super theo cron (8h sáng hằng ngày)
mvn exec:java -Dexec.mainClass="demo.webapp.scheduler.SchedulerApp" -Dexec.args='--super "0 0 8 * * ?"'
```

## 5. Các tiến trình và cơ chế hoạt động

### Regular Alpha (`RegularAlphaUtils`)

Xử lý các alpha REGULAR **chưa submit**:

1. Lấy danh sách alpha theo filter (region, ngày tạo, fitness…).
2. Với mỗi alpha (chạy song song theo thread pool):
   - Đặt tên alpha = chính alpha ID.
   - Gọi API lấy **prod correlation** (retry đến khi API trả kết quả).
   - Nếu corr ≥ ngưỡng (`alpha.min.correlation`) → đánh dấu **favorite** (corr cao = trùng với alpha đã có, không nên submit).
3. Alpha có corr **thấp** là ứng viên tốt — submit thủ công qua tab Jobs (auto-submit đã tắt).

### Super Alpha (`SuperAlphaUtils`)

Quét các alpha SUPER, mục tiêu là **tìm alpha có corr thấp**:

- Kiểm tra prod correlation từng alpha như trên.
- Nếu tìm được alpha có **corr < ngưỡng** → **gửi email thông báo và dừng** toàn bộ tiến trình (đã tìm được cái cần tìm).
- Corr cao → đánh favorite rồi xử lý tiếp; hết danh sách thì lặp lại vòng mới.

### Regular for Gen Super (`RegularAlphaForGenSuperAlphaUtils`)

Chuẩn bị nguyên liệu để tạo super alpha: lấy các alpha REGULAR **đã submit** (sắp theo prod correlation) và đặt tên = alpha ID, giúp dễ chọn khi ghép super alpha trên platform.

### Mark Failed Alphas (`MarkFailedAlphasUtils`)

Quét các alpha chưa submit có check **FAIL** và đánh dấu **favorite** để dễ lọc/loại bỏ trên platform.

## 6. Cơ chế chung

- **Kiểm tra phiên:** mọi tiến trình gọi `SessionValidator` trước khi chạy — cookie hết hạn thì dừng ngay và báo lỗi.
- **Lưu tiến độ (resume):** mỗi bước hoàn thành của từng alpha được ghi vào file JSON (`progress_regular.json`, `progress_super.json`, `progress_regular_gen_super.json`). Bị crash/dừng giữa chừng → chạy lại là tiếp tục đúng chỗ cũ, bước đã làm sẽ bỏ qua. Dùng `--clear` để làm lại từ đầu.
- **Chạy song song:** dùng thread pool (`thread.pool.size`), mỗi alpha là một task độc lập.
- **Chống rate-limit:** gặp HTTP 429 sẽ tự chờ và retry (exponential backoff).
- **Dừng an toàn:** nút Stop trong app đặt cờ `JobControl` — các task đang chờ sẽ thoát, tiến độ vẫn được lưu.

## 7. Xử lý sự cố

| Vấn đề | Cách xử lý |
|--------|-----------|
| Session invalid | Đăng nhập lại, copy cookie mới, cập nhật tab Session hoặc `config.properties` |
| Bị rate limit (429) | Giảm `thread.pool.size` xuống 2–3 |
| Job không chạy | Kiểm tra session hợp lệ, filter có trả về alpha không, xem tab Logs |
| Muốn chạy lại từ đầu | Chạy với `--clear` hoặc xóa file `progress_*.json` |

## Cấu trúc mã nguồn

```
src/main/java/demo/webapp/
├── ConfigLoader.java        # Đọc config.properties + biến môi trường
├── SessionValidator.java    # Kiểm tra cookie/phiên
├── ProgressTracker.java     # Lưu & khôi phục tiến độ (JSON)
├── JobControl.java          # Cờ dừng tiến trình
├── regular/                 # 4 tiến trình chính + gửi email
├── scheduler/               # Chạy theo lịch (Quartz)
├── desktop/                 # App JavaFX (entry point: DesktopLauncher)
└── web/                     # HTTP server + REST API (legacy)
```

## License

MIT
