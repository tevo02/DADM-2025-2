package co.edu.unal.tictactoe.harding;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import co.edu.unal.tictactoe.R;

public class BoardView extends View {

    public static final int GRID_WIDTH = 6;

    private Bitmap mHumanBitmap;
    private Bitmap mComputerBitmap;

    private Paint mPaint;
    private TicTacToeGame mGame;

    public BoardView(Context context) {
        super(context);
        initialize();
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public BoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Ajusta los nombres a tus drawables reales
        mHumanBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.x_img);
        mComputerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.o_img);
    }

    public void setGame(TicTacToeGame game) {
        this.mGame = game;
        invalidate();
    }

    public int getBoardCellWidth()  { return getWidth() / 3; }
    public int getBoardCellHeight() { return getHeight() / 3; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        int cellW = getBoardCellWidth();
        int cellH = getBoardCellHeight();

        // Grilla
        mPaint.setColor(Color.LTGRAY);
        mPaint.setStrokeWidth(GRID_WIDTH);

        // Verticales
        canvas.drawLine(cellW, 0, cellW, h, mPaint);
        canvas.drawLine(cellW * 2, 0, cellW * 2, h, mPaint);
        // Horizontales
        canvas.drawLine(0, cellH, w, cellH, mPaint);
        canvas.drawLine(0, cellH * 2, w, cellH * 2, mPaint);

        // X/O
        if (mGame == null) return;
        int pad = GRID_WIDTH + 8;

        for (int i = 0; i < TicTacToeGame.BOARD_SIZE; i++) {
            int col = i % 3;
            int row = i / 3;

            int left   = col * cellW + pad;
            int top    = row * cellH + pad;
            int right  = (col + 1) * cellW - pad;
            int bottom = (row + 1) * cellH - pad;

            char occ = mGame.getBoardOccupant(i);
            if (occ == TicTacToeGame.HUMAN_PLAYER) {
                canvas.drawBitmap(mHumanBitmap, null, new Rect(left, top, right, bottom), null);
            } else if (occ == TicTacToeGame.COMPUTER_PLAYER) {
                canvas.drawBitmap(mComputerBitmap, null, new Rect(left, top, right, bottom), null);
            }
        }
    }
}
