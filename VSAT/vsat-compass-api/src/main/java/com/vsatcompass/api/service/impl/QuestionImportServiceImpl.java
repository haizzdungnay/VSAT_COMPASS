package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.QuestionImportItemDto;
import com.vsatcompass.api.dto.response.QuestionImportResultResponse;
import com.vsatcompass.api.entity.*;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.*;
import com.vsatcompass.api.service.QuestionImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Sheet 1: Single Questions
            Sheet sheet1 = workbook.createSheet("Single Questions");
            String[] headers1 = {
                    "Subject ID/Code", "Topic ID/Code", "Subtopic ID", "Difficulty (NB/TH/VD/VDC)",
                    "Question Type", "Question Text", "Option A", "Option B", "Option C", "Option D",
                    "Correct Answer (A/B/C/D)", "Explanation", "Tags"
            };
            Row headerRow1 = sheet1.createRow(0);
            for (int i = 0; i < headers1.length; i++) {
                Cell cell = headerRow1.createCell(i);
                cell.setCellValue(headers1[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample row Sheet 1
            Row sampleRow1 = sheet1.createRow(1);
            sampleRow1.createCell(0).setCellValue("1");
            sampleRow1.createCell(1).setCellValue("1");
            sampleRow1.createCell(2).setCellValue("");
            sampleRow1.createCell(3).setCellValue("TH");
            sampleRow1.createCell(4).setCellValue("SINGLE_CHOICE");
            sampleRow1.createCell(5).setCellValue("Cho hàm số y = f(x) liên tục trên R. Hàm số đồng biến trên khoảng nào sau đây?");
            sampleRow1.createCell(6).setCellValue("(-\\infty; 0)");
            sampleRow1.createCell(7).setCellValue("(0; 2)");
            sampleRow1.createCell(8).setCellValue("(2; +\\infty)");
            sampleRow1.createCell(9).setCellValue("(-1; 1)");
            sampleRow1.createCell(10).setCellValue("B");
            sampleRow1.createCell(11).setCellValue("Dựa vào đạo hàm f'(x) > 0 trên (0; 2)");
            sampleRow1.createCell(12).setCellValue("toan,ham-so");

            for (int i = 0; i < headers1.length; i++) {
                sheet1.autoSizeColumn(i);
            }

            // Sheet 2: Question Groups
            Sheet sheet2 = workbook.createSheet("Question Groups");
            String[] headers2 = {
                    "Group Code", "Subject ID/Code", "Group Title", "Passage Text",
                    "Topic ID/Code", "Subtopic ID", "Difficulty (NB/TH/VD/VDC)", "Question Type",
                    "Question Text", "Option A", "Option B", "Option C", "Option D",
                    "Correct Answer (A/B/C/D)", "Explanation"
            };
            Row headerRow2 = sheet2.createRow(0);
            for (int i = 0; i < headers2.length; i++) {
                Cell cell = headerRow2.createCell(i);
                cell.setCellValue(headers2[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample rows Sheet 2 (chùm 2 câu)
            Row sampleGroup1 = sheet2.createRow(1);
            sampleGroup1.createCell(0).setCellValue("GRP_01");
            sampleGroup1.createCell(1).setCellValue("1");
            sampleGroup1.createCell(2).setCellValue("Đoạn trích đọc hiểu Ngữ văn");
            sampleGroup1.createCell(3).setCellValue("Mỗi cuốn sách là một bức tranh kì diệu về cuộc sống. Đọc sách giúp ta mở rộng tầm nhìn và bồi dưỡng tâm hồn...");
            sampleGroup1.createCell(4).setCellValue("1");
            sampleGroup1.createCell(5).setCellValue("");
            sampleGroup1.createCell(6).setCellValue("NB");
            sampleGroup1.createCell(7).setCellValue("SINGLE_CHOICE");
            sampleGroup1.createCell(8).setCellValue("Phương thức biểu đạt chính của đoạn văn trên là gì?");
            sampleGroup1.createCell(9).setCellValue("Tự sự");
            sampleGroup1.createCell(10).setCellValue("Nghị luận");
            sampleGroup1.createCell(11).setCellValue("Biểu cảm");
            sampleGroup1.createCell(12).setCellValue("Miêu tả");
            sampleGroup1.createCell(13).setCellValue("B");
            sampleGroup1.createCell(14).setCellValue("Đoạn văn chủ yếu trình bày luận điểm và lí lẽ nghị luận.");

            Row sampleGroup2 = sheet2.createRow(2);
            sampleGroup2.createCell(0).setCellValue("GRP_01");
            sampleGroup2.createCell(1).setCellValue("1");
            sampleGroup2.createCell(2).setCellValue("Đoạn trích đọc hiểu Ngữ văn");
            sampleGroup2.createCell(3).setCellValue("Mỗi cuốn sách là một bức tranh kì diệu về cuộc sống. Đọc sách giúp ta mở rộng tầm nhìn và bồi dưỡng tâm hồn...");
            sampleGroup2.createCell(4).setCellValue("1");
            sampleGroup2.createCell(5).setCellValue("");
            sampleGroup2.createCell(6).setCellValue("TH");
            sampleGroup2.createCell(7).setCellValue("SINGLE_CHOICE");
            sampleGroup2.createCell(8).setCellValue("Theo tác giả, tác dụng lớn nhất của việc đọc sách là gì?");
            sampleGroup2.createCell(9).setCellValue("Bồi dưỡng tâm hồn và mở rộng tầm nhìn");
            sampleGroup2.createCell(10).setCellValue("Giúp kiếm tiền");
            sampleGroup2.createCell(11).setCellValue("Giải trí đơn thuần");
            sampleGroup2.createCell(12).setCellValue("Vượt qua kì thi");
            sampleGroup2.createCell(13).setCellValue("A");
            sampleGroup2.createCell(14).setCellValue("Ý chính được nhấn mạnh trong câu thứ hai của đoạn văn.");

            for (int i = 0; i < headers2.length; i++) {
                sheet2.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw AppException.badRequest("Không thể tạo template Excel: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public QuestionImportResultResponse importQuestions(MultipartFile file, boolean dryRun, Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw AppException.badRequest("File tải lên không được để trống");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw AppException.badRequest("Kích thước file tối đa cho phép là 10MB");
        }
        String fileName = file.getOriginalFilename();
        if (fileName != null && !fileName.toLowerCase().endsWith(".xlsx")) {
            throw AppException.badRequest("Định dạng file không hỗ trợ, vui lòng tải lên file Excel .xlsx");
        }

        List<QuestionImportItemDto> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            // Parse Sheet 1: Single Questions (if present)
            Sheet sheet1 = workbook.getSheet("Single Questions");
            if (sheet1 == null && workbook.getNumberOfSheets() > 0) {
                sheet1 = workbook.getSheetAt(0);
            }
            if (sheet1 != null && !isGroupSheet(sheet1)) {
                parseSingleQuestionsSheet(sheet1, items, errors);
            }

            // Parse Sheet 2: Question Groups (if present)
            Sheet sheet2 = workbook.getSheet("Question Groups");
            if (sheet2 == null && workbook.getNumberOfSheets() > 1) {
                sheet2 = workbook.getSheetAt(1);
            }
            if (sheet2 != null && isGroupSheet(sheet2)) {
                parseGroupQuestionsSheet(sheet2, items, errors);
            }

        } catch (Exception e) {
            log.error("Lỗi parse file Excel: {}", e.getMessage(), e);
            throw AppException.badRequest("Không thể đọc file Excel: " + e.getMessage());
        }

        int totalRows = items.size();
        if (totalRows > 1000) {
            throw AppException.badRequest("Số lượng dòng câu hỏi vượt quá giới hạn cho phép (tối đa 1000 dòng)");
        }
        int validCount = (int) items.stream().filter(QuestionImportItemDto::isValid).count();
        int errorCount = totalRows - validCount;

        if (!dryRun && errorCount > 0) {
            throw AppException.badRequest("Không thể nạp vào CSDL vì còn " + errorCount + " dòng lỗi. Vui lòng sửa lỗi trước khi nạp.");
        }

        int importedQuestions = 0;
        int createdGroups = 0;

        if (!dryRun && validCount > 0) {
            // Save valid items to DB
            Map<String, QuestionGroup> groupCache = new HashMap<>();

            // 1. Process group items first to assign group IDs
            for (QuestionImportItemDto item : items) {
                if (!item.isValid() || item.getGroupCode() == null || item.getGroupCode().isBlank()) {
                    continue;
                }
                String groupCode = item.getGroupCode().trim();
                QuestionGroup group = groupCache.get(groupCode);
                if (group == null) {
                    Optional<QuestionGroup> existing = questionGroupRepository.findByCode(groupCode);
                    if (existing.isPresent()) {
                        group = existing.get();
                    } else {
                        group = QuestionGroup.builder()
                                .code(groupCode)
                                .subjectId(item.getSubjectId())
                                .title(item.getGroupTitle() != null ? item.getGroupTitle() : groupCode)
                                .passageText(item.getPassageText())
                                .passageHtml(item.getPassageText())
                                .createdBy(currentUserId)
                                .build();
                        group = questionGroupRepository.save(group);
                        createdGroups++;
                    }
                    groupCache.put(groupCode, group);
                }

                saveQuestionAndOptions(item, group.getId(), currentUserId);
                importedQuestions++;
            }

            // 2. Process single items
            for (QuestionImportItemDto item : items) {
                if (!item.isValid() || (item.getGroupCode() != null && !item.getGroupCode().isBlank())) {
                    continue;
                }
                saveQuestionAndOptions(item, null, currentUserId);
                importedQuestions++;
            }
        }

        return QuestionImportResultResponse.builder()
                .totalRows(totalRows)
                .validCount(validCount)
                .errorCount(errorCount)
                .importedQuestions(importedQuestions)
                .createdGroups(createdGroups)
                .dryRun(dryRun)
                .items(items)
                .errors(errors)
                .build();
    }

    private boolean isGroupSheet(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() == 0) return false;
        Row header = sheet.getRow(0);
        if (header == null) return false;
        String firstCol = getCellString(header.getCell(0)).toLowerCase();
        return firstCol.contains("group") || firstCol.contains("nhóm") || firstCol.contains("chùm");
    }

    private void parseSingleQuestionsSheet(Sheet sheet, List<QuestionImportItemDto> items, List<String> errors) {
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row)) continue;

            int rowNumber = r + 1;
            String sheetName = sheet.getSheetName();
            String subjectRaw = getCellString(row.getCell(0));
            String topicRaw = getCellString(row.getCell(1));
            String subtopicRaw = getCellString(row.getCell(2));
            String diffRaw = getCellString(row.getCell(3));
            String typeRaw = getCellString(row.getCell(4));
            String questionText = getCellString(row.getCell(5));
            String optA = getCellString(row.getCell(6));
            String optB = getCellString(row.getCell(7));
            String optC = getCellString(row.getCell(8));
            String optD = getCellString(row.getCell(9));
            String correctRaw = getCellString(row.getCell(10));
            String explanation = getCellString(row.getCell(11));
            String tags = getCellString(row.getCell(12));

            QuestionImportItemDto item = QuestionImportItemDto.builder()
                    .sheetName(sheetName)
                    .rowNumber(rowNumber)
                    .questionText(questionText)
                    .options(List.of(optA, optB, optC, optD))
                    .correctAnswer(correctRaw)
                    .explanation(explanation)
                    .tags(tags)
                    .valid(true)
                    .build();

            validateAndPopulateCommon(item, subjectRaw, topicRaw, subtopicRaw, diffRaw, typeRaw, optA, optB, optC, optD, correctRaw, errors);
            items.add(item);
        }
    }

    private void parseGroupQuestionsSheet(Sheet sheet, List<QuestionImportItemDto> items, List<String> errors) {
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row)) continue;

            int rowNumber = r + 1;
            String sheetName = sheet.getSheetName();
            String groupCode = getCellString(row.getCell(0));
            String subjectRaw = getCellString(row.getCell(1));
            String groupTitle = getCellString(row.getCell(2));
            String passageText = getCellString(row.getCell(3));
            String topicRaw = getCellString(row.getCell(4));
            String subtopicRaw = getCellString(row.getCell(5));
            String diffRaw = getCellString(row.getCell(6));
            String typeRaw = getCellString(row.getCell(7));
            String questionText = getCellString(row.getCell(8));
            String optA = getCellString(row.getCell(9));
            String optB = getCellString(row.getCell(10));
            String optC = getCellString(row.getCell(11));
            String optD = getCellString(row.getCell(12));
            String correctRaw = getCellString(row.getCell(13));
            String explanation = getCellString(row.getCell(14));

            QuestionImportItemDto item = QuestionImportItemDto.builder()
                    .sheetName(sheetName)
                    .rowNumber(rowNumber)
                    .groupCode(groupCode)
                    .groupTitle(groupTitle)
                    .passageText(passageText)
                    .questionText(questionText)
                    .options(List.of(optA, optB, optC, optD))
                    .correctAnswer(correctRaw)
                    .explanation(explanation)
                    .valid(true)
                    .build();

            if (groupCode.isBlank()) {
                item.setValid(false);
                item.setError("Mã nhóm / Group Code không được để trống");
                errors.add(String.format("[%s Hàng %d] %s", sheetName, rowNumber, item.getError()));
            } else if (passageText.isBlank()) {
                item.setValid(false);
                item.setError("Đoạn văn đọc hiểu / Passage Text không được để trống");
                errors.add(String.format("[%s Hàng %d] %s", sheetName, rowNumber, item.getError()));
            }

            if (item.isValid()) {
                validateAndPopulateCommon(item, subjectRaw, topicRaw, subtopicRaw, diffRaw, typeRaw, optA, optB, optC, optD, correctRaw, errors);
            }
            items.add(item);
        }
    }

    private void validateAndPopulateCommon(
            QuestionImportItemDto item,
            String subjectRaw,
            String topicRaw,
            String subtopicRaw,
            String diffRaw,
            String typeRaw,
            String optA,
            String optB,
            String optC,
            String optD,
            String correctRaw,
            List<String> errors
    ) {
        String sheetName = item.getSheetName();
        int rowNumber = item.getRowNumber();

        // 1. Resolve Subject
        if (subjectRaw.isBlank()) {
            failItem(item, "Thiếu thông tin Môn học (Subject)", errors);
            return;
        }
        Subject subject = resolveSubject(subjectRaw);
        if (subject == null) {
            failItem(item, "Không tìm thấy Môn học: " + subjectRaw, errors);
            return;
        }
        item.setSubjectId(subject.getId());
        item.setSubjectCode(subject.getCode());

        // 2. Resolve Topic
        if (topicRaw.isBlank()) {
            failItem(item, "Thiếu thông tin Chủ đề (Topic)", errors);
            return;
        }
        Topic topic = resolveTopic(topicRaw, subject.getId());
        if (topic == null) {
            failItem(item, "Không tìm thấy Chủ đề: " + topicRaw + " thuộc môn " + subject.getName(), errors);
            return;
        }
        item.setTopicId(topic.getId());

        // 3. Resolve Subtopic (optional)
        if (!subtopicRaw.isBlank()) {
            try {
                Long subId = Long.parseLong(subtopicRaw);
                Subtopic subtopic = subtopicRepository.findById(subId).orElse(null);
                if (subtopic != null && subtopic.getTopicId().equals(topic.getId())) {
                    item.setSubtopicId(subId);
                }
            } catch (NumberFormatException ignored) {}
        }

        // 4. Resolve Difficulty
        item.setDifficulty(parseDifficulty(diffRaw));

        // 5. Resolve Question Type
        item.setQuestionType(parseQuestionType(typeRaw));

        // 6. Validate Question Text
        if (item.getQuestionText() == null || item.getQuestionText().isBlank()) {
            failItem(item, "Nội dung câu hỏi không được để trống", errors);
            return;
        }

        // 7. Validate Options
        int validOptionsCount = 0;
        if (!optA.isBlank()) validOptionsCount++;
        if (!optB.isBlank()) validOptionsCount++;
        if (!optC.isBlank()) validOptionsCount++;
        if (!optD.isBlank()) validOptionsCount++;

        if (validOptionsCount < 2) {
            failItem(item, "Câu hỏi phải có ít nhất 2 phương án lựa chọn (A, B)", errors);
            return;
        }

        // 8. Validate Correct Answer
        if (correctRaw.isBlank()) {
            failItem(item, "Đáp án đúng không được để trống", errors);
            return;
        }
        String cleanAnswer = correctRaw.trim().toUpperCase();
        boolean validAns = cleanAnswer.matches("^[A-D](,[A-D])*$") || cleanAnswer.matches("^[A-D]+$");
        if (!validAns) {
            failItem(item, "Đáp án đúng không hợp lệ (phải là A, B, C hoặc D): " + correctRaw, errors);
        }
    }

    private void failItem(QuestionImportItemDto item, String msg, List<String> errors) {
        item.setValid(false);
        item.setError(msg);
        errors.add(String.format("[%s Hàng %d] %s", item.getSheetName(), item.getRowNumber(), msg));
    }

    private Subject resolveSubject(String raw) {
        String clean = raw.trim();
        try {
            Long id = Long.parseLong(clean);
            return subjectRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            return subjectRepository.findByCode(clean.toUpperCase()).orElse(null);
        }
    }

    private Topic resolveTopic(String raw, Long subjectId) {
        String clean = raw.trim();
        try {
            Long id = Long.parseLong(clean);
            Topic topic = topicRepository.findById(id).orElse(null);
            if (topic != null && topic.getSubjectId().equals(subjectId)) {
                return topic;
            }
            return null;
        } catch (NumberFormatException e) {
            return topicRepository.findByCode(clean.toUpperCase())
                    .filter(t -> t.getSubjectId().equals(subjectId))
                    .orElse(null);
        }
    }

    private Difficulty parseDifficulty(String raw) {
        if (raw == null || raw.isBlank()) return Difficulty.MEDIUM;
        String val = raw.trim().toUpperCase();
        return switch (val) {
            case "NB", "NHAN_BIET", "NHAN BIET", "EASY", "1" -> Difficulty.EASY;
            case "TH", "THONG_HIEU", "THONG HIEU", "MEDIUM", "2" -> Difficulty.MEDIUM;
            case "VD", "VAN_DUNG", "VAN DUNG", "HARD", "3" -> Difficulty.HARD;
            case "VDC", "VAN_DUNG_CAO", "VAN DUNG CAO", "VERY_HARD", "4" -> Difficulty.VERY_HARD;
            default -> Difficulty.MEDIUM;
        };
    }

    private QuestionType parseQuestionType(String raw) {
        if (raw == null || raw.isBlank()) return QuestionType.SINGLE_CHOICE;
        String val = raw.trim().toUpperCase();
        return switch (val) {
            case "MULTIPLE_CHOICE", "NHIEU_DAP_AN", "MC" -> QuestionType.MULTIPLE_CHOICE;
            case "TRUE_FALSE", "DUNG_SAI", "TF" -> QuestionType.TRUE_FALSE;
            case "SHORT_ANSWER", "DIEN_KHUYET" -> QuestionType.SHORT_ANSWER;
            default -> QuestionType.SINGLE_CHOICE;
        };
    }

    private void saveQuestionAndOptions(QuestionImportItemDto item, Long questionGroupId, Long currentUserId) {
        String code = "Q-T" + item.getTopicId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Question question = Question.builder()
                .questionCode(code)
                .subjectId(item.getSubjectId())
                .topicId(item.getTopicId())
                .subtopicId(item.getSubtopicId())
                .questionGroupId(questionGroupId)
                .difficulty(item.getDifficulty())
                .questionType(item.getQuestionType())
                .questionText(item.getQuestionText())
                .explanation(item.getExplanation())
                .tags(item.getTags())
                .status(QuestionStatus.APPROVED)
                .version(1)
                .createdBy(currentUserId)
                .build();

        Question saved = questionRepository.save(question);

        List<String> options = item.getOptions();
        String[] labels = {"A", "B", "C", "D"};
        String correct = item.getCorrectAnswer().trim().toUpperCase();

        for (int i = 0; i < options.size(); i++) {
            String text = options.get(i);
            if (text == null || text.isBlank()) continue;
            String label = labels[i];
            boolean isCorrect = correct.contains(label);

            questionOptionRepository.save(QuestionOption.builder()
                    .questionId(saved.getId())
                    .optionLabel(label)
                    .optionText(text)
                    .isCorrect(isCorrect)
                    .displayOrder(i + 1)
                    .build());
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        try {
            return dataFormatter.formatCellValue(cell).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
