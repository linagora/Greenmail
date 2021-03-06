/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.icegreen.greenmail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;

import org.junit.After;
import org.junit.Test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class Pop3ServerTest {

    GreenMail greenMail;

    @After
    public void tearDown() {
        try {
            greenMail.stop();
        } catch (NullPointerException ignored) {
            //empty
        }
    }

    @Test
    public void testRetreive() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_POP3);
        assertNotNull(greenMail.getPop3());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random() + "\r\n" + GreenMailUtil.random() + "\r\n" + GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getPop3());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());
    }

    @Test
    public void testImapExpunge() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        assertNotNull(greenMail.getImap());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random() + "\r\n" + GreenMailUtil.random() + "\r\n" + GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImap());
        Message[] messages = retriever.getMessages(to);
        assertEquals(4, messages.length);
        for (Message message : messages) {
        	message.setFlag(javax.mail.Flags.Flag.DELETED, true);
        }
        retriever.logoutAndExpunge();
        retriever = new Retriever(greenMail.getImap());
        messages = retriever.getMessages(to);
        assertEquals(0, messages.length);
    }


    @Test
    public void testPop3sReceive() throws Throwable {
        greenMail = new GreenMail(new ServerSetup[]{ServerSetupTest.SMTPS, ServerSetupTest.POP3S});
        assertNull(greenMail.getPop3());
        assertNotNull(greenMail.getPop3s());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailSecureTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getPop3s());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());
    }

    @Test
    public void testRetreiveWithNonDefaultPassword() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_POP3);
        assertNotNull(greenMail.getPop3());
        final String to = "test@localhost.com";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getPop3());
        boolean login_failed = false;
        try {
            retriever.getMessages(to, "wrongpassword");
        } catch (Throwable e) {
            login_failed = true;
        }
        assertTrue(login_failed);

        Message[] messages = retriever.getMessages(to, password);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());
    }

    @Test
    public void testRetriveMultipart() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_POP3);
        assertNotNull(greenMail.getPop3());
        greenMail.start();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost.com", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getPop3());
        Message[] messages = retriever.getMessages(to);

        Object o = messages[0].getContent();
        assertTrue(o instanceof MimeMultipart);
        MimeMultipart mp = (MimeMultipart) o;
        assertEquals(2, mp.getCount());
        BodyPart bp;
        bp = mp.getBodyPart(0);
        assertEquals(body, GreenMailUtil.getBody(bp).trim());

        bp = mp.getBodyPart(1);
        assertEquals("AAEC", GreenMailUtil.getBody(bp).trim());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GreenMailUtil.copyStream(bp.getInputStream(), bout);
        byte[] gif = bout.toByteArray();
        for (int i = 0; i < gif.length; i++) {
            assertEquals(i, gif[i]);
        }
    }
}
