package co.edu.unal.tictactoe.harding;

import android.app.AlertDialog;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import co.edu.unal.tictactoe.R;

public class AndroidTicTacToeActivity extends AppCompatActivity {

    private int selected = 0;
    static final int DIALOG_DIFFICULTY_ID = 0;
    static final int DIALOG_QUIT_ID = 1;

    private boolean mGameOver = false;
    private boolean isCpuTurn = false;
    private boolean soundEnabled = true;

    private TicTacToeGame mGame;
    private BoardView mBoardView;
    private TextView mInfoTextView;

    private MediaPlayer humanMp, cpuMp;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case DIALOG_DIFFICULTY_ID: {
                builder.setTitle(R.string.difficulty_choose);
                final CharSequence[] levels = {
                        getResources().getString(R.string.difficulty_easy),
                        getResources().getString(R.string.difficulty_harder),
                        getResources().getString(R.string.difficulty_expert)
                };
                builder.setSingleChoiceItems(levels, selected, (d, item) -> {
                    d.dismiss();
                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[item]);
                    selected = item;
                    Toast.makeText(getApplicationContext(), levels[item], Toast.LENGTH_SHORT).show();
                });
                dialog = builder.create();
                break;
            }
            case DIALOG_QUIT_ID: {
                builder.setMessage(R.string.quit_question)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, (d, _id) -> AndroidTicTacToeActivity.this.finish())
                        .setNegativeButton(R.string.no, null);
                dialog = builder.create();
                break;
            }
        }
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mInfoTextView = findViewById(R.id.information);
        mGame = new TicTacToeGame();

        mBoardView = findViewById(R.id.board);
        mBoardView.setGame(mGame);
        mBoardView.setOnTouchListener(mTouchListener);

        // Bottom bar (nav inferior)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_new_game) {
                startNewGame();
                return true;
            } else if (id == R.id.nav_difficulty) {
                showDialog(DIALOG_DIFFICULTY_ID);
                return true;
            } else if (id == R.id.nav_quit) {
                showDialog(DIALOG_QUIT_ID);
                return true;
            }
            return false;
        });

        startNewGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        humanMp = MediaPlayer.create(getApplicationContext(), R.raw.move_cpu);
        cpuMp   = MediaPlayer.create(getApplicationContext(), R.raw.move_human);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (humanMp != null) { humanMp.release(); humanMp = null; }
        if (cpuMp != null)   { cpuMp.release();   cpuMp = null;   }
    }

    private void startNewGame() {
        mGame.clearBoard();
        mGameOver = false;
        isCpuTurn = false;
        mInfoTextView.setText("Empiezas Tú");
        mBoardView.invalidate();
    }

    // Tocar una celda
    private final OnTouchListener mTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
            if (mGameOver || isCpuTurn) return false;

            int col = (int) (event.getX() / mBoardView.getBoardCellWidth());
            int row = (int) (event.getY() / mBoardView.getBoardCellHeight());
            int pos = row * 3 + col;

            if (setMove(TicTacToeGame.HUMAN_PLAYER, pos)) {
                int winner = mGame.checkForWinner();
                if (winner == 0) {
                    mInfoTextView.setText("Turno de la máquina.");
                    postComputerMoveWithDelay();
                } else {
                    endWithWinner(winner);
                }
            }
            return false;
        }
    };

    private void postComputerMoveWithDelay() {
        isCpuTurn = true;
        handler.postDelayed(() -> {
            if (mGameOver) { isCpuTurn = false; return; }
            int move = mGame.getComputerMove();
            if (setMove(TicTacToeGame.COMPUTER_PLAYER, move)) {
                int winner = mGame.checkForWinner();
                if (winner == 0) {
                    mInfoTextView.setText("Te toca jugar");
                } else {
                    endWithWinner(winner);
                }
            }
            isCpuTurn = false;
        }, 2000);
    }

    private void endWithWinner(int winner) {
        mGameOver = true;
        if (winner == 1) {
            mInfoTextView.setText("Tablas :o");
        } else if (winner == 2) {
            mInfoTextView.setText("¡Ganaste!! c:");
        } else {
            mInfoTextView.setText("Mejor suerte la próxima :c");
        }
    }

    private boolean setMove(char player, int location) {
        if (mGame.setMove(player, location)) {
            if (soundEnabled) {
                if (player == TicTacToeGame.HUMAN_PLAYER && humanMp != null) humanMp.start();
                if (player == TicTacToeGame.COMPUTER_PLAYER && cpuMp != null)  cpuMp.start();
            }
            mBoardView.invalidate();
            return true;
        }
        return false;
    }
}
