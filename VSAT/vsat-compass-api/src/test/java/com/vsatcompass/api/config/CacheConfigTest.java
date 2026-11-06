package com.vsatcompass.api.config;

import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.request.GenerateByMatrixRequest;
import com.vsatcompass.api.dto.request.GenerateVariantsRequest;
import com.vsatcompass.api.dto.request.SessionRequest;
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
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.lang.reflect.Method;
import java.time.Duration;
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
}
