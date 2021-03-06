/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.icegreen.greenmail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;

import org.junit.After;
import org.junit.Test;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class ImapServerTest {

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
    public void testRetreiveSimple() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        assertNotNull(greenMail.getImap());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random() + "\r\n" + GreenMailUtil.random() + "\r\n" + GreenMailUtil.random();
        final String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImap());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    @Test
    public void testImapsReceive() throws Throwable {
        greenMail = new GreenMail(ServerSetupTest.SMTPS_IMAPS);
        assertNull(greenMail.getImap());
        assertNotNull(greenMail.getImaps());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailSecureTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImaps());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    @Test
    public void testRetreiveSimpleWithNonDefaultPassword() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        assertNotNull(greenMail.getImap());
        final String to = "test@localhost.com";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImap());
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
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    @Test
    public void testRetriveMultipart() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        assertNotNull(greenMail.getImap());
        greenMail.start();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost.com", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getImap());
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
        retriever.logout();
    }

    @Test
    public void listMailboxesShouldWorkWhenPatternIsInTheMiddleOfMailboxName() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.start();
        
		GreenMailUser admin = greenMail.setAdminUser("admin", "admin");
		greenMail.getManagers().getImapHostManager().deleteMailbox(admin, "INBOX");
		greenMail.getManagers().getImapHostManager().createMailbox(admin, "user/admin@mydomain");
		
		GreenMailUser usera = greenMail.setAdminUser("usera", "usera");
		greenMail.getManagers().getImapHostManager().deleteMailbox(usera, "INBOX");
		greenMail.getManagers().getImapHostManager().createMailbox(usera, "user/usera@mydomain");
		greenMail.getManagers().getImapHostManager().createMailbox(usera, "user/usera/folder@mydomain");
		
		GreenMailUser userb = greenMail.setAdminUser("usera-test", "usera");
		greenMail.getManagers().getImapHostManager().deleteMailbox(userb, "INBOX");
		greenMail.getManagers().getImapHostManager().createMailbox(userb, "user/usera-test@mydomain");
		
		Collection<MailFolder> listMailboxes = greenMail.getManagers().getImapHostManager().listMailboxes(admin, "*user/usera*");
		assertEquals(listMailboxes.size(), 3);
    }

    @Test
    public void listMailboxesShouldWorkWhenPatternEndsWithPercent() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.start();
        
		GreenMailUser admin = greenMail.setAdminUser("admin", "admin");
		greenMail.getManagers().getImapHostManager().deleteMailbox(admin, "INBOX");
		greenMail.getManagers().getImapHostManager().createMailbox(admin, "user/admin@mydomain");
		
		GreenMailUser usera = greenMail.setAdminUser("usera", "usera");
		greenMail.getManagers().getImapHostManager().deleteMailbox(usera, "INBOX");
		greenMail.getManagers().getImapHostManager().createMailbox(usera, "user/usera@mydomain");
		greenMail.getManagers().getImapHostManager().createMailbox(usera, "user/usera/folder@mydomain");
		
		GreenMailUser userb = greenMail.setAdminUser("usera-test", "usera");
		greenMail.getManagers().getImapHostManager().deleteMailbox(userb, "INBOX");
		greenMail.getManagers().getImapHostManager().createMailbox(userb, "user/usera-test@mydomain");
		
		Collection<MailFolder> listMailboxes = greenMail.getManagers().getImapHostManager().listMailboxes(admin, "*user/%");
		assertEquals(listMailboxes.size(), 3);
    }

    @Test
    public void listMailboxesShouldWorkWhenDomainsContainsDot() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.start();
        
		GreenMailUser admin = greenMail.setAdminUser("admin", "admin");
		greenMail.getManagers().getImapHostManager().deleteMailbox(admin, "INBOX");
		greenMail.getManagers().getImapHostManager().createMailbox(admin, "user/admin@mydomain.org");
		
		GreenMailUser usera = greenMail.setAdminUser("usera", "usera");
		greenMail.getManagers().getImapHostManager().deleteMailbox(usera, "INBOX");
		greenMail.getManagers().getImapHostManager().createMailbox(usera, "user/usera@mydomain.org");
		greenMail.getManagers().getImapHostManager().createMailbox(usera, "user/usera/folder@mydomain.org");
		
		Collection<MailFolder> listMailboxes = greenMail.getManagers().getImapHostManager().listMailboxes(admin, "*user/usera/*");
		assertEquals(listMailboxes.size(), 1);
    }
}
