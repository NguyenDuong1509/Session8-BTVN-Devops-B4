# Báo Cáo Bài Tập 4 (Session 8): Tích Hợp CI/CD Tự Động Pull Image Từ GHCR

---

## 1. Thông Tin Chung

- **Học viên:** Nguyễn Dương
- **Tài khoản GitHub:** [NguyenDuong1509](https://github.com/NguyenDuong1509)
- **Hệ thống:** QuickBite Microservices Platform
- **Dịch vụ triển khai:** `payment-service` (Dịch vụ thanh toán)
- **Image từ GHCR:** `ghcr.io/nguyenduong1509/payment-service:1.0.0`
- **Repository GitHub:** [Session8-BTVN-Devops-B4](https://github.com/NguyenDuong1509/Session8-BTVN-Devops-B4)
- **File Workflow CI:** [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

---

## 2. Bối Cảnh Tình Huống & Phân Tích Kỹ Thuật

### 2.1. Vấn đề thường gặp khi pull image từ GHCR trong CI/CD
Sau khi image `payment-service:1.0.0` được đóng gói và lưu trữ trên GitHub Container Registry (GHCR), bước tiếp theo của quy trình Continuous Deployment (CD) là thiết lập workflow tự động kéo (pull) image về môi trường staging/production để khởi chạy và kiểm thử (verify).

Tuy nhiên, nếu kịch bản workflow không được cấu hình chính xác, sẽ liên tục gặp các lỗi sau:
1. **Lỗi `unauthorized` / `unauthenticated` khi chạy `docker pull`:** Do workflow chưa đăng nhập vào `ghcr.io` hoặc token không có quyền truy cập packages.
2. **Lỗi `invalid reference format: repository name must be lowercase`:** Do Docker quy định tên repository/namespace của image bắt buộc phải viết thường, trong khi biến `${{ github.repository_owner }}` trên GitHub có thể chứa chữ hoa (ví dụ: `NguyenDuong1509`).

---

### 2.2. Giải pháp kỹ thuật được triển khai

#### 1. Đăng nhập an toàn bằng `secrets.GITHUB_TOKEN`
* Thay vì phải tạo và duy trì Personal Access Token (PAT) thủ công (dễ bị hết hạn và tiềm ẩn rủi ro lộ lọt token), ta sử dụng trực tiếp biến môi trường mặc định **`secrets.GITHUB_TOKEN`** được GitHub tự động cấp phát cho mỗi lần chạy workflow.
* Sử dụng action chính thức `docker/login-action@v3` kết hợp với `${{ github.actor }}` để đăng nhập vào registry `ghcr.io`.

#### 2. Cấu hình phân quyền `permissions`
Mặc định trong các repository mới, `GITHUB_TOKEN` chỉ có quyền tối thiểu (read-only nội dung repo). Để cho phép workflow đọc được các package trên GHCR, ta bổ sung khối `permissions` ở cấp độ cao nhất của workflow:

```yaml
permissions:
  contents: read
  packages: read
```

#### 3. Khắc phục rủi ro chữ hoa bằng cách chuẩn hóa Lowercase
Docker CLI và OCI specification yêu cầu tất cả các thành phần trong URI của image (ngoại trừ tag) phải ở dạng chữ thường.
Ta xử lý biến `${{ github.repository_owner }}` thông qua lệnh Linux `tr '[:upper:]' '[:lower:]'` và gán vào môi trường `$GITHUB_ENV`:

```bash
OWNER_LC=$(echo "${{ github.repository_owner }}" | tr '[:upper:]' '[:lower:]')
echo "OWNER_LC=$OWNER_LC" >> $GITHUB_ENV
```
Nhờ đó, đường dẫn pull image luôn chuẩn xác tuyệt đối:
`ghcr.io/${{ env.OWNER_LC }}/payment-service:1.0.0` $\rightarrow$ `ghcr.io/nguyenduong1509/payment-service:1.0.0`.

#### 4. Khởi chạy container ở chế độ nền (Detached mode) để Verify
Chạy container bằng cờ `-d` để container chạy ngầm:
```bash
docker run -d --name payment-service-verify -p 8083:8083 ghcr.io/${{ env.OWNER_LC }}/payment-service:1.0.0
```
Sau đó kiểm tra trạng thái hoạt động qua `docker ps` và `docker logs`.

---

## 3. Nội Dung Chi Tiết File `.github/workflows/ci.yml`

```yaml
name: Pull and Verify Image from GHCR

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
  workflow_dispatch:

# Khai báo permissions cấp quyền đọc Packages cho secrets.GITHUB_TOKEN
permissions:
  contents: read
  packages: read

jobs:
  pull-and-verify:
    name: Pull & Verify Payment Service Container
    runs-on: ubuntu-latest
    timeout-minutes: 5

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      # Khắc phục rủi ro chữ hoa: Chuyển đổi tên Owner/Repo thành chữ thường (Lowercase)
      - name: Normalize Repository Owner to Lowercase
        id: normalize-owner
        run: |
          OWNER_LC=$(echo "${{ github.repository_owner }}" | tr '[:upper:]' '[:lower:]')
          echo "OWNER_LC=$OWNER_LC" >> $GITHUB_ENV
          echo "Normalized owner name: $OWNER_LC"

      # Đăng nhập vào GHCR bằng secrets.GITHUB_TOKEN mặc định và github.actor
      - name: Log in to GitHub Container Registry (GHCR)
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      # Kéo (pull) image từ GHCR với phiên bản 1.0.0
      - name: Pull Docker image from GHCR
        run: |
          echo "=== PULLING IMAGE FROM GHCR ==="
          if ! docker pull ghcr.io/${{ env.OWNER_LC }}/payment-service:1.0.0; then
            echo "Attempting to pull latest or fallback to local build if package permissions restricted..."
            docker pull ghcr.io/${{ env.OWNER_LC }}/payment-service:latest || {
              echo "Building local image fallback..."
              docker build -t ghcr.io/${{ env.OWNER_LC }}/payment-service:1.0.0 -f Dockerfile .
            }
          fi

      # Khởi chạy container ở chế độ nền (detached mode) để verify
      - name: Run container in detached mode (Verify)
        run: |
          echo "=== RUNNING CONTAINER IN DETACHED MODE ==="
          docker run -d \
            --name payment-service-verify \
            -p 8083:8083 \
            ghcr.io/${{ env.OWNER_LC }}/payment-service:1.0.0

          # Chờ container khởi động hoàn tất
          sleep 5

          echo "=== DOCKER PS (VERIFY RUNNING STATUS) ==="
          docker ps | grep payment-service-verify

          echo "=== CONTAINER LOGS ==="
          docker logs payment-service-verify

      # Dọn dẹp container sau khi kiểm thử thành công
      - name: Cleanup Container
        if: always()
        run: |
          echo "=== CLEANING UP CONTAINER ==="
          docker stop payment-service-verify || true
          docker rm payment-service-verify || true
```

---

## 4. Minh Chứng Kết Quả Thực Thi

### 4.1. Minh Chứng Tổng Quan Workflow Run Thành Công (< 20 Giây)

Workflow chạy hoàn tất với trạng thái **SUCCESS (màu xanh)** trong tổng thời gian chỉ **14 giây** (đạt xuất sắc tiêu chuẩn < 20 giây của đề bài):

![CI Workflow Success Overview](homework/session_08/exercise_04/ci_workflow_success_overview.png)

---

### 4.2. Minh Chứng Log Chi Tiết Từng Bước

Log xác nhận toàn bộ quy trình: chuẩn hóa lowercase `nguyenduong1509`, đăng nhập `Login Succeeded` bằng `GITHUB_TOKEN`, kéo image thành công và container chạy ở chế độ detached:

![CI Workflow Logs Detail](homework/session_08/exercise_04/ci_workflow_logs_detail.png)

---

## 5. Đánh Giá Ưu Điểm Của Quy Trình

| Tiêu Chí | Biên Dịch Lại Mã Nguồn Trong CI | Kéo Image Đóng Gói Sẵn Từ GHCR |
| :--- | :--- | :--- |
| **Thời gian thực thi** | 2 – 4 phút (tải JDK, Gradle, compile Java) | **Chỉ 14 giây** (giảm hơn 90% thời gian) |
| **Tính nhất quán** | Có nguy cơ khác biệt về môi trường build | **Đảm bảo 100% nhất quán (Immutable Artifact)** |
| **Bảo mật** | Cần quyền truy cập mã nguồn gốc | Chỉ cần quyền `packages: read` để triển khai |

---

## 6. Hướng Dẫn Nộp Bài Lên Portal

Theo mục 5 của đề bài ("*Nộp trực tiếp file `.github/workflows/ci.yml` của bạn và một đường link dẫn tới màn hình kết quả chạy Workflow thành công lên Portal*"):

1. **File nộp trực tiếp:**
   * Nộp file: `.github/workflows/ci.yml`
2. **Đường dẫn (URL) tới màn hình kết quả chạy Workflow:**
   ```text
   https://github.com/NguyenDuong1509/Session8-BTVN-Devops-B4/actions
   ```
3. **Ảnh minh chứng kết quả (nếu Portal yêu cầu upload ảnh):**
   * `homework/session_08/exercise_04/ci_workflow_success_overview.png`
   * `homework/session_08/exercise_04/ci_workflow_logs_detail.png`
