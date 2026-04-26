package com.example.register

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 로그인한 사용자 정보 화면에 표시
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnFamily = findViewById<Button>(R.id.btnFamily)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        tvWelcome.text = "안녕하세요, ${UserSession.loginId}님! (${UserSession.role})"



        // 가족 연동 화면으로
        btnFamily.setOnClickListener {
            startActivity(Intent(this, FamilyActivity::class.java))
        }

        // 로그아웃
        btnLogout.setOnClickListener {
            UserSession.clear() // 저장된 로그인 정보 지우기
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}