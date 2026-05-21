package com.example.v_sat_compass.ui.collaborator;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.v_sat_compass.data.model.SubtopicResponse;
import com.example.v_sat_compass.data.model.TopicResponse;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.model.question.UpdateQuestionRequest;
import com.example.v_sat_compass.data.repository.CollaboratorQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.data.repository.TopicRepository;

import java.util.List;

public class CollaboratorQuestionViewModel extends ViewModel {

    private final CollaboratorQuestionRepository questionRepository;
    private final TopicRepository topicRepository;

    private final MutableLiveData<Resource<QuestionResponse>> createQuestionState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<QuestionResponse>> updateQuestionState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<QuestionResponse>> submitForReviewState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<QuestionResponse>> questionState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<PageResponse<QuestionListItemResponse>>> listState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<List<TopicResponse>>> topicListState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<List<SubtopicResponse>>> subtopicListState =
            new MutableLiveData<>();

    public CollaboratorQuestionViewModel() {
        this(new CollaboratorQuestionRepository(), new TopicRepository());
    }

    public CollaboratorQuestionViewModel(CollaboratorQuestionRepository questionRepository) {
        this(questionRepository, new TopicRepository());
    }

    public CollaboratorQuestionViewModel(
            CollaboratorQuestionRepository questionRepository,
            TopicRepository topicRepository
    ) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
    }

    public LiveData<Resource<QuestionResponse>> getCreateQuestionState() {
        return createQuestionState;
    }

    public LiveData<Resource<QuestionResponse>> getUpdateQuestionState() {
        return updateQuestionState;
    }

    public LiveData<Resource<QuestionResponse>> getSubmitForReviewState() {
        return submitForReviewState;
    }

    public LiveData<Resource<QuestionResponse>> getQuestionState() {
        return questionState;
    }

    public LiveData<Resource<PageResponse<QuestionListItemResponse>>> getListState() {
        return listState;
    }

    public LiveData<Resource<List<TopicResponse>>> getTopicListState() {
        return topicListState;
    }

    public LiveData<Resource<List<SubtopicResponse>>> getSubtopicListState() {
        return subtopicListState;
    }

    public void createQuestion(CreateQuestionRequest request) {
        createQuestionState.setValue(Resource.loading());
        questionRepository.createQuestion(request, new QuestionResponseCallback(createQuestionState));
    }

    public void updateQuestion(Long id, UpdateQuestionRequest request) {
        updateQuestionState.setValue(Resource.loading());
        questionRepository.updateQuestion(id, request, new QuestionResponseCallback(updateQuestionState));
    }

    public void submitForReview(Long id) {
        submitForReviewState.setValue(Resource.loading());
        questionRepository.submitForReview(id, new QuestionResponseCallback(submitForReviewState));
    }

    public void getQuestion(Long id) {
        questionState.setValue(Resource.loading());
        questionRepository.getQuestion(id, new QuestionResponseCallback(questionState));
    }

    public void listMyQuestions(QuestionStatus status, int page, int size) {
        listState.setValue(Resource.loading());
        questionRepository.listMyQuestions(
                status,
                page,
                size,
                new CollaboratorQuestionRepository.RepositoryCallback<PageResponse<QuestionListItemResponse>>() {
                    @Override
                    public void onSuccess(PageResponse<QuestionListItemResponse> data) {
                        listState.setValue(Resource.success(data));
                    }

                    @Override
                    public void onError(
                            CollaboratorQuestionRepository.CollaboratorQuestionError error
                    ) {
                        listState.setValue(Resource.error(message(error)));
                    }
                }
        );
    }

    public void listTopics(Long subjectId) {
        topicListState.setValue(Resource.loading());
        topicRepository.listTopics(subjectId, new TopicRepository.RepositoryCallback<List<TopicResponse>>() {
            @Override
            public void onSuccess(List<TopicResponse> data) {
                topicListState.setValue(Resource.success(data));
            }

            @Override
            public void onError(TopicRepository.TopicError error) {
                topicListState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void listSubtopics(Long subjectId, Long topicId) {
        subtopicListState.setValue(Resource.loading());
        topicRepository.listSubtopics(
                subjectId,
                topicId,
                new TopicRepository.RepositoryCallback<List<SubtopicResponse>>() {
                    @Override
                    public void onSuccess(List<SubtopicResponse> data) {
                        subtopicListState.setValue(Resource.success(data));
                    }

                    @Override
                    public void onError(TopicRepository.TopicError error) {
                        subtopicListState.setValue(Resource.error(message(error)));
                    }
                }
        );
    }

    private static String message(
            CollaboratorQuestionRepository.CollaboratorQuestionError error
    ) {
        if (error == null) {
            return "Unknown error";
        }
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return error.getType().name();
    }

    private static String message(TopicRepository.TopicError error) {
        if (error == null) {
            return "Unknown error";
        }
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return error.getType().name();
    }

    private static class QuestionResponseCallback
            implements CollaboratorQuestionRepository.RepositoryCallback<QuestionResponse> {

        private final MutableLiveData<Resource<QuestionResponse>> state;

        QuestionResponseCallback(MutableLiveData<Resource<QuestionResponse>> state) {
            this.state = state;
        }

        @Override
        public void onSuccess(QuestionResponse data) {
            state.setValue(Resource.success(data));
        }

        @Override
        public void onError(CollaboratorQuestionRepository.CollaboratorQuestionError error) {
            state.setValue(Resource.error(message(error)));
        }
    }
}
