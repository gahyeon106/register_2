package com.example.register

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class FamilyActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var pendingInvitationId: String? = null // 받은 초대장 ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family)

        val tvMyInfo = findViewById<TextView>(R.id.tvMyInfo)
        val layoutParent = findViewById<LinearLayout>(R.id.layoutParent)
        val layoutChild = findViewById<LinearLayout>(R.id.layoutChild)
        val btnCreateFamily = findViewById<Button>(R.id.btnCreateFamily)
        val etChildId = findViewById<EditText>(R.id.etChildId)
        val btnInviteChild = findViewById<Button>(R.id.btnInviteChild)
        val tvInvitation = findViewById<TextView>(R.id.tvInvitation)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val tvFamilyId = findViewById<TextView>(R.id.tvFamilyId)

        // 내 정보 표시
        tvMyInfo.text = "내 아이디: ${UserSession.loginId} | 역할: ${UserSession.role}"
        if (UserSession.familyId != null) {
            tvFamilyId.text = "현재 가족 ID: ${UserSession.familyId}"
        }

        // 역할에 따라 다른 UI 보여주기
        if (UserSession.role == "parent") {
            layoutParent.visibility = View.VISIBLE

            // [부모] : 가족 그룹 만들기
            btnCreateFamily.setOnClickListener {
                createFamilyGroup()
            }

            // [부모] : 자녀에게 초대 요청 보내기
            btnInviteChild.setOnClickListener {
                val childId = etChildId.text.toString().trim()
                if (childId.isEmpty()) {
                    Toast.makeText(this, "자녀 아이디를 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                sendInvitation(childId)
            }
        } else {
            // [자녀] : 받은 초대 확인
            layoutChild.visibility = View.VISIBLE
            checkInvitation(tvInvitation, btnAccept)

            btnAccept.setOnClickListener {
                acceptInvitation(tvFamilyId, tvInvitation, btnAccept)
            }
        }
    }

    // ** 부모: 가족 그룹 만들기
    private fun createFamilyGroup() {
        //이미 familyId가 존재하면 불가
        if (UserSession.familyId != null) {
            Toast.makeText(this, "이미 가족 그룹이 있습니다: ${UserSession.familyId}", Toast.LENGTH_LONG).show()
            return
        }

        // UUID로 고유 가족 ID 생성
        val newFamilyId = UUID.randomUUID().toString().take(8) // 8자리로 짧게

        // 내 Users 문서에 familyId 저장
        db.collection("users").document(UserSession.uid)
            .update("familyId", newFamilyId)
            .addOnSuccessListener {
                UserSession.familyId = newFamilyId
                Toast.makeText(this, "가족 그룹 생성! ID: $newFamilyId", Toast.LENGTH_LONG).show()
                findViewById<TextView>(R.id.tvFamilyId).text = "가족 ID: $newFamilyId"
            }
    }

    //** 부모: 자녀에게 초대 요청 보내기
    private fun sendInvitation(childLoginId: String) {
        if (UserSession.familyId == null) {
            Toast.makeText(this, "먼저 가족 그룹을 만들어주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 자녀 아이디로 자녀 UID 찾기
        db.collection("users")
            .whereEqualTo("loginId", childLoginId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "존재하지 않는 아이디입니다", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val childDoc = documents.documents[0]
                val childUid = childDoc.getString("uid") ?: childDoc.id

                // Invitations 컬렉션에 요청 저장
                val invitation = hashMapOf(
                    "fromUid" to UserSession.uid,       // 보내는 사람 (부모)
                    "toUid" to childUid,                 // 받는 사람 (자녀)
                    "fromLoginId" to UserSession.loginId,
                    "familyId" to UserSession.familyId,  // 연동할 가족 ID
                    "status" to "pending"                // 대기 중
                )

                db.collection("invitations")
                    .add(invitation)
                    .addOnSuccessListener {
                        Toast.makeText(this, "${childLoginId}님에게 요청을 보냈습니다!", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    //** 자녀: 받은 초대 확인하기
    private fun checkInvitation(tvInvitation: TextView, btnAccept: Button) {
        db.collection("invitations")
            .whereEqualTo("toUid", UserSession.uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvInvitation.text = "받은 연동 요청이 없습니다"
                } else {
                    //초대장이 있으면
                    val doc = documents.documents[0]
                    pendingInvitationId = doc.id
                    val fromId = doc.getString("fromLoginId") ?: "알 수 없음"
                    tvInvitation.text = "${fromId}님이 연동 요청을 보냈습니다"
                    btnAccept.visibility = View.VISIBLE
                }
            }
    }

    //** 자녀: 초대 수락하기
    private fun acceptInvitation(tvFamilyId: TextView, tvInvitation: TextView, btnAccept: Button) {
        val invitationId = pendingInvitationId ?: return

        // 초대장에서 familyId 가져오기
        db.collection("invitations").document(invitationId)
            .get()
            .addOnSuccessListener { doc ->
                val familyId = doc.getString("familyId") ?: return@addOnSuccessListener

                // 내 Users 문서에 familyId 저장
                db.collection("users").document(UserSession.uid)
                    .update("familyId", familyId)
                    .addOnSuccessListener {
                        UserSession.familyId = familyId

                        // 사용한 초대장 삭제
                        db.collection("invitations").document(invitationId).delete()

                        Toast.makeText(this, "가족 연동 완료!", Toast.LENGTH_SHORT).show()
                        tvFamilyId.text = "가족 ID: $familyId"
                        tvInvitation.text = "연동 완료"
                        btnAccept.visibility = View.GONE
                    }
            }
    }
}