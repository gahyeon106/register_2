package com.example.register

object UserSession{
    var uid: String=""
    var loginId: String=""
    var role:String=""
    var familyId: String?= null

    fun clear(){
        uid = ""
        loginId=""
        role=""
        familyId=null

    }

}

//Salt??
