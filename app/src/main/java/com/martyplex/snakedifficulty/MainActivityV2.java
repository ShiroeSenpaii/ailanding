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

public class MainActivityV2 extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new GlyphboundV2View(this));
    }

    private static class GlyphboundV2View extends View {
        private static final int MENU = 0;
        private static final int CATEGORY = 1;
        private static final int GAME = 2;
        private static final int REVEAL = 3;
        private static final int RESULT = 4;
        private static final int SHOP = 5;
        private static final int GAME_OVER = 6;

        private static final int MAX_MISTAKES = 6;
        private static final int PHRASES_PER_ROUND = 3;
        private static final int STARTING_HEARTS = 3;
        private static final int REROLL_BASE_COST = 70;

        private static final String[] QWERTY_ROWS = {"QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ArrayList<Hit> hits = new ArrayList<>();
        private final Random random = new Random();
        private final SharedPreferences prefs;
        private final Category[] categories;
        private final Relic[] relics;
        private final String[] bossPhrases;

        private int screen = MENU;
        private int round = 1;
        private int hearts = STARTING_HEARTS;
        private int bestRound;
        private int bestInk;
        private int totalInk;
        private int shopInk;
        private int inkRound;
        private int targetInk;
        private int phraseIndex;
        private int mistakes;
        private int streak;
        private int revealDelayMs = 1800;
        private int roundShopGain;
        private int rerollsThisShop;
        private int lastGain;
        private int pulseColor = 0;
        private long pulseUntil = 0L;

        private float multiplier = 1.0f;
        private boolean chargeMode;
        private boolean bossRound;
        private boolean roundSuccess;
        private boolean inputLocked;
        private boolean luckyUsed;
        private boolean vowelShieldUsed;
        private boolean echoUsed;
        private boolean insuranceUsed;

        private String currentPhrase = "";
        private String revealTitle = "";
        private String revealLine = "";
        private String message = "";
        private String resultTitle = "";
        private String resultLine = "";
        private Category activeCategory;
        private String[] roundPhrases = new String[0];
        private final boolean[] guessed = new boolean[26];
        private final HashSet<Integer> ownedRelics = new HashSet<>();
        private int[] shopOffers = new int[0];

        GlyphboundV2View(Context context) {
            super(context);
            setFocusable(true);
            prefs = context.getSharedPreferences("glyphbound_v2_stats", Context.MODE_PRIVATE);
            bestRound = prefs.getInt("best_round", 0);
            bestInk = prefs.getInt("best_ink", 0);
            categories = makeCategories();
            relics = makeRelics();
            bossPhrases = new String[]{
                    "THE PAGE DEMANDS AN ANSWER",
                    "CURSED INK NEVER DRIES",
                    "FORGOTTEN WORDS HAVE SHARP TEETH",
                    "THE REDACTED KING RETURNS",
                    "SILENCE FEEDS THE BOOK",
                    "EVERY LETTER HAS A PRICE"
            };
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            hits.clear();
            drawBackground(canvas);
            if (screen == MENU) drawMenu(canvas);
            else if (screen == CATEGORY) drawCategory(canvas);
            else if (screen == GAME) drawGame(canvas);
            else if (screen == REVEAL) drawReveal(canvas);
            else if (screen == RESULT) drawResult(canvas);
            else if (screen == SHOP) drawShop(canvas);
            else if (screen == GAME_OVER) drawGameOver(canvas);
        }

        private void drawBackground(Canvas canvas) {
            canvas.drawColor(Color.rgb(12, 10, 17));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(20, 17, 29));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            if (System.currentTimeMillis() < pulseUntil) {
                paint.setColor(pulseColor);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        }

        private void drawMenu(Canvas c) {
            text(c, "GLYPHBOUND", cx(), h(0.13f), 58, gold(), true, Paint.Align.CENTER);
            text(c, "Hidden phrase roguelike", cx(), h(0.19f), 27, muted(), false, Paint.Align.CENTER);
            panel(c, 38, h(0.27f), getWidth() - 38, h(0.52f), rgb(28, 24, 39));
            text(c, "Best Round", cx(), h(0.34f), 23, muted(), false, Paint.Align.CENTER);
            text(c, String.valueOf(bestRound), cx(), h(0.40f), 54, Color.WHITE, true, Paint.Align.CENTER);
            text(c, "Best Ink: " + bestInk, cx(), h(0.46f), 26, gold(), false, Paint.Align.CENTER);
            button(c, new RectF(50, h(0.80f), getWidth() - 50, h(0.88f)), "START RUN", "START", purple(), true, 32);
        }

        private void drawCategory(Canvas c) {
            text(c, bossRoundNext() ? "BOSS PAGE ROUTE" : "CHOOSE NEXT PAGE", cx(), h(0.07f), 34, gold(), true, Paint.Align.CENTER);
            text(c, "Round " + round + "   Hearts " + hearts + "   Shop Ink " + shopInk, cx(), h(0.11f), 21, muted(), false, Paint.Align.CENTER);
            int cols = 2;
            float gap = 14;
            float left = 20;
            float top = h(0.17f);
            float cardW = (getWidth() - left * 2 - gap) / 2f;
            float cardH = h(0.20f);
            for (int i = 0; i < categories.length; i++) {
                int row = i / cols;
                int col = i % cols;
                float x = left + col * (cardW + gap);
                float y = top + row * (cardH + gap);
                Category cat = categories[i];
                RectF r = new RectF(x, y, x + cardW, y + cardH);
                panel(c, r.left, r.top, r.right, r.bottom, cat.color);
                text(c, cat.name, r.left + 14, r.top + 34, 25, Color.WHITE, true, Paint.Align.LEFT);
                text(c, cat.diff, r.left + 14, r.top + 65, 18, gold(), false, Paint.Align.LEFT);
                text(c, String.format(Locale.US, "%.1fx Ink", cat.mult), r.right - 14, r.top + 65, 18, gold(), true, Paint.Align.RIGHT);
                wrapped(c, cat.note, r.left + 14, r.top + 96, r.right - 14, 17, Color.rgb(226, 218, 200));
                hits.add(new Hit(r, "CAT_" + i, true));
            }
            panel(c, 24, h(0.66f), getWidth() - 24, h(0.78f), rgb(25, 22, 34));
            text(c, "Route tip", 44, h(0.70f), 22, gold(), true, Paint.Align.LEFT);
            text(c, "Pick easier pages to survive, harder pages to farm Ink for the shop.", 44, h(0.735f), 18, muted(), false, Paint.Align.LEFT);
        }

        private void drawGame(Canvas c) {
            drawHud(c);
            drawEffigy(c, h(0.145f), h(0.335f));
            drawPhraseCard(c, h(0.355f), h(0.58f), false);
            drawActions(c);
            drawKeyboard(c);
        }

        private void drawHud(Canvas c) {
            text(c, "Round " + round + (bossRound ? "  BOSS" : ""), 16, 30, 20, Color.WHITE, true, Paint.Align.LEFT);
            text(c, "Hearts " + hearts, getWidth() - 16, 30, 20, red(), true, Paint.Align.RIGHT);
            text(c, activeCategory.name + "   Phrase " + (phraseIndex + 1) + "/" + roundPhrases.length, 16, 58, 17, muted(), false, Paint.Align.LEFT);
            text(c, "x" + String.format(Locale.US, "%.1f", multiplier) + "  Streak " + streak, getWidth() - 16, 58, 17, green(), true, Paint.Align.RIGHT);
            RectF bar = new RectF(16, 76, getWidth() - 16, 103);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(rgb(48, 41, 62));
            c.drawRoundRect(bar, 14, 14, paint);
            paint.setColor(orange());
            float progress = Math.min(1f, inkRound / (float) Math.max(1, targetInk));
            c.drawRoundRect(new RectF(bar.left, bar.top, bar.left + bar.width() * progress, bar.bottom), 14, 14, paint);
            text(c, inkRound + " / " + targetInk + " Ink", cx(), 97, 16, Color.WHITE, true, Paint.Align.CENTER);
            if (lastGain > 0) text(c, "+" + lastGain, getWidth() - 18, 125, 24, green(), true, Paint.Align.RIGHT);
        }

        private void drawEffigy(Canvas c, float top, float bottom) {
            panel(c, 18, top, getWidth() - 18, bottom, rgb(30, 24, 39));
            text(c, "Cursed Effigy", 36, top + 28, 19, gold(), true, Paint.Align.LEFT);
            text(c, mistakes + "/" + MAX_MISTAKES, getWidth() - 36, top + 28, 19, mistakes >= 5 ? red() : muted(), true, Paint.Align.RIGHT);
            float cx = cx();
            float gy = top + 50;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(7);
            paint.setColor(rgb(100, 83, 120));
            c.drawLine(cx - 105, bottom - 20, cx + 105, bottom - 20, paint);
            c.drawLine(cx - 70, bottom - 20, cx - 70, gy, paint);
            c.drawLine(cx - 70, gy, cx + 24, gy, paint);
            c.drawLine(cx + 24, gy, cx + 24, gy + 25, paint);
            paint.setStrokeWidth(8);
            paint.setColor(mistakes >= 5 ? red() : Color.rgb(230, 222, 205));
            if (mistakes >= 1) c.drawCircle(cx + 24, gy + 44, 18, paint);
            if (mistakes >= 2) c.drawLine(cx + 24, gy + 63, cx + 24, gy + 102, paint);
            if (mistakes >= 3) c.drawLine(cx + 24, gy + 78, cx - 5, gy + 96, paint);
            if (mistakes >= 4) c.drawLine(cx + 24, gy + 78, cx + 53, gy + 96, paint);
            if (mistakes >= 5) c.drawLine(cx + 24, gy + 102, cx + 4, gy + 132, paint);
            if (mistakes >= 6) c.drawLine(cx + 24, gy + 102, cx + 50, gy + 132, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawPhraseCard(Canvas c, float top, float bottom, boolean revealAll) {
            panel(c, 18, top, getWidth() - 18, bottom, rgb(29, 27, 42));
            text(c, activeCategory == null ? "" : activeCategory.name, cx(), top + 30, 19, gold(), true, Paint.Align.CENTER);
            drawPhrase(c, revealAll ? currentPhrase : maskedPhrase(), top + 58, bottom - 44);
            text(c, message, cx(), bottom - 17, 17, muted(), false, Paint.Align.CENTER);
        }

        private void drawActions(Canvas c) {
            float top = h(0.595f);
            RectF charge = new RectF(18, top, getWidth() - 18, top + h(0.052f));
            button(c, charge, chargeMode ? "CHARGE ON  -  next miss hurts more" : "CHARGE OFF  -  double next correct guess", "CHARGE", chargeMode ? rgb(168, 58, 83) : rgb(70, 61, 92), true, 18);
            text(c, ownedRelics.isEmpty() ? "Relics: none" : "Relics: " + relicNames(), 20, top + h(0.083f), 15, muted(), false, Paint.Align.LEFT);
        }

        private void drawKeyboard(Canvas c) {
            float keyboardTop = h(0.71f);
            float rowH = h(0.057f);
            float gap = 8;
            for (int r = 0; r < QWERTY_ROWS.length; r++) {
                String row = QWERTY_ROWS[r];
                float side = r == 0 ? 10 : (r == 1 ? 26 : 68);
                float keyW = (getWidth() - side * 2 - gap * (row.length() - 1)) / row.length();
                float y = keyboardTop + r * (rowH + gap);
                for (int i = 0; i < row.length(); i++) {
                    char letter = row.charAt(i);
                    boolean used = guessed[letter - 'A'];
                    RectF rect = new RectF(side + i * (keyW + gap), y, side + i * (keyW + gap) + keyW, y + rowH);
                    button(c, rect, String.valueOf(letter), "L_" + letter, used ? rgb(39, 35, 48) : rgb(83, 72, 112), !used && !inputLocked, 20);
                }
            }
        }

        private void drawReveal(Canvas c) {
            text(c, revealTitle, cx(), h(0.08f), 34, revealTitle.contains("LOST") ? red() : green(), true, Paint.Align.CENTER);
            drawPhraseCard(c, h(0.18f), h(0.49f), true);
            text(c, revealLine, cx(), h(0.56f), 24, Color.WHITE, true, Paint.Align.CENTER);
            text(c, "Full answer revealed", cx(), h(0.61f), 20, muted(), false, Paint.Align.CENTER);
        }

        private void drawResult(Canvas c) {
            text(c, resultTitle, cx(), h(0.08f), 34, roundSuccess ? green() : red(), true, Paint.Align.CENTER);
            panel(c, 28, h(0.20f), getWidth() - 28, h(0.46f), rgb(31, 27, 43));
            text(c, resultLine, cx(), h(0.27f), 25, Color.WHITE, true, Paint.Align.CENTER);
            text(c, "Round Ink: " + inkRound + " / " + targetInk, cx(), h(0.33f), 22, gold(), false, Paint.Align.CENTER);
            text(c, "Shop Ink: " + shopInk + "   Hearts: " + hearts, cx(), h(0.38f), 21, muted(), false, Paint.Align.CENTER);
            button(c, new RectF(42, h(0.82f), getWidth() - 42, h(0.895f)), roundSuccess ? "OPEN SHOP" : "CONTINUE", "RESULT_NEXT", purple(), true, 26);
        }

        private void drawShop(Canvas c) {
            text(c, "RELIC SHOP", cx(), h(0.065f), 34, gold(), true, Paint.Align.CENTER);
            text(c, "Buy multiple, reroll, or bank Ink for later.", cx(), h(0.10f), 17, muted(), false, Paint.Align.CENTER);
            text(c, "Shop Ink: " + shopInk, cx(), h(0.135f), 25, orange(), true, Paint.Align.CENTER);
            float left = 16;
            float top = h(0.18f);
            float gap = 12;
            float cardW = (getWidth() - left * 2 - gap) / 2f;
            float cardH = h(0.22f);
            for (int i = 0; i < shopOffers.length; i++) {
                int id = shopOffers[i];
                if (id < 0) continue;
                Relic relic = relics[id];
                int row = i / 2;
                int col = i % 2;
                RectF r = new RectF(left + col * (cardW + gap), top + row * (cardH + gap), left + col * (cardW + gap) + cardW, top + row * (cardH + gap) + cardH);
                boolean canBuy = shopInk >= relic.cost && !ownedRelics.contains(relic.id);
                panel(c, r.left, r.top, r.right, r.bottom, canBuy ? rgb(43, 36, 60) : rgb(31, 29, 38));
                text(c, relic.name, r.left + 12, r.top + 30, 20, canBuy ? Color.WHITE : Color.rgb(135, 130, 145), true, Paint.Align.LEFT);
                text(c, relic.cost + " Ink", r.left + 12, r.top + 57, 18, canBuy ? orange() : Color.rgb(125, 118, 130), true, Paint.Align.LEFT);
                wrapped(c, relic.shortText, r.left + 12, r.top + 86, r.right - 12, 16, canBuy ? Color.rgb(224, 216, 200) : Color.rgb(130, 126, 137));
                hits.add(new Hit(r, "BUY_" + i, canBuy));
            }
            float btnY = h(0.80f);
            button(c, new RectF(18, btnY, getWidth() / 2f - 8, btnY + h(0.07f)), "REROLL " + rerollCost(), "REROLL", rgb(75, 64, 95), shopInk >= rerollCost(), 22);
            button(c, new RectF(getWidth() / 2f + 8, btnY, getWidth() - 18, btnY + h(0.07f)), "LEAVE SHOP", "LEAVE_SHOP", purple(), true, 22);
        }

        private void drawGameOver(Canvas c) {
            text(c, "GAME OVER", cx(), h(0.10f), 45, red(), true, Paint.Align.CENTER);
            panel(c, 34, h(0.22f), getWidth() - 34, h(0.55f), rgb(31, 27, 43));
            text(c, "The book got its answer.", cx(), h(0.30f), 24, Color.WHITE, true, Paint.Align.CENTER);
            text(c, "Round reached: " + round, cx(), h(0.38f), 26, gold(), false, Paint.Align.CENTER);
            text(c, "Total Ink: " + totalInk, cx(), h(0.44f), 26, orange(), false, Paint.Align.CENTER);
            text(c, "Relics: " + ownedRelics.size(), cx(), h(0.50f), 22, muted(), false, Paint.Align.CENTER);
            button(c, new RectF(48, h(0.76f), getWidth() - 48, h(0.84f)), "NEW RUN", "RESTART", purple(), true, 28);
            button(c, new RectF(48, h(0.86f), getWidth() - 48, h(0.93f)), "MAIN MENU", "MENU", rgb(70, 61, 92), true, 22);
        }

        private void startRun() {
            round = 1;
            hearts = STARTING_HEARTS;
            totalInk = 0;
            shopInk = 0;
            inkRound = 0;
            phraseIndex = 0;
            ownedRelics.clear();
            insuranceUsed = false;
            inputLocked = false;
            message = "Choose your first page.";
            screen = CATEGORY;
            invalidate();
        }

        private void startRound(Category category) {
            activeCategory = category;
            bossRound = bossRoundNext();
            inkRound = 0;
            phraseIndex = 0;
            targetInk = 430 + (round - 1) * 250;
            if (bossRound) targetInk = Math.round(targetInk * 1.18f);
            if (hasRelic(5)) targetInk = Math.round(targetInk * 1.12f);
            roundPhrases = bossRound ? new String[]{bossPhrases[random.nextInt(bossPhrases.length)]} : choosePhrases(category.phrases, PHRASES_PER_ROUND);
            startPhrase();
            screen = GAME;
            invalidate();
        }

        private void startPhrase() {
            if (phraseIndex >= roundPhrases.length) {
                finishRound();
                return;
            }
            currentPhrase = roundPhrases[phraseIndex].toUpperCase(Locale.US);
            for (int i = 0; i < guessed.length; i++) guessed[i] = false;
            mistakes = 0;
            streak = 0;
            multiplier = 1.0f;
            chargeMode = false;
            luckyUsed = false;
            vowelShieldUsed = false;
            echoUsed = false;
            lastGain = 0;
            message = bossRound ? "Boss phrase. Earn enough Ink before the page closes." : "Guess letters. Charge when you are confident.";
        }

        private void guess(char letter) {
            if (inputLocked || screen != GAME) return;
            int idx = letter - 'A';
            if (idx < 0 || idx > 25 || guessed[idx]) return;
            guessed[idx] = true;
            boolean charged = chargeMode;
            chargeMode = false;
            int count = count(letter);
            if (count > 0) correctGuess(letter, count, charged);
            else wrongGuess(letter, charged);
            invalidate();
        }

        private void correctGuess(char letter, int count, boolean charged) {
            int base = value(letter);
            boolean vowel = isVowel(letter);
            if (hasRelic(7) && vowel) base = 0;
            int gain = Math.round(base * count * multiplier * activeCategory.mult);
            if (charged) gain *= 2;
            if (hasRelic(1) && count >= 2) gain += Math.round(45 * activeCategory.mult);
            inkRound += gain;
            totalInk += gain;
            lastGain = gain;
            streak++;
            multiplier += hasRelic(4) ? 0.16f : 0.08f;
            if (count >= 3) multiplier += 0.12f;
            if (hasRelic(7) && vowel) multiplier += 0.10f;
            message = (charged ? "Charged " : "") + letter + " hit " + count + " time" + (count == 1 ? "" : "s") + ".";
            pulse(Color.argb(45, 80, 255, 120));
            if (hasRelic(10) && !echoUsed) {
                char echoed = revealRandomLetter();
                echoUsed = true;
                if (echoed != 0) message += " Echo revealed " + echoed + ".";
            }
            if (complete()) phraseWon();
        }

        private void wrongGuess(char letter, boolean charged) {
            int cost = charged ? 2 : 1;
            if (hasRelic(0) && !luckyUsed) {
                luckyUsed = true;
                cost = 0;
                message = letter + " missed, but Lucky Ink blocked it.";
            } else if (hasRelic(3) && isVowel(letter) && !vowelShieldUsed) {
                vowelShieldUsed = true;
                cost = 0;
                message = letter + " missed, but Vowel Candle blocked it.";
            } else {
                message = letter + " missed. The effigy jumps.";
            }
            mistakes = Math.min(MAX_MISTAKES, mistakes + cost);
            multiplier = 1.0f;
            streak = 0;
            lastGain = 0;
            pulse(Color.argb(70, 255, 0, 0));
            if (bossRound) inkRound = Math.max(0, inkRound - 60);
            if (mistakes >= MAX_MISTAKES) phraseLost();
        }

        private void phraseWon() {
            int remaining = MAX_MISTAKES - mistakes;
            int bonus = Math.round((70 + remaining * 22) * activeCategory.mult);
            if (mistakes == 0) bonus += 110;
            if (hasRelic(8) && remaining == 1) bonus += 180;
            if (hasRelic(9)) bonus += remaining * 40;
            inkRound += bonus;
            totalInk += bonus;
            lastGain = bonus;
            revealTitle = "PHRASE CLEARED";
            revealLine = "+" + bonus + " clear bonus";
            showReveal(1700, true);
        }

        private void phraseLost() {
            revealTitle = "PHRASE LOST";
            revealLine = "The answer was revealed.";
            showReveal(2300, false);
        }

        private void showReveal(int delay, final boolean won) {
            inputLocked = true;
            revealDelayMs = delay;
            screen = REVEAL;
            invalidate();
            postDelayed(new Runnable() {
                @Override public void run() {
                    phraseIndex++;
                    inputLocked = false;
                    if (phraseIndex >= roundPhrases.length) finishRound();
                    else {
                        startPhrase();
                        screen = GAME;
                    }
                    invalidate();
                }
            }, revealDelayMs);
        }

        private void finishRound() {
            roundSuccess = inkRound >= targetInk;
            if (roundSuccess) {
                int excess = Math.max(0, inkRound - targetInk);
                roundShopGain = 160 + round * 35 + Math.round(excess * 0.45f);
                if (hasRelic(5)) roundShopGain = Math.round(roundShopGain * 1.35f);
                shopInk += roundShopGain;
                resultTitle = bossRound ? "BOSS PAGE BROKEN" : "PAGE SURVIVED";
                resultLine = "+" + roundShopGain + " shop Ink earned";
            } else {
                boolean saved = hasRelic(11) && !insuranceUsed;
                if (saved) {
                    insuranceUsed = true;
                    resultTitle = "INSURANCE TRIGGERED";
                    resultLine = "Target missed, but no heart was lost.";
                } else {
                    hearts--;
                    resultTitle = hearts <= 0 ? "THE BOOK WINS" : "PAGE FAILED";
                    resultLine = "Target missed. Lost 1 heart.";
                }
                if (hearts <= 0) endRun();
            }
            screen = hearts <= 0 ? GAME_OVER : RESULT;
            invalidate();
        }

        private void openShop() {
            rerollsThisShop = 0;
            makeShopOffers();
            screen = SHOP;
            invalidate();
        }

        private void makeShopOffers() {
            ArrayList<Integer> pool = new ArrayList<>();
            for (Relic r : relics) if (!ownedRelics.contains(r.id)) pool.add(r.id);
            Collections.shuffle(pool, random);
            int n = Math.min(4, pool.size());
            shopOffers = new int[n];
            for (int i = 0; i < n; i++) shopOffers[i] = pool.get(i);
        }

        private void buy(int offerIndex) {
            if (offerIndex < 0 || offerIndex >= shopOffers.length) return;
            int id = shopOffers[offerIndex];
            if (id < 0) return;
            Relic r = relics[id];
            if (shopInk < r.cost || ownedRelics.contains(id)) return;
            shopInk -= r.cost;
            ownedRelics.add(id);
            shopOffers[offerIndex] = -1;
            message = "Bought " + r.name + ".";
            pulse(Color.argb(50, 255, 210, 70));
            invalidate();
        }

        private void reroll() {
            int cost = rerollCost();
            if (shopInk < cost) return;
            shopInk -= cost;
            rerollsThisShop++;
            makeShopOffers();
            invalidate();
        }

        private int rerollCost() {
            return REROLL_BASE_COST + rerollsThisShop * 55;
        }

        private void leaveShop() {
            round++;
            screen = CATEGORY;
            invalidate();
        }

        private void continueResult() {
            if (roundSuccess) openShop();
            else if (hearts <= 0) screen = GAME_OVER;
            else {
                round++;
                screen = CATEGORY;
            }
            invalidate();
        }

        private void endRun() {
            if (round > bestRound) bestRound = round;
            if (totalInk > bestInk) bestInk = totalInk;
            prefs.edit().putInt("best_round", bestRound).putInt("best_ink", bestInk).apply();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP || inputLocked) return true;
            float x = event.getX();
            float y = event.getY();
            for (Hit hit : hits) {
                if (hit.enabled && hit.rect.contains(x, y)) {
                    action(hit.action);
                    return true;
                }
            }
            return true;
        }

        private void action(String action) {
            if (action.equals("START") || action.equals("RESTART")) startRun();
            else if (action.equals("MENU")) { screen = MENU; invalidate(); }
            else if (action.startsWith("CAT_")) startRound(categories[Integer.parseInt(action.substring(4))]);
            else if (action.startsWith("L_")) guess(action.charAt(2));
            else if (action.equals("CHARGE")) { chargeMode = !chargeMode; invalidate(); }
            else if (action.equals("RESULT_NEXT")) continueResult();
            else if (action.startsWith("BUY_")) buy(Integer.parseInt(action.substring(4)));
            else if (action.equals("REROLL")) reroll();
            else if (action.equals("LEAVE_SHOP")) leaveShop();
        }

        private boolean bossRoundNext() { return round % 3 == 0; }
        private boolean hasRelic(int id) { return ownedRelics.contains(id); }
        private float cx() { return getWidth() / 2f; }
        private float h(float pct) { return getHeight() * pct; }

        private int count(char letter) {
            int n = 0;
            for (int i = 0; i < currentPhrase.length(); i++) if (currentPhrase.charAt(i) == letter) n++;
            return n;
        }

        private int value(char letter) {
            boolean rare = "JQXZVK".indexOf(letter) >= 0;
            boolean common = "EARIOTNSL".indexOf(letter) >= 0;
            int v = rare ? 22 : (common ? 8 : 13);
            if (rare && hasRelic(2)) v *= 2;
            if (!isVowel(letter) && hasRelic(6)) v = Math.round(v * 1.25f);
            return v;
        }

        private boolean isVowel(char c) { return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U'; }

        private boolean complete() {
            for (int i = 0; i < currentPhrase.length(); i++) {
                char c = currentPhrase.charAt(i);
                if (c >= 'A' && c <= 'Z' && !guessed[c - 'A']) return false;
            }
            return true;
        }

        private char revealRandomLetter() {
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

        private String maskedPhrase() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < currentPhrase.length(); i++) {
                char c = currentPhrase.charAt(i);
                if (c >= 'A' && c <= 'Z') b.append(guessed[c - 'A'] ? c : '_');
                else b.append(c);
            }
            return b.toString();
        }

        private String[] choosePhrases(String[] source, int n) {
            ArrayList<String> list = new ArrayList<>();
            Collections.addAll(list, source);
            Collections.shuffle(list, random);
            String[] out = new String[Math.min(n, list.size())];
            for (int i = 0; i < out.length; i++) out[i] = list.get(i);
            return out;
        }

        private String relicNames() {
            StringBuilder b = new StringBuilder();
            int shown = 0;
            for (Integer id : ownedRelics) {
                if (shown > 0) b.append(", ");
                b.append(relics[id].name);
                shown++;
                if (shown == 3 && ownedRelics.size() > 3) {
                    b.append(" +").append(ownedRelics.size() - 3);
                    break;
                }
            }
            return b.toString();
        }

        private void pulse(int color) {
            pulseColor = color;
            pulseUntil = System.currentTimeMillis() + 160;
        }

        private Category[] makeCategories() {
            return new Category[]{
                    new Category("Food", "Easy", 0.9f, "Safe words. Lower reward.", rgb(58, 82, 53), new String[]{"SPICY NOODLES", "BURGER AND FRIES", "GARLIC BREAD", "CHICKEN WINGS", "FISH AND CHIPS", "HOT SAUCE", "CHEESE PIZZA", "SUNDAY ROAST", "FRIED RICE", "TOMATO SOUP"}),
                    new Category("Animals", "Easy", 0.9f, "Shorter phrases. Safer route.", rgb(52, 77, 86), new String[]{"BLACK CAT", "GOLDEN EAGLE", "WILD HORSE", "SLEEPY PANDA", "HUNGRY WOLF", "RIVER OTTER", "GIANT SQUID", "DESERT FOX", "ANGRY GOOSE", "TINY FROG"}),
                    new Category("Cinema", "Medium", 1.0f, "Social movie-ish phrases.", rgb(82, 65, 106), new String[]{"FINAL SCENE", "HERO RETURNS", "DARK THEATER", "LOST TREASURE", "SPACE BATTLE", "SECRET AGENT", "MONSTER ATTACK", "OPENING NIGHT", "CHASE SEQUENCE", "END CREDITS"}),
                    new Category("Mythology", "Hard", 1.25f, "Harder words. Better payout.", rgb(111, 64, 54), new String[]{"DRAGON FIRE", "ANCIENT ORACLE", "CURSED TEMPLE", "GOLDEN FLEECE", "SHADOW TITAN", "PHOENIX ASHES", "FORGOTTEN GODS", "MINOTAUR MAZE", "CELESTIAL SPEAR", "UNDERWORLD GATE"})
            };
        }

        private Relic[] makeRelics() {
            return new Relic[]{
                    new Relic(0, "Lucky Ink", "First wrong guess each phrase is blocked.", 180),
                    new Relic(1, "Double Tap", "Letters appearing twice give bonus Ink.", 220),
                    new Relic(2, "Rare Ink", "J Q X Z V K are worth double.", 260),
                    new Relic(3, "Vowel Candle", "First wrong vowel each phrase is blocked.", 180),
                    new Relic(4, "Chain Script", "Correct guesses grow multiplier faster.", 320),
                    new Relic(5, "Black Candle", "Higher targets. Better shop rewards.", 300),
                    new Relic(6, "Sharp Consonants", "Consonants give 25 percent more Ink.", 220),
                    new Relic(7, "Soft Vowels", "Vowels pay no Ink but build multiplier.", 190),
                    new Relic(8, "Last Breath", "Clear with 1 mistake left for big bonus.", 260),
                    new Relic(9, "Golden Margin", "Remaining mistakes add more clear bonus.", 320),
                    new Relic(10, "Echo Letter", "First correct guess reveals extra letter.", 380),
                    new Relic(11, "Ink Insurance", "First missed target costs no heart.", 380)
            };
        }

        private void button(Canvas c, RectF r, String label, String action, int fill, boolean enabled, float size) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(enabled ? fill : rgb(37, 34, 43));
            c.drawRoundRect(r, 16, 16, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(Color.argb(enabled ? 120 : 45, 255, 255, 255));
            c.drawRoundRect(r, 16, 16, paint);
            paint.setStyle(Paint.Style.FILL);
            text(c, label, r.centerX(), r.centerY() + size * 0.34f, size, enabled ? Color.WHITE : Color.rgb(126, 120, 135), true, Paint.Align.CENTER);
            hits.add(new Hit(new RectF(r), action, enabled));
        }

        private void panel(Canvas c, float l, float t, float r, float b, int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            c.drawRoundRect(new RectF(l, t, r, b), 20, 20, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(Color.argb(75, 255, 255, 255));
            c.drawRoundRect(new RectF(l, t, r, b), 20, 20, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawPhrase(Canvas c, String phrase, float top, float bottom) {
            ArrayList<String> lines = phraseLines(phrase, getWidth() - 70, 34);
            float y = top + ((bottom - top) - lines.size() * 43f) / 2f + 34;
            for (String line : lines) {
                text(c, line, cx(), y, 34, Color.WHITE, true, Paint.Align.CENTER);
                y += 43;
            }
        }

        private ArrayList<String> phraseLines(String phrase, float maxW, float size) {
            paint.setTextSize(size);
            ArrayList<String> lines = new ArrayList<>();
            String[] words = phrase.split(" ");
            String line = "";
            for (String word : words) {
                String display = spaced(word);
                String test = line.length() == 0 ? display : line + "   " + display;
                if (paint.measureText(test) > maxW && line.length() > 0) {
                    lines.add(line);
                    line = display;
                } else line = test;
            }
            if (line.length() > 0) lines.add(line);
            return lines;
        }

        private String spaced(String word) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                b.append(word.charAt(i));
                if (i < word.length() - 1) b.append(' ');
            }
            return b.toString();
        }

        private void wrapped(Canvas c, String text, float left, float y, float right, float size, int color) {
            paint.setTextSize(size);
            paint.setColor(color);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setFakeBoldText(false);
            String[] words = text.split(" ");
            String line = "";
            float yy = y;
            for (String word : words) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (paint.measureText(test) > right - left && line.length() > 0) {
                    c.drawText(line, left, yy, paint);
                    line = word;
                    yy += size + 5;
                } else line = test;
            }
            if (line.length() > 0) c.drawText(line, left, yy, paint);
        }

        private void text(Canvas c, String s, float x, float y, float size, int color, boolean bold, Paint.Align align) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(align);
            paint.setFakeBoldText(bold);
            c.drawText(s, x, y, paint);
            paint.setFakeBoldText(false);
        }

        private int rgb(int r, int g, int b) { return Color.rgb(r, g, b); }
        private int gold() { return rgb(236, 219, 174); }
        private int muted() { return rgb(190, 181, 166); }
        private int purple() { return rgb(111, 76, 180); }
        private int orange() { return rgb(241, 174, 73); }
        private int green() { return rgb(136, 225, 145); }
        private int red() { return rgb(245, 91, 101); }

        private static class Hit {
            final RectF rect;
            final String action;
            final boolean enabled;
            Hit(RectF rect, String action, boolean enabled) { this.rect = rect; this.action = action; this.enabled = enabled; }
        }
        private static class Category {
            final String name, diff, note;
            final float mult;
            final int color;
            final String[] phrases;
            Category(String name, String diff, float mult, String note, int color, String[] phrases) { this.name = name; this.diff = diff; this.mult = mult; this.note = note; this.color = color; this.phrases = phrases; }
        }
        private static class Relic {
            final int id, cost;
            final String name, shortText;
            Relic(int id, String name, String shortText, int cost) { this.id = id; this.name = name; this.shortText = shortText; this.cost = cost; }
        }
    }
}
