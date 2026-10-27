package com.example.v_sat_compass.ui.flashcards;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.data.model.Flashcard;
import com.example.v_sat_compass.data.repository.FlashcardRepository;
import com.example.v_sat_compass.data.repository.FlashcardScheduler;
import com.example.v_sat_compass.data.repository.GamificationRepository;
import com.example.v_sat_compass.databinding.ActivityFlashcardReviewBinding;

import java.util.ArrayList;
import java.util.List;

public class FlashcardReviewActivity extends AppCompatActivity {
    private ActivityFlashcardReviewBinding binding;
    private final List<Flashcard> dueCards = new ArrayList<>();
    private int currentIndex;
    private boolean showingBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFlashcardReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvBack.setOnClickListener(v -> finish());
        binding.cardFlashcard.setOnClickListener(v -> flipCard());
        binding.btnAgain.setOnClickListener(v -> reviewCurrent(FlashcardScheduler.Rating.AGAIN));
        binding.btnHard.setOnClickListener(v -> reviewCurrent(FlashcardScheduler.Rating.HARD));
        binding.btnGood.setOnClickListener(v -> reviewCurrent(FlashcardScheduler.Rating.GOOD));
        binding.btnEasy.setOnClickListener(v -> reviewCurrent(FlashcardScheduler.Rating.EASY));

        loadDueCards();
    }

    private void loadDueCards() {
        FlashcardRepository.getInstance().getDueCards(this, cards -> {
            dueCards.clear();
            if (cards != null) dueCards.addAll(cards);
            currentIndex = 0;
            renderCurrent();
        });
    }

    private void renderCurrent() {
        boolean hasCards = currentIndex < dueCards.size();
        binding.contentReview.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        binding.contentEmpty.setVisibility(hasCards ? View.GONE : View.VISIBLE);
        binding.tvCounter.setText(hasCards ? (currentIndex + 1) + "/" + dueCards.size() : "0");
        if (!hasCards) return;

        showingBack = false;
        Flashcard card = dueCards.get(currentIndex);
        binding.tvDeck.setText(card.getSubject() + " / " + card.getDeck());
        binding.tvSideLabel.setText("FRONT");
        binding.tvCardText.setText(card.getFront());
    }

    private void flipCard() {
        if (currentIndex >= dueCards.size()) return;
        showingBack = !showingBack;
        Flashcard card = dueCards.get(currentIndex);
        binding.tvSideLabel.setText(showingBack ? "BACK" : "FRONT");
        binding.tvCardText.setText(showingBack ? card.getBack() : card.getFront());
    }

    private void reviewCurrent(FlashcardScheduler.Rating rating) {
        if (currentIndex >= dueCards.size()) return;
        Flashcard card = dueCards.get(currentIndex);
        FlashcardRepository.getInstance().review(this, card, rating, () -> {
            GamificationRepository.getInstance().recordFlashcardReviewed(this);
            if (rating == FlashcardScheduler.Rating.AGAIN) {
                Toast.makeText(this, "Again: returns in about 10 minutes", Toast.LENGTH_SHORT).show();
            }
            currentIndex += 1;
            renderCurrent();
        });
    }
}
