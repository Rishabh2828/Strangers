package com.shurish.strangers

data class User(
     var uId: String? = null,
     var name: String? = null,
     var profile: String? = null,
     var city: String? = null,
     val coins: Long = 0
)
