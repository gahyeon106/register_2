package com.example.register
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //  xml이랑 연결
        val etLoginId = findViewById<EditText>(R.id.etLoginId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val rbParent = findViewById<RadioButton>(R.id.rbParent)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val loginId = etLoginId.text.toString().trim()
            val password = etPassword.text.toString()
            // 선택된 버튼에 따라 parent, child
            val role = if (rgRole.checkedRadioButtonId == R.id.rbParent) "parent" else "child"

            // 입력값 검사
            if (loginId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 아이디 중복 확인 후 가입
            registerUser(loginId, password, role)
        }
    }

    private fun registerUser(loginId: String, password: String, role: String) {
        // 같은 아이디가 있는지 확인
        db.collection("users")
            .whereEqualTo("loginId", loginId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // 이미 있는 아이디면 XX
                    Toast.makeText(this, "이미 사용 중인 아이디입니다", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 중복 없음 -> 비밀번호 암호화
                val salt = PasswordUtils.generateSalt()           // Salt 생성
                val passwordHash = PasswordUtils.hashPassword(password, salt) // 해싱

                // DB에 저장할 데이터 형태
                val userData = hashMapOf(
                    "loginId" to loginId,
                    "passwordHash" to passwordHash,
                    "salt" to salt,
                    "role" to role,
                    "familyId" to null
                )

                // "users" 컬렉션에 문서 추가 .add 사용
                db.collection("users")
                    .add(userData)
                    .addOnSuccessListener { documentRef ->
                        // 자동 생성된 uid를 문서
                        documentRef.update("uid", documentRef.id)
                        Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()

                        // 로그인 화면으로 이동
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish() // 이 화면 닫기
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}