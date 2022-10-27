package com.tiket.email;

import com.tiket.io.PropertiesReader;
import lombok.SneakyThrows;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

public class EmailProcessor {

    private static final Properties PROPERTIES = PropertiesReader.read("src/main/resources/email.properties");
    private static final String TO = System.getProperty("emailIDs");
    private static final String REPORT_LOCATION = "report/report.html";

    @SneakyThrows
    public static void processEmail(String htmlReportContent) {

        if(Objects.isNull(TO)) {
            return;
        }

        Properties p = System.getProperties();
        p.put("mail.smtp.host", PROPERTIES.getProperty("mail.smtp.host"));
        p.put("mail.smtp.port", PROPERTIES.getProperty("mail.smtp.port"));
        p.put("mail.smtp.ssl.enable", PROPERTIES.getProperty("mail.smtp.ssl.enable"));
        p.put("mail.smtp.auth", PROPERTIES.getProperty("mail.smtp.auth"));

        Session session = Session.getInstance(p, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(PROPERTIES.getProperty("email"), PROPERTIES.getProperty("password"));
            }
        });

        session.setDebug(false);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(PROPERTIES.getProperty("email"));
        message.setRecipients(Message.RecipientType.TO, TO);

        message.setSubject(createSubject());

        String htmlBody;
        if(isJenkinsSystem()) {
            String buildUrl = System.getenv("BUILD_URL");
            String reportUrl = buildUrl + "artifact/report/report.html";
            htmlBody = "<html><head><style>table, th, td {border: 1px solid;}</style></head><body><h2><a href='" + buildUrl + "'>" + buildUrl + "</a></h2><h2><a href='" + reportUrl + "'>Report</a></h2><h1>Results</h1>" + htmlReportContent + "</body></html>";
        } else {
            htmlBody = "<html><head><style>table, th, td {border: 1px solid;}</style></head><body><h1>Results</h1>" + htmlReportContent + "</body></html>";
        }

        BodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(htmlBody, "text/html");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(bodyPart);
        if(!isJenkinsSystem()) {
            MimeBodyPart extentAttachment = new MimeBodyPart();
            extentAttachment.attachFile(REPORT_LOCATION);
            multipart.addBodyPart(extentAttachment);
        }
        message.setContent(multipart);

        Transport.send(message);
    }

    private static String createSubject() {
        StringBuilder subject = new StringBuilder();
        subject.append("Automation Report").append("-");
        subject.append("Time(").append(getCurrentDateTime()).append(")-");
        if(isJenkinsSystem()) {
            subject.append("JobId(").append(System.getenv("BUILD_ID")).append(")");
        }
        return subject.toString();
    }

    private static boolean isJenkinsSystem() {
        String pwd = System.getProperty("user.dir");
        return pwd != null && pwd.contains("jenkins");
    }

    private static String getCurrentDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
