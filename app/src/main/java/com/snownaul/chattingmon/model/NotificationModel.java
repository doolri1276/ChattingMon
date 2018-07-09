package com.snownaul.chattingmon.model;

/**
 * Created by alfo6-11 on 2018-07-04.
 */

public class NotificationModel {

    public String to;
    public Notification notification = new Notification();
    public Data data = new Data();

    public static class Notification{
        public String title;
        public String text;
    }

    public static class Data{
        public String title;
        public String text;
    }
}
