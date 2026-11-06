-- V13: Create universities and admission_benchmarks tables with seed data (P2 - Issue #15)

CREATE TABLE IF NOT EXISTS universities (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(50),
    website VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS admission_benchmarks (
    id BIGSERIAL PRIMARY KEY,
    university_id BIGINT NOT NULL REFERENCES universities(id) ON DELETE CASCADE,
    major_code VARCHAR(50) NOT NULL,
    major_name VARCHAR(255) NOT NULL,
    subject_combo VARCHAR(50) NOT NULL,
    year INTEGER NOT NULL,
    cutoff_score NUMERIC(6,2) NOT NULL,
    notes TEXT,
    CONSTRAINT uq_admission_benchmark UNIQUE (university_id, major_code, subject_combo, year)
);

CREATE INDEX IF NOT EXISTS idx_admission_benchmarks_combo_year ON admission_benchmarks(subject_combo, year);

-- Seed universities
INSERT INTO universities (code, name, region, website) VALUES
('HUB', 'Trường Đại học Ngân hàng TP.HCM', 'Miền Nam', 'https://hub.edu.vn'),
('UEL', 'Trường Đại học Kinh tế - Luật ĐHQG-HCM', 'Miền Nam', 'https://uel.edu.vn'),
('CTU', 'Trường Đại học Cần Thơ', 'Miền Nam', 'https://ctu.edu.vn'),
('UFM', 'Trường Đại học Tài chính - Marketing', 'Miền Nam', 'https://ufm.edu.vn'),
('HCMUS', 'Trường Đại học Khoa học Tự nhiên ĐHQG-HCM', 'Miền Nam', 'https://hcmus.edu.vn'),
('SGU', 'Trường Đại học Sài Gòn', 'Miền Nam', 'https://sgu.edu.vn')
ON CONFLICT (code) DO NOTHING;

-- Seed admission benchmarks (V-SAT scale 450 total for 3 subjects)
-- HUB (ĐH Ngân hàng TP.HCM)
INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340201', 'Tài chính - Ngân hàng', 'A00', 2024, 315.00, 'Xét tuyển dựa trên kết quả thi V-SAT 2024'
FROM universities u WHERE u.code = 'HUB'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340201', 'Tài chính - Ngân hàng', 'A01', 2024, 320.00, 'Xét tuyển dựa trên kết quả thi V-SAT 2024'
FROM universities u WHERE u.code = 'HUB'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340201', 'Tài chính - Ngân hàng', 'D01', 2024, 318.00, 'Xét tuyển dựa trên kết quả thi V-SAT 2024'
FROM universities u WHERE u.code = 'HUB'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340101', 'Quản trị kinh doanh', 'A00', 2024, 310.00, 'Xét tuyển V-SAT'
FROM universities u WHERE u.code = 'HUB'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7480201', 'Công nghệ thông tin', 'A00', 2024, 325.00, 'Xét tuyển V-SAT'
FROM universities u WHERE u.code = 'HUB'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7480201', 'Công nghệ thông tin', 'A01', 2024, 328.00, 'Xét tuyển V-SAT'
FROM universities u WHERE u.code = 'HUB'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

-- UEL (ĐH Kinh tế - Luật ĐHQG-HCM)
INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340120', 'Kinh doanh quốc tế', 'A01', 2024, 350.00, 'Xét tuyển dựa trên kết quả thi V-SAT'
FROM universities u WHERE u.code = 'UEL'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340120', 'Kinh doanh quốc tế', 'D01', 2024, 348.00, 'Xét tuyển dựa trên kết quả thi V-SAT'
FROM universities u WHERE u.code = 'UEL'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7380101', 'Luật kinh tế', 'A00', 2024, 335.00, 'Xét tuyển dựa trên kết quả thi V-SAT'
FROM universities u WHERE u.code = 'UEL'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7380101', 'Luật kinh tế', 'D01', 2024, 332.00, 'Xét tuyển dựa trên kết quả thi V-SAT'
FROM universities u WHERE u.code = 'UEL'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

-- CTU (ĐH Cần Thơ)
INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7480201', 'Công nghệ thông tin', 'A00', 2024, 310.00, 'Điểm chuẩn V-SAT'
FROM universities u WHERE u.code = 'CTU'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7480201', 'Công nghệ thông tin', 'A01', 2024, 312.00, 'Điểm chuẩn V-SAT'
FROM universities u WHERE u.code = 'CTU'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7620105', 'Cây trồng / Nông nghiệp', 'B00', 2024, 275.00, 'Điểm chuẩn V-SAT'
FROM universities u WHERE u.code = 'CTU'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7220201', 'Ngôn ngữ Anh', 'D01', 2024, 295.00, 'Môn Anh tính hệ số 1'
FROM universities u WHERE u.code = 'CTU'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

-- UFM (ĐH Tài chính - Marketing)
INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340115', 'Marketing', 'A00', 2024, 320.00, 'Điểm chuẩn xét tuyển V-SAT'
FROM universities u WHERE u.code = 'UFM'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340115', 'Marketing', 'A01', 2024, 322.00, 'Điểm chuẩn xét tuyển V-SAT'
FROM universities u WHERE u.code = 'UFM'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340115', 'Marketing', 'D01', 2024, 325.00, 'Điểm chuẩn xét tuyển V-SAT'
FROM universities u WHERE u.code = 'UFM'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7340301', 'Kế toán', 'A00', 2024, 305.00, 'Điểm chuẩn xét tuyển V-SAT'
FROM universities u WHERE u.code = 'UFM'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

-- HCMUS (ĐH Khoa học Tự nhiên ĐHQG-HCM)
INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7480101', 'Khoa học máy tính', 'A00', 2024, 365.00, 'Xét tuyển dựa trên kết quả thi V-SAT'
FROM universities u WHERE u.code = 'HCMUS'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7480101', 'Khoa học máy tính', 'A01', 2024, 368.00, 'Xét tuyển dựa trên kết quả thi V-SAT'
FROM universities u WHERE u.code = 'HCMUS'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7420201', 'Công nghệ sinh học', 'B00', 2024, 320.00, 'Xét tuyển dựa trên kết quả thi V-SAT'
FROM universities u WHERE u.code = 'HCMUS'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

-- SGU (ĐH Sài Gòn)
INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7140217', 'Sư phạm Ngữ văn', 'C00', 2024, 340.00, 'Xét tuyển V-SAT ngành Sư phạm'
FROM universities u WHERE u.code = 'SGU'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7140209', 'Sư phạm Toán học', 'A00', 2024, 355.00, 'Xét tuyển V-SAT ngành Sư phạm'
FROM universities u WHERE u.code = 'SGU'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;

INSERT INTO admission_benchmarks (university_id, major_code, major_name, subject_combo, year, cutoff_score, notes)
SELECT u.id, '7220201', 'Ngôn ngữ Anh', 'D01', 2024, 330.00, 'Xét tuyển V-SAT'
FROM universities u WHERE u.code = 'SGU'
ON CONFLICT (university_id, major_code, subject_combo, year) DO NOTHING;
