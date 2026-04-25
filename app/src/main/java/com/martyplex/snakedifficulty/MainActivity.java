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
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new SnakeGameView(this));
    }

    private static class SnakeGameView extends View {
        private static final int GRID_WIDTH = 24;
        private static final int GRID_HEIGHT = 32;
        private static final int START_DELAY_MS = 230;
        private static final int MIN_DELAY_MS = 70;
        private static final int SPEED_STEP_MS = 10;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final ArrayList<Cell> snake = new ArrayList<>();
        private final SharedPreferences prefs;

        private int directionX = 1;
        private int directionY = 0;
        private int pendingDirectionX = 1;
        private int pendingDirectionY = 0;
        private int foodX;
        private int foodY;
        private int score = 0;
        private int highScore = 0;
        private int delayMs = START_DELAY_MS;
        private boolean gameOver = false;
        private float downX;
        private float downY;

        private final Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!gameOver) {
                    updateGame();
                    invalidate();
                    postDelayed(this, delayMs);
                }
            }
        };

        SnakeGameView(Context context) {
            super(context);
            setFocusable(true);
            prefs = context.getSharedPreferences("snake_stats", Context.MODE_PRIVATE);
            highScore = prefs.getInt("high_score", 0);
            resetGame();
        }

        private void resetGame() {
            snake.clear();
            int startX = GRID_WIDTH / 2;
            int startY = GRID_HEIGHT / 2;
            snake.add(new Cell(startX, startY));
            snake.add(new Cell(startX - 1, startY));
            snake.add(new Cell(startX - 2, startY));

            directionX = 1;
            directionY = 0;
            pendingDirectionX = 1;
            pendingDirectionY = 0;
            score = 0;
            delayMs = START_DELAY_MS;
            gameOver = false;
            placeFood();
            removeCallbacks(tick);
            postDelayed(tick, delayMs);
        }

        private void updateGame() {
            directionX = pendingDirectionX;
            directionY = pendingDirectionY;

            Cell head = snake.get(0);
            int nextX = head.x + directionX;
            int nextY = head.y + directionY;

            if (nextX < 0 || nextX >= GRID_WIDTH || nextY < 0 || nextY >= GRID_HEIGHT || hitsSnake(nextX, nextY)) {
                endGame();
                return;
            }

            snake.add(0, new Cell(nextX, nextY));

            if (nextX == foodX && nextY == foodY) {
                score++;
                delayMs = Math.max(MIN_DELAY_MS, START_DELAY_MS - (score * SPEED_STEP_MS));
                placeFood();
            } else {
                snake.remove(snake.size() - 1);
            }
        }

        private boolean hitsSnake(int x, int y) {
            for (Cell cell : snake) {
                if (cell.x == x && cell.y == y) return true;
            }
            return false;
        }

        private void placeFood() {
            do {
                foodX = random.nextInt(GRID_WIDTH);
                foodY = random.nextInt(GRID_HEIGHT);
            } while (hitsSnake(foodX, foodY));
        }

        private void endGame() {
            gameOver = true;
            if (score > highScore) {
                highScore = score;
                prefs.edit().putInt("high_score", highScore).apply();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.rgb(16, 24, 32));

            float cellSize = Math.min(getWidth() / (float) GRID_WIDTH, getHeight() / (float) GRID_HEIGHT);
            float boardWidth = cellSize * GRID_WIDTH;
            float boardHeight = cellSize * GRID_HEIGHT;
            float left = (getWidth() - boardWidth) / 2f;
            float top = (getHeight() - boardHeight) / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(26, 38, 50));
            canvas.drawRoundRect(new RectF(left, top, left + boardWidth, top + boardHeight), 18, 18, paint);

            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(44f);
            paint.setColor(Color.WHITE);
            canvas.drawText("Score: " + score, 24, 58, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Best: " + highScore, getWidth() - 24, 58, paint);

            drawCell(canvas, left, top, cellSize, foodX, foodY, Color.rgb(231, 76, 60));

            for (int i = snake.size() - 1; i >= 0; i--) {
                Cell cell = snake.get(i);
                int color = i == 0 ? Color.rgb(46, 204, 113) : Color.rgb(39, 174, 96);
                drawCell(canvas, left, top, cellSize, cell.x, cell.y, color);
            }

            if (gameOver) {
                paint.setColor(Color.argb(190, 0, 0, 0));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(Color.WHITE);
                paint.setTextSize(64f);
                canvas.drawText("Game Over", getWidth() / 2f, getHeight() / 2f - 60, paint);
                paint.setTextSize(38f);
                canvas.drawText("Tap to restart", getWidth() / 2f, getHeight() / 2f + 10, paint);
                canvas.drawText("Final score: " + score, getWidth() / 2f, getHeight() / 2f + 64, paint);
            }
        }

        private void drawCell(Canvas canvas, float left, float top, float cellSize, int gridX, int gridY, int color) {
            float padding = Math.max(2f, cellSize * 0.08f);
            float x = left + gridX * cellSize + padding;
            float y = top + gridY * cellSize + padding;
            paint.setColor(color);
            canvas.drawRoundRect(new RectF(x, y, x + cellSize - padding * 2, y + cellSize - padding * 2), 10, 10, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX = event.getX();
                downY = event.getY();
                if (gameOver) {
                    resetGame();
                    invalidate();
                    return true;
                }
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;

                if (Math.abs(dx) < 30 && Math.abs(dy) < 30) return true;

                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0) setDirection(1, 0);
                    else setDirection(-1, 0);
                } else {
                    if (dy > 0) setDirection(0, 1);
                    else setDirection(0, -1);
                }
                return true;
            }
            return true;
        }

        private void setDirection(int x, int y) {
            if (x == -directionX && y == -directionY) return;
            pendingDirectionX = x;
            pendingDirectionY = y;
        }

        private static class Cell {
            final int x;
            final int y;

            Cell(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }
    }
}
