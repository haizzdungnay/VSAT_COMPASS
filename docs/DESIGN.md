---
colors:
  brand:
    primary: "#4A3ABA"
    primary_dark: "#352A8A"
    primary_light: "#7C6BD9"
    accent: "#6C5CE7"
  gradient:
    start: "#4A3ABA"
    end: "#7C5BF0"
    angle: 135
  text:
    primary: "#1A1A2E"
    secondary: "#757575"
    hint: "#B0B0B0"
    on_primary: "#FFFFFF"
  background:
    default: "#F5F5FA"
    card: "#FFFFFF"
    surface: "#FFFFFF"
  semantic:
    error: "#D32F2F"
    success: "#27AE60"
    warning: "#F39C12"
    info: "#2196F3"
  exam_states:
    answered: "#27AE60"
    unanswered: "#B0B0B0"
    current: "#4A3ABA"
    bookmarked: "#F39C12"
    selected_bg: "#E8E3FF"
    correct_bg: "#D5F5E3"
    wrong_bg: "#FDE8E8"
  navigation:
    selected: "#4A3ABA"
    unselected: "#9E9E9E"
  progress:
    green: "#27AE60"
    orange: "#F39C12"
    red: "#E74C3C"
    blue: "#3498DB"
    purple: "#4A3ABA"
    track: "#E0E0E0"

typography:
  scale:
    display: { size: "24sp", weight: "bold" }
    headline: { size: "20sp", weight: "bold" }
    title: { size: "17sp", weight: "bold" }
    body: { size: "16sp", weight: "normal" }
    caption: { size: "14sp", weight: "normal" }
    label: { size: "12sp", weight: "normal" }
  font_family: "sans-serif"
  system: "Material3"

spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "20dp"
  xl: "24dp"
  xxl: "32dp"

radii:
  badge: "8dp"
  chip: "20dp"
  option: "12dp"
  button: "24dp"
  card: "16dp"
  progress: "4dp"

sizes:
  icon_sm: "20dp"
  icon_md: "24dp"
  icon_lg: "36dp"
  avatar: "36dp"
  answer_badge: "40dp"
  progress_ring_thickness: "8dp"
  border_thin: "1dp"
  border_medium: "1.5dp"
  border_thick: "2dp"

components:
  button_primary:
    shape: "rounded_rect"
    radius: "24dp"
    fill: "$colors.brand.primary"
    text_color: "$colors.text.on_primary"
  button_outline:
    shape: "rounded_rect"
    radius: "24dp"
    fill: "transparent"
    border: "1.5dp $colors.brand.primary"
    text_color: "$colors.brand.primary"
  chip_selected:
    shape: "rounded_rect"
    radius: "20dp"
    fill: "$colors.brand.primary"
    text_color: "$colors.text.on_primary"
  chip_unselected:
    shape: "rounded_rect"
    radius: "20dp"
    fill: "transparent"
    border: "1dp $colors.text.secondary"
    text_color: "$colors.text.secondary"
  card:
    shape: "rounded_rect"
    radius: "16dp"
    fill: "$colors.background.card"
    elevation: "2dp"
  gradient_card:
    shape: "rounded_rect"
    radius: "16dp"
    fill: "gradient($colors.gradient.start, $colors.gradient.end, $colors.gradient.angle)"
  progress_bar:
    height: "4dp"
    radius: "4dp"
    track: "$colors.progress.track"
  answer_indicator:
    shape: "oval"
    size: "40dp"
    states:
      answered: { fill: "$colors.exam_states.answered" }
      unanswered: { fill: "$colors.exam_states.unanswered" }
      current: { fill: "$colors.exam_states.selected_bg", border: "2dp $colors.exam_states.current" }
  option_row:
    shape: "rounded_rect"
    radius: "12dp"
    states:
      default: { fill: "transparent", border: "0dp" }
      selected: { fill: "$colors.exam_states.selected_bg", border: "2dp $colors.brand.primary" }
      correct: { fill: "$colors.exam_states.correct_bg", border: "2dp $colors.semantic.success" }
      wrong: { fill: "$colors.exam_states.wrong_bg", border: "2dp $colors.semantic.error" }
  timer_badge:
    shape: "rounded_rect"
    radius: "8dp"
    states:
      normal: { fill: "$colors.semantic.warning" }
      urgent: { fill: "$colors.semantic.error" }
  nav_bar:
    selected_color: "$colors.navigation.selected"
    unselected_color: "$colors.navigation.unselected"

theme:
  base: "Theme.Material3.DayNight.NoActionBar"
  version: "Material3"
  dark_mode: true
  language: "vi"
  platform: "Android"
  min_sdk: 28
---

# VSAT Compass Design System

## Brand Identity

**VSAT Compass** là ứng dụng thi thử và luyện tập cho học sinh. Identity dựa trên màu **tím/indigo chuyên nghiệp** (#4A3ABA) — truyền đạt uy tín học thuật và sự tập trung.

## Design Philosophy

**Nguyên tắc cốt lõi:**
- **Clarity first:** Học sinh cần thấy trạng thái câu hỏi ngay lập tức — dùng màu sắc semantic nhất quán, không dùng icon đơn độc
- **Hierarchy through color:** Gradient header và card nổi bật tạo visual depth mà không cần shadow phức tạp
- **State visibility:** Mỗi trạng thái đáp án (answered/unanswered/current/bookmarked) có màu riêng biệt, không bao giờ dùng chung màu cho 2 trạng thái khác nhau

## Color Usage Rules

### Brand Colors
- `primary` (#4A3ABA): CTA buttons, active states, current item indicators
- `primary_light` (#7C6BD9): Gradient end, secondary accents, hover states
- `accent` (#6C5CE7): Bổ sung cho interactive elements khi cần tách biệt khỏi primary

### Semantic Colors — không dùng lẫn lộn
| Color | Hex | Chỉ dùng cho |
|-------|-----|--------------|
| Success green | #27AE60 | Đã trả lời, đáp án đúng, hoàn thành |
| Warning orange | #F39C12 | Bookmark, cảnh báo thời gian, badge thông báo |
| Error red | #D32F2F | Lỗi, đáp án sai, hết giờ |
| Info blue | #2196F3 | Thông tin trung lập, không liên quan đúng/sai |

### Background Hierarchy
```
Page bg   →  #F5F5FA  (tinted, phân biệt với card)
Card bg   →  #FFFFFF  (pure white, nổi lên trên page)
Surface   →  #FFFFFF  (dialogs, bottom sheets)
```

## Typography Guidelines

Dùng Material 3 text appearance attributes — không hardcode textSize trừ khi cần override:

```xml
<!-- ✅ Đúng -->
style="@style/TextAppearance.Material3.HeadlineMedium"

<!-- ⚠️ Chấp nhận khi cần custom size -->
android:textSize="20sp"
android:textStyle="bold"
```

**Size scale cần nhớ:**
- Tên thi, tiêu đề chính: 20sp bold
- Label form, nút: 16sp
- Caption, hint: 14sp hoặc 12sp

## Spacing Conventions

Padding/margin theo bội số của 4dp. Các giá trị chuẩn:
- **md (16dp):** Horizontal screen padding, item internal padding
- **xl (24dp):** Section spacing, card padding
- **xxl (32dp):** Top/bottom section separators

## Component Application

### Exam Screen Patterns
Màn hình thi có 3 vùng cần thiết kế nhất quán:
1. **Header:** `bg_gradient_header` (full-width, no radius)
2. **Answer grid:** `bg_question_*` ovals (40dp, oval shape, state-based color)
3. **Option rows:** `bg_option_*` (12dp radius, state-based fill + border)

### Button Hierarchy
```
Primary action  →  bg_button_primary    (filled, primary color)
Secondary       →  bg_button_outline    (outline, primary color)
On dark bg      →  bg_button_outline_white (outline, white)
```

## Drawables Reference

All drawables located in `app/src/main/res/drawable/`:

| Drawable | Dùng cho |
|----------|---------|
| `bg_button_primary` | Nút hành động chính |
| `bg_button_outline` | Nút phụ trên nền sáng |
| `bg_button_outline_white` | Nút phụ trên gradient/nền tối |
| `bg_gradient_card` | Card nổi bật (stats, featured) |
| `bg_gradient_header` | Header activity |
| `bg_chip_selected` | Chip đang được chọn |
| `bg_chip_unselected` | Chip chưa chọn |
| `bg_question_answered` | Ô câu hỏi đã trả lời |
| `bg_question_current` | Ô câu hỏi đang xem |
| `bg_question_unanswered` | Ô câu hỏi chưa trả lời |
| `bg_option_selected` | Đáp án đang chọn (chưa nộp) |
| `bg_option_default` | Đáp án bình thường |
| `progress_green/orange/purple` | Thanh tiến độ theo ngữ nghĩa |
| `circular_progress` | Vòng tròn tiến độ (8dp ring) |
| `bg_timer_orange/red` | Badge đếm giờ theo mức urgency |
| `bg_badge_warning` | Badge thông báo/cảnh báo |

## Dark Mode

Dark variant overrides ở `res/values-night/themes.xml`. Khi thêm color mới:
1. Định nghĩa trong `res/values/colors.xml`
2. Thêm override tối trong `res/values-night/themes.xml` nếu cần
3. **Không** hardcode màu trực tiếp trong layout — luôn dùng `@color/` hoặc `?attr/`
