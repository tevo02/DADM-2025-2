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

    private int mHumanWins = 0, mComputerWins = 0, mTies = 0;
    private TextView tvScoreHuman, tvScoreCpu, tvScoreTies, tvDifficulty;
    private android.content.SharedPreferences mPrefs;

    private int difficultyToIndex(TicTacToeGame.DifficultyLevel d) {
        switch (d) {
            case EASY:   return 0;
            case HARDER: return 1;
            case EXPERT: return 2;
        }
        return 2;
    }
    private TicTacToeGame.DifficultyLevel indexToDifficulty(int i) {
        if (i == 0) return TicTacToeGame.DifficultyLevel.EASY;
        if (i == 1) return TicTacToeGame.DifficultyLevel.HARDER;
        return TicTacToeGame.DifficultyLevel.EXPERT;
    }

    private void showDifficultyDialog() {
        final CharSequence[] levels = {
                getString(R.string.difficulty_easy),
                getString(R.string.difficulty_harder),
                getString(R.string.difficulty_expert)
        };

        // sincroniza índice con el valor actual antes de mostrar
        selected = difficultyToIndex(mGame.getDifficultyLevel());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.difficulty_choose)
                .setSingleChoiceItems(levels, selected, (d, which) -> {
                    selected = which;
                    mGame.setDifficultyLevel(indexToDifficulty(which));

                    // persiste inmediatamente (además se guardará en onStop)
                    if (mPrefs != null) {
                        mPrefs.edit()
                                .putInt("difficulty", mGame.getDifficultyLevel().ordinal())
                                .apply();
                    }
                    updateDifficultyLabel();
                    Toast.makeText(this, levels[which], Toast.LENGTH_SHORT).show();
                    d.dismiss();

                    // (opcional) reiniciar partida al cambiar dificultad:
                    // startNewGame();
                })
                .create()
                .show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tvScoreHuman = findViewById(R.id.tv_score_human);
        tvScoreTies  = findViewById(R.id.tv_score_ties);
        tvScoreCpu   = findViewById(R.id.tv_score_cpu);
        tvDifficulty = findViewById(R.id.tv_difficulty);

        mInfoTextView = findViewById(R.id.information);
        mGame = new TicTacToeGame();

        mBoardView = findViewById(R.id.board);
        mBoardView.setGame(mGame);
        mBoardView.setOnTouchListener(mTouchListener);
        mPrefs = getSharedPreferences("ttt_prefs", MODE_PRIVATE);

        // restaurar puntajes persistentes
        mHumanWins    = mPrefs.getInt("mHumanWins", 0);
        mComputerWins = mPrefs.getInt("mComputerWins", 0);
        mTies         = mPrefs.getInt("mTies", 0);

        // restaurar dificultad persistente (por defecto: EXPERT o la que prefieras)
        int diffOrdinal = mPrefs.getInt("difficulty", TicTacToeGame.DifficultyLevel.EXPERT.ordinal());
        mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[diffOrdinal]);
        // guardar inmediatamente (opcional; igual se guarda en onStop)
        mPrefs.edit().putInt("difficulty", mGame.getDifficultyLevel().ordinal()).apply();
        updateDifficultyLabel();
        updateScoreViews();




        // Bottom bar (nav inferior)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_new_game) {
                startNewGame();
                return true;
            } else if (id == R.id.nav_difficulty) {
                showDifficultyDialog();
                return true;
            } else if (id == R.id.nav_quit) {
                showDialog(DIALOG_QUIT_ID);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            startNewGame();
        } else {
            mGame.setBoardState(savedInstanceState.getCharArray("board"));
            mGameOver = savedInstanceState.getBoolean("mGameOver");
            mInfoTextView.setText(savedInstanceState.getCharSequence("info"));
            isCpuTurn = savedInstanceState.getBoolean("isCpuTurn", false);

            mBoardView.invalidate();

            // Extra del reto: si era turno de la CPU cuando rotaste, relanza su jugada en esta nueva Activity
            if (!mGameOver && isCpuTurn) {
                postComputerMoveWithDelay();
            }
        }


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
        // Evita que un postDelayed de la instancia vieja dispare luego
        handler.removeCallbacksAndMessages(null);
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
            mTies++;
            mInfoTextView.setText("Tablas :o");
        } else if (winner == 2) {
            mHumanWins++;
            mInfoTextView.setText("¡Ganaste!! c:");
        } else {
            mComputerWins++;
            mInfoTextView.setText("Mejor suerte la próxima :c");
        }
        updateScoreViews();
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

    // --- GUARDAR ESTADO ---
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharArray("board", mGame.getBoardState());
        outState.putBoolean("mGameOver", mGameOver);
        outState.putCharSequence("info", mInfoTextView.getText());
        outState.putBoolean("isCpuTurn", isCpuTurn);
    }

    @Override
    protected void onStop() {
        super.onStop();
        android.content.SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("mHumanWins", mHumanWins);
        ed.putInt("mComputerWins", mComputerWins);
        ed.putInt("mTies", mTies);
        ed.putInt("difficulty", mGame.getDifficultyLevel().ordinal());
        ed.apply(); // o commit()
        if (mPrefs != null) {
            mPrefs.edit()
                    .putInt("difficulty", mGame.getDifficultyLevel().ordinal())
                    // si ya guardas los puntajes aquí, déjalos también
                    .apply();
        }
    }

    private void updateDifficultyLabel() {
        TicTacToeGame.DifficultyLevel d = mGame.getDifficultyLevel();
        String texto = "Dificultad: ";
        switch (d) {
            case EASY:   texto += "Fácil"; break;
            case HARDER: texto += "Dificil"; break;
            case EXPERT: texto += "Experto"; break;
        }
        tvDifficulty.setText(texto);
    }


    private void updateScoreViews() {
        tvScoreHuman.setText("Ganados: " + mHumanWins);
        tvScoreTies.setText("Empates: " + mTies);
        tvScoreCpu.setText("Máquina: " + mComputerWins);
    }





}
