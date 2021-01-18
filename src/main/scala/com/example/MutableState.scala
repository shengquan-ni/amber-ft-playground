package com.example

import scala.collection.mutable

class MutableState(val name:String){
  var arrayState: mutable.ArrayBuffer[Any] = mutable.ArrayBuffer.empty

}
