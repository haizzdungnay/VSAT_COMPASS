package com.vsatcompass.api.config;

import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.request.GenerateByMatrixRequest;
import com.vsatcompass.api.dto.request.GenerateVariantsRequest;
import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.BlueprintResponse;
import com.vsatcompass.api.dto.response.ExamDetailResponse;
import com.vsatcompass.api.dto.response.ExamDownloadBundleResponse;
import com.vsatcompass.api.dto.response.ExamSummaryResponse;
import com.vsatcompass.api.dto.response.LeaderboardEntryResponse;
import com.vsatcompass.api.dto.response.QuestionGroupResponse;
import com.vsatcompass.api.entity.enums.CognitiveLevel;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.service.impl.AdminBlueprintServiceImpl;
import com.vsatcompass.api.service.impl.AdminExamServiceImpl;
import com.vsatcompass.api.service.impl.ExamServiceImpl;
import com.vsatcompass.api.service.impl.LeaderboardServiceImpl;
import com.vsatcompass.api.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("CacheConfig & Service Caching Annotations Test")
class CacheConfigTest {

    @Test
    @DisplayName("RedisCacheManagerBuilderCustomizer sets proper TTL for each cache name")
    void testRedisCacheManagerBuilderCustomizer() {
        CacheConfig config = new CacheConfig();
        RedisCacheManagerBuilderCustomizer customizer = config.redisCacheManagerBuilderCustomizer();
        assertThat(customizer).isNotNull();

        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory);
        customizer.customize(builder);

        RedisCacheManager cacheManager = builder.build();
        cacheManager.afterPropertiesSet();
        Map<String, RedisCacheConfiguration> configs = cacheManager.getCacheConfigurations();

        assertThat(configs).containsKeys(
                CacheConfig.CACHE_LEADERBOARD,
                CacheConfig.CACHE_BLUEPRINTS,
                CacheConfig.CACHE_BLUEPRINT_DETAIL,
                CacheConfig.CACHE_PUBLIC_EXAMS,
                CacheConfig.CACHE_PUBLIC_EXAM_DETAIL,
                CacheConfig.CACHE_PUBLIC_EXAM_BUNDLE
        );

        assertThat(configs.get(CacheConfig.CACHE_LEADERBOARD).getTtl()).isEqualTo(Duration.ofSeconds(60));
        assertThat(configs.get(CacheConfig.CACHE_BLUEPRINTS).getTtl()).isEqualTo(Duration.ofHours(1));
        assertThat(configs.get(CacheConfig.CACHE_BLUEPRINT_DETAIL).getTtl()).isEqualTo(Duration.ofHours(1));
        assertThat(configs.get(CacheConfig.CACHE_PUBLIC_EXAMS).getTtl()).isEqualTo(Duration.ofSeconds(60));
        assertThat(configs.get(CacheConfig.CACHE_PUBLIC_EXAM_DETAIL).getTtl()).isEqualTo(Duration.ofSeconds(60));
        assertThat(configs.get(CacheConfig.CACHE_PUBLIC_EXAM_BUNDLE).getTtl()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("When spring.cache.type=simple, cache manager is ConcurrentMapCacheManager without Redis")
    void testSimpleCacheContext() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withUserConfiguration(CacheConfig.class)
                .withPropertyValues("spring.cache.type=simple")
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheManager.class);
                    assertThat(context.getBean(CacheManager.class))
                            .isInstanceOf(ConcurrentMapCacheManager.class);
                });
    }

    @Test
    @DisplayName("LeaderboardServiceImpl has correct @Cacheable and @CacheEvict")
    void testLeaderboardServiceImplCaching() throws NoSuchMethodException {
        Method getTop = LeaderboardServiceImpl.class.getMethod("getTop", int.class);
        Cacheable cacheable = getTop.getAnnotation(Cacheable.class);
        assertThat(cacheable).isNotNull();
        assertThat(cacheable.value()).contains("leaderboard");
        assertThat(cacheable.key()).isEqualTo("#limit");

        Method setOptIn = LeaderboardServiceImpl.class.getMethod("setOptIn", Long.class, boolean.class);
        CacheEvict cacheEvict = setOptIn.getAnnotation(CacheEvict.class);
        assertThat(cacheEvict).isNotNull();
        assertThat(cacheEvict.value()).contains("leaderboard");
        assertThat(cacheEvict.allEntries()).isTrue();
    }

    @Test
    @DisplayName("SessionServiceImpl has correct @CacheEvict on submits")
    void testSessionServiceImplCaching() throws NoSuchMethodException {
        Method serverSubmit = SessionServiceImpl.class.getMethod("serverSubmit", Long.class, Long.class);
        CacheEvict serverEvict = serverSubmit.getAnnotation(CacheEvict.class);
        assertThat(serverEvict).isNotNull();
        assertThat(serverEvict.value()).contains("leaderboard");
        assertThat(serverEvict.allEntries()).isTrue();

        Method clientSubmit = SessionServiceImpl.class.getMethod(
                "clientSubmit", Long.class, Long.class, SessionRequest.ClientSubmit.class);
        CacheEvict clientEvict = clientSubmit.getAnnotation(CacheEvict.class);
        assertThat(clientEvict).isNotNull();
        assertThat(clientEvict.value()).contains("leaderboard");
        assertThat(clientEvict.allEntries()).isTrue();
    }

    @Test
    @DisplayName("AdminBlueprintServiceImpl has correct @Cacheable")
    void testAdminBlueprintServiceImplCaching() throws NoSuchMethodException {
        Method listBlueprints = AdminBlueprintServiceImpl.class.getMethod("listBlueprints", Long.class);
        Cacheable listCache = listBlueprints.getAnnotation(Cacheable.class);
        assertThat(listCache).isNotNull();
        assertThat(listCache.value()).contains("blueprints");
        assertThat(listCache.key()).isEqualTo("#subjectId != null ? #subjectId : 'ALL'");

        Method getBlueprint = AdminBlueprintServiceImpl.class.getMethod("getBlueprint", Long.class);
        Cacheable getCache = getBlueprint.getAnnotation(Cacheable.class);
        assertThat(getCache).isNotNull();
        assertThat(getCache.value()).contains("blueprintDetail");
        assertThat(getCache.key()).isEqualTo("#id");
    }

    @Test
    @DisplayName("ExamServiceImpl has correct @Cacheable on public methods")
    void testExamServiceImplCaching() throws NoSuchMethodException {
        Method listExams = ExamServiceImpl.class.getMethod("listPublishedFreeExams", Pageable.class);
        Cacheable listCache = listExams.getAnnotation(Cacheable.class);
        assertThat(listCache).isNotNull();
        assertThat(listCache.value()).contains("publicExams");
        assertThat(listCache.key()).isEqualTo("#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort");

        Method getExam = ExamServiceImpl.class.getMethod("getPublicExam", Long.class);
        Cacheable getCache = getExam.getAnnotation(Cacheable.class);
        assertThat(getCache).isNotNull();
        assertThat(getCache.value()).contains("publicExamDetail");
        assertThat(getCache.key()).isEqualTo("#id");

        Method getBundle = ExamServiceImpl.class.getMethod("getDownloadBundle", Long.class);
        Cacheable bundleCache = getBundle.getAnnotation(Cacheable.class);
        assertThat(bundleCache).isNotNull();
        assertThat(bundleCache.value()).contains("publicExamBundle");
        assertThat(bundleCache.key()).isEqualTo("#id");
    }

    @Test
    @DisplayName("AdminExamServiceImpl mutating methods have @CacheEvict")
    void testAdminExamServiceImplEviction() throws NoSuchMethodException {
        List<Method> mutatingMethods = List.of(
                AdminExamServiceImpl.class.getMethod("createExam", Long.class, AdminExamCreateRequest.class),
                AdminExamServiceImpl.class.getMethod("updateExam", Long.class, AdminExamUpdateRequest.class),
                AdminExamServiceImpl.class.getMethod("discardDraftExam", Long.class),
                AdminExamServiceImpl.class.getMethod("addQuestion", Long.class, Long.class),
                AdminExamServiceImpl.class.getMethod("removeQuestion", Long.class, Long.class),
                AdminExamServiceImpl.class.getMethod("reorderQuestions", Long.class, List.class),
                AdminExamServiceImpl.class.getMethod("submitReview", Long.class),
                AdminExamServiceImpl.class.getMethod("publish", Long.class, Long.class),
                AdminExamServiceImpl.class.getMethod("hide", Long.class),
                AdminExamServiceImpl.class.getMethod("archive", Long.class),
                AdminExamServiceImpl.class.getMethod("rejectReview", Long.class),
                AdminExamServiceImpl.class.getMethod("returnToDraft", Long.class),
                AdminExamServiceImpl.class.getMethod("generateVariants", Long.class, GenerateVariantsRequest.class),
                AdminExamServiceImpl.class.getMethod("generateByMatrix", GenerateByMatrixRequest.class),
                AdminExamServiceImpl.class.getMethod("generateByMatrixForExam", Long.class, GenerateByMatrixRequest.class)
        );

        for (Method method : mutatingMethods) {
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            assertThat(evict)
                    .as("Method %s must be annotated with @CacheEvict", method.getName())
                    .isNotNull();
            assertThat(evict.value()).containsExactlyInAnyOrder(
                    "publicExams", "publicExamDetail", "publicExamBundle"
            );
            assertThat(evict.allEntries()).isTrue();
        }
    }

    @Test
    @DisplayName("LeaderboardServiceImpl has correct @Cacheable on getSubjectLeaderboard")
    void testGetSubjectLeaderboardCaching() throws NoSuchMethodException {
        Method getSubjectLeaderboard = LeaderboardServiceImpl.class.getMethod(
                "getSubjectLeaderboard", Long.class, int.class);
        Cacheable cacheable = getSubjectLeaderboard.getAnnotation(Cacheable.class);
        assertThat(cacheable).isNotNull();
        assertThat(cacheable.value()).contains("leaderboard");
        assertThat(cacheable.key()).isEqualTo("'subject:' + #subjectId + ':' + #limit");
    }

    @Test
    @DisplayName("CacheConfig implements CachingConfigurer and provides LoggingCacheErrorHandler")
    void testCacheErrorHandler() {
        CacheConfig config = new CacheConfig();
        assertThat(config).isInstanceOf(CachingConfigurer.class);
        CacheErrorHandler errorHandler = config.errorHandler();
        assertThat(errorHandler).isNotNull();
        assertThat(errorHandler).isInstanceOf(LoggingCacheErrorHandler.class);
    }

    @Test
    @DisplayName("Round-trip Redis serialization works for all 7 cached DTOs and PageImpl")
    void testRedisSerializationRoundTripForCachedDtos() {
        CacheConfig config = new CacheConfig();
        RedisCacheManagerBuilderCustomizer customizer = config.redisCacheManagerBuilderCustomizer();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory);
        customizer.customize(builder);

        RedisCacheManager cacheManager = builder.build();
        cacheManager.afterPropertiesSet();
        RedisCacheConfiguration cacheConfig = cacheManager.getCacheConfigurations()
                .get(CacheConfig.CACHE_LEADERBOARD);
        RedisSerializationContext.SerializationPair<Object> pair = cacheConfig.getValueSerializationPair();

        ExamSummaryResponse summary = ExamSummaryResponse.builder()
                .id(1L)
                .examCode("EX-01")
                .title("Exam Summary Title")
                .subjectId(10L)
                .description("Summary Desc")
                .questionCount(40)
                .durationMinutes(60)
                .difficulty(Difficulty.MEDIUM)
                .pricingType(ExamPricingType.FREE)
                .tags("tag1,tag2")
                .build();
        Object readSummary = pair.read(pair.write(summary));
        assertThat(readSummary).isInstanceOf(ExamSummaryResponse.class);
        assertThat(((ExamSummaryResponse) readSummary).getTitle()).isEqualTo(summary.getTitle());

        ExamDetailResponse detail = ExamDetailResponse.builder()
                .id(2L)
                .examCode("EX-02")
                .title("Exam Detail Title")
                .subjectId(10L)
                .description("Detail Desc")
                .questionCount(40)
                .durationMinutes(60)
                .difficulty(Difficulty.HARD)
                .pricingType(ExamPricingType.FREE)
                .tags("math")
                .publishDate(OffsetDateTime.now())
                .build();
        Object readDetail = pair.read(pair.write(detail));
        assertThat(readDetail).isInstanceOf(ExamDetailResponse.class);
        assertThat(((ExamDetailResponse) readDetail).getExamCode()).isEqualTo(detail.getExamCode());

        QuestionGroupResponse group = QuestionGroupResponse.builder()
                .id(3L)
                .code("GRP-01")
                .subjectId(10L)
                .title("Passage Title")
                .passageText("Passage Text")
                .passageHtml("<p>Passage</p>")
                .imageUrl("https://example.com/img.png")
                .audioUrl("https://example.com/audio.mp3")
                .audioDurationSec(120)
                .maxPlays(2)
                .build();
        Object readGroup = pair.read(pair.write(group));
        assertThat(readGroup).isInstanceOf(QuestionGroupResponse.class);
        assertThat(((QuestionGroupResponse) readGroup).getCode()).isEqualTo(group.getCode());

        ExamDownloadBundleResponse.OptionBundleItem option = ExamDownloadBundleResponse.OptionBundleItem.builder()
                .id(401L)
                .optionLabel("A")
                .optionText("Option A text")
                .optionHtml("<p>Option A</p>")
                .imageUrl("https://example.com/opt.png")
                .isCorrect(true)
                .displayOrder(1)
                .build();
        Object readOption = pair.read(pair.write(option));
        assertThat(readOption).isInstanceOf(ExamDownloadBundleResponse.OptionBundleItem.class);

        ExamDownloadBundleResponse.QuestionBundleItem question = ExamDownloadBundleResponse.QuestionBundleItem.builder()
                .id(402L)
                .questionCode("Q-01")
                .subjectId(10L)
                .questionGroup(group)
                .difficulty(Difficulty.EASY)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .questionText("Question text")
                .correctAnswer("A")
                .correctOptionIds(List.of(401L))
                .options(List.of(option))
                .build();
        Object readQuestion = pair.read(pair.write(question));
        assertThat(readQuestion).isInstanceOf(ExamDownloadBundleResponse.QuestionBundleItem.class);

        ExamDownloadBundleResponse bundle = ExamDownloadBundleResponse.builder()
                .id(4L)
                .examCode("EX-BUNDLE")
                .title("Bundle Exam")
                .subjectId(10L)
                .questionCount(1)
                .durationMinutes(60)
                .difficulty(Difficulty.EASY)
                .pricingType(ExamPricingType.FREE)
                .publishDate(OffsetDateTime.now())
                .examVersion(1)
                .updatedAt(OffsetDateTime.now())
                .questions(List.of(question))
                .build();
        Object readBundle = pair.read(pair.write(bundle));
        assertThat(readBundle).isInstanceOf(ExamDownloadBundleResponse.class);
        assertThat(((ExamDownloadBundleResponse) readBundle).getQuestions()).hasSize(1);

        BlueprintResponse.BlueprintCellResponse cell = BlueprintResponse.BlueprintCellResponse.builder()
                .id(501L)
                .topicId(20L)
                .topicName("Calculus")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .cognitiveLevel(CognitiveLevel.APPLICATION)
                .difficulty(Difficulty.MEDIUM)
                .questionCount(5)
                .build();
        Object readCell = pair.read(pair.write(cell));
        assertThat(readCell).isInstanceOf(BlueprintResponse.BlueprintCellResponse.class);

        BlueprintResponse blueprint = BlueprintResponse.builder()
                .id(5L)
                .code("BP-01")
                .name("Blueprint Name")
                .subjectId(10L)
                .subjectCode("MATH")
                .subjectName("Toan Hoc")
                .totalQuestions(40)
                .maxRawScore(BigDecimal.valueOf(100))
                .version(1)
                .cells(List.of(cell))
                .build();
        Object readBlueprint = pair.read(pair.write(blueprint));
        assertThat(readBlueprint).isInstanceOf(BlueprintResponse.class);
        assertThat(((BlueprintResponse) readBlueprint).getName()).isEqualTo(blueprint.getName());

        LeaderboardEntryResponse entry = LeaderboardEntryResponse.builder()
                .rank(1)
                .userId(100L)
                .displayName("Top Student")
                .avatarUrl("https://example.com/avatar.jpg")
                .totalScore(1500L)
                .examCount(8L)
                .build();
        Object readEntry = pair.read(pair.write(entry));
        assertThat(readEntry).isInstanceOf(LeaderboardEntryResponse.class);
        assertThat(((LeaderboardEntryResponse) readEntry).getDisplayName()).isEqualTo(entry.getDisplayName());

        Page<ExamSummaryResponse> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1);
        Object readPage = pair.read(pair.write(page));
        assertThat(readPage).isInstanceOf(PageImpl.class);
        Page<?> deserializedPage = (Page<?>) readPage;
        assertThat(deserializedPage.getTotalElements()).isEqualTo(1L);
        assertThat(deserializedPage.getContent().get(0)).isInstanceOf(ExamSummaryResponse.class);
    }
}
