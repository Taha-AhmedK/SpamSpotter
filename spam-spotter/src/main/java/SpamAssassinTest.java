import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class SpamAssassinTest {

    /**
     * Windows Setup
     * Spamassassin Installation: https://www.jam-software.com/spamassassin
     * OpenSource MTA: https://www.hmailserver.com/
     */
    private final static String SPAM_ASSASSIN_INSTALLATION_PATH = "C:\\Program Files\\JAM Software\\SpamAssassin for Windows\\";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String html = "<b>Hello World!<b>";
        String text = "Hello World!";
        String subjectLine = "Test Email 1";
        String senderAddress = "taha@localhost";
        String fromName = "Taha";
        String replyTo = "taha@localhost";
        String recipientAddress = "tahaahmedkhn@gmail.com";

        String report = spamassassin(html, text, subjectLine, senderAddress, fromName, replyTo, recipientAddress);

        System.out.println(report);

        Double score = getScoreFromReport(report);
        System.out.println("Spamassassin Score [" + score + "].");
       
        if (score != null && score >= 5) {
            System.out.println("Email is SPAM");
        } else {
            System.out.println("Email is HAM");
        }
    }

    private static String spamassassin(String html, String text, String subjectLine,
            String senderAddress, String fromName, String replyTo, String recipientAddress) {
        try {
            Properties props = System.getProperties();
            Session session = Session.getInstance(props, null);

            // body content
            String htmlbody = html;
            String plainbody = text;

            // define message
            MimeMessage message = new MimeMessage(session);

            // headers
            message.setFrom(new InternetAddress(senderAddress, fromName, "utf-8"));
            InternetAddress[] address = {new InternetAddress(replyTo, fromName, "utf-8")};
            message.setReplyTo(address);
            message.setRecipients(RecipientType.TO, recipientAddress);
            message.setSubject(subjectLine, "utf-8");
            message.setSentDate(new Date());

            // create the mail root multipart
            MimeMultipart mpRoot = new MimeMultipart("mixed");

            // create the content multipart (for text and HTML)
            MimeMultipart mpContent = new MimeMultipart("alternative");

            // create a body part to house the multipart/alternative Part
            MimeBodyPart contentPartRoot = new MimeBodyPart();
            contentPartRoot.setContent(mpContent);

            // add the root body part to the root multipart
            mpRoot.addBodyPart(contentPartRoot);

            // add text
            MimeBodyPart mbp1 = new MimeBodyPart();
            mbp1.setText(plainbody);
            mpContent.addBodyPart(mbp1);

            // add html
            MimeBodyPart mbp2 = new MimeBodyPart();
            mbp2.setContent(htmlbody, "text/html; charset=utf-8");
            mpContent.addBodyPart(mbp2);

            // put parts in message
            message.setContent(mpRoot);

            Process process = Runtime.getRuntime().exec(SPAM_ASSASSIN_INSTALLATION_PATH + "spamassassin -t");
            String domain = senderAddress.split("@")[1];
            try (OutputStream out = process.getOutputStream()) {
                String s = "Received: from localhost ([127.0.0.1] helo=" + domain + ")\n"
                        + "        by " + domain + " with esmtp \n"
                        + "        (envelope-from <" + senderAddress + ">)\n"
                        + "        for " + recipientAddress + "; " + new Date() + "\n";

                out.write(s.getBytes());
                message.saveChanges();
                message.writeTo(out);
            }
            String result;
            try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                result = "";
                Boolean start = false;
                while ((line = input.readLine()) != null) {
                    if (line.contains("Content analysis details:")) {
                        start = true;
                        result = "";
                    }
                    if (start) {
                        result += line + "\n";
                    }
                }
            }

            return result;
        } catch (MessagingException | IOException e) {
            e.printStackTrace(System.out);
            return "Error: " + e.getLocalizedMessage();
        }
    }

    private static Double getScoreFromReport(String report) {
        Double score = null;
        try {
            String[] result = report.split("\\R", 2);
            String scores = result[0].substring(result[0].indexOf("(") + 1, result[0].indexOf(")"));
            String scoreString = scores.split(",")[0].replace(" points", "");
            score = Double.parseDouble(scoreString);
        } catch (Exception exp) {
            exp.printStackTrace();
        }

        return score;
    }
}