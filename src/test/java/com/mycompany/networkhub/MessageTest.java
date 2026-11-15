package com.mycompany.networkhub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Message class, using the exact test data supplied in
 * the brief for Part 2 (Test Case 1 and 2) and Part 3 (Test Data Messages 1-5).
 *
 * @author Student
 */
public class MessageTest {

    /**
     * A fresh temporary folder for every test, so tests that store messages
     * write to a throw-away JSON file instead of your real storedMessages.json.
     */
    @TempDir
    Path tempDir;

    // The message texts from the brief's Part 3 test data.
    private static final String MESSAGE_1 = "Did you get the cake?";
    private static final String MESSAGE_2 = "Where are you? You are late! I have asked you to be on time.";
    private static final String MESSAGE_3 = "Yohoooo, I am at your gate.";
    private static final String MESSAGE_4 = "It is dinner time!";
    private static final String MESSAGE_5 = "Ok, I am leaving without you.";

    /**
     * Runs before every test. The arrays and counters are shared (static)
     * across all Message objects, so they are emptied each time to stop one
     * test's data leaking into the next one.
     */
    @BeforeEach
    public void resetSharedState() {
        Message.resetMessageHistoryForTesting();
        Message.setStorageFile(tempDir.resolve("storedMessages.json").toString());
        Message.setCurrentSender("Test User");
    }

    /**
     * Loads the brief's Test Data Messages 1 to 5 into the arrays by running
     * each one through sentMessage() with its flag (Sent = 1, Disregard = 2,
     * Stored = 3). Returns the five messages in order (index 0 = message 1).
     *
     * Message 4 uses the developer-defined ID 0838884567 from the brief.
     * ASSUMPTION: the brief's table labels that value "Developer", so it is
     * used as both the message ID and the recipient of message 4.
     */
    private Message[] loadTestData() {
        Message message1 = new Message("+27834557896", MESSAGE_1);
        message1.sentMessage(1); // Sent

        Message message2 = new Message("+27838884567", MESSAGE_2);
        message2.sentMessage(3); // Stored

        Message message3 = new Message("+27834484567", MESSAGE_3);
        message3.sentMessage(2); // Disregard

        Message message4 = new Message("0838884567", MESSAGE_4, "0838884567");
        message4.sentMessage(1); // Sent

        Message message5 = new Message("+27838884567", MESSAGE_5);
        message5.sentMessage(3); // Stored

        return new Message[]{message1, message2, message3, message4, message5};
    }

    // =====================================================
    //  PART 2 TESTS
    // =====================================================

    // ---------- Message length tests ----------

    @Test
    public void testMessageUnderLimitReturnsReadyToSend() {
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?");
        assertEquals("Message ready to send.", message.checkMessageLength());
    }

    @Test
    public void testMessageOverLimitReturnsExceedsMessage() {
        // 260 characters is 10 characters over the 250 limit.
        String longText = "A".repeat(260);
        Message message = new Message("+27718693002", longText);
        assertEquals("Message exceeds 250 characters by 10; please reduce the size.",
                message.checkMessageLength());
    }

    // ---------- Recipient cell number tests ----------

    @Test
    public void testRecipientCorrectlyFormatted() {
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?");
        assertEquals("Cell phone number successfully captured.",
                message.checkRecipientCell());
    }

    @Test
    public void testRecipientIncorrectlyFormatted() {
        Message message = new Message("08575975889",
                "Hi Keegan, did you receive the payment?");
        assertEquals("Cell phone number is incorrectly formatted or does not contain "
                + "an international code. Please correct the number and try again.",
                message.checkRecipientCell());
    }

    // ---------- Message hash tests ----------

    @Test
    public void testMessageHashMatchesTestCase1() {
        // A fixed ID starting "00" makes the first part of the hash predictable.
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?", "0012345678");

        assertEquals("00:0:HITONIGHT", message.createMessageHash());
    }

    @Test
    public void testMessageHashIncrementsWithEachSentMessage() {
        Message first = new Message("+27718693002", "Hi Mike, dinner tonight?", "0011111111");
        first.sentMessage(1); // sending increments the message number

        Message second = new Message("+27718693002", "Hi Keegan, payment received", "0022222222");
        String secondHash = second.createMessageHash();

        // The second message sent should carry message number 1, not 0.
        assertTrue(secondHash.contains(":1:"));
    }

    @Test
    public void testMessageHashesInALoop() {
        String[] texts = {
            "Hi Mike, can you join us for dinner tonight?",
            "Hi Keegan, did you receive the payment?",
            "Where are you? You are late! I have asked you to be on time."
        };
        String[] expectedWords = {"HITONIGHT", "HIPAYMENT", "WHERETIME"};

        for (int i = 0; i < texts.length; i++) {
            // The ID starts with "00" every time so the hash prefix is predictable.
            Message message = new Message("+27718693002", texts[i], "00123456" + i + "9");
            message.sentMessage(1); // sending increments the message number
            // i is the message number this message was created with.
            assertEquals("00:" + i + ":" + expectedWords[i], message.getMessageHash());
        }
    }

    // ---------- Message ID tests ----------

    @Test
    public void testMessageIDIsCreatedAndValid() {
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?");
        assertTrue(message.checkMessageID());
        assertEquals("Message ID generated: " + message.getMessageID(),
                message.returnMessageIDMessage());
    }

    // ---------- Sending, disregarding, storing ----------

    @Test
    public void testSendMessageReturnsSuccessMessage() {
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?");
        assertEquals("Message successfully sent.", message.sentMessage(1));
    }

    @Test
    public void testDisregardMessageReturnsDeletePrompt() {
        Message message = new Message("08575975889",
                "Hi Keegan, did you receive the payment?");
        // Option 2 = Disregard, as listed in the brief.
        assertEquals("Press 0 to delete the message.", message.sentMessage(2));
    }

    @Test
    public void testStoreMessageReturnsStoredMessage() {
        Message message = new Message("+27718693002",
                "Hi Mike, can you join us for dinner tonight?");
        // Option 3 = Store. This also saves the message to the (temporary) JSON file.
        assertEquals("Message successfully stored.", message.sentMessage(3));
    }

    // ---------- Total messages sent ----------

    @Test
    public void testReturnTotalMessagesCountsOnlySentMessages() {
        Message sent = new Message("+27718693002", "Hi Mike, dinner tonight?");
        sent.sentMessage(1); // counts

        Message disregarded = new Message("+27718693002", "Never mind");
        disregarded.sentMessage(2); // disregarded, does not count

        assertEquals(1, Message.returnTotalMessagess());
    }

    // =====================================================
    //  PART 3 TESTS (Test Data Messages 1 to 5)
    // =====================================================

    @Test
    public void testSentMessagesArrayCorrectlyPopulated() {
        loadTestData();

        // Messages 1 and 4 are flagged "Sent".
        assertEquals(Arrays.asList("Did you get the cake?", "It is dinner time!"),
                Message.getSentMessages());
    }

    @Test
    public void testDisregardedMessagesArrayCorrectlyPopulated() {
        loadTestData();

        // Only message 3 is flagged "Disregard".
        assertEquals(Arrays.asList(MESSAGE_3), Message.getDisregardedMessages());
    }

    @Test
    public void testStoredMessagesArrayCorrectlyPopulated() {
        loadTestData();

        // Messages 2 and 5 are flagged "Stored".
        assertEquals(Arrays.asList(MESSAGE_2, MESSAGE_5), Message.getStoredMessages());
    }

    @Test
    public void testHashAndIDArraysContainEveryMessage() {
        Message[] messages = loadTestData();

        assertEquals(5, Message.getMessageHashes().size());
        assertEquals(5, Message.getMessageIDs().size());
        assertTrue(Message.getMessageIDs().contains("0838884567"));
        assertTrue(Message.getMessageHashes().contains(messages[1].getMessageHash()));
    }

    @Test
    public void testTotalMessagesSentFromTestData() {
        loadTestData();

        // Messages 1 and 4 were sent: "Num Messages 2" in the brief.
        assertEquals(2, Message.returnTotalMessagess());
    }

    @Test
    public void testDisplayTheLongestMessage() {
        loadTestData();

        assertEquals("Where are you? You are late! I have asked you to be on time.",
                Message.getLongestMessage());
        // Message 2 is also the longest of the two stored messages.
        assertEquals(MESSAGE_2, Message.getLongestStoredMessage());
    }

    @Test
    public void testSearchForMessageID() {
        loadTestData();

        Message found = Message.findMessageByID("0838884567");

        assertNotNull(found);
        assertEquals("It is dinner time!", found.getMessageText());
    }

    @Test
    public void testSearchForMessageIDThatDoesNotExist() {
        loadTestData();

        assertNull(Message.findMessageByID("9999999999"));
    }

    @Test
    public void testSearchAllMessagesForAParticularRecipient() {
        loadTestData();

        // Messages 2 and 5 are both addressed to +27838884567.
        List<String> found = Message.searchMessagesByRecipient("+27838884567");

        assertEquals(Arrays.asList(
                "Where are you? You are late! I have asked you to be on time.",
                "Ok, I am leaving without you."), found);
    }

    @Test
    public void testDeleteAMessageUsingTheMessageHash() {
        Message[] messages = loadTestData();
        String hashOfMessage2 = messages[1].getMessageHash();

        // The brief's expected text (note: no full stop before the closing quote).
        assertEquals("Message: \"Where are you? You are late! I have asked you to be on time\" "
                + "successfully deleted.", Message.deleteMessageByHash(hashOfMessage2));

        // Message 2 is gone from the array; message 5 is still there.
        assertEquals(Arrays.asList(MESSAGE_5), Message.getStoredMessages());
    }

    @Test
    public void testDeleteWithAHashThatDoesNotExist() {
        loadTestData();

        assertEquals("No stored message was found with the hash: 99:9:NOPE",
                Message.deleteMessageByHash("99:9:NOPE"));
        assertEquals(2, Message.getStoredMessages().size());
    }

    @Test
    public void testDisplayReportShowsSentMessageDetails() {
        Message[] messages = loadTestData();

        String report = Message.printMessages();

        // The report lists the hash, recipient and message of every SENT message.
        assertTrue(report.contains("Message Hash: " + messages[0].getMessageHash()));
        assertTrue(report.contains("Recipient: +27834557896"));
        assertTrue(report.contains("Message: Did you get the cake?"));
        assertTrue(report.contains("Message: It is dinner time!"));
        // ...and nothing from the stored or disregarded messages.
        assertFalse(report.contains("Yohoooo"));
        assertFalse(report.contains("Where are you?"));
    }

    @Test
    public void testStoredMessagesReportShowsFullDetails() {
        Message[] messages = loadTestData();

        String report = Message.displayStoredMessagesReport();

        assertTrue(report.contains("Message ID: " + messages[1].getMessageID()));
        assertTrue(report.contains("Message Hash: " + messages[1].getMessageHash()));
        assertTrue(report.contains("Sender: Test User"));
        assertTrue(report.contains("Recipient: +27838884567"));
        assertTrue(report.contains("Message: " + MESSAGE_5));
    }

    @Test
    public void testSenderAndRecipientOfStoredMessages() {
        loadTestData();

        assertEquals("Sender: Test User | Recipient: +27838884567\n"
                + "Sender: Test User | Recipient: +27838884567",
                Message.displaySenderAndRecipientOfStoredMessages());
    }

    @Test
    public void testStoredMessagesAreReadBackFromTheJsonFile() {
        Message[] messages = loadTestData();
        String hashOfMessage2 = messages[1].getMessageHash();

        // Wipe the arrays from memory, as if the program had been restarted...
        Message.resetMessageHistoryForTesting();
        assertTrue(Message.getStoredMessages().isEmpty());

        // ...then read the JSON file back into the Stored Messages array.
        int loaded = Message.loadStoredMessagesFromFile();

        assertEquals(2, loaded);
        assertEquals(Arrays.asList(MESSAGE_2, MESSAGE_5), Message.getStoredMessages());
        assertTrue(Message.getMessageHashes().contains(hashOfMessage2));
    }

    @Test
    public void testDeletedMessageIsAlsoRemovedFromTheJsonFile() {
        Message[] messages = loadTestData();
        Message.deleteMessageByHash(messages[1].getMessageHash());

        // Simulate a restart: only message 5 should come back from the file.
        Message.resetMessageHistoryForTesting();
        assertEquals(1, Message.loadStoredMessagesFromFile());
        assertEquals(Arrays.asList(MESSAGE_5), Message.getStoredMessages());
    }
}