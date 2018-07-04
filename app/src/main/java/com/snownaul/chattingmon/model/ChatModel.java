package com.snownaul.chattingmon.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by alfo6-11 on 2018-07-03.
 */

public class ChatModel {

    public Map<String, Boolean> users=new HashMap<>();//채팅방의 유저들
    public Map<String, Comment>comments=new HashMap<>();//채팅방의 내용


    public static class Comment{
        public String uid;
        public String message;
    }

}
