package com.cpipdm.sap.service

import javax.inject.Singleton

@Singleton
class Utility {

    String toHex(String text) {
        StringBuffer stringBuffer = new StringBuffer()
        char[] text_ = text.toCharArray()
        for (int i = 0; i < text_.length; i++) {
            stringBuffer.append(Integer.toHexString(text_[i] as Integer))
        }
        return stringBuffer.toString()
    }
}
