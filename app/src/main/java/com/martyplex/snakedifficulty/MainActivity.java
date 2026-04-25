package com.martyplex.snakedifficulty;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new GlyphboundView(this));
    }

    private static class GlyphboundView extends View {
        private static final int SCREEN_MENU = 0;
        private static final int SCREEN_CATEGORY = 1;
        private static final int SCREEN_GAME = 2;
        private static final int SCREEN_ROUND_RESULT = 3;
        private static final int SCREEN_SHOP = 4;
        private static final int SCREEN_GAME_OVER = 5;

        private static final int MAX_MISTAKES = 6;
        private static final int STARTING_HEARTS = 3;
        private static final int PHRASES_PER_ROUND = 3;

        private static final int CURSE_NONE = -1;
        private static final int CURSE_RED_MARGIN = 0;
        private static final int CURSE_HEAVY_TARGET = 1;

        private static final int RELIC_LUCKY_INK = 0;
        private static final int RELIC_DOUBLE_TAP = 1;
        private static final int RELIC_RARE_INK = 2;
        private static final int RELIC_VOWEL_CANDLE = 3;
        private static final int RELIC_CHAIN_SCRIPT = 4;
        private static final int RELIC_BLACK_CANDLE = 5;
        private static final int RELIC_SHARP_CONSONANTS = 6;
        private static final int RELIC_SOFT_VOWELS = 7;
        private static final int RELIC_LAST_BREATH = 8;
        private static final int RELIC_GOLDEN_MARGIN = 9;
        private static final int RELIC_ECHO_LETTER = 10;
        private static final int RELIC_INK_INSURANCE = 11;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final ArrayList<ButtonHitbox> buttons = new ArrayList<>();
        private final SharedPreferences prefs;

        private final Category[] categories;
        private final Relic[] relics;
        private final String[] bossPhrases;

        private int screen = SCREEN_MENU;
        private int round = 1;
        private int hearts = STARTING_HEARTS;
        private int bestRound = 0;
        private int bestInk = 0;
        private int totalInk = 0;
        private int shopInk = 0;
        private int targetInk = 500;
        private int inkThisRound = 0;
        private int phraseIndex = 0;
        private int mistakes = 0;
        private int streak = 0;
        private int activeCurse = CURSE_NONE;
        private int selectedShopIndex = -1;

        private float multiplier = 1.0f;
        private boolean chargeMode = false;
        private boolean roundSuccessful = false;
        private boolean bossRound = false;
        private boolean luckyUsed = false;
        private boolean vowelCandleUsed = false;
        private boolean echoUsed = false;
        private boolean insuranceUsed = false;

        private Category selectedCategory;
        private String[] phrasesThisRound = new String[0];
        private String currentPhrase = "";
        private final boolean[] guessed = new boolean[26];
        private final HashSet<Integer> ownedRelics = new HashSet<>();
        private int[] shopOfferIds = new int[0];
        private String lastMessage = "";
        private String resultTitle = "";
        private String resultMessage = "";

        GlyphboundView(Context context) {
            super(context);
            setFocusable(true);
            prefs = context.getSharedPreferences("glyphbound_stats", Context.MODE_PRIVATE);
            bestRound = prefs.getInt("best_round", 0);
            bestInk = prefs.getInt("best_ink", 0);
            categories = createCategories();
            relics = createRelics();
            bossPhrases = new String[]{
                    "FORGOTTEN WORDS HAVE SHARP TEETH",
                    "THE PAGE DEMANDS AN ANSWER",
                    "CURSED INK NEVER DRIES",
                    "SILENCE FEEDS THE BOOK",
                    "THE REDACTED KING RETURNS",
                    "EVERY LETTER HAS A PRICE"
            };
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            buttons.clear();
            canvas.drawColor(Color.rgb(14, 13, 20));

            if (screen == SCREEN_MENU) drawMenu(canvas);
            else if (screen == SCREEN_CATEGORY) drawCategoryChoice(canvas);
            else if (screen == SCREEN_GAME) drawGame(canvas);
            else if (screen == SCREEN_ROUND_RESULT) drawRoundResult(canvas);
            else if (screen == SCREEN_SHOP) drawShop(canvas);
            else if (screen == SCREEN_GAME_OVER) drawGameOver(canvas);
        }

        private void drawMenu(Canvas canvas) {
            drawTitle(canvas, "GLYPHBOUND", 118, 58, Color.rgb(235, 218, 169));
            drawCenteredText(canvas, "A hidden-phrase roguelike", getWidth() / 2f, 175, 28, Color.rgb(205, 190, 160), false);
            drawCenteredText(canvas, "Clear 3 cursed phrases, earn Ink, buy relics, survive the book.", getWidth() / 2f, 220, 24, Color.rgb(180, 170, 150), false);

            drawPanel(canvas, 50, 280, getWidth() - 50, 515, Color.rgb(27, 24, 36));
            drawCenteredText(canvas, "Best Round: " + bestRound, getWidth() / 2f, 350, 36, Color.WHITE, true);
            drawCenteredText(canvas, "Best Ink: " + bestInk, getWidth() / 2f, 405, 30, Color.rgb(235, 218, 169), false);
            drawCenteredText(canvas, "MVP build: categories, shop, relics, charge guesses and boss pages.", getWidth() / 2f, 465, 22, Color.rgb(170, 160, 145), false);

            drawButton(canvas, new RectF(70, getHeight() - 190, getWidth() - 70, getHeight() - 105), "START RUN", "START", true, Color.rgb(109, 72, 173), 34);
        }

        private void drawCategoryChoice(Canvas canvas) {
            String title = bossRoundNext() ? "CHOOSE THE BOSS PAGE" : "CHOOSE NEXT PAGE";
            drawTitle(canvas, title, 72, 38, Color.rgb(235, 218, 169));
            drawCenteredText(canvas, "Round " + round + "  |  Hearts " + hearts + "  |  Shop Ink " + shopInk, getWidth() / 2f, 118, 25, Color.rgb(210, 200, 180), false);
            drawCenteredText(canvas, "Your category controls the next " + (bossRoundNext() ? "boss phrase." : "3 phrases."), getWidth() / 2f, 153, 22, Color.rgb(170, 160, 145), false);

            float top = 205;
            float cardH = 122;
            for (int i = 0; i < categories.length; i++) {
                Category cat = categories[i];
                float y = top + i * (cardH + 18);
                RectF rect = new RectF(34, y, getWidth() - 34, y + cardH);
                drawPanel(canvas, rect.left, rect.top, rect.right, rect.bottom, cat.color);
                drawText(canvas, cat.name, rect.left + 24, y + 42, 32, Color.WHITE, true, Paint.Align.LEFT);
                drawText(canvas, cat.difficulty + "  |  " + String.format(Locale.US, "%.2fx Ink", cat.rewardMultiplier), rect.left + 24, y + 76, 23, Color.rgb(235, 218, 169), false, Paint.Align.LEFT);
                drawText(canvas, cat.identity, rect.left + 24, y + 106, 21, Color.rgb(230, 226, 216), false, Paint.Align.LEFT);
                buttons.add(new ButtonHitbox(rect, "CAT_" + i, true));
            }
        }

        private void drawGame(Canvas canvas) {
            drawTopHud(canvas);
            drawEffigy(canvas, mistakes);
            drawPhraseArea(canvas);
            drawAlphabet(canvas);
            drawRelicStrip(canvas);
        }

        private void drawTopHud(Canvas canvas) {
            drawText(canvas, "Round " + round + (bossRound ? " - BOSS" : ""), 28, 42, 25, Color.WHITE, true, Paint.Align.LEFT);
            drawText(canvas, "Hearts " + hearts, getWidth() - 28, 42, 25, Color.rgb(255, 130, 130), true, Paint.Align.RIGHT);
            drawText(canvas, selectedCategory.name + "  |  Phrase " + (phraseIndex + 1) + "/" + phrasesThisRound.length, 28, 76, 22, Color.rgb(205, 190, 160), false, Paint.Align.LEFT);

            float barLeft = 28;
            float barTop = 96;
            float barRight = getWidth() - 28;
            float barBottom = 126;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(42, 36, 56));
            canvas.drawRoundRect(new RectF(barLeft, barTop, barRight, barBottom), 16, 16, paint);
            float progress = Math.min(1f, inkThisRound / (float) Math.max(1, targetInk));
            paint.setColor(Color.rgb(235, 181, 74));
            canvas.drawRoundRect(new RectF(barLeft, barTop, barLeft + (barRight - barLeft) * progress, barBottom), 16, 16, paint);
            drawCenteredText(canvas, inkThisRound + " / " + targetInk + " Ink", getWidth() / 2f, 119, 19, Color.WHITE, true);

            String curseText = activeCurse == CURSE_NONE ? "No curse" : activeCurseName();
            drawText(canvas, curseText, 28, 156, 21, activeCurse == CURSE_NONE ? Color.rgb(145, 135, 125) : Color.rgb(255, 120, 120), false, Paint.Align.LEFT);
            drawText(canvas, "x" + String.format(Locale.US, "%.1f", multiplier) + "  Streak " + streak, getWidth() - 28, 156, 21, Color.rgb(150, 220, 170), true, Paint.Align.RIGHT);
        }

        private void drawEffigy(Canvas canvas, int stage) {
            float left = 52;
            float top = 178;
            float right = getWidth() - 52;
            float bottom = 342;
            drawPanel(canvas, left, top, right, bottom, Color.rgb(25, 22, 33));
            drawText(canvas, "Cursed Effigy", left + 22, top + 34, 23, Color.rgb(235, 218, 169), true, Paint.Align.LEFT);
            drawText(canvas, "Mistakes " + Math.min(stage, MAX_MISTAKES) + "/" + MAX_MISTAKES, right - 22, top + 34, 23, stage >= 5 ? Color.rgb(255, 95, 95) : Color.rgb(205, 190, 160), true, Paint.Align.RIGHT);

            float cx = getWidth() / 2f;
            float gy = top + 55;
            paint.setStrokeWidth(7f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.rgb(105, 92, 125));
            canvas.drawLine(cx - 110, bottom - 20, cx + 110, bottom - 20, paint);
            canvas.drawLine(cx - 90, bottom - 20, cx - 90, gy, paint);
            canvas.drawLine(cx - 90, gy, cx + 30, gy, paint);
            canvas.drawLine(cx + 30, gy, cx + 30, gy + 25, paint);

            paint.setStrokeWidth(8f);
            paint.setColor(stage >= 5 ? Color.rgb(230, 70, 80) : Color.rgb(220, 210, 190));
            paint.setStyle(Paint.Style.STROKE);
            if (stage >= 1) canvas.drawCircle(cx + 30, gy + 45, 20, paint);
            if (stage >= 2) canvas.drawLine(cx + 30, gy + 65, cx + 30, gy + 105, paint);
            if (stage >= 3) canvas.drawLine(cx + 30, gy + 80, cx, gy + 96, paint);
            if (stage >= 4) canvas.drawLine(cx + 30, gy + 80, cx + 60, gy + 96, paint);
            if (stage >= 5) canvas.drawLine(cx + 30, gy + 105, cx + 8, gy + 136, paint);
            if (stage >= 6) canvas.drawLine(cx + 30, gy + 105, cx + 55, gy + 136, paint);

            if (stage >= 5) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(55, 255, 0, 0));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        }

        private void drawPhraseArea(Canvas canvas) {
            float top = 370;
            float bottom = 595;
            drawPanel(canvas, 28, top, getWidth() - 28, bottom, Color.rgb(31, 28, 43));
            drawCenteredText(canvas, "Category: " + selectedCategory.name, getWidth() / 2f, top + 34, 23, Color.rgb(235, 218, 169), true);
            drawHiddenPhrase(canvas, currentPhrase, top + 84, bottom - 45);
            drawCenteredText(canvas, lastMessage, getWidth() / 2f, bottom - 17, 20, Color.rgb(205, 198, 180), false);

            RectF chargeRect = new RectF(38, bottom + 18, getWidth() - 38, bottom + 80);
            int chargeColor = chargeMode ? Color.rgb(174, 67, 92) : Color.rgb(62, 55, 78);
            drawButton(canvas, chargeRect, chargeMode ? "CHARGE GUESS ON - next wrong costs 2" : "CHARGE GUESS OFF - tap to risk double Ink", "CHARGE", true, chargeColor, 23);
        }

        private void drawAlphabet(Canvas canvas) {
            float startY = 705;
            float margin = 18;
            float gap = 8;
            int cols = 7;
            float buttonW = (getWidth() - margin * 2 - gap * (cols - 1)) / cols;
            float buttonH = 54;
            for (int i = 0; i < 26; i++) {
                int row = i / cols;
                int col = i % cols;
                float x = margin + col * (buttonW + gap);
                float y = startY + row * (buttonH + gap);
                char letter = (char) ('A' + i);
                boolean used = guessed[i];
                boolean enabled = !used;
                int fill = used ? Color.rgb(36, 33, 45) : Color.rgb(75, 63, 101);
                drawButton(canvas, new RectF(x, y, x + buttonW, y + buttonH), String.valueOf(letter), "LETTER_" + letter, enabled, fill, 25);
            }
        }

        private void drawRelicStrip(Canvas canvas) {
            float y = getHeight() - 78;
            drawPanel(canvas, 20, y, getWidth() - 20, getHeight() - 18, Color.rgb(22, 20, 30));
            String text = ownedRelics.isEmpty() ? "Relics: none yet" : "Relics: " + ownedRelicNames();
            drawText(canvas, text, 38, y + 38, 19, Color.rgb(215, 204, 176), false, Paint.Align.LEFT);
        }

        private void drawRoundResult(Canvas canvas) {
            drawTitle(canvas, resultTitle, 76, 42, roundSuccessful ? Color.rgb(150, 230, 160) : Color.rgb(255, 120, 120));
            drawPanel(canvas, 40, 180, getWidth() - 40, 520, Color.rgb(31, 28, 43));
            drawCenteredText(canvas, resultMessage, getWidth() / 2f, 250, 28, Color.WHITE, true);
            drawCenteredText(canvas, "Round Ink: " + inkThisRound + " / " + targetInk, getWidth() / 2f, 315, 28, Color.rgb(235, 218, 169), false);
            drawCenteredText(canvas, "Hearts: " + hearts + "  |  Shop Ink: " + shopInk, getWidth() / 2f, 365, 26, Color.rgb(210, 200, 180), false);
            String next = roundSuccessful ? "Continue to shop" : (hearts > 0 ? "Continue, wounded" : "The book closes");
            drawButton(canvas, new RectF(65, getHeight() - 170, getWidth() - 65, getHeight() - 88), next.toUpperCase(Locale.US), "RESULT_CONTINUE", true, Color.rgb(109, 72, 173), 28);
        }

        private void drawShop(Canvas canvas) {
            drawTitle(canvas, "RELIC SHOP", 78, 42, Color.rgb(235, 218, 169));
            drawCenteredText(canvas, "Spend excess Ink. Choose one relic, or skip.", getWidth() / 2f, 122, 23, Color.rgb(190, 180, 160), false);
            drawCenteredText(canvas, "Shop Ink: " + shopInk, getWidth() / 2f, 160, 30, Color.rgb(235, 181, 74), true);

            float top = 215;
            float cardH = 142;
            for (int i = 0; i < shopOfferIds.length; i++) {
                Relic relic = relics[shopOfferIds[i]];
                float y = top + i * (cardH + 20);
                RectF rect = new RectF(36, y, getWidth() - 36, y + cardH);
                boolean canBuy = shopInk >= relic.cost;
                int fill = canBuy ? Color.rgb(39, 34, 56) : Color.rgb(29, 27, 35);
                drawPanel(canvas, rect.left, rect.top, rect.right, rect.bottom, fill);
                drawText(canvas, relic.name + "  -  " + relic.cost + " Ink", rect.left + 22, y + 38, 27, canBuy ? Color.WHITE : Color.rgb(135, 130, 140), true, Paint.Align.LEFT);
                drawWrappedText(canvas, relic.description, rect.left + 22, y + 72, rect.right - 22, 22, canBuy ? Color.rgb(220, 210, 190) : Color.rgb(125, 120, 130));
                buttons.add(new ButtonHitbox(rect, "SHOP_" + i, canBuy));
            }

            drawButton(canvas, new RectF(54, getHeight() - 112, getWidth() - 54, getHeight() - 42), "SKIP SHOP", "SHOP_SKIP", true, Color.rgb(62, 55, 78), 27);
        }

        private void drawGameOver(Canvas canvas) {
            drawTitle(canvas, "GAME OVER", 92, 48, Color.rgb(255, 120, 120));
            drawPanel(canvas, 38, 190, getWidth() - 38, 555, Color.rgb(31, 28, 43));
            drawCenteredText(canvas, "The cursed book got its answer.", getWidth() / 2f, 260, 29, Color.WHITE, true);
            drawCenteredText(canvas, "Round reached: " + round, getWidth() / 2f, 335, 31, Color.rgb(235, 218, 169), false);
            drawCenteredText(canvas, "Total Ink: " + totalInk, getWidth() / 2f, 388, 31, Color.rgb(235, 181, 74), false);
            drawCenteredText(canvas, "Relics collected: " + ownedRelics.size(), getWidth() / 2f, 441, 27, Color.rgb(210, 200, 180), false);
            drawButton(canvas, new RectF(65, getHeight() - 190, getWidth() - 65, getHeight() - 110), "NEW RUN", "RESTART", true, Color.rgb(109, 72, 173), 31);
            drawButton(canvas, new RectF(65, getHeight() - 92, getWidth() - 65, getHeight() - 32), "MAIN MENU", "MENU", true, Color.rgb(62, 55, 78), 24);
        }

        private void startRun() {
            round = 1;
            hearts = STARTING_HEARTS;
            totalInk = 0;
            shopInk = 0;
            targetInk = 500;
            inkThisRound = 0;
            phraseIndex = 0;
            activeCurse = CURSE_NONE;
            selectedCategory = null;
            ownedRelics.clear();
            insuranceUsed = false;
            lastMessage = "Choose your first page.";
            screen = SCREEN_CATEGORY;
            invalidate();
        }

        private void startRound(Category category) {
            selectedCategory = category;
            bossRound = bossRoundNext();
            inkThisRound = 0;
            phraseIndex = 0;
            targetInk = 500 + (round - 1) * 300;
            if (bossRound) targetInk = Math.round(targetInk * 1.25f);

            activeCurse = CURSE_NONE;
            if (round >= 3) {
                if (bossRound) activeCurse = CURSE_RED_MARGIN;
                else if (round % 2 == 0) activeCurse = CURSE_HEAVY_TARGET;
            }
            if (activeCurse == CURSE_HEAVY_TARGET) targetInk = Math.round(targetInk * 1.20f);
            if (hasRelic(RELIC_BLACK_CANDLE)) targetInk = Math.round(targetInk * 1.15f);

            if (bossRound) {
                phrasesThisRound = new String[]{bossPhrases[random.nextInt(bossPhrases.length)]};
            } else {
                phrasesThisRound = pickPhrases(category.phrases, PHRASES_PER_ROUND);
            }
            startPhrase();
            screen = SCREEN_GAME;
            invalidate();
        }

        private void startPhrase() {
            currentPhrase = phrasesThisRound[phraseIndex].toUpperCase(Locale.US);
            for (int i = 0; i < guessed.length; i++) guessed[i] = false;
            mistakes = 0;
            streak = 0;
            multiplier = 1.0f;
            chargeMode = false;
            luckyUsed = false;
            vowelCandleUsed = false;
            echoUsed = false;
            lastMessage = bossRound ? "Boss phrase. No panic guesses." : "Guess a letter or charge your next guess.";
        }

        private void processLetter(char letter) {
            int index = letter - 'A';
            if (index < 0 || index >= 26 || guessed[index]) return;

            boolean charged = chargeMode;
            chargeMode = false;
            guessed[index] = true;

            int count = countLetter(currentPhrase, letter);
            if (count > 0) {
                int base = letterValue(letter);
                boolean vowel = isVowel(letter);
                if (hasRelic(RELIC_SOFT_VOWELS) && vowel) {
                    base = 0;
                }

                float gainFloat = base * count * multiplier * selectedCategory.rewardMultiplier;
                if (charged) gainFloat *= 2f;
                int gain = Math.round(gainFloat);

                if (hasRelic(RELIC_DOUBLE_TAP) && count >= 2) gain += Math.round(100 * selectedCategory.rewardMultiplier);

                inkThisRound += gain;
                totalInk += gain;
                streak++;

                float increase = hasRelic(RELIC_CHAIN_SCRIPT) ? 0.2f : 0.1f;
                if (count >= 3) increase += 0.2f;
                if (hasRelic(RELIC_SOFT_VOWELS) && vowel) increase += 0.1f;
                multiplier += increase;

                lastMessage = (charged ? "Charged " : "") + letter + " hit " + count + " tile" + (count == 1 ? "" : "s") + " for " + gain + " Ink.";

                if (hasRelic(RELIC_ECHO_LETTER) && !echoUsed) {
                    char echo = revealRandomHiddenLetter();
                    echoUsed = true;
                    if (echo != 0) lastMessage += " Echo revealed " + echo + ".";
                }

                if (isPhraseComplete()) clearPhrase();
            } else {
                handleWrongGuess(letter, charged);
            }
            invalidate();
        }

        private void handleWrongGuess(char letter, boolean charged) {
            int cost = charged ? 2 : 1;
            boolean protectedGuess = false;
            if (hasRelic(RELIC_LUCKY_INK) && !luckyUsed) {
                luckyUsed = true;
                protectedGuess = true;
                cost = 0;
            } else if (hasRelic(RELIC_VOWEL_CANDLE) && isVowel(letter) && !vowelCandleUsed) {
                vowelCandleUsed = true;
                protectedGuess = true;
                cost = 0;
            }

            mistakes = Math.min(MAX_MISTAKES, mistakes + cost);
            multiplier = 1.0f;
            streak = 0;

            lastMessage = protectedGuess ? letter + " missed, but a relic blocked the damage." : letter + " missed. The effigy stirs.";
            if (activeCurse == CURSE_RED_MARGIN) {
                inkThisRound = Math.max(0, inkThisRound - 100);
                lastMessage += " Red Margin burned 100 Ink.";
            }
            if (mistakes >= MAX_MISTAKES) failPhrase();
        }

        private void clearPhrase() {
            int remaining = Math.max(0, MAX_MISTAKES - mistakes);
            int bonus = Math.round((100 + remaining * 40) * selectedCategory.rewardMultiplier);
            if (mistakes == 0) bonus += 250;
            if (hasRelic(RELIC_GOLDEN_MARGIN)) bonus += remaining * 75;
            if (hasRelic(RELIC_LAST_BREATH) && remaining == 1) bonus += 300;
            inkThisRound += bonus;
            totalInk += bonus;

            phraseIndex++;
            if (phraseIndex >= phrasesThisRound.length) {
                lastMessage = "Page cleared. +" + bonus + " bonus Ink.";
                finishRound();
            } else {
                startPhrase();
                lastMessage = "Phrase cleared. +" + bonus + " bonus Ink. Next phrase.";
            }
        }

        private void failPhrase() {
            phraseIndex++;
            if (phraseIndex >= phrasesThisRound.length) {
                finishRound();
            } else {
                startPhrase();
                lastMessage = "A phrase was lost to the curse. Next phrase.";
            }
        }

        private void finishRound() {
            roundSuccessful = inkThisRound >= targetInk;
            if (roundSuccessful) {
                int excess = Math.max(0, inkThisRound - targetInk);
                int gainedShopInk = 200 + round * 50 + excess;
                if (hasRelic(RELIC_BLACK_CANDLE)) gainedShopInk = Math.round(gainedShopInk * 1.4f);
                shopInk += gainedShopInk;
                resultTitle = bossRound ? "BOSS PAGE BROKEN" : "PAGE SURVIVED";
                resultMessage = "+" + gainedShopInk + " shop Ink earned";
            } else {
                boolean insuranceBlocked = hasRelic(RELIC_INK_INSURANCE) && !insuranceUsed;
                if (insuranceBlocked) {
                    insuranceUsed = true;
                    resultTitle = "INSURANCE TRIGGERED";
                    resultMessage = "Target missed, but Ink Insurance blocked the heart loss.";
                } else {
                    hearts--;
                    resultTitle = hearts > 0 ? "PAGE FAILED" : "THE BOOK WINS";
                    resultMessage = "Target missed. Lost 1 heart.";
                }
                if (hearts <= 0) endRun(false);
            }
            screen = hearts <= 0 ? SCREEN_GAME_OVER : SCREEN_ROUND_RESULT;
        }

        private void prepareShop() {
            ArrayList<Integer> available = new ArrayList<>();
            for (Relic relic : relics) {
                if (!ownedRelics.contains(relic.id)) available.add(relic.id);
            }
            Collections.shuffle(available, random);
            int offerCount = Math.min(3, available.size());
            shopOfferIds = new int[offerCount];
            for (int i = 0; i < offerCount; i++) shopOfferIds[i] = available.get(i);
            if (offerCount == 0) shopOfferIds = new int[]{RELIC_LUCKY_INK};
        }

        private void buyRelic(int index) {
            if (index < 0 || index >= shopOfferIds.length) return;
            Relic relic = relics[shopOfferIds[index]];
            if (shopInk < relic.cost) return;
            shopInk -= relic.cost;
            ownedRelics.add(relic.id);
            round++;
            screen = SCREEN_CATEGORY;
            lastMessage = relic.name + " added.";
            invalidate();
        }

        private void skipShop() {
            round++;
            screen = SCREEN_CATEGORY;
            invalidate();
        }

        private void continueResult() {
            if (roundSuccessful) {
                prepareShop();
                screen = SCREEN_SHOP;
            } else if (hearts <= 0) {
                screen = SCREEN_GAME_OVER;
            } else {
                round++;
                screen = SCREEN_CATEGORY;
            }
            invalidate();
        }

        private void endRun(boolean force) {
            if (round > bestRound) bestRound = round;
            if (totalInk > bestInk) bestInk = totalInk;
            prefs.edit().putInt("best_round", bestRound).putInt("best_ink", bestInk).apply();
            if (force) screen = SCREEN_GAME_OVER;
        }

        private void handleAction(String action) {
            if (action.equals("START")) startRun();
            else if (action.equals("MENU")) {
                screen = SCREEN_MENU;
                invalidate();
            } else if (action.equals("RESTART")) startRun();
            else if (action.startsWith("CAT_")) {
                int index = Integer.parseInt(action.substring(4));
                startRound(categories[index]);
            } else if (action.startsWith("LETTER_")) {
                processLetter(action.charAt(7));
            } else if (action.equals("CHARGE")) {
                chargeMode = !chargeMode;
                invalidate();
            } else if (action.equals("RESULT_CONTINUE")) continueResult();
            else if (action.startsWith("SHOP_")) {
                int index = Integer.parseInt(action.substring(5));
                buyRelic(index);
            } else if (action.equals("SHOP_SKIP")) skipShop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                float y = event.getY();
                for (ButtonHitbox button : buttons) {
                    if (button.enabled && button.rect.contains(x, y)) {
                        handleAction(button.action);
                        return true;
                    }
                }
            }
            return true;
        }

        private boolean bossRoundNext() {
            return round > 0 && round % 3 == 0;
        }

        private String[] pickPhrases(String[] source, int count) {
            ArrayList<String> list = new ArrayList<>();
            Collections.addAll(list, source);
            Collections.shuffle(list, random);
            String[] result = new String[Math.min(count, list.size())];
            for (int i = 0; i < result.length; i++) result[i] = list.get(i);
            return result;
        }

        private int countLetter(String phrase, char letter) {
            int count = 0;
            for (int i = 0; i < phrase.length(); i++) {
                if (phrase.charAt(i) == letter) count++;
            }
            return count;
        }

        private boolean isPhraseComplete() {
            for (int i = 0; i < currentPhrase.length(); i++) {
                char c = currentPhrase.charAt(i);
                if (c >= 'A' && c <= 'Z' && !guessed[c - 'A']) return false;
            }
            return true;
        }

        private char revealRandomHiddenLetter() {
            ArrayList<Character> hidden = new ArrayList<>();
            for (int i = 0; i < currentPhrase.length(); i++) {
                char c = currentPhrase.charAt(i);
                if (c >= 'A' && c <= 'Z' && !guessed[c - 'A'] && !hidden.contains(c)) hidden.add(c);
            }
            if (hidden.isEmpty()) return 0;
            char c = hidden.get(random.nextInt(hidden.size()));
            guessed[c - 'A'] = true;
            return c;
        }

        private int letterValue(char letter) {
            boolean rare = "JQXZVK".indexOf(letter) >= 0;
            boolean common = "EARIOTNSL".indexOf(letter) >= 0;
            int value = rare ? 60 : (common ? 20 : 30);
            if (rare && hasRelic(RELIC_RARE_INK)) value *= 2;
            if (!isVowel(letter) && hasRelic(RELIC_SHARP_CONSONANTS)) value = Math.round(value * 1.25f);
            return value;
        }

        private boolean isVowel(char c) {
            return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
        }

        private boolean hasRelic(int id) {
            return ownedRelics.contains(id);
        }

        private String activeCurseName() {
            if (activeCurse == CURSE_RED_MARGIN) return "Curse: Red Margin - wrong guesses burn 100 Ink";
            if (activeCurse == CURSE_HEAVY_TARGET) return "Curse: Heavy Target - target Ink is higher";
            return "No curse";
        }

        private String ownedRelicNames() {
            StringBuilder builder = new StringBuilder();
            int i = 0;
            for (Integer id : ownedRelics) {
                if (i > 0) builder.append(", ");
                builder.append(relics[id].name);
                i++;
                if (i >= 3 && ownedRelics.size() > 3) {
                    builder.append(" +").append(ownedRelics.size() - 3).append(" more");
                    break;
                }
            }
            return builder.toString();
        }

        private Category[] createCategories() {
            return new Category[]{
                    new Category("Food", "Easy", 0.90f, "Safe phrases, common letters", Color.rgb(69, 91, 54), new String[]{
                            "SPICY NOODLES", "BURGER AND FRIES", "GARLIC BREAD", "CHICKEN WINGS", "FISH AND CHIPS",
                            "HOT SAUCE", "CHEESE PIZZA", "SUNDAY ROAST", "FRIED RICE", "TOMATO SOUP"
                    }),
                    new Category("Animals", "Easy", 0.90f, "Shorter words, safer guessing", Color.rgb(58, 87, 91), new String[]{
                            "BLACK CAT", "GOLDEN EAGLE", "WILD HORSE", "SLEEPY PANDA", "HUNGRY WOLF",
                            "RIVER OTTER", "GIANT SQUID", "DESERT FOX", "ANGRY GOOSE", "TINY FROG"
                    }),
                    new Category("Cinema", "Medium", 1.00f, "Longer social movie phrases", Color.rgb(82, 65, 106), new String[]{
                            "FINAL SCENE", "HERO RETURNS", "DARK THEATER", "LOST TREASURE", "SPACE BATTLE",
                            "SECRET AGENT", "MONSTER ATTACK", "OPENING NIGHT", "CHASE SEQUENCE", "END CREDITS"
                    }),
                    new Category("Mythology", "Hard", 1.25f, "Rare letters, bigger payouts", Color.rgb(106, 66, 54), new String[]{
                            "DRAGON FIRE", "ANCIENT ORACLE", "CURSED TEMPLE", "GOLDEN FLEECE", "SHADOW TITAN",
                            "PHOENIX ASHES", "FORGOTTEN GODS", "MINOTAUR MAZE", "CELESTIAL SPEAR", "UNDERWORLD GATE"
                    })
            };
        }

        private Relic[] createRelics() {
            return new Relic[]{
                    new Relic(RELIC_LUCKY_INK, "Lucky Ink", "First wrong guess each phrase does not damage the effigy.", 250),
                    new Relic(RELIC_DOUBLE_TAP, "Double Tap", "Letters appearing 2+ times give +100 bonus Ink.", 300),
                    new Relic(RELIC_RARE_INK, "Rare Ink", "Rare letters J, Q, X, Z, V and K are worth double Ink.", 350),
                    new Relic(RELIC_VOWEL_CANDLE, "Vowel Candle", "First wrong vowel each phrase is free.", 250),
                    new Relic(RELIC_CHAIN_SCRIPT, "Chain Script", "Correct guesses grow multiplier by +0.2x instead of +0.1x.", 450),
                    new Relic(RELIC_BLACK_CANDLE, "Black Candle", "Targets rise by 15%, but shop Ink rewards rise by 40%.", 400),
                    new Relic(RELIC_SHARP_CONSONANTS, "Sharp Consonants", "Consonants give +25% Ink.", 300),
                    new Relic(RELIC_SOFT_VOWELS, "Soft Vowels", "Correct vowels give no Ink, but build extra multiplier.", 250),
                    new Relic(RELIC_LAST_BREATH, "Last Breath", "Clearing a phrase with exactly 1 mistake left gives +300 Ink.", 350),
                    new Relic(RELIC_GOLDEN_MARGIN, "Golden Margin", "Phrase clear bonuses gain +75 Ink per mistake remaining.", 450),
                    new Relic(RELIC_ECHO_LETTER, "Echo Letter", "First correct guess each phrase reveals one extra hidden letter.", 500),
                    new Relic(RELIC_INK_INSURANCE, "Ink Insurance", "First missed target each run does not cost a heart.", 500)
            };
        }

        private void drawTitle(Canvas canvas, String text, float y, float size, int color) {
            drawCenteredText(canvas, text, getWidth() / 2f, y, size, color, true);
        }

        private void drawPanel(Canvas canvas, float left, float top, float right, float bottom, int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRoundRect(new RectF(left, top, right, bottom), 24, 24, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(Color.argb(70, 255, 255, 255));
            canvas.drawRoundRect(new RectF(left, top, right, bottom), 24, 24, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawButton(Canvas canvas, RectF rect, String label, String action, boolean enabled, int fill, float textSize) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(enabled ? fill : Color.rgb(40, 38, 46));
            canvas.drawRoundRect(rect, 18, 18, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(Color.argb(enabled ? 110 : 45, 255, 255, 255));
            canvas.drawRoundRect(rect, 18, 18, paint);
            paint.setStyle(Paint.Style.FILL);
            drawCenteredText(canvas, label, rect.centerX(), rect.centerY() + textSize * 0.35f, textSize, enabled ? Color.WHITE : Color.rgb(120, 120, 128), true);
            if (action != null) buttons.add(new ButtonHitbox(new RectF(rect), action, enabled));
        }

        private void drawCenteredText(Canvas canvas, String text, float x, float y, float size, int color, boolean bold) {
            drawText(canvas, text, x, y, size, color, bold, Paint.Align.CENTER);
        }

        private void drawText(Canvas canvas, String text, float x, float y, float size, int color, boolean bold, Paint.Align align) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(align);
            paint.setFakeBoldText(bold);
            canvas.drawText(text, x, y, paint);
            paint.setFakeBoldText(false);
        }

        private void drawWrappedText(Canvas canvas, String text, float left, float y, float right, float size, int color) {
            paint.setTextSize(size);
            paint.setFakeBoldText(false);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(color);
            String[] words = text.split(" ");
            String line = "";
            float lineY = y;
            for (String word : words) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (paint.measureText(test) > right - left) {
                    canvas.drawText(line, left, lineY, paint);
                    line = word;
                    lineY += size + 6;
                } else {
                    line = test;
                }
            }
            if (line.length() > 0) canvas.drawText(line, left, lineY, paint);
        }

        private void drawHiddenPhrase(Canvas canvas, String phrase, float top, float bottom) {
            String[] words = phrase.split(" ");
            ArrayList<String> lines = new ArrayList<>();
            String line = "";
            paint.setTextSize(36);
            for (String word : words) {
                String displayWord = hiddenWord(word);
                String test = line.length() == 0 ? displayWord : line + "   " + displayWord;
                if (paint.measureText(test) > getWidth() - 86) {
                    lines.add(line);
                    line = displayWord;
                } else {
                    line = test;
                }
            }
            if (line.length() > 0) lines.add(line);

            float totalHeight = lines.size() * 46f;
            float y = top + ((bottom - top) - totalHeight) / 2f + 36;
            for (String l : lines) {
                drawCenteredText(canvas, l, getWidth() / 2f, y, 36, Color.WHITE, true);
                y += 46;
            }
        }

        private String hiddenWord(String word) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (c >= 'A' && c <= 'Z') {
                    builder.append(guessed[c - 'A'] ? c : '_');
                    if (i < word.length() - 1) builder.append(' ');
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

        private static class ButtonHitbox {
            final RectF rect;
            final String action;
            final boolean enabled;

            ButtonHitbox(RectF rect, String action, boolean enabled) {
                this.rect = rect;
                this.action = action;
                this.enabled = enabled;
            }
        }

        private static class Category {
            final String name;
            final String difficulty;
            final float rewardMultiplier;
            final String identity;
            final int color;
            final String[] phrases;

            Category(String name, String difficulty, float rewardMultiplier, String identity, int color, String[] phrases) {
                this.name = name;
                this.difficulty = difficulty;
                this.rewardMultiplier = rewardMultiplier;
                this.identity = identity;
                this.color = color;
                this.phrases = phrases;
            }
        }

        private static class Relic {
            final int id;
            final String name;
            final String description;
            final int cost;

            Relic(int id, String name, String description, int cost) {
                this.id = id;
                this.name = name;
                this.description = description;
                this.cost = cost;
            }
        }
    }
}
