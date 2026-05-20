package com.animetracker.module;

import com.animetracker.entity.Anime;
import com.animetracker.entity.AnimeGenre;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisplayCardsModule {

    public String buildCardText(Anime anime, Integer userScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎌 *").append(escape(anime.getTitle())).append("*\n");

        if (anime.getTitleEnglish() != null && !anime.getTitleEnglish().isBlank()) {
            sb.append("🔤 ").append(escape(anime.getTitleEnglish())).append("\n");
        }
        sb.append("\n");

        if (anime.getType() != null) sb.append("📺 Тип: ").append(anime.getType()).append("\n");
        if (anime.getStatus() != null) sb.append("📋 Статус: ").append(anime.getStatus()).append("\n");
        if (anime.getEpisodes() != null) sb.append("📌 Эпизоды: ").append(anime.getEpisodes()).append("\n");
        if (anime.getScore() != null) sb.append("⭐ Рейтинг: ").append(String.format("%.2f", anime.getScore())).append("\n");
        if (userScore != null) sb.append("📝 Ваша оценка: ").append(userScore).append("/10\n");

        String dates = buildDateRange(anime);
        if (!dates.isBlank()) sb.append("📅 Дата: ").append(dates).append("\n");

        String genres = buildGenres(anime);
        if (!genres.isBlank()) sb.append("🏷 Жанры: ").append(genres).append("\n");

        if (anime.getSynopsis() != null && !anime.getSynopsis().isBlank()) {
            String synopsis = anime.getSynopsis();
            if (synopsis.length() > 300) synopsis = synopsis.substring(0, 300) + "…";
            sb.append("\n📖 ").append(escape(synopsis));
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup buildCardKeyboard(Anime anime, boolean inViewed, boolean inToView, Integer userScore) {
        Long animeId = anime.getId();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        if (inViewed) {
            row1.add(btn("✅ Удалить из просмотренных", "rv:" + animeId));
        } else {
            row1.add(btn("👁 Добавить в просмотренные", "av:" + animeId));
        }
        rows.add(row1);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        if (inToView) {
            row2.add(btn("📌 Удалить из отслеживаемых", "rt:" + animeId));
        } else {
            row2.add(btn("📌 Добавить в отслеживаемые", "at:" + animeId));
        }
        rows.add(row2);

        if (inViewed) {
            InlineKeyboardRow row3 = new InlineKeyboardRow();
            String rateLabel = (userScore != null)
                    ? "⭐ Оценка: " + userScore + "/10 (изменить)"
                    : "⭐ Поставить оценку";
            row3.add(btn(rateLabel, "ra:" + animeId));
            rows.add(row3);
        }

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup buildRatingKeyboard(Long animeId) {
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        for (int i = 1; i <= 5; i++) row1.add(btn(String.valueOf(i), "rs:" + animeId + ":" + i));
        for (int i = 6; i <= 10; i++) row2.add(btn(String.valueOf(i), "rs:" + animeId + ":" + i));
        return new InlineKeyboardMarkup(List.of(row1, row2));
    }

    public InlineKeyboardMarkup buildNavKeyboard(boolean hasPrev, boolean hasNext, int currentPage, int totalPages) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (hasPrev) row.add(btn("⬅ Предыдущая", "pp"));
        row.add(btn((currentPage + 1) + "/" + totalPages, "noop"));
        if (hasNext) row.add(btn("Следующая ➡", "np"));
        return new InlineKeyboardMarkup(List.of(row));
    }

    private InlineKeyboardButton btn(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    private String buildGenres(Anime anime) {
        if (anime.getAnimeGenres() == null) return "";
        return anime.getAnimeGenres().stream()
                .map(ag -> ag.getGenre() != null ? ag.getGenre().getName() : "")
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String buildDateRange(Anime anime) {
        if (anime.getStartDate() == null) return "";
        String result = anime.getStartDate().toString();
        if (anime.getEndDate() != null) result += " — " + anime.getEndDate();
        return result;
    }

    private String escape(String text) {
        if (text == null) return "";
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
    }
}
