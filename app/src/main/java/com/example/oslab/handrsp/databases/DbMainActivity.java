package com.example.oslab.handrsp.databases;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.oslab.handrsp.Menu;
import com.example.oslab.handrsp.R;

import java.util.Random;

/**
 * Created by 김기혁 on 2016-11-13.
 */

public class DbMainActivity extends Activity{

    static final int ROCK = 0;
    static final int SCISSORS = 1;
    static final int PAPER = 2;
    static final int USER_WIN = 3;
    static final int COMPUTER_WIN = 4;
    static final int DRAW = 5;

    private TextView humanWin, humanDraw, humanDefeat, humanR, humanS, humanP, comWin, comDraw, comDefeat, comR, comS, comP, result;
    private ImageView humanImg, comImg;
    private  SQLiteDatabase db;
    private  MySQLiteOpenHelper helper;
    private Button retHome, reset;

    public DbMainActivity() {
//        selectAll();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.db_view);

        helper = new MySQLiteOpenHelper(DbMainActivity.this, "rsp",  null, 1);
        humanWin = (TextView)findViewById(R.id.humanWinCount);
        humanDraw = (TextView)findViewById(R.id.humanDrawCount);
        humanDefeat = (TextView)findViewById(R.id.humanDefeatCount);
        humanR = (TextView)findViewById(R.id.humanRcount);
        humanS =(TextView)findViewById(R.id.humanScount);
        humanP = (TextView)findViewById(R.id.humanPcount);

        comWin = (TextView)findViewById(R.id.comWinCount);
        comDraw = (TextView)findViewById(R.id.comDrawCount);
        comDefeat = (TextView)findViewById(R.id.comDefeatCount);
        comR = (TextView)findViewById(R.id.comRcount);
        comS = (TextView)findViewById(R.id.comScount);
        comP = (TextView)findViewById(R.id.comPcount);

        result = (TextView)findViewById(R.id.winner);

        humanImg = (ImageView)findViewById(R.id.humanRst);
        comImg = (ImageView)findViewById(R.id.comRst);

        retHome = (Button)findViewById(R.id.retHome);
        reset = (Button)findViewById(R.id.reset);

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete("rsp");
            }
        });

        retHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getApplicationContext(), Menu.class);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        int finger = intent.getIntExtra("finger", -1);
        int pass = intent.getIntExtra("pass", -1);

        Log.d("pass", "finger : " + Integer.toString(finger));
        Log.d("pass", "pass : " + Integer.toString(pass));
        //delete("rsp");
        if(pass == 0){
            selectAll();
            reset.setVisibility(View.VISIBLE);
            result.setText("DB 전용 입니다.");
            return;
        }

        if(finger == -1 )
            return;
        else {
            reset.setVisibility(View.INVISIBLE);
            rsp(finger, new Random().nextInt(3));
        }
    }

    public void insert(int user_result, int computer_result, int winner){
        db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_result", user_result);
        values.put("computer_result", computer_result);
        values.put("winner", winner);
        db.insert("rsp", null, values);

        Log.d("num", "winner : " + winner);
    }

    public void selectAll(){
        int cnt = 0;
        int user_state[] = { 0, 0, 0, 0, 0};   //[0] : rock  *  [1] : scissor * [2] : paper * [3] : win * [4] defeat
        int com_state[] = { 0, 0, 0, 0, 0};   //[0] : rock  *  [1] : scissor * [2] : paper * [3] : win  * [4] defeat
        int draw = 0;
        db = helper.getReadableDatabase();
        Cursor c = db.query("rsp", null, null, null, null, null, null);
        while(c.moveToNext()){
            cnt++;
            int user_result = c.getInt(c.getColumnIndex("user_result"));
            int computer_result = c.getInt(c.getColumnIndex("computer_result"));
            int winner = c.getInt(c.getColumnIndex("winner"));

            Log.d("db", " user result : " + user_result + " computer result : " + computer_result + " winner : " +  winner);

            if(user_result == ROCK) {
                user_state[0] += 1;
                Log.d("user_rock", Integer.toString(user_state[0]));
            }
            else if(user_result == SCISSORS)
                user_state[1] += 1;
            else
                user_state[2] += 1;

            if(computer_result == ROCK)
                com_state[0] += 1;
            else if(computer_result == SCISSORS)
                com_state[1] += 1;
            else
                com_state[2] += 1;

            if(winner == USER_WIN) {
                user_state[3] += 1;
                com_state[4] += 1;
            }
            else if(winner == COMPUTER_WIN) {
                com_state[3] += 1;
                user_state[4] += 1;
            }

            if(winner == DRAW)
                draw += 1;
        }

        humanR.setText(Integer.toString(user_state[0]));
        humanS.setText(Integer.toString(user_state[1]));
        humanP.setText(Integer.toString(user_state[2]));
        humanWin.setText(Integer.toString(user_state[3]));
        humanDefeat.setText(Integer.toString(user_state[4]));
        humanDraw.setText(Integer.toString(draw));

        for(int i = 0; i < 5; i++)
            Log.d("human", Integer.toString(user_state[i]));

        comR.setText(Integer.toString(com_state[0]));
        comS.setText(Integer.toString(com_state[1]));
        comP.setText(Integer.toString(com_state[2]));
        comWin.setText(Integer.toString(com_state[3]));
        comDefeat.setText(Integer.toString(com_state[4]));
        comDraw.setText(Integer.toString(draw));
        for(int i = 0; i < 5; i++)
            Log.d("computer", Integer.toString(com_state[i]));


    }

    public void delete(String title){
        db.delete("rsp", null, null);
        this.startActivity(new Intent(getApplicationContext(), DbMainActivity.class));
    }

    public MySQLiteOpenHelper getHelper() {
        return helper;
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public void setHelper(MySQLiteOpenHelper helper) {
        this.helper = helper;
    }

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    public void rsp(int finger, int num)
    {
        if(finger <= 3 && finger >= 2){ //가위
            if(num == ROCK){
                // Rock
                insert(SCISSORS, ROCK, COMPUTER_WIN);
                generic_setText(SCISSORS, ROCK, COMPUTER_WIN);
            }
            else if(num == SCISSORS){
                //Sessior
                insert(SCISSORS, SCISSORS, DRAW);
                generic_setText(SCISSORS, SCISSORS, DRAW);
            }
            else{
                //Paper
                insert(SCISSORS, PAPER, USER_WIN);
                generic_setText(SCISSORS, PAPER, USER_WIN);
            }
        }
        else if(finger <= 5 && finger >= 4){
            if(num == ROCK){
                // Rock
                insert(PAPER, ROCK, USER_WIN);
                generic_setText(PAPER, ROCK, USER_WIN);
            }
            else if(num == SCISSORS){
                //Sessior
                insert(PAPER, SCISSORS, COMPUTER_WIN);
                generic_setText(PAPER, SCISSORS, COMPUTER_WIN);
            }
            else{
                //Paper
                insert(PAPER, PAPER, DRAW);
                generic_setText(PAPER, PAPER, DRAW);
            }
        }
        else{
            if(num == ROCK){
                // Rock
                insert(ROCK, ROCK, DRAW);
                generic_setText(ROCK, ROCK, DRAW);
            }
            else if(num == SCISSORS){
                //Sessior
                insert(ROCK, SCISSORS, USER_WIN);
                generic_setText(ROCK, SCISSORS, USER_WIN);
            }
            else{
                //Paper
              insert(ROCK, PAPER, COMPUTER_WIN);
              generic_setText(ROCK, PAPER, COMPUTER_WIN);
            }
        }

        selectAll();
    }
    void generic_setText(int user, int com, int winner){
        if(user == ROCK){
            humanImg.setImageResource(R.drawable.rock);
        }
        else if(user == SCISSORS){
            humanImg.setImageResource(R.drawable.scissors);
        }
        else{
            humanImg.setImageResource(R.drawable.paper);
        }

        if(com == ROCK){
            comImg.setImageResource(R.drawable.rock);
        }
        else if (com == SCISSORS){
            comImg.setImageResource(R.drawable.scissors);
        }
        else{
            comImg.setImageResource(R.drawable.paper);
        }

        if(winner == USER_WIN)
            result.setText("승자는 당신입니다!");
        else if(winner == COMPUTER_WIN)
            result.setText("패배 하셨습니다");
        else{
            result.setText("비겼습니다");
        }
    }
}
