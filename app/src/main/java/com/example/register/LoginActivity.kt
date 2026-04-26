package com.example.register

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    //xml이랑 연결한 것
        val etLoginId = findViewById<EditText>(R.id.etLoginId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoRegister = findViewById<Button>(R.id.btnGoRegister)

        //로그인 버튼 -> 입력칸에서 id, 비번 읽기-> login() 함수 실행
        btnLogin.setOnClickListener {
            val loginId = etLoginId.text.toString().trim()
            val password = etPassword.text.toString()

            if (loginId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            login(loginId, password)
        }

        //회원가입화면으로 이동. (RegisterActivity.kt)
        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(loginId: String, password: String) {
        // 입력한 아이디로 DB에서 사용자 문서 찾기
        db.collection("users")
            .whereEqualTo("loginId", loginId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "존재하지 않는 아이디입니다", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userDoc = documents.documents[0] // 찾은 문서

                //DB에 저장된 salt와 passwordHash 가져오기
                val savedHash = userDoc.getString("passwordHash") ?: ""
                val savedSalt = userDoc.getString("salt") ?: ""

                // 입력한 비밀번호 + 저장된 salt로 암호화
                val inputHash = PasswordUtils.hashPassword(password, savedSalt)

                // 해싱 결과 비교
                if (inputHash == savedHash) {
                    // 로그인 성공! -> 사용자 정보 기억
                    UserSession.uid = userDoc.getString("uid") ?: userDoc.id
                    UserSession.loginId = loginId
                    UserSession.role = userDoc.getString("role") ?: ""
                    UserSession.familyId = userDoc.getString("familyId")

                    Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()

                    // 로그인 후 사용자 메인 화면으로
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // 비밀번호 틀림
                    Toast.makeText(this, "비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}