package com.vlad.schedule

import java.util.*

class Settings(var notification:Boolean = true,
               var systemSetings:Boolean = true,
               var vibroNotification:Boolean = true,
               var backgroungImage:String = "",
               var locale : Locale = Locale.getDefault())